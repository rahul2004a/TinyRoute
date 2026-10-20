import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, expect, test } from "vitest";
import { AnalyticsCountryMap } from "./analytics-country-map";

afterEach(cleanup);
const geography = {
  countries: [
    { countryCode: "SE", label: "Sweden", count: 12 },
    { countryCode: "IN", label: "India", count: 4 },
  ],
  unknownClicks: 3,
};

test("country counts are available on focus and zoom remains bounded with keyboard reset", () => {
  render(<AnalyticsCountryMap geography={geography} />);
  const viewport = screen.getByRole("group", {
    name: "World map of redirect countries",
  });
  const zoomIn = screen.getByRole("button", { name: "Zoom in" });
  const zoomOut = screen.getByRole("button", { name: "Zoom out" });
  expect((zoomOut as HTMLButtonElement).disabled).toBe(true);
  expect(fireEvent.keyDown(viewport, { key: "+", ctrlKey: true })).toBe(true);
  expect(fireEvent.keyDown(viewport, { key: "-", metaKey: true })).toBe(true);
  expect(screen.getByLabelText("Map zoom").textContent).toBe("1×");
  fireEvent.focus(screen.getByRole("button", { name: "Sweden: 12 redirects" }));
  expect(screen.getByText("Sweden · 12 redirects")).toBeTruthy();
  expect(screen.getByText("Unknown country: 3 redirects.")).toBeTruthy();
  for (let i = 0; i < 10; i++) fireEvent.click(zoomIn);
  expect((zoomIn as HTMLButtonElement).disabled).toBe(true);
  expect(screen.getByLabelText("Map zoom").textContent).toBe("8×");
  fireEvent.keyDown(viewport, { key: "Home" });
  expect((zoomOut as HTMLButtonElement).disabled).toBe(true);
  fireEvent.keyDown(viewport, { key: "+" });
  expect(screen.getByLabelText("Map zoom").textContent).toBe("2×");
  fireEvent.click(screen.getByRole("button", { name: "Reset map" }));
  expect(screen.getByLabelText("Map zoom").textContent).toBe("1×");
});

test("unknown-only and empty ranges explain missing locations without inventing a country", () => {
  const { rerender } = render(
    <AnalyticsCountryMap geography={{ countries: [], unknownClicks: 4 }} />,
  );
  expect(
    screen.getByText("Country information is unavailable for these redirects."),
  ).toBeTruthy();
  expect(screen.getByText("Unknown country: 4 redirects.")).toBeTruthy();
  rerender(
    <AnalyticsCountryMap geography={{ countries: [], unknownClicks: 0 }} />,
  );
  expect(screen.getByText("No redirects in this date range.")).toBeTruthy();
});

test("countries absent from the small-scale geometry retain their counts explicitly", () => {
  render(
    <AnalyticsCountryMap
      geography={{
        countries: [
          { countryCode: "SJ", label: "Svalbard and Jan Mayen", count: 2 },
        ],
        unknownClicks: 0,
      }}
    />,
  );
  expect(
    screen.getByText(
      "2 redirects are from countries not shown on this map. See the Countries table.",
    ),
  ).toBeTruthy();
});

test("zero-activity country selection uses the country name for grouped territories", () => {
  render(
    <AnalyticsCountryMap geography={{ countries: [], unknownClicks: 0 }} />,
  );
  for (const country of ["Australia", "France"]) {
    fireEvent.focus(
      screen.getByRole("button", {
        name: `${country}: 0 redirects`,
      }),
    );
    expect(screen.getByText(`${country} · 0 redirects`)).toBeTruthy();
  }
});
