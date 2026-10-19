import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  clearCsrfToken,
  fetchCsrfToken,
  getCurrentSession,
  login,
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

const csrfFailure = {
  error: {
    code: "CSRF_INVALID",
    message: "CSRF token is invalid",
    requestId: "csrf-fixture",
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

  it("restores an idle session when another tab invalidated its cached CSRF token", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(200, { csrfToken: "old-csrf" }))
      .mockResolvedValueOnce(jsonResponse(401, authenticationFailure))
      .mockResolvedValueOnce(jsonResponse(403, csrfFailure))
      .mockResolvedValueOnce(jsonResponse(200, { csrfToken: "current-csrf" }))
      .mockResolvedValueOnce(jsonResponse(200, signedInSession));
    vi.stubGlobal("fetch", fetchMock);
    await fetchCsrfToken();

    await expect(getCurrentSession()).resolves.toEqual(signedInSession);

    const refreshRequests = fetchMock.mock.calls.filter(([url]) =>
      url.endsWith("/api/auth/refresh"),
    );
    expect(refreshRequests).toHaveLength(2);
    expect(refreshRequests.map(([, request]) => csrfHeader(request))).toEqual([
      "old-csrf",
      "current-csrf",
    ]);
  });

  it.each(["login", "logout"] as const)(
    "recovers %s after a confirmed CSRF rejection",
    async (action) => {
      const fetchMock = vi
        .fn()
        .mockResolvedValueOnce(jsonResponse(200, { csrfToken: "old-csrf" }))
        .mockResolvedValueOnce(jsonResponse(403, csrfFailure))
        .mockResolvedValueOnce(jsonResponse(200, { csrfToken: "current-csrf" }))
        .mockResolvedValueOnce(
          action === "login"
            ? jsonResponse(200, signedInSession)
            : jsonResponse(204),
        );
      vi.stubGlobal("fetch", fetchMock);
      const csrf = await fetchCsrfToken();

      await expect(
        action === "login"
          ? login({ email: "person@example.com", password: "password" }, csrf)
          : logout(),
      ).resolves.toBeUndefined();

      expect(
        fetchMock.mock.calls.map(([url]) => new URL(url).pathname),
      ).toEqual([
        "/api/auth/csrf",
        `/api/auth/${action}`,
        "/api/auth/csrf",
        `/api/auth/${action}`,
      ]);
      expect(csrfHeader(fetchMock.mock.calls[3][1])).toBe("current-csrf");
      if (action === "login") {
        expect(fetchMock.mock.calls[1][1].body).toBe(
          fetchMock.mock.calls[3][1].body,
        );
      }
    },
  );

  it("stops after one CSRF recovery and drops a repeatedly rejected token", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(200, { csrfToken: "old-csrf" }))
      .mockResolvedValueOnce(jsonResponse(403, csrfFailure))
      .mockResolvedValueOnce(jsonResponse(200, { csrfToken: "rejected-csrf" }))
      .mockResolvedValueOnce(jsonResponse(403, csrfFailure))
      .mockResolvedValueOnce(jsonResponse(200, { csrfToken: "next-csrf" }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(refreshSession()).rejects.toMatchObject({ status: 403 });
    expect(fetchMock).toHaveBeenCalledTimes(4);
    await expect(fetchCsrfToken()).resolves.toBe("next-csrf");
  });

  it.each([
    [401, authenticationFailure],
    [403, undefined],
    [403, authenticationFailure],
    [
      429,
      { error: { code: "RATE_LIMITED", message: "Wait", requestId: "rate" } },
    ],
    [
      503,
      {
        error: {
          code: "SESSION_UNAVAILABLE",
          message: "Unavailable",
          requestId: "outage",
        },
      },
    ],
  ])(
    "does not replay login for a non-CSRF rejection (%s)",
    async (status, payload) => {
      const fetchMock = vi
        .fn()
        .mockResolvedValue(jsonResponse(status as number, payload));
      vi.stubGlobal("fetch", fetchMock);

      await expect(
        login({ email: "person@example.com", password: "password" }, "csrf"),
      ).rejects.toMatchObject({ status });
      expect(fetchMock).toHaveBeenCalledOnce();
    },
  );

  it("does not retry login when the network result is ambiguous", async () => {
    const failure = new TypeError("Failed to fetch");
    const fetchMock = vi.fn().mockRejectedValue(failure);
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      login({ email: "person@example.com", password: "password" }, "csrf"),
    ).rejects.toBe(failure);
    expect(fetchMock).toHaveBeenCalledOnce();
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
