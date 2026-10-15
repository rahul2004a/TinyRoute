import { expect, test } from "@playwright/test";

const upstream = "http://localhost:18080";

test.beforeEach(async ({ request }) => {
  await request.delete(`${upstream}/__requests`);
});

test("short links return one 302 with the exact destination and no-store", async ({
  request,
}) => {
  const response = await request.get("/Docs2026?incoming=ignored", {
    maxRedirects: 0,
  });
  expect(response.status()).toBe(302);
  expect(response.headers().location).toBe(
    "https://example.com/docs?q=java#setup",
  );
  expect(response.headers()["cache-control"]).toBe("no-store");
  expect(await response.text()).toBe("");
  expect(await (await request.get(`${upstream}/__requests`)).json()).toEqual([
    { method: "GET", path: "/Docs2026" },
  ]);
});

for (const [code, status] of [
  ["missing", 404],
  ["docs2026", 404],
  ["disabled", 403],
  ["deleted", 404],
  ["expired", 404],
  ["throttled", 429],
  ["outage", 503],
] as const) {
  test(`${code} preserves backend ${status} HTML without a redirect`, async ({
    request,
  }) => {
    const response = await request.get(`/${code}`, { maxRedirects: 0 });
    expect(response.status()).toBe(status);
    expect(response.headers().location).toBeUndefined();
    expect(response.headers()["cache-control"]).toBe("no-store");
    expect(response.headers()["content-type"]).toContain("text/html");
    expect(await response.text()).toBe(
      `<!doctype html><h1>Backend link outcome ${status}</h1>`,
    );
    if (status === 429) expect(response.headers()["retry-after"]).toBe("31");
  });
}

test("HEAD preserves redirect metadata without a response body", async ({
  request,
}) => {
  const response = await request.head("/Docs2026", { maxRedirects: 0 });
  expect(response.status()).toBe(302);
  expect(response.headers().location).toBe(
    "https://example.com/docs?q=java#setup",
  );
  expect(await response.body()).toHaveLength(0);
});

test("pages, assets, infrastructure names and nested unknown paths bypass Spring", async ({
  request,
}) => {
  for (const path of [
    "/",
    "/login",
    "/register",
    "/verify-email",
    "/links",
    "/settings",
    "/password-reset",
    "/password-reset/confirm",
    "/google-g.png",
  ]) {
    expect((await request.get(path)).status(), path).toBe(200);
  }
  for (const path of [
    "/Docs2026/extra",
    "/actuator",
    "/actuator/health",
    "/error",
    "/_next",
    "/_next/static/missing.js",
    "/missing.ico",
  ]) {
    expect((await request.get(path)).status(), path).toBe(404);
  }
  const reset = await request.get("/password-reset/confirm");
  expect(reset.headers()["referrer-policy"]).toBe("no-referrer");
  expect(await (await request.get(`${upstream}/__requests`)).json()).toEqual(
    [],
  );
});

test("API proxy preserves secure cookies, CSRF header, POST method and JSON", async ({
  request,
}) => {
  const csrf = await request.get("/api/auth/csrf");
  expect(csrf.status()).toBe(200);
  expect(csrf.headers()["cache-control"]).toBe("no-store");
  expect(await csrf.json()).toEqual({ csrfToken: "csrf-value" });
  expect(
    csrf
      .headersArray()
      .filter(({ name }) => name.toLowerCase() === "set-cookie")
      .map(({ value }) => value),
  ).toEqual([
    "fixture_csrf=csrf-value; Path=/; Secure; HttpOnly; SameSite=Lax",
    "fixture_session=session-value; Path=/; Secure; HttpOnly; SameSite=Lax",
  ]);

  const body = { destinationUrl: "https://example.com/path?q=1#fragment" };
  const created = await request.post("/api/links", {
    headers: { "X-CSRF-TOKEN": "csrf-value" },
    data: body,
  });
  expect(created.status()).toBe(201);
  expect(await created.json()).toEqual({
    method: "POST",
    body,
    cookie: "fixture_csrf=csrf-value; fixture_session=session-value",
    csrf: "csrf-value",
  });
  expect(await (await request.get(`${upstream}/__requests`)).json()).toEqual([
    { method: "GET", path: "/api/auth/csrf" },
    { method: "POST", path: "/api/links" },
  ]);
});
