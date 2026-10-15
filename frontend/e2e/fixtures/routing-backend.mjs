import { createServer } from "node:http";

let requests = [];
const errors = new Map([
  ["/disabled", 403],
  ["/deleted", 404],
  ["/expired", 404],
  ["/throttled", 429],
  ["/outage", 503],
]);

const server = createServer(async (request, response) => {
  const path = new URL(request.url, "http://localhost:18080").pathname;
  if (path === "/__health") {
    response.end("ready");
    return;
  }
  if (path === "/__requests") {
    if (request.method === "DELETE") requests = [];
    response.setHeader("Content-Type", "application/json");
    response.end(JSON.stringify(requests));
    return;
  }

  requests.push({ method: request.method, path });
  response.setHeader("Cache-Control", "no-store");
  if (path === "/api/auth/csrf") {
    response.setHeader("Content-Type", "application/json");
    response.setHeader("Set-Cookie", [
      "fixture_csrf=csrf-value; Path=/; Secure; HttpOnly; SameSite=Lax",
      "fixture_session=session-value; Path=/; Secure; HttpOnly; SameSite=Lax",
    ]);
    response.end(JSON.stringify({ csrfToken: "csrf-value" }));
    return;
  }
  if (path === "/api/links" && request.method === "POST") {
    let body = "";
    for await (const chunk of request) body += chunk;
    response.writeHead(201, { "Content-Type": "application/json" });
    response.end(
      JSON.stringify({
        method: request.method,
        body: JSON.parse(body),
        cookie: request.headers.cookie,
        csrf: request.headers["x-csrf-token"],
      }),
    );
    return;
  }
  if (path === "/Docs2026") {
    response.writeHead(302, {
      Location: "https://example.com/docs?q=java#setup",
    });
    response.end();
    return;
  }
  const status = errors.get(path) ?? 404;
  response.writeHead(status, {
    "Content-Type": "text/html; charset=utf-8",
    ...(status === 429 ? { "Retry-After": "31" } : {}),
  });
  response.end(`<!doctype html><h1>Backend link outcome ${status}</h1>`);
});

server.listen(18080, "localhost");
process.on("SIGTERM", () => server.close());
process.on("SIGINT", () => server.close());
