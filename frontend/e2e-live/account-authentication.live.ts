import { createServer, type Server, type Socket } from "node:net";

import { expect, test } from "@playwright/test";

test.setTimeout(120_000);

class SmtpSink {
  private readonly messages: string[] = [];
  private readonly waiters: Array<(message: string) => void> = [];
  private server: Server | undefined;

  async start(port = 1025) {
    this.server = createServer((socket) => this.accept(socket));
    await new Promise<void>((resolve, reject) => {
      this.server?.once("error", reject);
      this.server?.listen(port, resolve);
    });
  }

  async stop() {
    if (!this.server) return;
    await new Promise<void>((resolve, reject) => {
      this.server?.close((error) => (error ? reject(error) : resolve()));
    });
  }

  nextMessage(timeoutMs = 15_000): Promise<string> {
    const queued = this.messages.shift();
    if (queued !== undefined) return Promise.resolve(queued);

    return new Promise<string>((resolve, reject) => {
      const timeout = setTimeout(
        () => reject(new Error("Timed out waiting for SMTP delivery")),
        timeoutMs,
      );
      this.waiters.push((message) => {
        clearTimeout(timeout);
        resolve(message);
      });
    });
  }

  private accept(socket: Socket) {
    let buffer = "";
    let receivingData = false;
    let messageLines: string[] = [];
    socket.setEncoding("utf8");
    socket.write("220 localhost TinyRoute E2E SMTP\r\n");
    socket.on("data", (chunk) => {
      buffer += chunk;
      let lineEnd = buffer.indexOf("\r\n");
      while (lineEnd >= 0) {
        const line = buffer.slice(0, lineEnd);
        buffer = buffer.slice(lineEnd + 2);
        if (receivingData) {
          if (line === ".") {
            receivingData = false;
            this.deliver(messageLines.join("\n"));
            messageLines = [];
            socket.write("250 2.0.0 queued\r\n");
          } else {
            messageLines.push(line.startsWith("..") ? line.slice(1) : line);
          }
        } else if (/^(EHLO|HELO)\b/i.test(line)) {
          socket.write("250-localhost\r\n250 8BITMIME\r\n");
        } else if (/^(MAIL FROM|RCPT TO|RSET)\b/i.test(line)) {
          socket.write("250 2.1.0 ok\r\n");
        } else if (/^DATA\b/i.test(line)) {
          receivingData = true;
          socket.write("354 End data with <CR><LF>.<CR><LF>\r\n");
        } else if (/^QUIT\b/i.test(line)) {
          socket.end("221 2.0.0 bye\r\n");
        } else {
          socket.write("250 2.0.0 ok\r\n");
        }
        lineEnd = buffer.indexOf("\r\n");
      }
    });
  }

  private deliver(message: string) {
    const waiter = this.waiters.shift();
    if (waiter) waiter(message);
    else this.messages.push(message);
  }
}

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
    await page.getByLabel("Password").fill(replacementPassword);
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
