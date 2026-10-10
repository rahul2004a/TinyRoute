import { mkdir } from "node:fs/promises";
import { resolve } from "node:path";
import { expect, test, type Page, type Route } from "@playwright/test";

const destination = "https://example.com/docs?q=java#setup";
const link = {
  id: "86b9f592-c6ec-43e1-aa62-adf606f03c1e",
  code: "Docs2026",
  shortUrl: "https://go.tinyroute.test/Docs2026",
  destinationUrl: destination,
  createdAt: "2026-10-08T00:00:00Z",
  expiresAt: null,
};
const account = { authenticated: true, user: { email: "fixture@example.com" } };
const failure = (code: string, fieldErrors?: Record<string, string>) => ({
  error: {
    code,
    message: "Safe fixture message",
    requestId: "fixture",
    fieldErrors,
    retryAfterSeconds: 31,
  },
});
type Response = { status: number; body?: unknown };
async function fixture(
  page: Page,
  options: {
    session?: "out" | "unknown";
    create?: (route: Route, count: number) => Promise<Response>;
    renew?: boolean;
  } = {},
) {
  let attempts = 0,
    refreshes = 0;
  await page.route("https://localhost:8443/api/**", async (route) => {
    const request = route.request(),
      path = new URL(request.url()).pathname;
    const headers = {
      "Access-Control-Allow-Origin": "https://localhost:3000",
      "Access-Control-Allow-Credentials": "true",
      "Access-Control-Allow-Headers": "Content-Type, X-CSRF-TOKEN, Accept",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Content-Type": "application/json",
      "Cache-Control": "no-store",
    };
    if (request.method() === "OPTIONS") {
      await route.fulfill({ status: 204, headers });
      return;
    }
    let response: Response;
    if (path.endsWith("/csrf"))
      response = { status: 200, body: { csrfToken: "fixture-csrf" } };
    else if (path.endsWith("/me"))
      response =
        options.session === "unknown"
          ? { status: 503, body: failure("SESSION_UNAVAILABLE") }
          : options.session === "out" ||
              (options.renew && attempts > 0 && refreshes === 0)
            ? { status: 401, body: failure("AUTHENTICATION_FAILED") }
            : { status: 200, body: account };
    else if (path.endsWith("/refresh")) {
      refreshes++;
      response =
        options.session === "out"
          ? { status: 401, body: failure("AUTHENTICATION_FAILED") }
          : { status: 200, body: account };
    } else if (path === "/api/links") {
      attempts++;
      expect(request.headers()["x-csrf-token"]).toBe("fixture-csrf");
      response = options.create
        ? await options.create(route, attempts)
        : { status: 201, body: link };
    } else throw new Error("Unexpected fixture request");
    await route.fulfill({
      status: response.status,
      headers,
      body: JSON.stringify(response.body),
    });
  });
  return { attempts: () => attempts, refreshes: () => refreshes };
}
async function fill(page: Page) {
  await page.goto("/links");
  await page.getByLabel("HTTPS destination").fill(destination);
  await page.getByLabel("Custom alias (optional)").fill("Docs2026");
}

test("signed-out and unknown sessions cannot submit", async ({ page }) => {
  const state = await fixture(page, { session: "out" });
  await page.goto("/links");
  await expect(
    page.getByRole("link", { name: "Sign in", exact: true }),
  ).toBeVisible();
  await expect(page.getByLabel("HTTPS destination")).toHaveCount(0);
  expect(state.attempts()).toBe(0);
  await page.unrouteAll();
  await fixture(page, { session: "unknown" });
  await page.reload();
  await expect(page.locator("main").getByRole("alert")).toContainText(
    "check your session",
    { timeout: 15000 },
  );
  await expect(
    page.getByRole("button", { name: "Retry session check" }),
  ).toBeVisible();
});
test("keyboard creation and one-click copying use the exact response URL", async ({
  page,
  context,
}) => {
  await context.grantPermissions(["clipboard-read", "clipboard-write"]);
  const state = await fixture(page);
  await fill(page);
  await page.getByLabel("HTTPS destination").focus();
  await page.keyboard.press("Tab");
  await expect(page.getByLabel("Custom alias (optional)")).toBeFocused();
  await page.getByRole("button", { name: "Create short link" }).focus();
  await page.keyboard.press("Enter");
  await expect(page.getByLabel("Short URL")).toHaveValue(link.shortUrl);
  await page.getByLabel("Short URL").focus();
  await page.keyboard.press("Tab");
  await expect(
    page.getByRole("button", { name: "Copy short link" }),
  ).toBeFocused();
  await page.keyboard.press("Enter");
  await expect(page.getByRole("status")).toContainText("Copied to clipboard.");
  expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(
    link.shortUrl,
  );
  expect(state.attempts()).toBe(1);
});
test("clipboard denial keeps a selectable URL and manual guidance", async ({
  page,
}) => {
  await page.addInitScript(() =>
    Object.defineProperty(navigator, "clipboard", {
      value: { writeText: () => Promise.reject(new Error("denied")) },
      configurable: true,
    }),
  );
  await fixture(page);
  await fill(page);
  await page.getByRole("button", { name: "Create short link" }).click();
  await page.getByRole("button", { name: "Copy short link" }).click();
  await expect(page.getByRole("status")).toContainText("copy it manually");
  await expect(page.getByLabel("Short URL")).toHaveValue(link.shortUrl);
  await expect(page.getByLabel("Short URL")).toBeFocused();
});
for (const [status, code] of [
  [400, "VALIDATION_ERROR"],
  [409, "ALIAS_UNAVAILABLE"],
  [429, "RATE_LIMITED"],
  [403, "CSRF_INVALID"],
  [503, "SERVICE_UNAVAILABLE"],
] as const) {
  test(`creation ${code} retains inputs without automatic resubmission`, async ({
    page,
  }) => {
    const state = await fixture(page, {
      create: async () => ({
        status,
        body: failure(code, {
          destinationUrl: "Destination must not point to the short-link host.",
        }),
      }),
    });
    await fill(page);
    await page.getByRole("button", { name: "Create short link" }).click();
    await expect(page.locator("main").getByRole("alert")).toBeVisible();
    await expect(page.getByLabel("HTTPS destination")).toHaveValue(destination);
    await expect(page.getByLabel("Custom alias (optional)")).toHaveValue(
      "Docs2026",
    );
    expect(state.attempts()).toBe(1);
  });
}
test("session renewal rechecks authentication but waits for manual creation", async ({
  page,
}) => {
  const state = await fixture(page, {
    renew: true,
    create: async (_route, count) =>
      count === 1
        ? { status: 401, body: failure("AUTHENTICATION_FAILED") }
        : { status: 201, body: link },
  });
  await fill(page);
  await page.getByRole("button", { name: "Create short link" }).click();
  await expect(page.locator("main").getByRole("alert")).toContainText(
    "Submit again",
  );
  expect(state.refreshes()).toBe(1);
  expect(state.attempts()).toBe(1);
  await page.getByRole("button", { name: "Create short link" }).click();
  await expect(page.getByLabel("Short URL")).toHaveValue(link.shortUrl);
  expect(state.attempts()).toBe(2);
});
test("a malformed server error stays generic and double submission sends one POST", async ({
  page,
}) => {
  const state = await fixture(page, {
    create: async () => {
      await new Promise((r) => setTimeout(r, 150));
      return { status: 500, body: { private: "must never appear" } };
    },
  });
  await fill(page);
  await page.getByRole("button", { name: "Create short link" }).dblclick();
  await expect(page.locator("main").getByRole("alert")).toContainText(
    "Creation may have succeeded",
  );
  await expect(page.getByText("must never appear")).toHaveCount(0);
  expect(state.attempts()).toBe(1);
});

for (const theme of ["light", "dark"] as const)
  for (const width of [320, 768, 1024, 1440]) {
    test(`${theme} ${width}px creation, result, and error are readable with reduced motion`, async ({
      page,
    }) => {
      await page.setViewportSize({ width, height: 1000 });
      await page.emulateMedia({ reducedMotion: "reduce" });
      await page.addInitScript(
        (theme) => localStorage.setItem("tinyroute-color-theme", theme),
        theme,
      );
      const errors: string[] = [];
      page.on("pageerror", (error) => errors.push(error.message));
      await fixture(page, {
        create: async (_route, count) =>
          count === 1
            ? { status: 201, body: link }
            : { status: 409, body: failure("ALIAS_UNAVAILABLE") },
      });
      await fill(page);
      await expect(page.locator("html")).toHaveAttribute("data-theme", theme);
      expect(
        await page
          .locator("main")
          .evaluate((element) => getComputedStyle(element).animationName),
      ).toBe("none");
      const folder = resolve("../docs/spec/link-creation-and-redirection/ui");
      await mkdir(folder, { recursive: true });
      async function capture(state: string) {
        expect(
          await page.evaluate(
            () => document.documentElement.scrollWidth <= innerWidth,
          ),
        ).toBe(true);
        const contrasts = await page.locator("main").evaluate((main) => {
          function rgba(value: string): number[] {
            if (!value.startsWith("rgb"))
              throw new Error(`Unsupported computed color: ${value}`);
            const channels = value.match(/[\d.]+/g)!.map(Number);
            return [channels[0], channels[1], channels[2], channels[3] ?? 1];
          }
          function luminance(channels: number[]) {
            const linear = channels.slice(0, 3).map((value) => {
              const c = value / 255;
              return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
            });
            return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2];
          }
          return [...main.querySelectorAll("label, p, a, button, input")]
            .filter(
              (element) =>
                element.getBoundingClientRect().width > 0 &&
                !(element instanceof HTMLButtonElement && element.disabled),
            )
            .map((element) => {
              const ancestry: Element[] = [];
              for (
                let current: Element | null = element;
                current;
                current = current.parentElement
              )
                ancestry.unshift(current);
              let background = [255, 255, 255];
              for (const ancestor of ancestry) {
                const color = rgba(getComputedStyle(ancestor).backgroundColor);
                background = background.map(
                  (channel, index) =>
                    color[index] * color[3] + channel * (1 - color[3]),
                );
              }
              const color = rgba(getComputedStyle(element).color);
              const foreground = background.map(
                (channel, index) =>
                  color[index] * color[3] + channel * (1 - color[3]),
              );
              const a = luminance(foreground),
                b = luminance(background);
              return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
            });
        });
        expect(contrasts.length).toBeGreaterThan(10);
        expect(Math.min(...contrasts)).toBeGreaterThanOrEqual(4.5);
        await page.screenshot({
          path: resolve(folder, `${theme}-${width}-${state}.png`),
          fullPage: true,
          style: "nextjs-portal { display: none !important; }",
        });
      }
      await capture("create");
      await page.getByRole("button", { name: "Create short link" }).click();
      await expect(page.getByLabel("Short URL")).toHaveValue(link.shortUrl);
      await capture("result");
      for (const name of ["Create short link", "Copy short link"]) {
        const size = await page.getByRole("button", { name }).boundingBox();
        expect(size?.height).toBeGreaterThanOrEqual(44);
        expect(size?.width).toBeGreaterThanOrEqual(44);
      }
      await page.getByRole("button", { name: "Create short link" }).click();
      await expect(page.locator("main").getByRole("alert")).toBeVisible();
      await capture("error");
      expect(errors).toEqual([]);
    });
  }
