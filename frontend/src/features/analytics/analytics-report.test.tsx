import {
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import { AnalyticsReport } from "./analytics-report";
import type { Analytics } from "./analytics-api";
afterEach(cleanup);
vi.stubGlobal(
  "ResizeObserver",
  class {
    observe() {}
    unobserve() {}
    disconnect() {}
  },
);
const report: Analytics = {
  range: { from: "2026-10-19", to: "2026-10-20", timeZone: "UTC" },
  generatedAt: "2026-10-20T12:00:00Z",
  recordingDelaySeconds: 60,
  totalClicks: 2,
  lifetimeClicks: 9,
  dailyClicks: [
    { date: "2026-10-19", count: 0 },
    { date: "2026-10-20", count: 2 },
  ],
  breakdowns: {
    referrers: [
      { kind: "DIRECT_OR_UNKNOWN", label: "Direct / unknown", count: 2 },
    ],
    devices: [{ kind: "UNKNOWN", label: "Unknown", count: 2 }],
    operatingSystems: [],
    browsers: [],
    countries: [],
    cities: [],
  },
};
test("shows returned range and lifetime separately with a full accessible daily table", () => {
  render(<AnalyticsReport report={report} />);
  expect(screen.getByText("Lifetime redirects")).toBeTruthy();
  expect(screen.getByText("9")).toBeTruthy();
  fireEvent.click(screen.getByText("Show data table"));
  const table = screen.getByRole("table", {
    name: "Daily redirect counts (UTC)",
  });
  expect(within(table).getAllByRole("row")).toHaveLength(3);
  expect(within(table).getByText("2026-10-19")).toBeTruthy();
  expect(within(table).getByText("0")).toBeTruthy();
  expect(screen.getByRole("table", { name: "Referrers" })).toBeTruthy();
  expect(screen.getByText("Direct / unknown")).toBeTruthy();
  expect(screen.getAllByRole("table")).toHaveLength(7);
});
test("zero range activity retains lifetime and shows an empty trend state", () => {
  render(
    <AnalyticsReport
      report={{
        ...report,
        totalClicks: 0,
        dailyClicks: report.dailyClicks.map((day) => ({ ...day, count: 0 })),
      }}
    />,
  );
  expect(screen.getByText("No redirects in this date range.")).toBeTruthy();
  expect(screen.getByText("9")).toBeTruthy();
});
