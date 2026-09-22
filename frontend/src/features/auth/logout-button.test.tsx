import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClientError } from "../../lib/api-client";
import { logout } from "./auth-api";
import { LogoutButton } from "./logout-button";
import { sessionQueryKey } from "./use-session";

vi.mock("./auth-api", () => ({
  logout: vi.fn(),
}));

const logoutMock = vi.mocked(logout);

function renderLogoutButton() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  queryClient.setQueryData(sessionQueryKey, {
    authenticated: true,
    user: { email: "person@example.com" },
  });

  return {
    queryClient,
    user: userEvent.setup(),
    ...render(
      <QueryClientProvider client={queryClient}>
        <LogoutButton />
      </QueryClientProvider>,
    ),
  };
}

describe("LogoutButton", () => {
  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    logoutMock.mockResolvedValue(undefined);
  });

  it("clears the local session after the server has completed sign-out", async () => {
    const { queryClient, user } = renderLogoutButton();

    await user.click(screen.getByRole("button", { name: "Sign out" }));

    await waitFor(() => {
      expect(logoutMock).toHaveBeenCalledOnce();
      expect(queryClient.getQueryData(sessionQueryKey)).toEqual({
        authenticated: false,
      });
    });
    expect(screen.getByRole("status").textContent).toContain(
      "You are signed out.",
    );
  });

  it("keeps sign-out retryable after a network failure", async () => {
    const { queryClient, user } = renderLogoutButton();
    logoutMock.mockRejectedValueOnce(new TypeError("Network unavailable"));

    await user.click(screen.getByRole("button", { name: "Sign out" }));

    expect((await screen.findByRole("alert")).textContent).toContain(
      "We couldn't sign you out. Please try again.",
    );
    expect(queryClient.getQueryData(sessionQueryKey)).toEqual({
      authenticated: true,
      user: { email: "person@example.com" },
    });
    expect(
      (screen.getByRole("button", { name: "Sign out" }) as HTMLButtonElement)
        .disabled,
    ).toBe(false);

    await user.click(screen.getByRole("button", { name: "Sign out" }));

    await waitFor(() => {
      expect(logoutMock).toHaveBeenCalledTimes(2);
      expect(queryClient.getQueryData(sessionQueryKey)).toEqual({
        authenticated: false,
      });
    });
  });

  it("does not reveal server error details", async () => {
    const { user } = renderLogoutButton();
    logoutMock.mockRejectedValue(
      new ApiClientError(500, {
        error: {
          code: "SERVICE_UNAVAILABLE",
          message: "Redis connection details",
          requestId: "request-4",
        },
      }),
    );

    await user.click(screen.getByRole("button", { name: "Sign out" }));

    expect((await screen.findByRole("alert")).textContent).not.toContain(
      "Redis connection details",
    );
  });
});
