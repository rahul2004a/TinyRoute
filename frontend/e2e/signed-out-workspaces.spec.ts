import { expect, test, type Page } from "@playwright/test";
import { mkdir } from "node:fs/promises";
import { resolve } from "node:path";

async function signedOutSession(page: Page) {
  const requests: string[] = [];
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
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
    requests.push(path);
    if (path === "/api/auth/csrf") {
      await route.fulfill({
        status: 200,
        headers,
        json: { csrfToken: "fixture" },
      });
      return;
    }
    await route.fulfill({
      status: 401,
      headers,
      json: {
        error: {
          code: "AUTHENTICATION_FAILED",
          message: "Private fixture detail",
          requestId: "fixture",
        },
      },
    });
  });
  return requests;
}

const cases = [
  {
    route: "settings",
    path: "/settings",
    title: "Account settings",
    href: "/login",
  },
  {
    route: "analytics",
    path: "/analytics?from=2026-10-01&to=2026-10-20",
    title: "Sign in to view analytics",
    href: "/login?returnTo=%2Fanalytics%3Ffrom%3D2026-10-01%26to%3D2026-10-20",
  },
  {
    route: "links",
    path: "/links",
    title: "Create a short link",
    href: "/login",
  },
] as const;

for (const entry of cases) {
  for (const theme of ["light", "dark"] as const) {
    for (const width of [320, 768, 1024, 1440]) {
      test(`signed-out ${entry.route} ${theme} at ${width}px keeps access clear and private`, async ({
        page,
      }) => {
        const requests = await signedOutSession(page);
        await page.setViewportSize({ width, height: 900 });
        await page.emulateMedia({ reducedMotion: "reduce" });
        await page.addInitScript(
          (value) => localStorage.setItem("tinyroute-color-theme", value),
          theme,
        );
        await page.goto(entry.path);
        const main = page.getByRole("main");
        await expect(
          main.getByRole("heading", { name: entry.title, level: 1 }),
        ).toBeVisible();
        await expect(main.getByRole("heading", { level: 1 })).toHaveCount(1);
        const signIn = main.getByRole("link", { name: "Sign in", exact: true });
        await expect(signIn).toHaveAttribute("href", entry.href);
        await expect(
          main.getByRole("button", { name: "Delete account", exact: true }),
        ).toHaveCount(0);
        await expect(
          main.getByRole("button", { name: "Sign out", exact: true }),
        ).toHaveCount(0);
        await expect(main.getByLabel("Destination URL")).toHaveCount(0);
        await expect(main.getByRole("table")).toHaveCount(0);
        await expect(main).not.toContainText("Private fixture detail");
        await expect(
          page.getByRole("link", { name: "Dashboard", exact: true }),
        ).toHaveCount(0);
        const bounds = await signIn.boundingBox();
        expect(bounds!.width).toBeGreaterThanOrEqual(44);
        expect(bounds!.height).toBeGreaterThanOrEqual(44);
        expect(bounds!.y + bounds!.height).toBeLessThanOrEqual(900);
        expect(
          await page.evaluate(
            () => document.documentElement.scrollWidth > innerWidth,
          ),
        ).toBe(false);
        if (entry.route === "links") {
          const footer = await page.getByRole("contentinfo").boundingBox();
          expect(footer!.y).toBeGreaterThanOrEqual(899);
        }
        if (entry.route !== "links") {
          if (width < 1024)
            await page
              .getByRole("button", { name: `Open ${entry.route} navigation` })
              .click();
          const nav = page.getByRole("navigation", {
            name: "Settings navigation",
            exact: true,
          });
          await expect(
            nav.getByRole("link", { name: "Your links", exact: true }),
          ).toHaveAttribute("href", "/links");
          if (width < 1024) {
            await page.keyboard.press("Escape");
            await expect(
              page.getByRole("button", {
                name: `Open ${entry.route} navigation`,
              }),
            ).toBeFocused();
          }
        }
        await page
          .getByRole("link", { name: "Skip to content", exact: true })
          .focus();
        await page.keyboard.press("Enter");
        await expect(main).toBeFocused();
        await page.keyboard.press("Tab");
        await expect(signIn).toBeFocused();
        expect(
          await signIn.evaluate(
            (element) => getComputedStyle(element).outlineStyle,
          ),
        ).not.toBe("none");
        if (width === 320 || width === 1440) {
          await page.mouse.click(0, 0);
          await page.evaluate(() => window.scrollTo(0, 0));
          if (entry.route === "links")
            await expect(page.getByRole("banner")).toBeVisible();
          const directory = resolve("../docs/spec/ui-polish/evidence");
          await mkdir(directory, { recursive: true });
          await page.screenshot({
            path: resolve(
              directory,
              `signed-out-${entry.route}-${width}-${theme}.png`,
            ),
            animations: "disabled",
            style: "nextjs-portal { display: none !important; }",
          });
        }
        if (width === 320) {
          await page.setViewportSize({ width, height: 480 });
          await signIn.scrollIntoViewIfNeeded();
          const short = await signIn.boundingBox();
          expect(short!.y).toBeGreaterThanOrEqual(0);
          expect(short!.y + short!.height).toBeLessThanOrEqual(480);
        }
        expect(requests.every((path) => path.startsWith("/api/auth/"))).toBe(
          true,
        );
        await signIn.click();
        await expect(
          page.getByRole("heading", {
            name: "Sign in to TinyRoute",
            exact: true,
          }),
        ).toBeVisible();
      });
    }
  }
}

test("signed-out detail analytics keeps its link and reporting range in the login target", async ({
  page,
}) => {
  const requests = await signedOutSession(page);
  const path = "/analytics/missing?from=2026-10-01&to=2026-10-20";
  await page.goto(path);
  await expect(
    page.getByRole("main").getByRole("link", { name: "Sign in", exact: true }),
  ).toHaveAttribute("href", `/login?returnTo=${encodeURIComponent(path)}`);
  expect(requests.every((path) => path.startsWith("/api/auth/"))).toBe(true);
});
