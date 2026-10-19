import { expect, test, type Page } from "@playwright/test";

const account = {
  authenticated: true,
  user: { email: "idle-session@example.com" },
};
const failure = (code: string) => ({
  error: { code, message: "Safe fixture error", requestId: "idle-fixture" },
});

async function stubApi(
  page: Page,
  respond: (
    path: string,
    csrf: string | undefined,
  ) => {
    status: number;
    body: unknown;
  },
) {
  // Support both the standalone API and the documented local HTTPS rewrite.
  // Every API request is fulfilled here; no real account is read or changed.
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const headers = {
      "Access-Control-Allow-Origin": new URL(page.url()).origin,
      "Access-Control-Allow-Credentials": "true",
      "Access-Control-Allow-Headers": "Content-Type, X-CSRF-TOKEN, Accept",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Cache-Control": "no-store",
    };
    if (request.method() === "OPTIONS") {
      await route.fulfill({ status: 204, headers });
      return;
    }
    const path = new URL(request.url()).pathname;
    const response =
      path === "/api/links"
        ? { status: 200, body: { items: [], nextCursor: null } }
        : respond(path, request.headers()["x-csrf-token"]);
    await route.fulfill({
      status: response.status,
      json: response.body,
      headers,
    });
  });
}

test("an idle tab restores its session after two hours and a shared CSRF change", async ({
  page,
}) => {
  let accessValid = false;
  let csrfToken = "initial-csrf";
  const refreshHeaders: (string | undefined)[] = [];
  await page.clock.install();
  await stubApi(page, (path, csrf) => {
    if (path.endsWith("/csrf")) return { status: 200, body: { csrfToken } };
    if (path.endsWith("/me"))
      return accessValid
        ? { status: 200, body: account }
        : { status: 401, body: failure("AUTHENTICATION_FAILED") };
    if (path.endsWith("/refresh")) {
      refreshHeaders.push(csrf);
      if (csrf !== csrfToken)
        return { status: 403, body: failure("CSRF_INVALID") };
      accessValid = true;
      return { status: 200, body: account };
    }
    return { status: 500, body: failure("SERVICE_UNAVAILABLE") };
  });

  await page.goto("/links");
  await expect(
    page.getByRole("heading", { name: "Create a short link" }),
  ).toBeVisible();
  await page.clock.fastForward("02:00:00");
  accessValid = false;
  csrfToken = "changed-in-another-tab";
  await page.getByRole("link", { name: "Account settings" }).first().click();

  await expect
    .poll(() => refreshHeaders)
    .toEqual(["initial-csrf", "initial-csrf", "changed-in-another-tab"]);
  await expect(
    page.getByText("Signed in as idle-session@example.com."),
  ).toBeVisible();
  await expect(page.getByRole("main").getByRole("alert")).toHaveCount(0);
});

test("sign-in recovers a stale CSRF token without asking for credentials again", async ({
  page,
}) => {
  let accessValid = false;
  let csrfToken = "initial-csrf";
  let rejectCredentials = true;
  const loginHeaders: (string | undefined)[] = [];
  await stubApi(page, (path, csrf) => {
    if (path.endsWith("/csrf")) return { status: 200, body: { csrfToken } };
    if (path.endsWith("/me"))
      return accessValid
        ? { status: 200, body: account }
        : { status: 401, body: failure("AUTHENTICATION_FAILED") };
    if (path.endsWith("/refresh"))
      return { status: 401, body: failure("AUTHENTICATION_FAILED") };
    if (path.endsWith("/login")) {
      loginHeaders.push(csrf);
      if (csrf !== csrfToken)
        return { status: 403, body: failure("CSRF_INVALID") };
      if (rejectCredentials)
        return { status: 401, body: failure("AUTHENTICATION_FAILED") };
      accessValid = true;
      return { status: 200, body: account };
    }
    return { status: 500, body: failure("SERVICE_UNAVAILABLE") };
  });

  await page.goto("/login");
  await page.getByLabel("Email address").fill(account.user.email);
  await page.getByLabel("Password", { exact: true }).fill("fixture-password");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page.getByRole("main").getByRole("alert")).toContainText(
    "Check your email and password",
  );

  csrfToken = "changed-in-another-tab";
  rejectCredentials = false;
  await page.getByLabel("Password", { exact: true }).fill("fixture-password");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();

  await expect(page).toHaveURL(/\/links$/);
  await expect(page.getByRole("main").getByRole("alert")).toHaveCount(0);
  expect(loginHeaders).toEqual([
    "initial-csrf",
    "initial-csrf",
    "changed-in-another-tab",
  ]);
});

test("a service outage shows one accurate sign-in error", async ({ page }) => {
  let sessionChecks = 0;
  await stubApi(page, (path) => {
    if (path.endsWith("/me")) sessionChecks += 1;
    return path.endsWith("/csrf")
      ? { status: 200, body: { csrfToken: "fixture-csrf" } }
      : { status: 503, body: failure("SERVICE_UNAVAILABLE") };
  });

  await page.goto("/login");
  await page.getByLabel("Email address").fill(account.user.email);
  expect(sessionChecks).toBe(1);
  await page.getByLabel("Password", { exact: true }).fill("fixture-password");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();

  await expect(page.getByRole("main").getByRole("alert")).toHaveCount(1);
  await expect(page.getByRole("main").getByRole("alert")).toHaveText(
    "Sign-in is temporarily unavailable. Please try again.",
  );
});

test("a missing API route stops checking immediately without retry backoff", async ({
  page,
}) => {
  let sessionChecks = 0;
  await stubApi(page, () => {
    sessionChecks += 1;
    return { status: 404, body: undefined };
  });

  await page.goto("/login");
  await expect(page.getByRole("main").getByRole("alert")).toHaveText(
    "We couldn’t check your session. Please refresh and try again.",
  );
  await expect(page.getByLabel("Email address")).toBeVisible();
  expect(sessionChecks).toBe(1);
});
