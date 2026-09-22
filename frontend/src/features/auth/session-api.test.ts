import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { clearCsrfToken, getCurrentSession, logout } from "./auth-api";

function jsonResponse(status: number, payload?: unknown) {
  return {
    json: vi.fn().mockResolvedValue(payload),
    ok: status >= 200 && status < 300,
    status,
  };
}

function csrfHeader(request: RequestInit): string | null {
  return new Headers(request.headers).get("X-CSRF-TOKEN");
}

describe("session API lifecycle", () => {
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

  it("recovers an expired access session through one CSRF-bound refresh", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse(401, {
          error: {
            code: "AUTHENTICATION_FAILED",
            message: "Authentication failed",
            requestId: "request-1",
          },
        }),
      )
      .mockResolvedValueOnce(jsonResponse(200, { csrfToken: "csrf-value" }))
      .mockResolvedValueOnce(
        jsonResponse(200, {
          authenticated: true,
          user: { email: "person@example.com" },
        }),
      );
    vi.stubGlobal("fetch", fetchMock);

    await expect(getCurrentSession()).resolves.toEqual({
      authenticated: true,
      user: { email: "person@example.com" },
    });

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      "https://api.tinyroute.test/api/auth/me",
      "https://api.tinyroute.test/api/auth/csrf",
      "https://api.tinyroute.test/api/auth/refresh",
    ]);
    expect(csrfHeader(fetchMock.mock.calls[2][1] as RequestInit)).toBe(
      "csrf-value",
    );
  });

  it("treats an unavailable refresh session as signed out", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        jsonResponse(401, {
          error: {
            code: "AUTHENTICATION_FAILED",
            message: "Authentication failed",
            requestId: "request-1",
          },
        }),
      )
      .mockResolvedValueOnce(jsonResponse(200, { csrfToken: "csrf-value" }))
      .mockResolvedValueOnce(
        jsonResponse(401, {
          error: {
            code: "AUTHENTICATION_FAILED",
            message: "Authentication failed",
            requestId: "request-2",
          },
        }),
      );
    vi.stubGlobal("fetch", fetchMock);

    await expect(getCurrentSession()).resolves.toEqual({
      authenticated: false,
    });
  });

  it("sends logout with the in-memory CSRF value and accepts an already-expired access session", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(200, { csrfToken: "csrf-value" }))
      .mockResolvedValueOnce(
        jsonResponse(401, {
          error: {
            code: "AUTHENTICATION_FAILED",
            message: "Authentication failed",
            requestId: "request-3",
          },
        }),
      );
    vi.stubGlobal("fetch", fetchMock);

    await expect(logout()).resolves.toBeUndefined();

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      "https://api.tinyroute.test/api/auth/csrf",
      "https://api.tinyroute.test/api/auth/logout",
    ]);
    expect(csrfHeader(fetchMock.mock.calls[1][1] as RequestInit)).toBe(
      "csrf-value",
    );
  });
});
