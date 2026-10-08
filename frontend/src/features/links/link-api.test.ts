import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { createLink } from "./link-api";
import { clearCsrfToken } from "../../lib/csrf";

export const created = {
  id: "86b9f592-c6ec-43e1-aa62-adf606f03c1e",
  code: "Abc12345",
  shortUrl: "https://go.tinyroute.test/Abc12345",
  destinationUrl: "https://example.com/docs?q=java#setup",
  createdAt: "2026-10-08T00:00:00Z",
  expiresAt: null,
};
beforeEach(() => {
  vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "https://api.tinyroute.test");
  clearCsrfToken();
});
afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
  clearCsrfToken();
});
it("bootstraps CSRF and sends exactly one credentialed POST without rebuilding the URL", async () => {
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(
      new Response(JSON.stringify({ csrfToken: "csrf" }), { status: 200 }),
    )
    .mockResolvedValueOnce(
      new Response(JSON.stringify(created), { status: 201 }),
    );
  vi.stubGlobal("fetch", fetch);
  expect(await createLink({ destinationUrl: created.destinationUrl })).toEqual(
    created,
  );
  expect(fetch).toHaveBeenCalledTimes(2);
  const [url, request] = fetch.mock.calls[1] as [string, RequestInit];
  expect(url).toBe("https://api.tinyroute.test/api/links");
  expect(request.credentials).toBe("include");
  expect(new Headers(request.headers).get("X-CSRF-TOKEN")).toBe("csrf");
  expect(JSON.parse(request.body as string)).toEqual({
    destinationUrl: created.destinationUrl,
  });
});
it.each([401, 403, 429, 503])(
  "does not retry a failed creation (%s)",
  async (status) => {
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ csrfToken: "csrf" }), { status: 200 }),
      )
      .mockResolvedValueOnce(new Response("{}", { status }));
    vi.stubGlobal("fetch", fetch);
    await expect(
      createLink({ destinationUrl: created.destinationUrl }),
    ).rejects.toMatchObject({ status });
    expect(fetch).toHaveBeenCalledTimes(2);
  },
);
it("does not retry an ambiguous network failure", async () => {
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(
      new Response(JSON.stringify({ csrfToken: "csrf" }), { status: 200 }),
    )
    .mockRejectedValueOnce(new TypeError("network"));
  vi.stubGlobal("fetch", fetch);
  await expect(
    createLink({ destinationUrl: created.destinationUrl }),
  ).rejects.toThrow("network");
  expect(fetch).toHaveBeenCalledTimes(2);
});
it("rejects a malformed success that omits nullable expiry", async () => {
  const malformed = { ...created, expiresAt: undefined };
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(
      new Response(JSON.stringify({ csrfToken: "csrf" }), { status: 200 }),
    )
    .mockResolvedValueOnce(
      new Response(JSON.stringify(malformed), { status: 201 }),
    );
  vi.stubGlobal("fetch", fetch);
  await expect(
    createLink({ destinationUrl: created.destinationUrl }),
  ).rejects.toThrow();
  expect(fetch).toHaveBeenCalledTimes(2);
});
