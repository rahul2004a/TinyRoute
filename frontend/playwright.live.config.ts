import { defineConfig } from "@playwright/test";

const port = process.env.TINYROUTE_E2E_FRONTEND_PORT ?? "3000";
if (!/^[0-9]{4,5}$/.test(port) || Number(port) > 65535 || Number(port) < 1024)
  throw new Error("Invalid live-verification frontend port");
const frontendBase = `https://localhost:${port}`;

export default defineConfig({
  testDir: "./e2e-live",
  testMatch: "**/*.live.ts",
  workers: 1,
  use: {
    baseURL: frontendBase,
    browserName: "chromium",
    ignoreHTTPSErrors: true,
  },
  webServer: {
    command: `pnpm dev --webpack --experimental-https --experimental-https-key ../.local-certs/localhost-key.pem --experimental-https-cert ../.local-certs/localhost.pem --port ${port}`,
    url: `${frontendBase}/login`,
    ignoreHTTPSErrors: true,
    reuseExistingServer: true,
    timeout: 120_000,
  },
});
