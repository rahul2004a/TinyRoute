import { expect, test } from "vitest";
import { analyticsReturnPath } from "./analytics-return-path";
test("returns only local analytics paths and rejects outside destinations", () => {
  expect(analyticsReturnPath("/analytics?from=2026-10-01&to=2026-10-20")).toBe(
    "/analytics?from=2026-10-01&to=2026-10-20",
  );
  for (const value of [
    "https://evil.test",
    "//evil.test/analytics",
    "/\\evil.test/analytics",
    "/settings",
    "/analytics/../../login",
    undefined,
  ])
    expect(analyticsReturnPath(value)).toBe("/links");
});
