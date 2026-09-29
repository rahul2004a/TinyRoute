import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { clearCsrfToken, deleteAccount } from "./auth-api";

function response(status: number, payload?: unknown) {
  return {
    json: vi.fn().mockResolvedValue(payload),
    ok: status >= 200 && status < 300,
    status,
  };
}

describe("account deletion API", () => {
  const originalApiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;

  beforeEach(() => {
    clearCsrfToken();
    process.env.NEXT_PUBLIC_API_BASE_URL = "https://api.tinyroute.test";
  });

  afterEach(() => {
    clearCsrfToken();
    if (originalApiBaseUrl === undefined) {
      delete process.env.NEXT_PUBLIC_API_BASE_URL;
    } else {
      process.env.NEXT_PUBLIC_API_BASE_URL = originalApiBaseUrl;
    }
    vi.unstubAllGlobals();
  });

  it("sends a credentialed CSRF-protected DELETE and accepts only the server's 204", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(response(200, { csrfToken: "csrf-fixture" }))
      .mockResolvedValueOnce(response(204));
    vi.stubGlobal("fetch", fetchMock);

    await expect(deleteAccount()).resolves.toBeUndefined();

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      "https://api.tinyroute.test/api/auth/csrf",
      "https://api.tinyroute.test/api/auth/account",
    ]);
    expect(fetchMock.mock.calls[1][1]).toMatchObject({
      method: "DELETE",
      credentials: "include",
    });
    const request = fetchMock.mock.calls[1][1] as RequestInit;
    expect(new Headers(request.headers).get("X-CSRF-TOKEN")).toBe(
      "csrf-fixture",
    );
    expect(request.body).toBeUndefined();
  });

  it("rejects a failed response so the UI can retain the session and retry", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(response(200, { csrfToken: "csrf-fixture" }))
        .mockResolvedValueOnce(
          response(503, {
            error: {
              code: "SERVICE_UNAVAILABLE",
              message: "internal detail",
              requestId: "request-1",
            },
          }),
        ),
    );

    await expect(deleteAccount()).rejects.toMatchObject({ status: 503 });
  });
});
