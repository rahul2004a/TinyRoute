import { mkdir } from "node:fs/promises";
import { resolve } from "node:path";
import { expect, test, type Page } from "@playwright/test";

const id = "86b9f592-c6ec-43e1-aa62-adf606f03c1e";
const saved = {
  id,
  code: "Docs2026",
  shortUrl: "https://go.tinyroute.test/Docs2026",
  destinationUrl: "https://example.com/docs",
  createdAt: "2026-10-01T00:00:00Z",
  expiresAt: null,
  status: "ACTIVE",
  clickCount: 80,
};
const failure = (code: string, fieldErrors?: Record<string, string>) => ({
  error: {
    code,
    message: "Safe fixture message",
    requestId: "analytics-fixture",
    fieldErrors,
  },
});
async function fixture(page: Page, initialFailure = false) {
  let fail = initialFailure;
  let signedIn = true;
  const requests: string[] = [];
  const visits: string[] = [];
  page.on("request", (request) => {
    if (request.url().includes("go.tinyroute.test")) visits.push(request.url());
  });
  await page.route("**/api/**", async (route) => {
    const request = route.request(),
      url = new URL(request.url()),
      path = url.pathname;
    const headers = {
      "Access-Control-Allow-Origin": "https://localhost:3000",
      "Access-Control-Allow-Credentials": "true",
      "Access-Control-Allow-Headers": "Content-Type, X-CSRF-TOKEN, Accept",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Cache-Control": "no-store",
    };
    if (request.method() === "OPTIONS") {
      await route.fulfill({ status: 204, headers });
      return;
    }
    const account = {
      authenticated: true,
      user: { email: "analytics@example.com" },
    };
    let status = 200,
      body: unknown;
    if (path.endsWith("/csrf")) body = { csrfToken: "fixture" };
    else if (path.endsWith("/me") || path.endsWith("/refresh")) {
      status = signedIn ? 200 : 401;
      body = signedIn ? account : failure("AUTHENTICATION_FAILED");
    } else if (path === "/api/links")
      body = { items: [saved], nextCursor: null };
    else if (path === "/api/analytics" || path.endsWith("/analytics")) {
      requests.push(url.pathname + url.search);
      if (!signedIn) {
        status = 401;
        body = failure("AUTHENTICATION_FAILED");
      } else if (
        path !== "/api/analytics" &&
        path !== `/api/links/${id}/analytics`
      ) {
        status = 404;
        body = failure("NOT_FOUND");
      } else if (fail) {
        status = 503;
        body = failure("SERVICE_UNAVAILABLE");
      } else {
        const from = url.searchParams.get("from") ?? "2026-09-21",
          to = url.searchParams.get("to") ?? "2026-10-20";
        if (
          !/^\d{4}-\d{2}-\d{2}$/.test(from) ||
          !/^\d{4}-\d{2}-\d{2}$/.test(to) ||
          from > to
        ) {
          status = 400;
          body = failure("VALIDATION_ERROR", {
            to: "End date must be on or after the start date.",
          });
        } else {
          const detail = path !== "/api/analytics";
          const activity: Record<string, number> = {
            "2026-10-01": 12,
            "2026-10-14": 2,
            "2026-10-20": detail ? 5 : 12,
          };
          const days = [];
          for (
            let day = Date.parse(from + "T00:00:00Z");
            day <= Date.parse(to + "T00:00:00Z");
            day += 86400000
          ) {
            const date = new Date(day).toISOString().slice(0, 10);
            days.push({ date, count: activity[date] ?? 0 });
          }
          const total = days.reduce((sum, day) => sum + day.count, 0);
          const bucket = (label: string) =>
            total ? [{ kind: "VALUE", label, count: total }] : [];
          body = {
            range: { from, to, timeZone: "UTC" },
            generatedAt: "2026-10-20T12:00:00Z",
            recordingDelaySeconds: 60,
            totalClicks: total,
            lifetimeClicks: detail ? 80 : 130,
            dailyClicks: days,
            breakdowns: {
              referrers: bucket("example.org"),
              devices: bucket("Mobile"),
              operatingSystems: bucket("iOS"),
              browsers: bucket("Safari"),
              countries: bucket("Sweden"),
              cities: bucket("Linköping, SE"),
            },
            ...(detail
              ? {
                  link: {
                    id,
                    code: saved.code,
                    shortUrl: saved.shortUrl,
                    destinationUrl: saved.destinationUrl,
                    status: "ACTIVE",
                  },
                }
              : {}),
          };
        }
      }
    } else {
      status = 404;
      body = failure("NOT_FOUND");
    }
    await route.fulfill({ status, json: body, headers });
  });
  return {
    requests,
    visits,
    recover: () => {
      fail = false;
    },
    signOut: () => {
      signedIn = false;
    },
  };
}

test("overview filters every report, opens a single link, and preserves applied range", async ({
  page,
}) => {
  const data = await fixture(page);
  await page.goto("/analytics");
  await expect(
    page.getByRole("heading", { name: "Analytics", exact: true }),
  ).toBeVisible();
  await expect(page.locator(".analytics-total").first()).toHaveText("26");
  await expect(
    page.getByText("Clicks may take up to one minute to appear.", {
      exact: false,
    }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Last 7 days" }).click();
  await expect(page).toHaveURL(/from=2026-10-14&to=2026-10-20/);
  await expect(page.locator(".analytics-total").first()).toHaveText("14");
  await page.getByText("Show data table").click();
  await expect(
    page
      .getByRole("table", { name: "Daily redirect counts (UTC)" })
      .getByRole("row"),
  ).toHaveCount(8);
  await page
    .getByRole("link", { name: "View analytics" })
    .filter({ visible: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Link analytics", exact: true }),
  ).toBeVisible();
  await expect(page.locator(".analytics-total").first()).toHaveText("7");
  await page.getByRole("link", { name: "All link analytics" }).click();
  await expect(page).toHaveURL(/\/analytics\?from=2026-10-14&to=2026-10-20/);
  expect(data.visits).toEqual([]);
});

test("validation, no activity, failure/retry, generic 404, and expired session stay distinct", async ({
  page,
}) => {
  const data = await fixture(page, true);
  await page.goto("/analytics");
  await expect(page.getByRole("main").getByRole("alert")).toContainText(
    "couldn’t load analytics",
  );
  data.recover();
  await page.getByRole("button", { name: "Retry analytics" }).click();
  await expect(page.locator(".analytics-total").first()).toHaveText("26");
  await page.getByLabel("From (UTC)").fill("2026-10-20");
  await page.getByLabel("To (UTC)").fill("2026-10-19");
  await page.getByRole("button", { name: "Apply range" }).click();
  await expect(page.getByRole("main").getByRole("alert")).toContainText(
    "valid reporting range",
  );
  await expect(page.getByLabel("To (UTC)")).toHaveAttribute(
    "aria-invalid",
    "true",
  );
  await page.goto("/analytics?from=2020-01-01&to=2020-01-03");
  await expect(
    page.getByText("No redirects in this date range."),
  ).toBeVisible();
  await expect(page.getByText("130", { exact: true })).toBeVisible();
  await page.goto("/analytics/missing");
  await expect(
    page.getByRole("heading", { name: "Link not found" }),
  ).toBeVisible();
  await page.goto("/analytics");
  await expect(page.locator(".analytics-total").first()).toHaveText("26");
  data.signOut();
  await page.getByRole("button", { name: "Refresh", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Sign in to view analytics" }),
  ).toBeVisible();
  await expect(page.locator(".analytics-total").first()).toHaveCount(0);
});

for (const theme of ["light", "dark"] as const)
  for (const width of [320, 768, 1024, 1440]) {
    test(`analytics ${theme} at ${width}px has accessible data and no page overflow`, async ({
      page,
    }) => {
      await fixture(page);
      await page.setViewportSize({ width, height: 1000 });
      await page.addInitScript(
        (value) => localStorage.setItem("tinyroute-color-theme", value),
        theme,
      );
      await page.goto("/analytics");
      await expect(page.locator(".analytics-total").first()).toHaveText("26");
      await expect(
        page.getByRole("table", { name: "Referrers" }),
      ).toBeVisible();
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth,
        ),
      ).toBe(true);
      if (width < 1024) {
        const trigger = page.getByRole("button", {
          name: "Open analytics navigation",
        });
        await trigger.click();
        await expect(page.getByRole("dialog")).toBeVisible();
        await page.keyboard.press("Escape");
        await expect(trigger).toBeFocused();
      }
      const directory = resolve("../docs/spec/short-link-analytics/ui");
      await mkdir(directory, { recursive: true });
      await page.screenshot({
        path: resolve(directory, `${theme}-${width}.png`),
        fullPage: true,
      });
    });
  }
