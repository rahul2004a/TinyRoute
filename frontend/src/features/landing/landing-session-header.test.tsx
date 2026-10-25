import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  act,
  cleanup,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { getCurrentSession, type Session } from "../auth/auth-api";
import { sessionQueryKey } from "../auth/use-session";
import { LandingSessionHeader } from "./landing-session-header";

vi.mock("../auth/auth-api", () => ({ getCurrentSession: vi.fn() }));

const signedIn: Session = {
  authenticated: true,
  user: { email: "header@example.com" },
};
let client: QueryClient;

beforeEach(() => {
  client = new QueryClient({
    defaultOptions: { queries: { staleTime: 60_000, gcTime: 0 } },
  });
  vi.stubGlobal("matchMedia", () => ({
    matches: false,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  }));
});

afterEach(() => {
  cleanup();
  client.clear();
  vi.unstubAllGlobals();
});

function setup(session?: Session) {
  if (session) client.setQueryData(sessionQueryKey, session);
  render(
    <QueryClientProvider client={client}>
      <LandingSessionHeader />
    </QueryClientProvider>,
  );
  return within(screen.getByRole("navigation", { name: "Primary navigation" }));
}

describe("landing session navigation", () => {
  it("replaces guest links for an existing session in desktop and mobile navigation", async () => {
    const nav = setup(signedIn);
    expect(
      nav.getByRole("link", { name: "Your links" }).getAttribute("href"),
    ).toBe("/links");
    expect(
      nav.getByRole("link", { name: "Account settings" }).getAttribute("href"),
    ).toBe("/settings");
    expect(screen.queryByRole("link", { name: "Sign in" })).toBeNull();
    expect(screen.queryByRole("link", { name: "Get started" })).toBeNull();
    await userEvent.click(screen.getByRole("button", { name: "Menu" }));
    const mobile = within(
      screen.getByRole("navigation", { name: "Mobile navigation" }),
    );
    expect(
      mobile.getAllByRole("link").map((link) => link.getAttribute("href")),
    ).toEqual(["/links", "/settings"]);
    expect(getCurrentSession).not.toHaveBeenCalled();
  });

  it("shows guest links after the backend confirms a signed-out session", async () => {
    vi.mocked(getCurrentSession).mockResolvedValue({ authenticated: false });
    const nav = setup();
    expect(
      (await nav.findByRole("link", { name: "Sign in" })).getAttribute("href"),
    ).toBe("/login");
    expect(
      nav.getByRole("link", { name: "Get started" }).getAttribute("href"),
    ).toBe("/register");
  });

  it("does not flash guest links while checking an existing session", async () => {
    let complete!: (session: Session) => void;
    vi.mocked(getCurrentSession).mockReturnValue(
      new Promise((resolve) => {
        complete = resolve;
      }),
    );
    const nav = setup();
    expect(nav.queryByRole("link", { name: "Sign in" })).toBeNull();
    expect(nav.queryByRole("link", { name: "Get started" })).toBeNull();
    await act(async () => complete(signedIn));
    expect(
      await nav.findByRole("link", { name: "Account settings" }),
    ).toBeTruthy();
    expect(nav.queryByRole("link", { name: "Sign in" })).toBeNull();
  });

  it.each(["desktop", "mobile"])(
    "preserves %s keyboard focus when a pending session resolves signed out",
    async (viewport) => {
      let complete!: (session: Session) => void;
      vi.mocked(getCurrentSession).mockReturnValue(
        new Promise((resolve) => {
          complete = resolve;
        }),
      );
      let nav = setup();
      if (viewport === "mobile") {
        await userEvent.click(screen.getByRole("button", { name: "Menu" }));
        nav = within(
          screen.getByRole("navigation", { name: "Mobile navigation" }),
        );
      }
      nav.getByRole("link", { name: "Your links" }).focus();
      await act(async () => complete({ authenticated: false }));
      const replacement = await nav.findByRole("link", { name: "Sign in" });
      expect(document.activeElement).toBe(replacement);
    },
  );

  it("does not move focus from another control when session links change", async () => {
    const nav = setup(signedIn);
    nav.getByRole("link", { name: "Account settings" }).focus();
    const menu = screen.getByRole("button", { name: "Menu" });
    menu.focus();
    act(() => {
      client.setQueryData(sessionQueryKey, { authenticated: false });
    });
    await nav.findByRole("link", { name: "Sign in" });
    expect(document.activeElement).toBe(menu);
  });

  it("updates immediately when sign-out changes the shared session cache", async () => {
    const nav = setup(signedIn);
    act(() => {
      client.setQueryData(sessionQueryKey, { authenticated: false });
    });
    expect(await nav.findByRole("link", { name: "Sign in" })).toBeTruthy();
    expect(nav.queryByRole("link", { name: "Account settings" })).toBeNull();
  });

  it("retains known signed-in navigation when a background session check fails", async () => {
    vi.mocked(getCurrentSession).mockRejectedValue(
      new Error("Session unavailable"),
    );
    const nav = setup(signedIn);
    await act(async () => {
      await client.invalidateQueries({ queryKey: sessionQueryKey });
    });
    await waitFor(() =>
      expect(client.getQueryState(sessionQueryKey)?.status).toBe("error"),
    );
    expect(nav.getByRole("link", { name: "Account settings" })).toBeTruthy();
    expect(nav.queryByRole("link", { name: "Sign in" })).toBeNull();
  });

  it("keeps a neutral links entry when the session cannot be confirmed", async () => {
    vi.mocked(getCurrentSession).mockRejectedValue(
      new Error("Session unavailable"),
    );
    const nav = setup();
    await waitFor(() =>
      expect(client.getQueryState(sessionQueryKey)?.status).toBe("error"),
    );
    expect(
      nav.getByRole("link", { name: "Your links" }).getAttribute("href"),
    ).toBe("/links");
    expect(nav.queryByRole("link", { name: "Sign in" })).toBeNull();
    expect(nav.queryByRole("link", { name: "Account settings" })).toBeNull();
  });
});
