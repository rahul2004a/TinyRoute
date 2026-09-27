import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e-live",
  testMatch: "**/*.live.ts",
  workers: 1,
  use: {
    baseURL: "https://localhost:3000",
    browserName: "chromium",
    ignoreHTTPSErrors: true,
  },
  webServer: {
    command:
      "pnpm dev --experimental-https --experimental-https-key ../.local-certs/localhost-key.pem --experimental-https-cert ../.local-certs/localhost.pem --port 3000",
    url: "https://localhost:3000/login",
    ignoreHTTPSErrors: true,
    reuseExistingServer: true,
    timeout: 120_000,
  },
});
