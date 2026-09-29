import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { z } from "zod";

import { ApiClientError, apiRequest } from "../lib/api-client";

const responseSchema = z.object({
  userId: z.string(),
});

describe("apiRequest", () => {
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

  it("sends credentials and validates a successful response", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      json: vi.fn().mockResolvedValue({ userId: "user-123" }),
      ok: true,
      status: 200,
    });
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      apiRequest("/api/auth/me", { responseSchema }),
    ).resolves.toEqual({ userId: "user-123" });

    expect(fetchMock).toHaveBeenCalledOnce();
    const [url, request] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe("https://api.tinyroute.test/api/auth/me");
    expect(request.credentials).toBe("include");
  });

  it("maps a documented API error without losing its request id", async () => {
    const apiError = {
      error: {
        code: "AUTHENTICATION_FAILED",
        message: "Invalid email or password",
        requestId: "request-456",
      },
    } as const;
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        json: vi.fn().mockResolvedValue(apiError),
        ok: false,
        status: 401,
      }),
    );

    const request = apiRequest("/api/auth/login", { responseSchema });

    await expect(request).rejects.toBeInstanceOf(ApiClientError);
    await expect(request).rejects.toMatchObject({
      apiError,
      message: "Invalid email or password",
      status: 401,
    });
  });

  it("rejects an absolute URL before sending credentials", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      apiRequest("https://example.test/collect", { responseSchema }),
    ).rejects.toThrow("API path must start with a single slash");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("rejects an HTTP API base URL before sending credentials", async () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost:8080";
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    await expect(
      apiRequest("/api/auth/login", { responseSchema }),
    ).rejects.toThrow("NEXT_PUBLIC_API_BASE_URL must use HTTPS");
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
