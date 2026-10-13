import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { LandingHeader } from "./landing-header";

let media: MediaQueryList;
let change: ((event: MediaQueryListEvent) => void) | undefined;

beforeEach(() => {
  media = {
    matches: false,
    media: "(min-width: 1024px)",
    onchange: null,
    addEventListener: (_type: string, listener: unknown) => {
      change = listener as typeof change;
    },
    removeEventListener: () => {
      change = undefined;
    },
    addListener: vi.fn(),
    removeListener: vi.fn(),
    dispatchEvent: vi.fn(),
  } as MediaQueryList;
  vi.stubGlobal("matchMedia", () => media);
});
afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

function setup() {
  render(
    <>
      <style>{`.landing-desktop-nav { display: none; }`}</style>
      <LandingHeader />
      <main id="main-content" tabIndex={-1}>
        <h2 id="features" tabIndex={-1}>
          Features
        </h2>
        <h2 id="how-it-works" tabIndex={-1}>
          How it works
        </h2>
      </main>
    </>,
  );
  return {
    user: userEvent.setup(),
    trigger: screen.getByRole("button", { name: "Menu" }),
  };
}

describe("LandingHeader disclosure", () => {
  it.each([true, false])(
    "restores a blurred Menu trigger only when its breakpoint hides it (%s)",
    (breakpointBlur) => {
      const { trigger } = setup();
      trigger.focus();
      if (breakpointBlur)
        Object.defineProperty(media, "matches", {
          value: true,
          configurable: true,
        });
      trigger.blur();
      document.querySelector("style")!.textContent =
        ".landing-desktop-nav { display: flex; }";
      Object.defineProperty(media, "matches", {
        value: true,
        configurable: true,
      });
      act(() => change?.({ matches: true } as MediaQueryListEvent));
      expect(document.activeElement).toBe(
        breakpointBlur
          ? within(
              screen.getByRole("navigation", { name: "Primary navigation" }),
            ).getByRole("link", { name: "Sign in" })
          : document.body,
      );
    },
  );

  it("keeps closed links out of the tab order and supports Enter, Tab, Shift+Tab and Escape", async () => {
    const { user, trigger } = setup();
    expect(
      screen.queryByRole("navigation", { name: "Mobile navigation" }),
    ).toBeNull();
    trigger.focus();
    await user.keyboard("{Enter}");
    expect(trigger.getAttribute("aria-expanded")).toBe("true");
    expect(document.activeElement).toBe(trigger);
    await user.tab();
    expect(document.activeElement?.getAttribute("href")).toBe("/login");
    await user.tab({ shift: true });
    expect(document.activeElement).toBe(trigger);
    await user.tab();
    await user.keyboard("{Escape}");
    expect(trigger.getAttribute("aria-expanded")).toBe("false");
    expect(document.activeElement).toBe(trigger);
    expect(
      screen.queryByRole("navigation", { name: "Mobile navigation" }),
    ).toBeNull();
    await user.keyboard("{Enter}{Escape}");
    expect(trigger.getAttribute("aria-expanded")).toBe("false");
  });

  it("offers only account links in the header and mobile navigation", async () => {
    const { user, trigger } = setup();
    await user.click(trigger);
    const nav = within(
      screen.getByRole("navigation", { name: "Mobile navigation" }),
    );
    expect(nav.queryByRole("link", { name: "Features" })).toBeNull();
    expect(nav.queryByRole("link", { name: "How it works" })).toBeNull();
    expect(
      nav.getAllByRole("link").map((link) => link.getAttribute("href")),
    ).toEqual(["/login", "/register"]);
  });

  it("keeps modified account clicks native and closes on account navigation", async () => {
    const { user, trigger } = setup();
    await user.click(trigger);
    const nav = within(
      screen.getByRole("navigation", { name: "Mobile navigation" }),
    );
    const section = nav.getByRole("link", { name: "Sign in" });
    expect(fireEvent.click(section, { ctrlKey: true })).toBe(true);
    expect(trigger.getAttribute("aria-expanded")).toBe("true");
    expect(
      nav.getByRole("link", { name: "Sign in" }).getAttribute("href"),
    ).toBe("/login");
    const register = nav.getByRole("link", { name: "Get started" });
    expect(register.getAttribute("href")).toBe("/register");
    await user.click(register);
    expect(trigger.getAttribute("aria-expanded")).toBe("false");
  });

  it("transfers focus to the desktop equivalent when an open panel disappears and returns closed", async () => {
    const { user, trigger } = setup();
    await user.click(trigger);
    const link = within(
      screen.getByRole("navigation", { name: "Mobile navigation" }),
    ).getByRole("link", { name: "Sign in" });
    link.focus();
    document.querySelector("style")!.textContent =
      ".landing-desktop-nav { display: flex; }";
    Object.defineProperty(media, "matches", {
      value: true,
      configurable: true,
    });
    act(() => change?.({ matches: true } as MediaQueryListEvent));
    expect(trigger.getAttribute("aria-expanded")).toBe("false");
    expect(document.activeElement).toBe(
      within(
        screen.getByRole("navigation", {
          name: "Primary navigation",
        }),
      ).getByRole("link", { name: "Sign in" }),
    );
    document.querySelector("style")!.textContent =
      ".landing-desktop-nav { display: none; }";
    Object.defineProperty(media, "matches", { value: false });
    // Browsers blur a display:none link before dispatching the media change.
    (document.activeElement as HTMLElement).blur();
    expect(document.activeElement).toBe(document.body);
    act(() => change?.({ matches: false } as MediaQueryListEvent));
    expect(trigger.getAttribute("aria-expanded")).toBe("false");
    expect(document.activeElement).toBe(trigger);
  });
});
