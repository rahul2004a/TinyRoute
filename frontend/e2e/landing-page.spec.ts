import { expect, test } from "@playwright/test";
import { mkdir } from "node:fs/promises";
import { resolve } from "node:path";

test("public landing page survives an unavailable backend without a session request", async ({
  page,
}) => {
  const requests: string[] = [];
  await page.route("**/api/**", async (route) => {
    requests.push(route.request().url());
    await route.abort();
  });
  await page.goto("/");
  await expect(
    page.getByRole("heading", { level: 1, name: "Short links. Your control." }),
  ).toBeVisible();
  await page.getByRole("contentinfo").scrollIntoViewIfNeeded();
  await expect(page.getByRole("link", { name: "Your links" })).toHaveAttribute(
    "href",
    "/links",
  );
  expect(requests).toEqual([]);
});

const viewports = [
  { width: 320, height: 568 },
  { width: 768, height: 1024 },
  { width: 1024, height: 768 },
  { width: 1440, height: 900 },
];

async function sessionFixture(
  page: import("@playwright/test").Page,
  state: "in" | "out" | "unavailable",
) {
  const requests: string[] = [];
  await page.route("https://localhost:8443/api/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
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
    requests.push(`${request.method()} ${path}`);
    if (path === "/api/links" && request.method() === "GET") {
      await route.fulfill({
        status: 200,
        headers,
        json: { items: [], nextCursor: null },
      });
      return;
    }
    if (path.endsWith("/csrf")) {
      await route.fulfill({
        status: 200,
        headers,
        json: { csrfToken: "fixture-csrf" },
      });
      return;
    }
    if (!path.endsWith("/me") && !path.endsWith("/refresh"))
      throw new Error(`Unexpected application request: ${path}`);
    await route.fulfill({
      status: state === "in" ? 200 : state === "out" ? 401 : 503,
      headers,
      json:
        state === "in"
          ? { authenticated: true, user: { email: "fixture@example.com" } }
          : {
              error: {
                code:
                  state === "out"
                    ? "AUTHENTICATION_FAILED"
                    : "SESSION_UNAVAILABLE",
                message: "Safe fixture message",
                requestId: "fixture",
              },
            },
    });
  });
  return requests;
}

for (const state of ["in", "out", "unavailable"] as const) {
  test(`Your links keeps existing ${state} session handling`, async ({
    page,
  }) => {
    const requests = await sessionFixture(page, state);
    await page.goto("/");
    expect(requests).toEqual([]);
    await page.getByRole("link", { name: "Your links" }).click();
    await expect(page).toHaveURL(/\/links$/);
    if (state === "in")
      await expect(page.getByLabel("HTTPS destination")).toBeVisible();
    else {
      await expect(page.getByLabel("HTTPS destination")).toHaveCount(0);
      if (state === "out")
        await expect(
          page.getByRole("link", { name: "Sign in", exact: true }),
        ).toBeVisible();
      else
        await expect(page.locator("main").getByRole("alert")).toContainText(
          "check your session",
          { timeout: 15000 },
        );
    }
    expect(requests).toContain("GET /api/auth/me");
    expect(requests.some((request) => request === "POST /api/links")).toBe(
      false,
    );
    if (state !== "in") expect(requests).not.toContain("GET /api/links");
  });
}

test("cached authentication never changes the landing navigation when the backend goes down", async ({
  page,
}) => {
  const requests = await sessionFixture(page, "in");
  await page.goto("/links");
  await expect(page.getByLabel("HTTPS destination")).toBeVisible();
  await expect(page.getByText("No saved links on this page.")).toBeVisible();
  await page.unrouteAll();
  const unexpected: string[] = [];
  await page.route("**/api/**", async (route) => {
    unexpected.push(route.request().url());
    await route.abort();
  });
  await page
    .getByRole("banner")
    .getByRole("link", { name: "TinyRoute", exact: true })
    .click();
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(
    "Short links. Your control.",
  );
  await expect(page.getByText("fixture@example.com")).toHaveCount(0);
  await expect(page.getByRole("button", { name: /sign out/i })).toHaveCount(0);
  await expect(
    page
      .locator(".landing-hero")
      .getByRole("link", { name: "Sign in", exact: true }),
  ).toBeVisible();
  expect(requests).toContain("GET /api/auth/me");
  expect(unexpected).toEqual([]);
});

for (const region of [
  "Primary navigation",
  "hero",
  "closing",
  "Footer account",
  "Mobile navigation",
]) {
  for (const [name, path, heading] of [
    ["Get started", "/register", "Create your account"],
    ["Sign in", "/login", "Sign in to TinyRoute"],
  ]) {
    test(`${region} ${name} opens ${path}`, async ({ page }) => {
      await page.setViewportSize({
        width: region === "Mobile navigation" ? 320 : 1440,
        height: 900,
      });
      await sessionFixture(page, "out");
      await page.goto("/");
      if (region === "Mobile navigation")
        await page.getByRole("button", { name: "Menu", exact: true }).click();
      const scope =
        region === "hero"
          ? page.locator(".landing-hero")
          : region === "closing"
            ? page.locator(".landing-closing")
            : page.getByRole("navigation", { name: region, exact: true });
      await scope.getByRole("link", { name, exact: true }).click();
      await expect(page).toHaveURL(new RegExp(`${path}$`));
      await expect(
        page.getByRole("heading", { name: heading, exact: true }),
      ).toBeVisible();
    });
  }
}

test("native section links, skip link and mobile disclosure preserve keyboard focus", async ({
  page,
}) => {
  await page.setViewportSize({ width: 320, height: 568 });
  await page.goto("/");
  await page.keyboard.press("Tab");
  await expect(
    page.getByRole("link", { name: "Skip to content" }),
  ).toBeFocused();
  await page.keyboard.press("Enter");
  await expect(page.getByRole("main")).toBeFocused();
  const trigger = page.getByRole("button", { name: "Menu", exact: true });
  await trigger.focus();
  await page.keyboard.press("Enter");
  await expect(trigger).toBeFocused();
  await expect(trigger).toHaveAttribute("aria-expanded", "true");
  await page.keyboard.press("Tab");
  const nav = page.getByRole("navigation", {
    name: "Mobile navigation",
    exact: true,
  });
  await expect(
    nav.getByRole("link", { name: "Sign in", exact: true }),
  ).toBeFocused();
  await page.keyboard.press("Shift+Tab");
  await expect(trigger).toBeFocused();
  await page.keyboard.press("Tab");
  await page.keyboard.press("Escape");
  await expect(trigger).toBeFocused();
  await expect(nav).toBeHidden();
  await page.keyboard.press("Enter");
  await page.keyboard.press("Escape");
  await expect(nav).toBeHidden();
  for (const [name, hash] of [
    ["Features", "#features"],
    ["How it works", "#how-it-works"],
  ]) {
    await page
      .getByRole("navigation", { name: "Footer sections" })
      .getByRole("link", { name, exact: true })
      .click();
    await expect(page).toHaveURL(new RegExp(`${hash}$`));
    await expect(page.locator(hash)).toBeFocused();
  }
});

test("desktop resize transfers focus and returning to mobile starts closed", async ({
  page,
}) => {
  await page.setViewportSize({ width: 768, height: 1024 });
  await page.goto("/");
  const trigger = page.getByRole("button", { name: "Menu", exact: true });
  await trigger.click();
  await page
    .getByRole("navigation", { name: "Mobile navigation" })
    .getByRole("link", { name: "Sign in", exact: true })
    .focus();
  await page.setViewportSize({ width: 1024, height: 768 });
  await expect(
    page
      .getByRole("navigation", { name: "Primary navigation" })
      .getByRole("link", { name: "Sign in", exact: true }),
  ).toBeFocused();
  await page.setViewportSize({ width: 768, height: 1024 });
  await expect(trigger).toBeFocused();
  await expect(trigger).toHaveAttribute("aria-expanded", "false");
  await expect(
    page.getByRole("navigation", { name: "Mobile navigation" }),
  ).toBeHidden();
});

test("resizing a focused Menu trigger moves focus to visible desktop navigation", async ({
  page,
}) => {
  for (const open of [false, true]) {
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.goto("/");
    const trigger = page.getByRole("button", { name: "Menu", exact: true });
    await trigger.focus();
    if (open) await page.keyboard.press("Enter");
    await expect(trigger).toHaveAttribute("aria-expanded", String(open));
    await page.setViewportSize({ width: 1024, height: 768 });
    await expect(
      page
        .getByRole("navigation", { name: "Primary navigation" })
        .getByRole("link", { name: "Sign in", exact: true }),
    ).toBeFocused();
  }
});

test("modified account activation opens its real destination without closing the disclosure", async ({
  page,
  context,
}) => {
  await page.setViewportSize({ width: 320, height: 568 });
  await page.goto("/");
  const trigger = page.getByRole("button", { name: "Menu", exact: true });
  await trigger.click();
  const opened = context.waitForEvent("page");
  await page
    .getByRole("navigation", { name: "Mobile navigation" })
    .getByRole("link", { name: "Sign in", exact: true })
    .click({ modifiers: ["ControlOrMeta"] });
  const newPage = await opened;
  await expect(newPage).toHaveURL(/\/login$/);
  await expect(trigger).toHaveAttribute("aria-expanded", "true");
  await newPage.close();
});

test("one header toggle persists theme through reload and account navigation", async ({
  page,
}) => {
  await sessionFixture(page, "out");
  await page.goto("/");
  const toggle = page.getByRole("button", { name: /Switch to .* mode/ });
  await expect(toggle).toHaveCount(1);
  await expect(toggle).toBeEnabled();
  await toggle.focus();
  await page.keyboard.press("Enter");
  await expect(page.locator("html")).toHaveAttribute("data-theme", "light");
  expect(
    await page.evaluate(() => localStorage.getItem("tinyroute-color-theme")),
  ).toBe("light");
  await page.reload();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "light");
  expect(
    await toggle.evaluate((element) => getComputedStyle(element).position),
  ).not.toBe("fixed");
  await page
    .locator(".landing-hero")
    .getByRole("link", { name: "Sign in", exact: true })
    .click();
  await expect(page).toHaveURL(/\/login$/);
  await expect(toggle).toHaveCount(1);
  expect(
    await toggle.evaluate((element) => getComputedStyle(element).position),
  ).toBe("fixed");
  await expect(page.locator("html")).toHaveAttribute("data-theme", "light");
  await toggle.click();
  await page.goto("/");
  await expect(toggle).toHaveCount(1);
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
});

test("static content and account links remain readable without JavaScript", async ({
  browser,
}) => {
  const context = await browser.newContext({
    javaScriptEnabled: false,
    ignoreHTTPSErrors: true,
  });
  try {
    const page = await context.newPage();
    await page.route("**/api/**", (route) => route.abort());
    await page.goto("https://localhost:3000/");
    expect(
      await page
        .locator(".landing-hero")
        .evaluate((element) => element.getAnimations({ subtree: true }).length),
    ).toBe(0);
    await expect(
      page.getByRole("heading", {
        level: 1,
        name: "Short links. Your control.",
      }),
    ).toBeVisible();
    await expect(
      page.getByRole("article", { name: "Analytics Planned" }),
    ).toBeVisible();
    await expect(page.locator(".landing-hero")).toHaveCSS("opacity", "1");
    await expect(page.locator(".typewriter-copy")).toHaveCSS(
      "clip-path",
      "none",
    );
    await expect(page.locator(".typewriter-cursor-track")).toBeHidden();
    await expect(
      page.getByRole("heading", { name: "Short links. Big possibilities." }),
    ).toBeVisible();
    await page
      .locator(".landing-hero")
      .getByRole("link", { name: "Get started", exact: true })
      .focus();
    await page.keyboard.press("Enter");
    await expect(page).toHaveURL("https://localhost:3000/register");
    // API-host cookies require a browser session check before showing the form.
    await expect(page.getByRole("status")).toHaveText("Checking your session.");
    await expect(page.getByLabel("Email address")).toHaveCount(0);
  } finally {
    await context.close().catch(() => undefined);
  }
});

test("reduced motion suppresses movement while links and theme remain usable", async ({
  page,
}) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.goto("/");
  await expect(page.locator(".typewriter-copy")).toHaveCSS("clip-path", "none");
  await expect(page.locator(".typewriter-cursor-track")).toBeHidden();
  expect(
    await page.locator(".landing-hero").evaluate((element) => {
      const style = getComputedStyle(element);
      return [style.animationName, style.transform, style.scrollBehavior];
    }),
  ).toEqual(["none", "none", "auto"]);
  const action = page
    .locator(".landing-hero")
    .getByRole("link", { name: "Get started", exact: true });
  expect(
    await action.evaluate(
      (element) => getComputedStyle(element).transitionDuration,
    ),
  ).toBe("0s");
  await page.getByRole("button", { name: "Switch to light mode" }).click();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "light");
  await expect(
    page.getByRole("button", { name: /background lights/ }),
  ).toHaveCount(0);
  expect(
    await page
      .locator(".landing-hero")
      .evaluate((element) => element.getAnimations({ subtree: true }).length),
  ).toBe(0);
});

function rightClipInset(element: Element) {
  const sides = getComputedStyle(element).clipPath.match(/[\d.]+/g);
  // Computed CSS serializes equal inset sides as one shorthand value.
  return Number(sides?.[1] ?? sides?.[0]);
}

test("closing CTA types once without shifting its content or replaying on scroll", async ({
  page,
}) => {
  await page.emulateMedia({ reducedMotion: "no-preference" });
  await page.goto("/");
  const cta = page.locator(".landing-closing");
  const writer = cta.locator('.typewriter-effect[data-variant="smooth"]');
  await expect(writer).toHaveCount(1);
  await cta.scrollIntoViewIfNeeded();
  await expect(writer).toHaveAttribute("data-active", "true");
  const box = await cta.boundingBox();
  const copy = writer.locator(".typewriter-copy");
  const initial = await copy.evaluate(
    (element) => getComputedStyle(element).clipPath,
  );
  await expect
    .poll(() => copy.evaluate((element) => getComputedStyle(element).clipPath))
    .not.toBe(initial);
  await expect.poll(() => copy.evaluate(rightClipInset)).toBe(0);
  expect((await cta.boundingBox())!.height).toBe(box!.height);
  await expect(
    cta.getByRole("heading", { name: "Short links. Big possibilities." }),
  ).toBeVisible();
  await page.getByRole("banner").scrollIntoViewIfNeeded();
  await cta.scrollIntoViewIfNeeded();
  expect(await copy.evaluate(rightClipInset)).toBe(0);
});

test("changing reduced motion during the CTA reveal restores static complete text", async ({
  page,
}) => {
  await page.emulateMedia({ reducedMotion: "no-preference" });
  await page.goto("/");
  const cta = page.locator(".landing-closing");
  await cta.scrollIntoViewIfNeeded();
  await expect(cta.locator(".typewriter-effect")).toHaveAttribute(
    "data-active",
    "true",
  );
  await expect
    .poll(() => cta.locator(".typewriter-copy").evaluate(rightClipInset))
    .toBeGreaterThan(0);
  await page.emulateMedia({ reducedMotion: "reduce" });
  await expect(cta.locator(".typewriter-effect")).toHaveAttribute(
    "data-active",
    "false",
    { timeout: 500 },
  );
  await expect(cta.locator(".typewriter-copy")).toHaveCSS("clip-path", "none");
  await expect(cta.locator(".typewriter-cursor-track")).toBeHidden();
  await expect
    .poll(
      () =>
        cta.evaluate(
          (element) =>
            element
              .getAnimations({ subtree: true })
              .filter(
                (animation) =>
                  animation.playState === "running" || animation.pending,
              ).length,
        ),
      { timeout: 500 },
    )
    .toBe(0);
  await page.emulateMedia({ reducedMotion: "no-preference" });
  await expect
    .poll(() => cta.locator(".typewriter-copy").evaluate(rightClipInset))
    .toBe(0);
});

test("enabling motion after viewing the static CTA keeps its complete text", async ({
  page,
}) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.goto("/");
  const cta = page.locator(".landing-closing");
  await cta.scrollIntoViewIfNeeded();
  const writer = cta.locator(".typewriter-effect");
  await expect(writer).toHaveAttribute("data-ready", "true");
  const copy = cta.locator(".typewriter-copy");
  await expect(copy).toHaveCSS("clip-path", "none");
  // Wait for the in-view static presentation to commit its final styles.
  await expect
    .poll(() =>
      copy.evaluate((element) => (element as HTMLElement).style.clipPath),
    )
    .toMatch(/^inset\(0%(?: 0%){0,3}\)$/);
  await page.emulateMedia({ reducedMotion: "no-preference" });
  await expect(writer).toHaveAttribute("data-active", "true", { timeout: 500 });
  expect(await copy.evaluate(rightClipInset)).toBe(0);
});

test("background lights loop independently without playback controls", async ({
  page,
}) => {
  await page.emulateMedia({ reducedMotion: "no-preference" });
  await page.goto("/");
  const header = page.getByRole("banner");
  await expect(
    header.getByRole("link", { name: "Features", exact: true }),
  ).toHaveCount(0);
  await expect(
    header.getByRole("link", { name: "How it works", exact: true }),
  ).toHaveCount(0);
  const hero = page.locator(".landing-hero");
  await expect(hero.getByTestId("auth-url-shortening")).toHaveCount(0);
  await expect(hero.getByRole("figure")).toHaveCount(0);
  await expect(hero.getByRole("textbox")).toHaveCount(0);
  const lights = hero.locator(".landing-route-light");
  await expect(lights).toHaveCount(6);
  const timings = await lights.evaluateAll((elements) =>
    elements.map((element) => {
      const style = getComputedStyle(element);
      return {
        name: style.animationName,
        duration: style.animationDuration,
        delay: style.animationDelay,
      };
    }),
  );
  expect(timings.every((timing) => timing.name === "landing-light-glow")).toBe(
    true,
  );
  expect(new Set(timings.map((timing) => timing.duration)).size).toBe(6);
  expect(new Set(timings.map((timing) => timing.delay)).size).toBe(6);
  expect(
    await hero
      .locator(".landing-hero-copy")
      .evaluate((element) => getComputedStyle(element).animationName),
  ).toBe("none");
  expect(
    await lights.evaluateAll((elements) =>
      elements.map(
        (element) => getComputedStyle(element).animationIterationCount,
      ),
    ),
  ).toEqual(Array(6).fill("infinite"));
  await expect(hero.getByRole("button")).toHaveCount(0);
  const before = await lights.evaluateAll((elements) =>
    elements.map((element) => getComputedStyle(element).opacity),
  );
  await expect
    .poll(() =>
      lights.evaluateAll((elements) =>
        elements.map((element) => getComputedStyle(element).opacity),
      ),
    )
    .not.toEqual(before);
});

test("each random light cycle starts fresh at its dim boundary", async ({
  page,
}) => {
  await page.emulateMedia({ reducedMotion: "no-preference" });
  await page.goto("/");
  await expect(page.locator(".landing-hero-lights")).toHaveAttribute(
    "data-ready",
    "true",
  );
  const light = page.locator(".landing-route-light").first();
  for (const random of [0.25, 0.75, 0.5]) {
    await light.evaluate(async (element) => {
      await element.getAnimations()[0].ready;
    });
    await expect
      .poll(() =>
        light.evaluate((element) =>
          Number(element.getAnimations()[0].currentTime),
        ),
      )
      .toBeGreaterThan(0);
    await light.evaluate((element, random) => {
      Math.random = () => random;
      const animation = element.getAnimations()[0];
      animation.currentTime = Number(animation.effect!.getTiming().duration);
    }, random);
    const expectedDuration = (8 + random * 6) * 1000;
    await expect
      .poll(() =>
        light.evaluate(
          (element) => element.getAnimations()[0].effect!.getTiming().duration,
        ),
      )
      .toBe(expectedDuration);
    const state = await light.evaluate((element) => ({
      time: Number(element.getAnimations()[0].currentTime),
      opacity: Number(getComputedStyle(element).opacity),
    }));
    expect(state.time).toBeLessThan(500);
    expect(state.opacity).toBeLessThan(0.1);
  }
});

async function geometryAndContrast(page: import("@playwright/test").Page) {
  return page.locator(".landing-page").evaluate((root) => {
    function rgb(value: string) {
      const v = value.match(/[\d.]+/g)!.map(Number);
      if (value.startsWith("color(srgb"))
        return [v[0] * 255, v[1] * 255, v[2] * 255, v[3] ?? 1];
      return [v[0], v[1], v[2], v[3] ?? 1];
    }
    function luminance(v: number[]) {
      const c = v
        .slice(0, 3)
        .map((n) =>
          n / 255 <= 0.04045
            ? n / 255 / 12.92
            : ((n / 255 + 0.055) / 1.055) ** 2.4,
        );
      return c[0] * 0.2126 + c[1] * 0.7152 + c[2] * 0.0722;
    }
    const targets = [...root.querySelectorAll("a, button")]
      .filter((el) => {
        const r = el.getBoundingClientRect();
        return r.width > 0 && r.height > 0 && r.bottom > 0;
      })
      .map((el) => {
        const r = el.getBoundingClientRect();
        return {
          name: el.getAttribute("aria-label") ?? el.textContent?.trim(),
          width: r.width,
          height: r.height,
        };
      });
    const contrasts = [
      ...root.querySelectorAll("p, h1, h2, h3, a, button, span"),
    ]
      .filter(
        (el) => el.getBoundingClientRect().width > 0 && el.textContent?.trim(),
      )
      .map((el) => {
        const ancestors: Element[] = [];
        for (
          let current: Element | null = el;
          current;
          current = current.parentElement
        )
          ancestors.unshift(current);
        let bg = [255, 255, 255];
        for (const ancestor of ancestors) {
          const c = rgb(getComputedStyle(ancestor).backgroundColor);
          bg = bg.map((v, i) => c[i] * c[3] + v * (1 - c[3]));
        }
        const style = getComputedStyle(el),
          color = rgb(style.color);
        const fg = bg.map((v, i) => color[i] * color[3] + v * (1 - color[3]));
        const a = luminance(fg),
          b = luminance(bg);
        const ratio = (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
        const size = parseFloat(style.fontSize),
          weight = parseFloat(style.fontWeight);
        return {
          text: el.textContent?.trim().slice(0, 48),
          ratio,
          required: size >= 24 || (size >= 18.66 && weight >= 700) ? 3 : 4.5,
        };
      });
    const h1 = root.querySelector("h1")!;
    const range = document.createRange();
    range.selectNodeContents(h1);
    return {
      overflow: document.documentElement.scrollWidth > innerWidth,
      targets,
      contrasts,
      lines: new Set(
        [...range.getClientRects()]
          .filter((rect) => rect.width > 0)
          .map((rect) => Math.round(rect.top)),
      ).size,
    };
  });
}

for (const theme of ["dark", "light"] as const)
  for (const viewport of viewports) {
    test(`${theme} ${viewport.width}px fits, has readable contrast and 44px targets`, async ({
      page,
    }) => {
      await page.setViewportSize(viewport);
      await page.addInitScript(
        (theme) => localStorage.setItem("tinyroute-color-theme", theme),
        theme,
      );
      const errors: string[] = [];
      page.on("pageerror", (error) => errors.push(error.message));
      page.on("console", (message) => {
        if (message.type() === "error") errors.push(message.text());
      });
      await page.goto("/");
      await expect(page.locator("html")).toHaveAttribute("data-theme", theme);
      await page.evaluate(async () => {
        await document.fonts.ready;
        await Promise.all(
          document
            .getAnimations()
            .filter(
              (animation) =>
                animation.effect?.getTiming().iterations !== Infinity,
            )
            .map((animation) => animation.finished),
        );
      });
      const metrics = await geometryAndContrast(page);
      expect((await page.getByRole("banner").boundingBox())!.height).toBe(
        viewport.width >= 1024 ? 80 : 64,
      );
      expect(metrics.overflow).toBe(false);
      expect(metrics.lines).toBeLessThanOrEqual(2.05);
      expect(
        metrics.targets.filter((t) => t.width < 43.99 || t.height < 43.99),
      ).toEqual([]);
      expect(metrics.contrasts.filter((c) => c.ratio < c.required)).toEqual([]);
      const wordmark = await page.locator("footer").evaluate((footer) => {
        const textBounds = [
          ...footer.querySelectorAll(".landing-footer-wordmark > div"),
        ].map((layer) => {
          const range = document.createRange();
          range.selectNodeContents(layer);
          const { x, y, width, height } = range.getBoundingClientRect();
          return { x, y, width, height };
        });
        const layerBounds = [
          ...footer.querySelectorAll(".landing-footer-wordmark > div"),
        ].map((layer) => {
          const { x, y, width, height } = layer.getBoundingClientRect();
          return { x, y, width, height };
        });
        return {
          footerCenter:
            footer.getBoundingClientRect().left +
            footer.getBoundingClientRect().width / 2,
          textBounds,
          layerBounds,
        };
      });
      expect(
        wordmark.textBounds[0].x + wordmark.textBounds[0].width / 2,
      ).toBeCloseTo(wordmark.footerCenter, 0);
      expect(wordmark.textBounds[1]).toEqual(wordmark.textBounds[0]);
      for (const [index, text] of wordmark.textBounds.entries()) {
        const layer = wordmark.layerBounds[index];
        expect(text.x - layer.x).toBeGreaterThanOrEqual(4);
        expect(
          layer.x + layer.width - text.x - text.width,
        ).toBeGreaterThanOrEqual(4);
        expect(text.y - layer.y).toBeGreaterThanOrEqual(4);
        expect(
          layer.y + layer.height - text.y - text.height,
        ).toBeGreaterThanOrEqual(4);
      }
      const cta = await page
        .locator(".landing-hero")
        .getByRole("link", { name: "Get started", exact: true })
        .boundingBox();
      expect(cta!.y).toBeGreaterThanOrEqual(0);
      expect(cta!.y + cta!.height).toBeLessThanOrEqual(viewport.height);
      console.log(
        JSON.stringify({
          theme,
          width: viewport.width,
          lines: metrics.lines,
          ctaBottom: cta!.y + cta!.height,
          minimumContrast: Math.min(...metrics.contrasts.map((c) => c.ratio)),
          minimumTarget: Math.min(
            ...metrics.targets.flatMap((t) => [t.width, t.height]),
          ),
        }),
      );
      const folder = resolve("../docs/spec/landing-page/evidence");
      await mkdir(folder, { recursive: true });
      await page.locator(".landing-closing").scrollIntoViewIfNeeded();
      await expect
        .poll(() => page.locator(".typewriter-copy").evaluate(rightClipInset))
        .toBe(0);
      const ctaTextFits = await page
        .locator(".landing-closing")
        .evaluate((section) => {
          const range = document.createRange();
          range.selectNodeContents(section.querySelector(".typewriter-copy")!);
          const text = range.getBoundingClientRect();
          const bounds = section.getBoundingClientRect();
          const styles = getComputedStyle(section);
          const inset =
            parseFloat(styles.paddingLeft) + parseFloat(styles.borderLeftWidth);
          return (
            text.left >= bounds.left + inset &&
            text.right <= bounds.right - inset
          );
        });
      expect(ctaTextFits).toBe(true);
      await page.getByRole("banner").scrollIntoViewIfNeeded();
      await page.screenshot({
        path: resolve(folder, `landing-${viewport.width}-${theme}.png`),
        fullPage: true,
        animations: "disabled",
        style: "nextjs-portal { display: none !important; }",
      });
      if (viewport.width === 1440) {
        await page.locator("footer").screenshot({
          path: resolve(folder, `footer-${theme}.png`),
          animations: "disabled",
          style: "nextjs-portal { display: none !important; }",
        });
      }
      if (viewport.width < 1024) {
        await page.getByRole("button", { name: "Menu", exact: true }).click();
        const expanded = await geometryAndContrast(page);
        expect(expanded.overflow).toBe(false);
        expect(
          expanded.targets.filter((t) => t.width < 43.99 || t.height < 43.99),
        ).toEqual([]);
        expect(expanded.contrasts.filter((c) => c.ratio < c.required)).toEqual(
          [],
        );
        if (viewport.width === 320) {
          await page.keyboard.press("Tab");
          await expect(
            page
              .getByRole("navigation", { name: "Mobile navigation" })
              .getByRole("link", { name: "Sign in", exact: true }),
          ).toBeFocused();
          await page.screenshot({
            path: resolve(folder, `navigation-320-${theme}.png`),
            animations: "disabled",
            style: "nextjs-portal { display: none !important; }",
          });
        }
      }
      expect(errors).toEqual([]);
    });
  }
