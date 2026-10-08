import test from "node:test";
import assert from "node:assert/strict";
import {
  percentile,
  evaluateRun,
  requestOptions,
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
