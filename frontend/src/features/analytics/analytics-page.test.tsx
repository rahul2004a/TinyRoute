import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import { AnalyticsPage } from "./analytics-page";
import { getCurrentSession, refreshSession } from "../auth/auth-api";
import { sessionQueryKey } from "../auth/use-session";
vi.mock("next/navigation", () => ({
  usePathname: () => "/analytics/86b9f592-c6ec-43e1-aa62-adf606f03c1e",
  useRouter: () => ({ push: vi.fn() }),
}));
vi.mock("../auth/auth-api", () => ({
  getCurrentSession: vi.fn(),
  refreshSession: vi.fn(),
}));
vi.stubGlobal(
  "ResizeObserver",
  class {
    observe() {}
    unobserve() {}
    disconnect() {}
  },
);
afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});
test.each([
  { rangeQuery: {}, href: "/analytics" },
  {
    rangeQuery: { from: "2026-10-01", to: "2026-10-20" },
    href: "/analytics?from=2026-10-01&to=2026-10-20",
  },
])(
  "404 recovery preserves the reporting range: $href",
  async ({ rangeQuery, href }) => {
    vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "https://api.tinyroute.test");
    vi.mocked(getCurrentSession).mockResolvedValue({
      authenticated: true,
      user: { email: "owner@example.com" },
    });
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        Response.json(
          {
            error: {
              code: "NOT_FOUND",
              message: "Private destination: https://private.example.com",
              requestId: "internal-request-id",
            },
          },
          { status: 404 },
        ),
      ),
    );
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    render(
      <QueryClientProvider client={client}>
        <AnalyticsPage
          id="86b9f592-c6ec-43e1-aa62-adf606f03c1e"
          rangeQuery={rangeQuery}
        />
      </QueryClientProvider>,
    );
    await screen.findByRole("heading", { name: "Link not found", level: 1 });
    expect(screen.getAllByRole("heading", { level: 1 })).toHaveLength(1);
    expect(screen.getAllByRole("link")).toHaveLength(1);
    expect(
      screen
        .getByRole("link", { name: "Back to analytics" })
        .getAttribute("href"),
    ).toBe(href);
    expect(screen.getByText("404").getAttribute("aria-hidden")).toBe("true");
    expect(
      screen.queryByText(/private\.example\.com|internal-request-id/),
    ).toBeNull();
  },
);
test("renewal to a different account removes the old report key before rendering the new scope", async () => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "https://api.tinyroute.test");
  vi.mocked(getCurrentSession).mockResolvedValue({
    authenticated: true,
    user: { email: "old@example.com" },
  });
  vi.mocked(refreshSession).mockResolvedValue({
    authenticated: true,
    user: { email: "new@example.com" },
  });
  const report = {
    range: { from: "2026-10-20", to: "2026-10-20", timeZone: "UTC" },
    generatedAt: "2026-10-20T12:00:00Z",
    recordingDelaySeconds: 60,
    totalClicks: 0,
    lifetimeClicks: 17,
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
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({}, { status: 401 }))
    .mockImplementation(() => Promise.resolve(Response.json(report)));
  vi.stubGlobal("fetch", fetcher);
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <AnalyticsPage
        id="86b9f592-c6ec-43e1-aa62-adf606f03c1e"
        rangeQuery={{}}
      />
    </QueryClientProvider>,
  );
  await screen.findByText("17");
  await waitFor(() =>
    expect(client.getQueryData(sessionQueryKey)).toEqual({
      authenticated: true,
      user: { email: "new@example.com" },
    }),
  );
  expect(
    client
      .getQueryCache()
      .findAll({ queryKey: ["analytics", "old@example.com"] }),
  ).toHaveLength(0);
  expect(
    client
      .getQueryCache()
      .findAll({ queryKey: ["analytics", "new@example.com"] }),
  ).toHaveLength(1);
  expect(fetcher).toHaveBeenCalledTimes(2);
});
