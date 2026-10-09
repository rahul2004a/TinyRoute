import {
  mkdtemp,
  stat,
  readFile,
  mkdir,
  rm,
  writeFile,
} from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import assert from "node:assert/strict";
import {
  percentile,
  evaluateRun,
  requestOptions,
  prepareReportOutput,
  arrivals,
} from "./link-performance.mjs";

const good = () => ({
  offered: 60000,
  issued: 60000,
  completed: 60000,
  elapsedSeconds: 600,
  non302: 0,
  timeouts: 0,
  dropped: 0,
  late: 0,
  overflowed: false,
  serverDurationsMs: Array(60000).fill(5),
});

test("nearest-rank percentiles do not average away a long tail", () => {
  assert.equal(percentile([5, 1, 4, 2, 3], 0.95), 5);
  assert.equal(percentile([5, 1, 4, 2, 3], 0.5), 3);
  assert.equal(
    percentile(
      Array.from({ length: 100 }, (_, i) => i + 1),
      0.99,
    ),
    99,
  );
  assert.throws(() => percentile([], 0.95));
  assert.throws(() => percentile([NaN], 0.95));
});
test("complete default-rate run with all server samples passes", () => {
  assert.equal(evaluateRun(good()).passed, true);
});
test("underissuance, dropped arrivals, missing samples and overflow fail", () => {
  for (const patch of [
    { issued: 59000, completed: 59000 },
    { dropped: 1 },
    { late: 1 },
    { completed: 59999 },
    { serverDurationsMs: Array(59999).fill(5) },
    { overflowed: true },
    { elapsedSeconds: 620 },
  ]) {
    assert.equal(
      evaluateRun({ ...good(), ...patch }).passed,
      false,
      JSON.stringify(patch),
    );
  }
});
test("non-302 and timeout errors use all completed and failed attempts", () => {
  assert.equal(evaluateRun({ ...good(), non302: 299 }).passed, true);
  assert.equal(evaluateRun({ ...good(), non302: 300 }).passed, false);
  assert.equal(
    evaluateRun({
      ...good(),
      timeouts: 300,
      serverDurationsMs: Array(59700).fill(5),
    }).passed,
    false,
  );
});
test("latency thresholds are strict and never exclude failed server samples", () => {
  assert.equal(
    evaluateRun({ ...good(), serverDurationsMs: Array(60000).fill(150) })
      .passed,
    false,
  );
  const tail = Array(59399).fill(5).concat(Array(601).fill(300));
  assert.equal(
    evaluateRun({ ...good(), serverDurationsMs: tail }).passed,
    false,
  );
});
test("driver options validate TLS, never follow redirects and bind the proxy source", () => {
  const options = requestOptions(
    new URL("https://localhost:8444/AbC"),
    "GET",
    {},
    "127.0.0.2",
  );
  assert.equal(options.rejectUnauthorized, true);
  assert.equal(options.localAddress, "127.0.0.2");
  assert.equal(options.family, 4);
  assert.equal(options.method, "GET");
  assert.throws(() =>
    requestOptions(new URL("http://localhost:8444/AbC"), "GET", {}),
  );
});

test("report preflight creates a private nested output before measurement", async () => {
  const directory = await mkdtemp(join(tmpdir(), "tinyroute-report-"));
  try {
    const output = join(directory, "missing-parent", "report.json");
    await prepareReportOutput(output);
    assert.equal((await stat(output)).isFile(), true);
    assert.equal((await stat(output)).mode & 0o777, 0o600);
    assert.equal(await readFile(output, "utf8"), "");
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});
test("report preflight rejects a directory masquerading as the report file", async () => {
  const directory = await mkdtemp(join(tmpdir(), "tinyroute-report-"));
  try {
    const output = join(directory, "report.json");
    await mkdir(output);
    await assert.rejects(prepareReportOutput(output));
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("report preflight preserves previous evidence until replacement is ready", async () => {
  const directory = await mkdtemp(join(tmpdir(), "tinyroute-report-"));
  try {
    const output = join(directory, "report.json");
    await writeFile(output, "previous evidence", { mode: 0o600 });
    await prepareReportOutput(output);
    assert.equal(await readFile(output, "utf8"), "previous evidence");
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});

test("open-loop arrivals cap active requests and drain outstanding responses", async () => {
  let clock = 0;
  const responses = [];
  let drainStarted;
  const waitingForDrain = new Promise((resolve) => {
    drainStarted = resolve;
  });
  const run = arrivals(
    () =>
      new Promise((resolve) => {
        responses.push(resolve);
      }),
    {
      rate: 1000,
      duration: 1,
      clients: 100,
      codes: ["fixture"],
      apiBase: new URL("https://localhost:8444"),
    },
    {
      now: () => clock,
      wait: async (ms) => {
        clock += ms;
        if (clock >= 1000) drainStarted();
      },
    },
  );
  await waitingForDrain;
  assert.equal(responses.length, 256);
  for (const resolve of responses) resolve({ status: 302 });
  const result = await run;
  assert.equal(result.offered, 1000);
  assert.equal(result.issued, 256);
  assert.equal(result.completed, 256);
  assert.equal(result.dropped, 744);
  assert.equal(result.late, 0);
  assert.equal(result.clientDurationsMs.length, 256);
  assert.deepEqual(result.statuses, { 302: 256 });
});

test("settled arrivals retain complete status and failure evidence", async () => {
  let clock = 0,
    count = 0;
  const result = await arrivals(
    async () => {
      count++;
      if (count % 3 === 0) throw new Error("transport failure");
      return { status: count % 3 === 1 ? 302 : 503 };
    },
    {
      rate: 100,
      duration: 1,
      clients: 100,
      codes: ["fixture"],
      apiBase: new URL("https://localhost:8444"),
    },
    {
      now: () => clock,
      wait: async (ms) => {
        clock += ms;
      },
    },
  );
  assert.equal(result.issued, 100);
  assert.equal(result.completed, 100);
  assert.equal(result.dropped, 0);
  assert.equal(result.timeouts, 33);
  assert.equal(result.non302, 33);
  assert.equal(result.clientDurationsMs.length, 100);
  assert.deepEqual(result.statuses, { 302: 34, 503: 33 });
});
