import https from "node:https";
import { performance } from "node:perf_hooks";
import { parseArgs } from "node:util";
import { mkdir, open, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { pathToFileURL } from "node:url";

export async function prepareReportOutput(output) {
  const path = resolve(output);
  await mkdir(dirname(path), { recursive: true, mode: 0o700 });
  const file = await open(path, "a", 0o600);
  await file.close();
}

export function percentile(values, fraction) {
  if (
    !values.length ||
    !(fraction > 0 && fraction <= 1) ||
    values.some((value) => !Number.isFinite(value) || value < 0)
  )
    throw new Error("Invalid percentile samples");
  const sorted = [...values].sort((a, b) => a - b);
  return sorted[Math.ceil(fraction * sorted.length) - 1];
}

export function evaluateRun(input) {
  const reasons = [];
  const {
    offered,
    issued,
    completed,
    elapsedSeconds,
    non302,
    timeouts,
    serverDurationsMs,
    dropped = 0,
    late = 0,
    overflowed = false,
  } = input;
  if (
    !Number.isInteger(offered) ||
    offered <= 0 ||
    issued !== offered ||
    completed !== issued
  )
    reasons.push("Incomplete offered/issued/completed requests");
  if (dropped || late) reasons.push("Dropped or excessively late arrivals");
  if (!(elapsedSeconds > 0) || completed / elapsedSeconds < 99.5)
    reasons.push("Achieved throughput below 99.5 requests/second");
  if (overflowed || serverDurationsMs.length !== issued - timeouts)
    reasons.push("Incomplete or overflowing server timing collection");
  const errors = non302 + timeouts;
  if (!Number.isInteger(errors) || errors < 0 || errors >= offered * 0.005)
    reasons.push("Error rate is not below 0.5 percent");
  let p95 = null,
    p99 = null;
  try {
    p95 = percentile(serverDurationsMs, 0.95);
    p99 = percentile(serverDurationsMs, 0.99);
    if (p95 >= 150 || p99 >= 300)
      reasons.push("Server latency exceeds a strict threshold");
  } catch {
    reasons.push("Invalid server timing samples");
  }
  return {
    passed: reasons.length === 0,
    reasons,
    p95,
    p99,
    errorRate: offered > 0 ? errors / offered : null,
  };
}

export function requestOptions(url, method, headers, localAddress) {
  if (url.protocol !== "https:") throw new Error("HTTPS is required");
  return {
    protocol: "https:",
    hostname: url.hostname,
    port: url.port || 443,
    path: url.pathname + url.search,
    method,
    headers,
    localAddress,
    family: 4,
    rejectUnauthorized: true,
  };
}

function transport(agent, localAddress) {
  return (url, method = "GET", headers = {}, data) =>
    new Promise((resolve, reject) => {
      const body = data === undefined ? undefined : JSON.stringify(data);
      const options = requestOptions(
        url,
        method,
        body === undefined
          ? headers
          : {
              ...headers,
              "Content-Type": "application/json",
              "Content-Length": Buffer.byteLength(body),
            },
        localAddress,
      );
      const request = https.request(
        { ...options, agent, signal: AbortSignal.timeout(5000) },
        (response) => {
          const chunks = [];
          let size = 0;
          response.on("data", (chunk) => {
            size += chunk.length;
            if (size > 16 * 1024 * 1024) {
              request.destroy(new Error("Response too large"));
              return;
            }
            chunks.push(chunk);
          });
          response.on("error", reject);
          response.on("end", () =>
            resolve({
              status: response.statusCode,
              headers: response.headers,
              body: Buffer.concat(chunks).toString("utf8"),
            }),
          );
        },
      );
      request.on("error", reject);
      request.end(body);
    });
}

export async function arrivals(
  send,
  { rate, duration, clients, codes, apiBase },
  {
    now = () => performance.now(),
    wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms)),
  } = {},
) {
  const offered = Math.round(rate * duration),
    started = now();
  const pending = new Set(),
    clientDurationsMs = [],
    statuses = {};
  let issued = 0,
    completed = 0,
    timeouts = 0,
    non302 = 0,
    dropped = 0,
    late = 0,
    inFlight = 0,
    maximumLatenessMs = 0;
  for (let slot = 0; slot < offered; slot++) {
    const deadline = started + (slot * 1000) / rate;
    let remaining;
    while ((remaining = deadline - now()) > 0)
      await wait(Math.max(1, Math.min(10, remaining)));
    const lateness = Math.max(0, now() - deadline);
    maximumLatenessMs = Math.max(maximumLatenessMs, lateness);
    if (lateness > 100 || inFlight >= 256) {
      dropped++;
      if (lateness > 100) late++;
      continue;
    }
    issued++;
    inFlight++;
    const requestStart = now();
    const request = send(
      new URL(`/${codes[slot % codes.length]}`, apiBase),
      "GET",
      {
        "X-Forwarded-For": `198.51.100.${1 + (slot % clients)}`,
      },
    )
      .then(
        (response) => {
          statuses[response.status] = (statuses[response.status] ?? 0) + 1;
          if (response.status !== 302) non302++;
        },
        () => {
          timeouts++;
        },
      )
      .finally(() => {
        completed++;
        inFlight--;
        clientDurationsMs.push(now() - requestStart);
        pending.delete(request);
      });
    pending.add(request);
  }
  const remaining = started + duration * 1000 - now();
  if (remaining > 0) await wait(remaining);
  await Promise.all(pending);
  return {
    offered,
    issued,
    completed,
    elapsedSeconds: (now() - started) / 1000,
    non302,
    timeouts,
    dropped,
    late,
    maximumLatenessMs,
    statuses,
    clientDurationsMs,
  };
}

async function main() {
  const { values } = parseArgs({
    options: {
      "api-base": { type: "string", default: "https://localhost:8443" },
      rate: { type: "string", default: "100" },
      duration: { type: "string", default: "600" },
      warmup: { type: "string", default: "30" },
      "create-samples": { type: "string", default: "200" },
      clients: { type: "string", default: "100" },
      "via-local-proxy": { type: "boolean", default: false },
      output: {
        type: "string",
        default: ".local-verification/link-performance.json",
      },
    },
  });
  const apiBase = new URL(values["api-base"]);
  if (
    apiBase.protocol !== "https:" ||
    apiBase.hostname !== "localhost" ||
    apiBase.pathname !== "/"
  )
    throw new Error(
      "This harness requires the disposable localhost HTTPS application",
    );
  const numbers = Object.fromEntries(
    ["rate", "duration", "warmup", "create-samples", "clients"].map((key) => {
      const value = Number(values[key]);
      if (!Number.isInteger(value) || value < 1)
        throw new Error("Invalid numeric protocol option");
      return [key, value];
    }),
  );
  if (
    numbers.clients > 254 ||
    numbers["create-samples"] > 297 ||
    numbers.rate > 1000 ||
    numbers.duration > 3600
  )
    throw new Error("Protocol option exceeds bounded fixture capacity");
  const token = process.env.TINYROUTE_VERIFICATION_TOKEN;
  if (!token || !/^[A-Za-z0-9_-]{32,128}$/.test(token))
    throw new Error("Private fixture token is required");
  await prepareReportOutput(values.output);
  const agent = new https.Agent({ keepAlive: true, maxSockets: 256 });
  const send = transport(
    agent,
    values["via-local-proxy"] ? "127.0.0.1" : "127.0.0.2",
  );
  const fixtureHeaders = { "X-Verification-Token": token };
  const fixture = async (path, method = "GET") => {
    const response = await send(
      new URL(`/__verification/${path}`, apiBase),
      method,
      fixtureHeaders,
    );
    if (response.status !== (method === "POST" ? 204 : 200))
      throw new Error("Fixture request failed");
    return method === "POST" ? null : JSON.parse(response.body);
  };
  try {
    const bootstrap = await fixture("bootstrap");
    if (bootstrap.accounts.length < 3 || bootstrap.codes.length < 100)
      throw new Error("Insufficient disposable fixtures");
    const sessions = [];
    for (let index = 0; index < 3; index++) {
      const jar = new Map();
      const authenticated = async (path, method = "GET", data, csrf) => {
        const headers = {
          "X-Forwarded-For": `203.0.113.${200 + index}`,
          Cookie: [...jar]
            .map(([name, value]) => `${name}=${value}`)
            .join("; "),
        };
        if (csrf) headers["X-CSRF-TOKEN"] = csrf;
        const response = await send(
          new URL(path, apiBase),
          method,
          headers,
          data,
        );
        for (const cookie of response.headers["set-cookie"] ?? []) {
          const pair = cookie.split(";", 1)[0],
            split = pair.indexOf("=");
          if (split > 0) {
            const name = pair.slice(0, split),
              value = pair.slice(split + 1);
            if (value) jar.set(name, value);
            else jar.delete(name);
          }
        }
        return response;
      };
      const before = await authenticated("/api/auth/csrf");
      if (before.status !== 200) throw new Error("CSRF setup failed");
      const login = await authenticated(
        "/api/auth/login",
        "POST",
        bootstrap.accounts[index],
        JSON.parse(before.body).csrfToken,
      );
      if (login.status !== 200) throw new Error("Disposable login failed");
      const after = await authenticated("/api/auth/csrf");
      if (after.status !== 200) throw new Error("Post-login CSRF setup failed");
      sessions.push({ authenticated, csrf: JSON.parse(after.body).csrfToken });
    }
    await fixture("timings/reset", "POST");
    const createStatuses = {};
    for (let index = 0; index < numbers["create-samples"]; index++) {
      const session = sessions[index % sessions.length];
      const csrfBootstrap = await session.authenticated("/api/auth/csrf");
      if (csrfBootstrap.status !== 200)
        throw new Error("Creation CSRF setup failed");
      session.csrf = JSON.parse(csrfBootstrap.body).csrfToken;
      const response = await session.authenticated(
        "/api/links",
        "POST",
        { destinationUrl: "https://example.com/docs?q=java#setup" },
        session.csrf,
      );
      createStatuses[response.status] =
        (createStatuses[response.status] ?? 0) + 1;
      if (response.status !== 201)
        throw new Error(
          "Counter-generated creation sample failed; no mutation retry",
        );
    }
    const creationReadout = await fixture("timings");
    const creationSamples = creationReadout.samples.filter(
      (sample) => sample.route === "create",
    );
    const creationDurationsMs = creationSamples.map(
      (sample) => sample.durationNanos / 1e6,
    );
    const creationPassed =
      !creationReadout.overflowed &&
      creationSamples.length === numbers["create-samples"] &&
      creationSamples.every((sample) => sample.status === 201) &&
      percentile(creationDurationsMs, 0.95) < 500;
    console.log(
      `Creation phase complete: ${creationSamples.length} server samples, p95 ${percentile(creationDurationsMs, 0.95).toFixed(2)} ms`,
    );
    const options = {
      rate: numbers.rate,
      clients: numbers.clients,
      codes: bootstrap.codes,
      apiBase,
    };
    const warmup = await arrivals(send, {
      ...options,
      duration: numbers.warmup,
    });
    if (warmup.non302 || warmup.timeouts || warmup.dropped)
      throw new Error("Warm-up traffic failed");
    await fixture("timings/reset", "POST");
    console.log(
      `Measured redirect phase: ${numbers.rate} requests/second for ${numbers.duration} seconds`,
    );
    const run = await arrivals(send, {
      ...options,
      duration: numbers.duration,
    });
    const redirectReadout = await fixture("timings");
    const redirectSamples = redirectReadout.samples.filter(
      (sample) => sample.route === "redirect",
    );
    const serverDurationsMs = redirectSamples.map(
      (sample) => sample.durationNanos / 1e6,
    );
    const evaluation = evaluateRun({
      ...run,
      serverDurationsMs,
      overflowed: redirectReadout.overflowed,
    });
    const acceptanceProtocol =
      numbers.rate === 100 &&
      numbers.duration >= 600 &&
      numbers.warmup >= 30 &&
      numbers["create-samples"] >= 200 &&
      numbers.clients >= 100;
    const report = {
      recordedAt: new Date().toISOString(),
      environment:
        "local disposable PostgreSQL/Redis; production topology unverified",
      nodeVersion: process.version,
      transportMode: values["via-local-proxy"]
        ? "local TLS pass-through proxy"
        : "direct trusted loopback",
      protocol: numbers,
      acceptanceProtocol,
      creation: {
        samples: creationSamples.length,
        statuses: createStatuses,
        p95: percentile(creationDurationsMs, 0.95),
        serverDurationsMs: creationDurationsMs,
        passed: creationPassed,
      },
      redirect: {
        ...run,
        achievedRequestsPerSecond: run.completed / run.elapsedSeconds,
        serverDurationsMs,
        serverStatuses: Object.fromEntries(
          [...new Set(redirectSamples.map((sample) => sample.status))].map(
            (status) => [
              status,
              redirectSamples.filter((sample) => sample.status === status)
                .length,
            ],
          ),
        ),
        overflowed: redirectReadout.overflowed,
        ...evaluation,
        clientP95: percentile(run.clientDurationsMs, 0.95),
        clientP99: percentile(run.clientDurationsMs, 0.99),
      },
      passed: acceptanceProtocol && creationPassed && evaluation.passed,
    };
    await writeFile(values.output, JSON.stringify(report, null, 2) + "\n", {
      mode: 0o600,
    });
    console.log(
      JSON.stringify({
        passed: report.passed,
        acceptanceProtocol,
        creationP95: report.creation.p95,
        redirectP95: evaluation.p95,
        redirectP99: evaluation.p99,
        errorRate: evaluation.errorRate,
        offered: run.offered,
        issued: run.issued,
        completed: run.completed,
        reasons: evaluation.reasons,
      }),
    );
    if (!report.passed) process.exitCode = 1;
  } finally {
    agent.destroy();
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href)
  main().catch(() => {
    console.error(
      "Verification failed; no credentials or response bodies are printed.",
    );
    process.exitCode = 1;
  });
