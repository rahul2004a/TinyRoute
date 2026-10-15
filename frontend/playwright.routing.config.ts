import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  testMatch: "short-link-routing.proxy.ts",
  workers: 1,
  use: {
    baseURL: "https://localhost:3300",
    ignoreHTTPSErrors: true,
  },
  webServer: [
    {
      command: "node e2e/fixtures/routing-backend.mjs",
      url: "http://localhost:18080/__health",
      reuseExistingServer: false,
    },
    {
      command:
        "pnpm dev --webpack --experimental-https --experimental-https-key ../.local-certs/localhost-key.pem --experimental-https-cert ../.local-certs/localhost.pem --port 3300",
      env: {
        BACKEND_ORIGIN: "http://localhost:18080",
        NEXT_PUBLIC_API_BASE_URL: "https://localhost:3300",
      },
      url: "https://localhost:3300/login",
      ignoreHTTPSErrors: true,
      reuseExistingServer: false,
      timeout: 120_000,
    },
  ],
});
