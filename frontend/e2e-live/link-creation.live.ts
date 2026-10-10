import { expect, test, request as api } from "@playwright/test";
import { SmtpSink } from "./fixtures/smtp-sink";

test.setTimeout(120_000);
test("real signed-in creation, clipboard, anonymous state/expiry, and account deletion", async ({
  page,
  context,
}) => {
  const token = process.env.TINYROUTE_VERIFICATION_TOKEN;
  if (!token)
    throw new Error(
      "Start the disposable verification application and supply its private token",
    );
  const apiBase =
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "https://localhost:8443";
  const smtp = new SmtpSink();
  await smtp.start(Number(process.env.TINYROUTE_E2E_SMTP_PORT ?? "1025"));
  const anonymous = await api.newContext({ ignoreHTTPSErrors: true });
  const destination = "https://example.com/docs?q=java#setup",
    alias = `LiveCase${Date.now()}`,
    email = `link-live-${Date.now()}@example.com`;
  async function state(
    code: string,
    status: "ACTIVE" | "DISABLED" | "DELETED",
    expiresAt: string | null = null,
  ) {
    const response = await anonymous.post(
      `${apiBase}/__verification/links/state`,
      {
        headers: { "X-Verification-Token": token! },
        data: { code, status, expiresAt },
      },
    );
    expect(response.status()).toBe(204);
  }
  async function redirect(url: string, status: number) {
    const response = await anonymous.get(url, { maxRedirects: 0 });
    expect(response.status()).toBe(status);
    expect(response.headers()["cache-control"]).toBe("no-store");
    if (status === 302) expect(response.headers().location).toBe(destination);
    else {
      expect(response.headers().location).toBeUndefined();
      expect(await response.text()).not.toContain("example.com");
    }
    return response;
  }
  try {
    await context.grantPermissions(["clipboard-read", "clipboard-write"]);
    await page.goto("/register");
    await page.getByLabel("Email address").fill(email);
    await page
      .getByLabel("Password", { exact: true })
      .fill("disposable-browser-password-123");
    const mail = smtp.nextMessage();
    await page.getByRole("button", { name: "Create account" }).click();
    await expect(
      page.getByRole("heading", { name: "Verify your email" }),
    ).toBeVisible();
    const otp = (await mail).match(/\b(\d{6})\b/)?.[1];
    expect(otp).toBeTruthy();
    await page.getByLabel("Verification code").fill(otp!);
    await page.getByRole("button", { name: "Verify email" }).click();
    await expect(page).toHaveURL(/\/settings$/);
    await page.goto("/links");
    await page.getByLabel("HTTPS destination").fill(destination);
    await page.getByLabel("Custom alias (optional)").fill(alias);
    const creation = page.waitForResponse(
      (response) =>
        response.url().endsWith("/api/links") &&
        response.request().method() === "POST",
    );
    await page.getByRole("button", { name: "Create short link" }).click();
    const created = await creation;
    expect(created.status()).toBe(201);
    const link = (await created.json()) as {
      code: string;
      shortUrl: string;
      expiresAt: string | null;
    };
    expect(link.code).toBe(alias);
    expect(link.expiresAt).toBeNull();
    await expect(page.getByLabel("Short URL")).toHaveValue(link.shortUrl);
    await page.getByRole("button", { name: "Copy short link" }).click();
    await expect(page.getByRole("status")).toContainText(
      "Copied to clipboard.",
    );
    expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(
      link.shortUrl,
    );
    await redirect(link.shortUrl, 302);
    const head = await anonymous.head(link.shortUrl, { maxRedirects: 0 });
    expect(head.status()).toBe(302);
    expect(head.headers().location).toBe(destination);
    expect(await head.body()).toHaveLength(0);
    await redirect(link.shortUrl.replace(alias, alias.toLowerCase()), 404);
    await page.getByRole("button", { name: "Create short link" }).click();
    await expect(page.locator("main").getByRole("alert")).toContainText(
      "different custom alias",
    );
    await state(alias, "DISABLED");
    await expect
      .poll(
        async () =>
          (await anonymous.get(link.shortUrl, { maxRedirects: 0 })).status(),
        { timeout: 6500, intervals: [100, 250, 500] },
      )
      .toBe(403);
    await redirect(link.shortUrl, 403);
    await state(alias, "DELETED");
    await expect
      .poll(
        async () =>
          (await anonymous.get(link.shortUrl, { maxRedirects: 0 })).status(),
        { timeout: 6500, intervals: [100, 250, 500] },
      )
      .toBe(404);
    await redirect(link.shortUrl, 404);
    const csrf = await page.request.get(`${apiBase}/api/auth/csrf`);
    const csrfToken = ((await csrf.json()) as { csrfToken: string }).csrfToken;
    const expiresAt = new Date(Date.now() + 2500).toISOString();
    const expiring = await page.request.post(`${apiBase}/api/links`, {
      headers: { "X-CSRF-TOKEN": csrfToken },
      data: { destinationUrl: destination, expiresAt },
    });
    expect(expiring.status()).toBe(201);
    const expiryLink = (await expiring.json()) as { shortUrl: string };
    await redirect(expiryLink.shortUrl, 302);
    await expect
      .poll(() => Date.now(), { timeout: 4000, intervals: [50] })
      .toBeGreaterThanOrEqual(Date.parse(expiresAt));
    await redirect(expiryLink.shortUrl, 404);
    await page.getByLabel("Custom alias (optional)").fill(`${alias}Delete`);
    const deletionCreation = page.waitForResponse(
      (response) =>
        response.url().endsWith("/api/links") && response.status() === 201,
    );
    await page.getByRole("button", { name: "Create short link" }).click();
    const deletionLink = (await (await deletionCreation).json()) as {
      shortUrl: string;
    };
    await redirect(deletionLink.shortUrl, 302);
    await page.getByRole("link", { name: "Account settings" }).click();
    await page.getByRole("button", { name: "Delete account" }).click();
    await page.getByRole("button", { name: "Confirm deletion" }).click();
    await expect(
      page.getByRole("heading", { name: "Account deleted" }),
    ).toBeVisible();
    await expect
      .poll(
        async () =>
          (
            await anonymous.get(deletionLink.shortUrl, { maxRedirects: 0 })
          ).status(),
        { timeout: 6500, intervals: [100, 250, 500] },
      )
      .toBe(404);
    await redirect(deletionLink.shortUrl, 404);
  } finally {
    await anonymous.dispose();
    await smtp.stop();
  }
});
