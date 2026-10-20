import { afterEach, expect, test, vi } from "vitest";
import { getAnalytics } from "./analytics-api";
import { refreshSession } from "../auth/auth-api";
vi.mock("../auth/auth-api", () => ({ refreshSession: vi.fn() }));
afterEach(() => vi.unstubAllGlobals());
const report = {
  range: { from: "2026-09-21", to: "2026-10-20", timeZone: "UTC" },
  generatedAt: "2026-10-20T12:00:00Z",
  recordingDelaySeconds: 60,
  totalClicks: 0,
  lifetimeClicks: 7,
  dailyClicks: [{ date: "2026-10-20", count: 0 }],
  breakdowns: {
    referrers: [],
    devices: [],
    operatingSystems: [],
    browsers: [],
    countries: [],
    cities: [],
  },
};
test("uses the authenticated API with range and cancellation", async () => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "https://api.tinyroute.test");
  const fetcher = vi.fn().mockResolvedValue(Response.json(report));
  vi.stubGlobal("fetch", fetcher);
  const signal = new AbortController().signal;
  expect(
    await getAnalytics(
      { id: "link-id", from: "2026-09-21", to: "2026-10-20" },
      signal,
    ),
  ).toEqual(report);
  expect(fetcher.mock.calls[0][0]).toBe(
    "https://api.tinyroute.test/api/links/link-id/analytics?from=2026-09-21&to=2026-10-20",
  );
  expect(fetcher.mock.calls[0][1]).toMatchObject({
    credentials: "include",
    signal,
  });
});
test("recovers once from 401 and keeps validation failures distinct from zero", async () => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "https://api.tinyroute.test");
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({}, { status: 401 }))
    .mockResolvedValueOnce(Response.json(report));
  vi.stubGlobal("fetch", fetcher);
  await getAnalytics({});
  expect(refreshSession).toHaveBeenCalledTimes(1);
  fetcher.mockResolvedValue(
    Response.json(
      {
        error: {
          code: "VALIDATION_ERROR",
          message: "Invalid",
          fieldErrors: { from: "Choose valid dates" },
          requestId: "test",
        },
      },
      { status: 400 },
    ),
  );
  await expect(getAnalytics({ from: "bad" })).rejects.toMatchObject({
    status: 400,
    apiError: { error: { fieldErrors: { from: "Choose valid dates" } } },
  });
});

test("account-changing refresh synchronizes identity and never replays under the old account", async () => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "https://api.tinyroute.test");
  const fetcher = vi.fn().mockResolvedValue(Response.json({}, { status: 401 }));
  vi.stubGlobal("fetch", fetcher);
  vi.mocked(refreshSession).mockResolvedValue({
    authenticated: true,
    user: { email: "new@example.com" },
  });
  const recovered = vi.fn();
  await expect(
    getAnalytics({ expectedAccount: "old@example.com" }, undefined, recovered),
  ).rejects.toThrow("Analytics account changed");
  expect(recovered).toHaveBeenCalledWith({
    authenticated: true,
    user: { email: "new@example.com" },
  });
  expect(fetcher).toHaveBeenCalledTimes(1);
});

test("cancellation during refresh never publishes a late identity", async () => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "https://api.tinyroute.test");
  const fetcher = vi.fn().mockResolvedValue(Response.json({}, { status: 401 }));
  vi.stubGlobal("fetch", fetcher);
  let complete!: (value: Awaited<ReturnType<typeof refreshSession>>) => void;
  vi.mocked(refreshSession).mockReturnValue(
    new Promise((resolve) => {
      complete = resolve;
    }),
  );
  const recovered = vi.fn(),
    controller = new AbortController();
  const request = getAnalytics(
    { expectedAccount: "old@example.com" },
    controller.signal,
    recovered,
  );
  await vi.waitFor(() => expect(refreshSession).toHaveBeenCalledTimes(1));
  controller.abort();
  complete({ authenticated: true, user: { email: "old@example.com" } });
  await expect(request).rejects.toMatchObject({ name: "AbortError" });
  expect(recovered).not.toHaveBeenCalled();
  expect(fetcher).toHaveBeenCalledTimes(1);
});
