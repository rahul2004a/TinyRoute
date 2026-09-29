import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { deleteAccount } from "./auth-api";
import { DeleteAccountPanel } from "./delete-account-panel";
import { sessionQueryKey, useSession } from "./use-session";

vi.mock("./auth-api", () => ({ deleteAccount: vi.fn() }));
vi.mock("./use-session", () => ({
  sessionQueryKey: ["auth", "session"],
  useSession: vi.fn(),
}));

const deleteAccountMock = vi.mocked(deleteAccount);
const useSessionMock = vi.mocked(useSession);
const signedInSession = {
  authenticated: true,
  user: { email: "person@example.com" },
};

function renderPanel() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  queryClient.setQueryData(sessionQueryKey, signedInSession);
  queryClient.setQueryData(["private", "links"], [{ code: "example" }]);
  return {
    queryClient,
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <DeleteAccountPanel />
      </QueryClientProvider>,
    ),
  };
}

describe("account deletion UI", () => {
  beforeEach(() => {
    useSessionMock.mockReturnValue({
      data: signedInSession,
      isPending: false,
      isError: false,
    } as ReturnType<typeof useSession>);
    deleteAccountMock.mockResolvedValue(undefined);
  });

  afterEach(() => cleanup());

  it("requires a separate confirmation and explains that links stop working", async () => {
    const { user } = renderPanel();

    expect(screen.getByText(/short links.*stop working/i)).toBeTruthy();
    await user.click(screen.getByRole("button", { name: "Delete account" }));

    expect(deleteAccountMock).not.toHaveBeenCalled();
    expect(
      screen.getByRole("button", { name: "Confirm deletion" }),
    ).toBeTruthy();
    expect(document.activeElement).toBe(
      screen.getByRole("heading", { name: "Confirm account deletion" }),
    );
    await user.click(screen.getByRole("button", { name: "Cancel" }));
    expect(deleteAccountMock).not.toHaveBeenCalled();
  });

  it("keeps client data until a confirmed response, then clears private cache and session", async () => {
    let confirmDeletion!: () => void;
    deleteAccountMock.mockReturnValueOnce(
      new Promise<void>((resolve) => {
        confirmDeletion = resolve;
      }),
    );
    const { queryClient, user } = renderPanel();

    await user.click(screen.getByRole("button", { name: "Delete account" }));
    await user.click(screen.getByRole("button", { name: "Confirm deletion" }));

    expect(deleteAccountMock).toHaveBeenCalledOnce();
    expect(queryClient.getQueryData(sessionQueryKey)).toEqual(signedInSession);
    expect(queryClient.getQueryData(["private", "links"])).toEqual([
      { code: "example" },
    ]);
    expect(screen.queryByText("Account deleted")).toBeNull();
    confirmDeletion();

    await waitFor(() => {
      expect(queryClient.getQueryData(sessionQueryKey)).toEqual({
        authenticated: false,
      });
    });
    expect(queryClient.getQueryData(["private", "links"])).toBeUndefined();
    expect(screen.getByRole("status").textContent).toMatch(/account.*deleted/i);
  });

  it("preserves state and offers retry after an unconfirmed failure", async () => {
    deleteAccountMock.mockRejectedValueOnce(new TypeError("network detail"));
    const { queryClient, user } = renderPanel();

    await user.click(screen.getByRole("button", { name: "Delete account" }));
    await user.click(screen.getByRole("button", { name: "Confirm deletion" }));

    expect((await screen.findByRole("alert")).textContent).toMatch(
      /couldn't confirm.*try again/i,
    );
    expect(screen.queryByText("network detail")).toBeNull();
    expect(queryClient.getQueryData(sessionQueryKey)).toEqual(signedInSession);
    expect(queryClient.getQueryData(["private", "links"])).toEqual([
      { code: "example" },
    ]);
    expect(screen.queryByText("Account deleted")).toBeNull();

    await user.click(screen.getByRole("button", { name: "Confirm deletion" }));
    await waitFor(() => expect(deleteAccountMock).toHaveBeenCalledTimes(2));
    expect(queryClient.getQueryData(sessionQueryKey)).toEqual({
      authenticated: false,
    });
  });

  it("does not show a destructive action without a signed-in session", () => {
    useSessionMock.mockReturnValue({
      data: { authenticated: false },
      isPending: false,
      isError: false,
    } as ReturnType<typeof useSession>);
    renderPanel();

    expect(screen.queryByRole("button", { name: "Delete account" })).toBeNull();
    expect(screen.getByRole("link", { name: "Sign in" })).toBeTruthy();
  });
});
