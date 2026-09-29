import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { useQueryClient } from "@tanstack/react-query";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { Providers } from "../app/providers";

function QueryClientProbe() {
  const queryClient = useQueryClient();

  return <span>{queryClient ? "provider-ready" : "provider-missing"}</span>;
}

describe("Providers", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "matchMedia",
      vi.fn().mockImplementation((query: string) => ({
        addEventListener: vi.fn(),
        addListener: vi.fn(),
        dispatchEvent: vi.fn(),
        matches: false,
        media: query,
        onchange: null,
        removeEventListener: vi.fn(),
        removeListener: vi.fn(),
      })),
    );
  });

  afterEach(() => {
    cleanup();
    localStorage.clear();
    vi.unstubAllGlobals();
  });

  it("provides a query client without making an application request", () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    render(
      <Providers>
        <QueryClientProbe />
      </Providers>,
    );

    expect(screen.getByText("provider-ready")).toBeTruthy();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("offers keyboard-accessible system, light, and dark theme choices", async () => {
    const user = userEvent.setup();

    render(
      <Providers>
        <QueryClientProbe />
      </Providers>,
    );

    const themeSelect = await screen.findByLabelText("Theme");
    await waitFor(() => {
      expect((themeSelect as HTMLSelectElement).disabled).toBe(false);
    });
    expect(
      Array.from((themeSelect as HTMLSelectElement).options).map(
        ({ value }) => value,
      ),
    ).toEqual(["system", "light", "dark"]);

    await user.selectOptions(themeSelect, "dark");

    await waitFor(() => {
      expect(document.documentElement.dataset.theme).toBe("dark");
    });
  });
});
