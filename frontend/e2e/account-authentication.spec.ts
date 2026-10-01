import { expect, test, type Page } from "@playwright/test";

type StubResponse = { status: number; body?: unknown };

const account = { authenticated: true, user: { email: "browser@example.com" } };
const failure = (code: string, message: string) => ({
  error: { code, message, requestId: "browser-fixture" },
});

async function stubAuthApi(
  page: Page,
  respond: (
    path: string,
    method: string,
    csrf: string | undefined,
  ) => StubResponse,
) {
  await page.route("https://localhost:8443/api/auth/**", async (route) => {
    const request = route.request();
    const headers = {
      "Access-Control-Allow-Origin": "https://localhost:3000",
      "Access-Control-Allow-Credentials": "true",
      "Access-Control-Allow-Headers": "Content-Type, X-CSRF-TOKEN, Accept",
      "Access-Control-Allow-Methods": "GET, POST, DELETE, OPTIONS",
      "Content-Type": "application/json",
      "Cache-Control": "no-store",
    };
    if (request.method() === "OPTIONS") {
      await route.fulfill({ status: 204, headers });
      return;
    }
    const response = respond(
      new URL(request.url()).pathname,
      request.method(),
      request.headers()["x-csrf-token"],
    );
    await route.fulfill({
      status: response.status,
      headers,
      body:
        response.body === undefined ? undefined : JSON.stringify(response.body),
    });
  });
}

test("password sign-in persists on reload and sign-out clears the session", async ({
  page,
}) => {
  let signedIn = false;
  await stubAuthApi(page, (path, method, csrf) => {
    if (path.endsWith("/csrf"))
      return { status: 200, body: { csrfToken: "browser-csrf" } };
    if (path.endsWith("/me"))
      return signedIn
        ? { status: 200, body: account }
        : {
            status: 401,
            body: failure(
              "AUTHENTICATION_FAILED",
              "Authentication is invalid or expired.",
            ),
          };
    if (path.endsWith("/refresh"))
      return {
        status: 401,
        body: failure(
          "AUTHENTICATION_FAILED",
          "Authentication is invalid or expired.",
        ),
      };
    if (
      path.endsWith("/login") &&
      method === "POST" &&
      csrf === "browser-csrf"
    ) {
      signedIn = true;
      return { status: 200, body: account };
    }
    if (
      path.endsWith("/logout") &&
      method === "POST" &&
      csrf === "browser-csrf"
    ) {
      signedIn = false;
      return { status: 204 };
    }
    return {
      status: 500,
      body: failure("SERVICE_UNAVAILABLE", "Fixture mismatch"),
    };
  });

  await page.goto("/login");
  await page.getByLabel("Email address").fill("browser@example.com");
  await page.getByLabel("Password").fill("browser-password-123");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "You’re signed in" }),
  ).toBeVisible();
  await page.reload();
  await expect(
    page.getByText("Signed in as browser@example.com."),
  ).toBeVisible();
  await page.getByRole("button", { name: "Sign out" }).click();
  await expect(
    page.getByRole("heading", { name: "Sign in to TinyRoute" }),
  ).toBeVisible();
});

test("account deletion requires confirmation and keeps the session on failure", async ({
  page,
}) => {
  let signedIn = true;
  let deleteAttempts = 0;
  await stubAuthApi(page, (path, method, csrf) => {
    if (path.endsWith("/csrf"))
      return { status: 200, body: { csrfToken: "browser-csrf" } };
    if (path.endsWith("/me"))
      return signedIn
        ? { status: 200, body: account }
        : {
            status: 401,
            body: failure(
              "AUTHENTICATION_FAILED",
              "Authentication is invalid or expired.",
            ),
          };
    if (
      path.endsWith("/account") &&
      method === "DELETE" &&
      csrf === "browser-csrf"
    ) {
      deleteAttempts += 1;
      if (deleteAttempts === 1)
        return {
          status: 503,
          body: failure("SERVICE_UNAVAILABLE", "Please try again."),
        };
      signedIn = false;
      return { status: 204 };
    }
    return {
      status: 500,
      body: failure("SERVICE_UNAVAILABLE", "Fixture mismatch"),
    };
  });

  await page.goto("/settings");
  await expect(
    page.getByRole("heading", { name: "Account settings" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Delete account" }).click();
  await expect(
    page.getByRole("heading", { name: "Confirm account deletion" }),
  ).toBeFocused();
  expect(deleteAttempts).toBe(0);
  await page.keyboard.press("Tab");
  await expect(
    page.getByRole("button", { name: "Confirm deletion" }),
  ).toBeFocused();
  await page.keyboard.press("Tab");
  await expect(page.getByRole("button", { name: "Cancel" })).toBeFocused();
  await page.keyboard.press("Enter");
  await expect(
    page.getByRole("button", { name: "Delete account" }),
  ).toBeFocused();
  expect(deleteAttempts).toBe(0);
  await page.getByRole("button", { name: "Delete account" }).click();
  await page.getByRole("button", { name: "Confirm deletion" }).click();
  await expect(
    page.getByText("We couldn't confirm deletion. Please try again."),
  ).toBeVisible();
  await expect(
    page.getByText("Signed in as browser@example.com."),
  ).toBeVisible();
  await page.getByRole("button", { name: "Confirm deletion" }).click();
  await expect(
    page.getByRole("heading", { name: "Account deleted" }),
  ).toBeVisible();
  expect(deleteAttempts).toBe(2);
});

test("a persisted refresh session restores settings and expiry denies the protected screen", async ({
  page,
}) => {
  let accessValid = false;
  let refreshValid = true;
  let refreshAttempts = 0;
  await stubAuthApi(page, (path, method, csrf) => {
    if (path.endsWith("/csrf"))
      return { status: 200, body: { csrfToken: "browser-csrf" } };
    if (path.endsWith("/me"))
      return accessValid
        ? { status: 200, body: account }
        : {
            status: 401,
            body: failure(
              "AUTHENTICATION_FAILED",
              "Authentication is invalid or expired.",
            ),
          };
    if (
      path.endsWith("/refresh") &&
      method === "POST" &&
      csrf === "browser-csrf"
    ) {
      refreshAttempts += 1;
      accessValid = refreshValid;
      return refreshValid
        ? { status: 200, body: account }
        : {
            status: 401,
            body: failure(
              "AUTHENTICATION_FAILED",
              "Authentication is invalid or expired.",
            ),
          };
    }
    return {
      status: 500,
      body: failure("SERVICE_UNAVAILABLE", "Fixture mismatch"),
    };
  });

  await page.goto("/settings");
  await expect(
    page.getByRole("heading", { name: "Account settings" }),
  ).toBeVisible();
  expect(refreshAttempts).toBe(1);

  accessValid = false;
  refreshValid = false;
  await page.reload();
  await expect(page.getByText("Sign in to manage your account.")).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Delete account" }),
  ).toHaveCount(0);
  expect(refreshAttempts).toBe(2);
});

test("registration requires an OTP before displaying a verified session", async ({
  page,
}) => {
  let verificationRequests = 0;
  await stubAuthApi(page, (path, method, csrf) => {
    if (path.endsWith("/csrf"))
      return { status: 200, body: { csrfToken: "browser-csrf" } };
    if (
      path.endsWith("/register") &&
      method === "POST" &&
      csrf === "browser-csrf"
    )
      return { status: 202, body: { status: "PENDING_VERIFICATION" } };
    if (
      path.endsWith("/register/verify") &&
      method === "POST" &&
      csrf === "browser-csrf"
    ) {
      verificationRequests += 1;
      return { status: 201, body: account };
    }
    return {
      status: 500,
      body: failure("SERVICE_UNAVAILABLE", "Fixture mismatch"),
    };
  });

  await page.goto("/register");
  await page.getByLabel("Email address").fill("browser@example.com");
  await page
    .getByLabel("Password", { exact: true })
    .fill("browser-password-123");
  await page.getByRole("button", { name: "Create account" }).click();
  await expect(
    page.getByRole("heading", { name: "Verify your email" }),
  ).toBeVisible();
  expect(verificationRequests).toBe(0);
  await page.getByLabel("Verification code").fill("123456");
  await page.getByRole("button", { name: "Verify email" }).click();
  await expect(page).toHaveURL(/\/settings$/);
  await expect(
    page.getByRole("heading", { name: "Account settings" }),
  ).toBeVisible();
  await expect(
    page.getByText("Signed in as browser@example.com."),
  ).toBeVisible();
  await expect(page.getByLabel("Verification code")).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Resend code" })).toHaveCount(
    0,
  );
  expect(verificationRequests).toBe(1);
});

test("password reset uses a fragment and handles a consumed token safely", async ({
  page,
}) => {
  let confirmAttempts = 0;
  await stubAuthApi(page, (path, method, csrf) => {
    if (path.endsWith("/csrf"))
      return { status: 200, body: { csrfToken: "browser-csrf" } };
    if (
      path.endsWith("/password-reset") &&
      method === "POST" &&
      csrf === "browser-csrf"
    )
      return { status: 202, body: { status: "ACCEPTED" } };
    if (
      path.endsWith("/password-reset/confirm") &&
      method === "POST" &&
      csrf === "browser-csrf"
    ) {
      confirmAttempts += 1;
      return {
        status: 400,
        body: failure(
          "RESET_TOKEN_INVALID",
          "This reset link is invalid or expired.",
        ),
      };
    }
    return {
      status: 500,
      body: failure("SERVICE_UNAVAILABLE", "Fixture mismatch"),
    };
  });

  await page.goto("/password-reset");
  await page.getByLabel("Email address").fill("browser@example.com");
  await page.getByRole("button", { name: "Send reset link" }).click();
  await expect(page.getByRole("status")).toContainText(
    "If an account uses that email address",
  );
  await page.goto("/password-reset/confirm#token=disposable-browser-token");
  await expect(page).toHaveURL(/\/password-reset\/confirm$/);
  await page
    .getByLabel("New password", { exact: true })
    .fill("new-browser-password-123");
  await page
    .getByLabel("Confirm new password")
    .fill("new-browser-password-123");
  await page.getByRole("button", { name: "Reset password" }).click();
  await expect(
    page.getByText(
      "This reset link is invalid or expired. Request a new link.",
    ),
  ).toBeVisible();
  expect(confirmAttempts).toBe(1);
});

test("a successful password reset denies a previously signed-in browser", async ({
  browser,
  page,
}) => {
  let oldSessionValid = true;
  let resetAttempts = 0;
  const otherContext = await browser.newContext({
    baseURL: "https://localhost:3000",
    ignoreHTTPSErrors: true,
  });
  try {
    const otherPage = await otherContext.newPage();
    await stubAuthApi(otherPage, (path) => {
      if (path.endsWith("/csrf"))
        return { status: 200, body: { csrfToken: "browser-csrf" } };
      if (path.endsWith("/me"))
        return oldSessionValid
          ? { status: 200, body: account }
          : {
              status: 401,
              body: failure("AUTHENTICATION_FAILED", "Session expired"),
            };
      if (path.endsWith("/refresh"))
        return {
          status: 401,
          body: failure("AUTHENTICATION_FAILED", "Session expired"),
        };
      return {
        status: 500,
        body: failure("SERVICE_UNAVAILABLE", "Fixture mismatch"),
      };
    });
    await otherPage.goto("/settings");
    await expect(
      otherPage.getByRole("heading", { name: "Account settings" }),
    ).toBeVisible();

    await stubAuthApi(page, (path, method, csrf) => {
      if (path.endsWith("/csrf"))
        return { status: 200, body: { csrfToken: "browser-csrf" } };
      if (
        path.endsWith("/password-reset/confirm") &&
        method === "POST" &&
        csrf === "browser-csrf"
      ) {
        resetAttempts += 1;
        oldSessionValid = false;
        return { status: 204 };
      }
      return {
        status: 500,
        body: failure("SERVICE_UNAVAILABLE", "Fixture mismatch"),
      };
    });
    await page.goto("/password-reset/confirm#token=disposable-browser-token");
    await page
      .getByLabel("New password", { exact: true })
      .fill("new-browser-password-123");
    await page
      .getByLabel("Confirm new password")
      .fill("new-browser-password-123");
    await page.getByRole("button", { name: "Reset password" }).click();
    await expect(page.getByRole("status")).toContainText(
      "Your password has been reset",
    );
    expect(resetAttempts).toBe(1);

    await otherPage.reload();
    await expect(
      otherPage.getByText("Sign in to manage your account."),
    ).toBeVisible();
    await expect(
      otherPage.getByRole("button", { name: "Delete account" }),
    ).toHaveCount(0);
  } finally {
    await otherContext.close();
  }
});

test("Google handoff reports a fixed failure without exposing provider details", async ({
  page,
}) => {
  await stubAuthApi(page, (path) => {
    if (path.endsWith("/csrf"))
      return { status: 200, body: { csrfToken: "browser-csrf" } };
    if (path.endsWith("/me") || path.endsWith("/refresh"))
      return {
        status: 401,
        body: failure(
          "AUTHENTICATION_FAILED",
          "Authentication is invalid or expired.",
        ),
      };
    return {
      status: 500,
      body: failure("SERVICE_UNAVAILABLE", "Fixture mismatch"),
    };
  });
  await page.route(
    "https://localhost:8443/api/auth/google/start",
    async (route) => {
      await route.fulfill({
        status: 302,
        headers: {
          Location: "https://localhost:3000/login?error=oauth_failed",
        },
      });
    },
  );

  await page.goto("/login");
  await page.getByRole("link", { name: "Continue with Google" }).click();
  await expect(page).toHaveURL(/\/login\?error=oauth_failed$/);
  await expect(
    page.getByText("Google sign-in could not be completed. Please try again."),
  ).toBeVisible();
});

test("Google success handoff displays the authenticated account", async ({
  page,
}) => {
  let signedIn = false;
  await stubAuthApi(page, (path) => {
    if (path.endsWith("/me"))
      return signedIn
        ? { status: 200, body: account }
        : {
            status: 401,
            body: failure("AUTHENTICATION_FAILED", "Session expired"),
          };
    if (path.endsWith("/refresh"))
      return {
        status: 401,
        body: failure("AUTHENTICATION_FAILED", "Session expired"),
      };
    if (path.endsWith("/csrf"))
      return { status: 200, body: { csrfToken: "browser-csrf" } };
    return {
      status: 500,
      body: failure("SERVICE_UNAVAILABLE", "Fixture mismatch"),
    };
  });
  await page.route("https://localhost:8443/api/auth/google/start", (route) => {
    signedIn = true;
    return route.fulfill({
      status: 302,
      headers: { Location: "https://localhost:3000/settings" },
    });
  });

  await page.goto("/login");
  await page.getByRole("link", { name: "Continue with Google" }).click();
  await expect(page).toHaveURL(/\/settings$/);
  await expect(
    page.getByRole("heading", { name: "Account settings" }),
  ).toBeVisible();
  await expect(
    page.getByText("Signed in as browser@example.com."),
  ).toBeVisible();
});

test("auth screens fit the required widths in light and dark themes", async ({
  page,
}) => {
  await stubAuthApi(page, (path) => {
    if (path.endsWith("/me") || path.endsWith("/refresh"))
      return {
        status: 401,
        body: failure(
          "AUTHENTICATION_FAILED",
          "Authentication is invalid or expired.",
        ),
      };
    if (path.endsWith("/csrf"))
      return { status: 200, body: { csrfToken: "browser-csrf" } };
    return {
      status: 500,
      body: failure("SERVICE_UNAVAILABLE", "Fixture mismatch"),
    };
  });

  for (const theme of ["light", "dark"] as const) {
    await page.goto("/login");
    if ((await page.locator("html").getAttribute("data-theme")) !== theme) {
      await page
        .getByRole("button", { name: `Switch to ${theme} mode` })
        .click();
    }
    await expect(page.locator("html")).toHaveAttribute("data-theme", theme);
    await page.reload();
    await expect(page.locator("html")).toHaveAttribute("data-theme", theme);
    for (const width of [320, 768, 1024, 1440]) {
      await page.setViewportSize({ width, height: 900 });
      await page.goto("/login");
      await expect(
        page.getByRole("heading", { name: "Sign in to TinyRoute" }),
      ).toBeVisible();
      const { scrollWidth, clientWidth } = await page.evaluate(() => ({
        scrollWidth: document.documentElement.scrollWidth,
        clientWidth: document.documentElement.clientWidth,
      }));
      expect(scrollWidth, `${theme} at ${width}px`).toBeLessThanOrEqual(
        clientWidth,
      );
    }
  }
});
