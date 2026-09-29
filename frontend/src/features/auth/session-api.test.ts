import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  clearCsrfToken,
  getCurrentSession,
  logout,
  refreshSession,
} from "./auth-api";

const signedInSession = {
  authenticated: true,
  user: { email: "person@example.com" },
};

const authenticationFailure = {
  error: {
    code: "AUTHENTICATION_FAILED",
    message: "Authentication failed",
    requestId: "request-1",
  },
};

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
    vi.useRealTimers();
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

  it("coalesces simultaneous refreshes within one tab", async () => {
    let finishRefresh:
      ((value: ReturnType<typeof jsonResponse>) => void) | undefined;
    const fetchMock = vi.fn((url: string) => {
      if (url.endsWith("/api/auth/csrf")) {
        return Promise.resolve(jsonResponse(200, { csrfToken: "csrf-value" }));
      }
      if (url.endsWith("/api/auth/refresh")) {
        return new Promise<ReturnType<typeof jsonResponse>>((resolve) => {
          finishRefresh = resolve;
        });
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const first = refreshSession();
    const second = refreshSession();
    await vi.waitFor(() => expect(finishRefresh).toBeDefined());
    finishRefresh!(jsonResponse(200, signedInSession));

    await expect(Promise.all([first, second])).resolves.toEqual([
      signedInSession,
      signedInSession,
    ]);
    expect(
      fetchMock.mock.calls.filter(([url]) => url.endsWith("/api/auth/refresh")),
    ).toHaveLength(1);
  });

  it("serializes refreshes across tabs and rechecks the updated access cookie", async () => {
    let releaseFirstRefresh: (() => void) | undefined;
    let accessValid = false;
    let lockQueue = Promise.resolve();
    const requestLock = vi.fn(
      (_name: string, callback: () => Promise<unknown>) => {
        const result = lockQueue.then(callback);
        lockQueue = result.then(
          () => undefined,
          () => undefined,
        );
        return result;
      },
    );
    vi.stubGlobal("navigator", { locks: { request: requestLock } });

    const fetchMock = vi.fn(async (url: string) => {
      if (url.endsWith("/api/auth/me")) {
        return accessValid
          ? jsonResponse(200, signedInSession)
          : jsonResponse(401, authenticationFailure);
      }
      if (url.endsWith("/api/auth/csrf")) {
        return jsonResponse(200, { csrfToken: "csrf-value" });
      }
      if (url.endsWith("/api/auth/refresh")) {
        await new Promise<void>((resolve) => {
          releaseFirstRefresh = resolve;
        });
        accessValid = true;
        return jsonResponse(200, signedInSession);
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const firstTab = getCurrentSession();
    await vi.waitFor(() => expect(releaseFirstRefresh).toBeDefined());
    vi.resetModules();
    const secondTabApi = await import("./auth-api");
    const secondTab = secondTabApi.getCurrentSession();
    await vi.waitFor(() => expect(requestLock).toHaveBeenCalledTimes(2));
    releaseFirstRefresh!();

    await expect(Promise.all([firstTab, secondTab])).resolves.toEqual([
      signedInSession,
      signedInSession,
    ]);
    expect(
      fetchMock.mock.calls.filter(([url]) => url.endsWith("/api/auth/refresh")),
    ).toHaveLength(1);
  });

  it("recovers from a concurrent rotation when Web Locks are unavailable", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(401, authenticationFailure))
      .mockResolvedValueOnce(jsonResponse(200, { csrfToken: "csrf-value" }))
      .mockResolvedValueOnce(
        jsonResponse(409, {
          error: {
            code: "REFRESH_CONCURRENT",
            message: "Refresh already in progress",
            requestId: "request-2",
          },
        }),
      )
      .mockResolvedValueOnce(jsonResponse(401, authenticationFailure))
      .mockResolvedValueOnce(jsonResponse(200, signedInSession));
    vi.stubGlobal("fetch", fetchMock);

    await expect(getCurrentSession()).resolves.toEqual(signedInSession);
    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      "https://api.tinyroute.test/api/auth/me",
      "https://api.tinyroute.test/api/auth/csrf",
      "https://api.tinyroute.test/api/auth/refresh",
      "https://api.tinyroute.test/api/auth/me",
      "https://api.tinyroute.test/api/auth/me",
    ]);
  });

  it("keeps a concurrent-rotation failure distinct from a signed-out session", async () => {
    vi.useFakeTimers();
    const concurrentRotation = {
      error: {
        code: "REFRESH_CONCURRENT",
        message: "Refresh already in progress",
        requestId: "request-2",
      },
    };
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(401, authenticationFailure))
      .mockResolvedValueOnce(jsonResponse(200, { csrfToken: "csrf-value" }))
      .mockResolvedValueOnce(jsonResponse(409, concurrentRotation))
      .mockResolvedValue(jsonResponse(401, authenticationFailure));
    vi.stubGlobal("fetch", fetchMock);

    const session = getCurrentSession();
    const failure = expect(session).rejects.toMatchObject({
      status: 409,
      apiError: concurrentRotation,
    });
    await vi.runAllTimersAsync();
    await failure;
    expect(
      fetchMock.mock.calls.filter(([url]) => url.endsWith("/api/auth/refresh")),
    ).toHaveLength(1);
  });

  it("refreshes an expired access session and retries logout so the refresh session is revoked", async () => {
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
      )
      .mockResolvedValueOnce(
        jsonResponse(200, {
          authenticated: true,
          user: { email: "person@example.com" },
        }),
      )
      .mockResolvedValueOnce(jsonResponse(204));
    vi.stubGlobal("fetch", fetchMock);

    await expect(logout()).resolves.toBeUndefined();

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      "https://api.tinyroute.test/api/auth/csrf",
      "https://api.tinyroute.test/api/auth/logout",
      "https://api.tinyroute.test/api/auth/refresh",
      "https://api.tinyroute.test/api/auth/logout",
    ]);
    expect(csrfHeader(fetchMock.mock.calls[1][1] as RequestInit)).toBe(
      "csrf-value",
    );
    expect(csrfHeader(fetchMock.mock.calls[2][1] as RequestInit)).toBe(
      "csrf-value",
    );
    expect(csrfHeader(fetchMock.mock.calls[3][1] as RequestInit)).toBe(
      "csrf-value",
    );
  });

  it("accepts logout after both access and refresh sessions are unavailable", async () => {
    const authenticationFailure = {
      error: {
        code: "AUTHENTICATION_FAILED",
        message: "Authentication failed",
        requestId: "request-4",
      },
    };
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(200, { csrfToken: "csrf-value" }))
      .mockResolvedValueOnce(jsonResponse(401, authenticationFailure))
      .mockResolvedValueOnce(jsonResponse(401, authenticationFailure));
    vi.stubGlobal("fetch", fetchMock);

    await expect(logout()).resolves.toBeUndefined();

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      "https://api.tinyroute.test/api/auth/csrf",
      "https://api.tinyroute.test/api/auth/logout",
      "https://api.tinyroute.test/api/auth/refresh",
    ]);
  });

  it("does not report success when logout still fails after refreshing access", async () => {
    const authenticationFailure = {
      error: {
        code: "AUTHENTICATION_FAILED",
        message: "Authentication failed",
        requestId: "request-5",
      },
    };
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(200, { csrfToken: "csrf-value" }))
      .mockResolvedValueOnce(jsonResponse(401, authenticationFailure))
      .mockResolvedValueOnce(
        jsonResponse(200, {
          authenticated: true,
          user: { email: "person@example.com" },
        }),
      )
      .mockResolvedValueOnce(jsonResponse(401, authenticationFailure));
    vi.stubGlobal("fetch", fetchMock);

    await expect(logout()).rejects.toMatchObject({ status: 401 });
  });
});
