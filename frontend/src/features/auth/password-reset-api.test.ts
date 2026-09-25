import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { confirmPasswordReset, requestPasswordReset } from "./auth-api";

describe("password-reset HTTP contract", () => {
  const originalApiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;

  beforeEach(() => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "https://api.tinyroute.test";
  });

  afterEach(() => {
    if (originalApiBaseUrl === undefined) {
      delete process.env.NEXT_PUBLIC_API_BASE_URL;
    } else {
      process.env.NEXT_PUBLIC_API_BASE_URL = originalApiBaseUrl;
    }
    vi.unstubAllGlobals();
  });

  it("submits the opaque reset token only in a credentialed JSON body", async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 204 });
    vi.stubGlobal("fetch", fetchMock);
    const token = "opaque-reset-fixture";

    await confirmPasswordReset(token, "correct-horse-battery", "csrf-fixture");

    const [url, request] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe(
      "https://api.tinyroute.test/api/auth/password-reset/confirm",
    );
    expect(url).not.toContain(token);
    expect(request.method).toBe("POST");
    expect(request.credentials).toBe("include");
    expect(new Headers(request.headers).get("X-CSRF-TOKEN")).toBe(
      "csrf-fixture",
    );
    expect(JSON.parse(request.body as string)).toEqual({
      token,
      newPassword: "correct-horse-battery",
    });
  });

  it("accepts only the generic request response", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 202,
      json: vi.fn().mockResolvedValue({ status: "ACCEPTED" }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      requestPasswordReset("person@example.com", "csrf-fixture"),
    ).resolves.toEqual({ status: "ACCEPTED" });

    const [url, request] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("https://api.tinyroute.test/api/auth/password-reset");
    expect(JSON.parse(request.body as string)).toEqual({
      email: "person@example.com",
    });
  });
});
