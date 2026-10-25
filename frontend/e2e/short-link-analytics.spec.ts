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
          const countries = total
            ? [
                {
                  countryCode: "SE",
                  label: "Sweden",
                  count: Math.ceil(total / 2),
                },
                {
                  countryCode: "IN",
                  label: "India",
                  count: Math.floor(total / 2),
                },
              ].filter((country) => country.count > 0)
            : [];
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
              countries: detail
                ? countries.map((country) => ({
                    kind: "VALUE",
                    label: country.label,
                    count: country.count,
                  }))
                : bucket("Sweden"),
              cities: bucket("Linköping, SE"),
            },
            ...(detail
              ? {
                  geography: { countries, unknownClicks: 0 },
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

test("individual-link map matches country counts and supports zoom, drag, reset, and range changes", async ({
  page,
}) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  await fixture(page);
  await page.goto("/analytics");
  await expect(page.locator(".analytics-total").first()).toHaveText("26");
  await expect(
    page.getByRole("heading", { name: "Traffic by country" }),
  ).toHaveCount(0);
  await page.goto(`/analytics/${id}`);
  const map = page.getByRole("group", {
    name: "World map of redirect countries",
  });
  await expect(map).toBeVisible();
  await page
    .getByRole("button", { name: "Sweden: 10 redirects", exact: true })
    .focus();
  await expect(page.getByText("Sweden · 10 redirects")).toBeVisible();
  await expect(
    page
      .getByRole("table", { name: "Countries", exact: true })
      .getByRole("rowheader", { name: "Sweden" }),
  ).toBeVisible();
  const shapes = map.locator("svg > g");
  const original = await shapes.getAttribute("transform");
  await page.getByRole("button", { name: "Zoom in", exact: true }).click();
  await expect(page.getByLabel("Map zoom", { exact: true })).toHaveText("2×");
  const zoomed = await shapes.getAttribute("transform");
  expect(zoomed).not.toBe(original);
  const rectangle = await map.boundingBox();
  if (!rectangle) throw new Error("Visible map has no bounds");
  await page.mouse.move(
    rectangle.x + rectangle.width / 2,
    rectangle.y + rectangle.height / 2,
  );
  await page.mouse.down();
  await page.mouse.move(
    rectangle.x + rectangle.width / 2 + 60,
    rectangle.y + rectangle.height / 2 + 30,
    { steps: 8 },
  );
  await page.mouse.up();
  await expect(shapes).not.toHaveAttribute("transform", zoomed!);
  await page.getByRole("button", { name: "Reset map", exact: true }).click();
  await expect(shapes).toHaveAttribute("transform", original!);
  await map.focus();
  await page.keyboard.press("+");
  await expect(page.getByLabel("Map zoom", { exact: true })).toHaveText("2×");
  await page.keyboard.press("ArrowLeft");
  await page.keyboard.press("Home");
  await expect(shapes).toHaveAttribute("transform", original!);
  await page.getByRole("button", { name: "Zoom in", exact: true }).click();
  await page.getByRole("button", { name: "Last 7 days", exact: true }).click();
  await expect(page.getByLabel("Map zoom", { exact: true })).toHaveText("1×");
  await page
    .getByRole("button", { name: "Sweden: 4 redirects", exact: true })
    .focus();
  await expect(page.getByText("Sweden · 4 redirects")).toBeVisible();
  for (let i = 0; i < 3; i++)
    await page.getByRole("button", { name: "Zoom in", exact: true }).click();
  await map.focus();
  for (let i = 0; i < 14; i++) await page.keyboard.press("ArrowRight");
  for (let i = 0; i < 4; i++) await page.keyboard.press("ArrowUp");
  await page.mouse.move(0, 0);
  await page
    .getByRole("button", { name: "India: 3 redirects", exact: true })
    .focus();
  await expect(page.getByText("India · 3 redirects")).toBeVisible();
  await page.locator(".analytics-map").screenshot({
    path: resolve(
      "../docs/spec/short-link-analytics/ui/india-boundary-view.png",
    ),
  });
});

test.describe("touch map", () => {
  test.use({ hasTouch: true, viewport: { width: 390, height: 844 } });
  test("country counts and zoom controls respond to taps", async ({ page }) => {
    await fixture(page);
    await page.goto(`/analytics/${id}`);
    await page
      .getByRole("button", { name: "India: 9 redirects", exact: true })
      .tap();
    await expect(page.getByText("India · 9 redirects")).toBeVisible();
    await page.getByRole("button", { name: "Zoom in", exact: true }).tap();
    await expect(page.getByLabel("Map zoom", { exact: true })).toHaveText("2×");
    await page.getByRole("button", { name: "Zoom out", exact: true }).tap();
    await expect(page.getByLabel("Map zoom", { exact: true })).toHaveText("1×");
  });
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

for (const theme of ["light", "dark"] as const) {
  for (const width of [320, 768, 1024, 1440]) {
    test(`analytics missing-link ${theme} at ${width}px preserves recovery and keyboard access`, async ({
      page,
    }) => {
      await fixture(page);
      await page.setViewportSize({ width, height: 900 });
      await page.emulateMedia({ reducedMotion: "reduce" });
      await page.addInitScript(
        (value) => localStorage.setItem("tinyroute-color-theme", value),
        theme,
      );
      const suffix = "?from=2026-10-01&to=2026-10-20";
      await page.goto(`/analytics/missing${suffix}`);
      const main = page.getByRole("main");
      const heading = main.getByRole("heading", {
        name: "Link not found",
        level: 1,
      });
      const back = main.getByRole("link", { name: "Back to analytics" });
      await expect(heading).toBeVisible();
      await expect(main.getByRole("heading", { level: 1 })).toHaveCount(1);
      await expect(main.getByText("404", { exact: true })).toHaveAttribute(
        "aria-hidden",
        "true",
      );
      await expect(back).toHaveAttribute("href", `/analytics${suffix}`);
      await expect(main).not.toContainText(saved.shortUrl);
      await expect(main).not.toContainText(saved.destinationUrl);
      await expect(main).not.toContainText("analytics@example.com");
      await expect(main.getByRole("table")).toHaveCount(0);
      const size = await back.boundingBox();
      expect(size!.width).toBeGreaterThanOrEqual(44);
      expect(size!.height).toBeGreaterThanOrEqual(44);
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth > innerWidth,
        ),
      ).toBe(false);
      await page.getByRole("link", { name: "Skip to content" }).focus();
      await page.keyboard.press("Enter");
      await expect(main).toBeFocused();
      await page.keyboard.press("Tab");
      await expect(back).toBeFocused();
      expect(
        await back.evaluate(
          (element) => getComputedStyle(element).outlineStyle,
        ),
      ).not.toBe("none");
      const directory = resolve("../docs/spec/ui-polish/evidence");
      await mkdir(directory, { recursive: true });
      await heading.click();
      await page.screenshot({
        path: resolve(directory, `analytics-not-found-${width}-${theme}.png`),
        fullPage: true,
        animations: "disabled",
        style: "nextjs-portal { display: none !important; }",
      });
      if (width < 1024) {
        const trigger = page.getByRole("button", {
          name: "Open analytics navigation",
        });
        await trigger.click();
        await expect(page.getByRole("dialog")).toBeVisible();
        await page.keyboard.press("Escape");
        await expect(trigger).toBeFocused();
      }
      if (width === 320) {
        await page.setViewportSize({ width, height: 480 });
        await back.scrollIntoViewIfNeeded();
        const shortScreenAction = await back.boundingBox();
        expect(shortScreenAction!.y).toBeGreaterThanOrEqual(0);
        expect(
          shortScreenAction!.y + shortScreenAction!.height,
        ).toBeLessThanOrEqual(480);
        expect(
          await page.evaluate(
            () => document.documentElement.scrollWidth > innerWidth,
          ),
        ).toBe(false);
      }
      await back.click();
      await expect(page).toHaveURL(`/analytics${suffix}`);
      await expect(page.locator(".analytics-total").first()).toHaveText("26");
    });
  }
}

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
      await page.goto(`/analytics/${id}`);
      await expect(
        page.getByRole("heading", { name: "Traffic by country" }),
      ).toBeVisible();
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth,
        ),
      ).toBe(true);
      await page.getByRole("button", { name: "Zoom in", exact: true }).click();
      await expect(page.getByLabel("Map zoom", { exact: true })).toHaveText(
        "2×",
      );
      await page
        .getByRole("button", { name: "Reset map", exact: true })
        .click();
      await page.evaluate(() => {
        (document.activeElement as HTMLElement | null)?.blur();
        window.scrollTo(0, 0);
      });
      await page.screenshot({
        path: resolve(directory, `map-${theme}-${width}.png`),
        fullPage: true,
      });
    });
  }
