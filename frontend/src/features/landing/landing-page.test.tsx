import { cleanup, render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import HomePage from "../../app/page";
import { Providers } from "../../app/providers";

vi.mock("next/navigation", () => ({ usePathname: () => "/" }));
beforeEach(() => {
  vi.stubGlobal(
    "IntersectionObserver",
    class {
      observe = vi.fn();
      unobserve = vi.fn();
      disconnect = vi.fn();
    },
  );
  vi.stubGlobal("matchMedia", () => ({
    matches: false,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
  }));
});
afterEach(() => {
  cleanup();
  localStorage.clear();
  vi.unstubAllGlobals();
});

function setup() {
  render(
    <Providers>
      <HomePage />
    </Providers>,
  );
}

describe("public landing page", () => {
  it("explains the product with one H1, all sections and no session request", () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    setup();
    expect(
      screen.getByRole("heading", {
        level: 1,
        name: "Short links. Your control.",
      }),
    ).toBeTruthy();
    expect(screen.getAllByRole("heading", { level: 1 })).toHaveLength(1);
    expect(screen.getByRole("banner")).toBeTruthy();
    expect(screen.getByRole("main").id).toBe("main-content");
    expect(screen.getByRole("heading", { name: "Features", level: 2 }).id).toBe(
      "features",
    );
    expect(
      screen.getByRole("heading", { name: "How it works", level: 2 }).id,
    ).toBe("how-it-works");
    expect(
      screen.getByRole("heading", {
        name: "Short links. Big possibilities.",
      }),
    ).toBeTruthy();
    expect(
      within(
        screen.getByRole("list", { name: "Steps to your first short link" }),
      ).getAllByRole("listitem"),
    ).toHaveLength(3);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("distinguishes planned capabilities next to their headings", () => {
    setup();
    for (const name of ["Short links", "Custom aliases", "Expiry"])
      expect(screen.getByRole("heading", { name, level: 3 })).toBeTruthy();
    for (const name of ["Link management"]) {
      const article = screen.getByRole("article", { name: `${name} Planned` });
      expect(within(article).getByText("Planned")).toBeTruthy();
    }
    expect(screen.getAllByText("Planned")).toHaveLength(1);
    expect(
      screen.getByRole("article", { name: "Analytics Available now" }),
    ).toBeTruthy();
    expect(screen.getByText(/delayed by up to one minute/)).toBeTruthy();
  });

  it("offers only real sections and existing account routes, including the protected links entry", () => {
    setup();
    const footer = within(screen.getByRole("contentinfo"));
    expect(
      footer.getByRole("heading", { name: "Pages", level: 2 }),
    ).toBeTruthy();
    expect(
      footer.getByRole("heading", { name: "Account", level: 2 }),
    ).toBeTruthy();
    for (const [name, href] of [
      ["TinyRoute", "/"],
      ["Features", "/#features"],
      ["How it works", "/#how-it-works"],
      ["Get started", "/register"],
      ["Sign in", "/login"],
      ["Your links", "/links"],
    ])
      expect(footer.getByRole("link", { name }).getAttribute("href")).toBe(
        href,
      );
    expect(
      footer.getByText(`© ${new Date().getFullYear()} TinyRoute`),
    ).toBeTruthy();
    for (const link of screen.getAllByRole("link"))
      expect([
        "/",
        "/#features",
        "/#how-it-works",
        "/register",
        "/login",
        "/links",
        "#main-content",
      ]).toContain(link.getAttribute("href"));
    expect(
      screen.getAllByRole("button", { name: /Switch to .* mode/ }),
    ).toHaveLength(1);
    expect(screen.queryByRole("button", { name: /sign out/i })).toBeNull();
  });
});
