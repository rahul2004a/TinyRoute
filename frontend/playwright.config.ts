import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  workers: 1,
  use: {
    baseURL: "https://localhost:3000",
    browserName: "chromium",
    ignoreHTTPSErrors: true,
  },
  webServer: {
    // These browser fixtures intercept the fixed HTTPS API origin directly.
    env: {
      BACKEND_ORIGIN: "",
      NEXT_PUBLIC_API_BASE_URL: "https://localhost:8443",
    },
    command:
      "pnpm dev --experimental-https --experimental-https-key ../.local-certs/localhost-key.pem --experimental-https-cert ../.local-certs/localhost.pem --port 3000",
    url: "https://localhost:3000/login",
    ignoreHTTPSErrors: true,
    reuseExistingServer: true,
    timeout: 120_000,
  },
});
