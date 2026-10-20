import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import { AnalyticsRangeForm } from "./analytics-range-form";
afterEach(cleanup);
test("presets use the server UTC end date and edits require Apply", async () => {
  const apply = vi.fn();
  render(
    <AnalyticsRangeForm
      from="2026-09-21"
      to="2026-10-20"
      serverEnd="2026-10-20"
      onApply={apply}
    />,
  );
  fireEvent.click(screen.getByRole("button", { name: "Last 7 days" }));
  expect(apply).toHaveBeenCalledWith({ from: "2026-10-14", to: "2026-10-20" });
  apply.mockClear();
  fireEvent.change(screen.getByLabelText("From (UTC)"), {
    target: { value: "2026-10-01" },
  });
  expect(apply).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole("button", { name: "Apply range" }));
  await waitFor(() =>
    expect(apply).toHaveBeenCalledWith({
      from: "2026-10-01",
      to: "2026-10-20",
    }),
  );
});
