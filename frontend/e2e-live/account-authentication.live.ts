import { SmtpSink } from "./fixtures/smtp-sink";

import { expect, test } from "@playwright/test";

test.setTimeout(120_000);

test("registration, persisted session, logout, reset, login, and deletion cross the real backend", async ({
  page,
}) => {
  const smtp = new SmtpSink();
  await smtp.start(Number(process.env.TINYROUTE_E2E_SMTP_PORT ?? "1025"));
  const email = `live-${Date.now()}@example.com`;
  const originalPassword = "live-browser-password-123";
  const replacementPassword = "live-browser-password-456";

  try {
    await page.goto("/register");
    await page.getByLabel("Email address").fill(email);
    await page.getByLabel("Password", { exact: true }).fill(originalPassword);
    const otpMail = smtp.nextMessage();
    await page.getByRole("button", { name: "Create account" }).click();
    await expect(
      page.getByRole("heading", { name: "Verify your email" }),
    ).toBeVisible();
    const otp = (await otpMail).match(/\b(\d{6})\b/)?.[1];
    expect(otp).toBeTruthy();
    await page.getByLabel("Verification code").fill(otp!);
    await page.getByRole("button", { name: "Verify email" }).click();

    await expect(page).toHaveURL(/\/settings$/);
    await expect(page.getByText(`Signed in as ${email}.`)).toBeVisible();
    await page.reload();
    await expect(page.getByText(`Signed in as ${email}.`)).toBeVisible();
    await page.context().clearCookies({ name: "__Host-tinyroute_access" });
    const persistedRefresh = page.waitForResponse(
      (response) =>
        response.url().endsWith("/api/auth/refresh") &&
        response.status() === 200,
    );
    await page.reload();
    await persistedRefresh;
    await expect(page.getByText(`Signed in as ${email}.`)).toBeVisible();

    await page.goto("/login");
    await expect(page.getByRole("button", { name: "Sign out" })).toBeVisible();
    await page.context().clearCookies({ name: "__Host-tinyroute_access" });
    const logoutRefresh = page.waitForResponse(
      (response) =>
        response.url().endsWith("/api/auth/refresh") &&
        response.status() === 200,
    );
    await page.getByRole("button", { name: "Sign out" }).click();
    await logoutRefresh;
    await expect(
      page.getByRole("heading", { name: "Sign in to TinyRoute" }),
    ).toBeVisible();

    await page.goto("/password-reset");
    await page.getByLabel("Email address").fill(email);
    const resetMail = smtp.nextMessage();
    await page.getByRole("button", { name: "Send reset link" }).click();
    const resetToken = (await resetMail).match(
      /#token=([A-Za-z0-9_-]{43})/,
    )?.[1];
    expect(resetToken).toBeTruthy();
    await page.goto(`/password-reset/confirm#token=${resetToken}`);
    await page
      .getByLabel("New password", { exact: true })
      .fill(replacementPassword);
    await page.getByLabel("Confirm new password").fill(replacementPassword);
    await page.getByRole("button", { name: "Reset password" }).click();
    await expect(page.getByRole("status")).toContainText(
      "Your password has been reset",
    );

    await page.goto("/login");
    await page.getByLabel("Email address").fill(email);
    await page
      .getByLabel("Password", { exact: true })
      .fill(replacementPassword);
    await page.getByRole("button", { name: "Sign in", exact: true }).click();
    await expect(
      page.getByRole("heading", { name: "You’re signed in" }),
    ).toBeVisible();
    await page.getByRole("link", { name: "Account settings" }).click();

    await page.getByRole("button", { name: "Delete account" }).click();
    await page.getByRole("button", { name: "Confirm deletion" }).click();
    await expect(
      page.getByRole("heading", { name: "Account deleted" }),
    ).toBeVisible();
  } finally {
    await smtp.stop();
  }
});
