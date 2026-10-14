import {
  cleanup,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { useQueryClient } from "@tanstack/react-query";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { Providers } from "../app/providers";
import { ThemeSelector } from "../app/theme-selector";

const route = vi.hoisted(() => ({ pathname: "/login" }));
vi.mock("next/navigation", () => ({ usePathname: () => route.pathname }));

function QueryClientProbe() {
  const queryClient = useQueryClient();

  return <span>{queryClient ? "provider-ready" : "provider-missing"}</span>;
}

describe("Providers", () => {
  beforeEach(() => {
    route.pathname = "/login";
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

  it.each(["/", "/links"])(
    "keeps one header toggle on %s and one global toggle on auth routes",
    (pathname) => {
      route.pathname = pathname;
      const view = render(
        <Providers>
          <header>
            <ThemeSelector placement="inline" />
          </header>
        </Providers>,
      );
      expect(
        screen.getAllByRole("button", { name: /Switch to .* mode/ }),
      ).toHaveLength(1);
      expect(
        within(screen.getByRole("banner")).getByRole("button"),
      ).toBeTruthy();
      route.pathname = "/login";
      view.rerender(
        <Providers>
          <QueryClientProbe />
        </Providers>,
      );
      expect(
        screen.getAllByRole("button", { name: /Switch to .* mode/ }),
      ).toHaveLength(1);
      expect(screen.queryByRole("banner")).toBeNull();
    },
  );

  it("toggles between dark and light with a keyboard-accessible button and persists the choice", async () => {
    const user = userEvent.setup();

    const view = render(
      <Providers>
        <QueryClientProbe />
      </Providers>,
    );

    const toggle = await screen.findByRole("button", {
      name: "Switch to light mode",
    });
    await waitFor(() => {
      expect((toggle as HTMLButtonElement).disabled).toBe(false);
      expect(document.documentElement.dataset.theme).toBe("dark");
    });

    await user.tab();
    expect(document.activeElement).toBe(toggle);
    await user.keyboard("{Enter}");

    await waitFor(() => {
      expect(document.documentElement.dataset.theme).toBe("light");
    });
    expect(localStorage.getItem("tinyroute-color-theme")).toBe("light");
    expect(
      screen.getByRole("button", { name: "Switch to dark mode" }),
    ).toBeTruthy();

    view.unmount();
    render(
      <Providers>
        <QueryClientProbe />
      </Providers>,
    );
    expect(
      screen.getByRole("button", { name: "Switch to dark mode" }),
    ).toBeTruthy();
    await user.click(
      screen.getByRole("button", { name: "Switch to dark mode" }),
    );
    await waitFor(() => {
      expect(document.documentElement.dataset.theme).toBe("dark");
    });
    expect(localStorage.getItem("tinyroute-color-theme")).toBe("dark");
  });
});
