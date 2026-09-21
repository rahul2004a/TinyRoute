import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClientError } from "../../lib/api-client";
import { LoginForm } from "./login-form";
import { fetchCsrfToken, getCurrentSession, login } from "./auth-api";

vi.mock("./auth-api", () => ({
  fetchCsrfToken: vi.fn(),
  getCurrentSession: vi.fn(),
  login: vi.fn(),
}));

const fetchCsrfTokenMock = vi.mocked(fetchCsrfToken);
const getCurrentSessionMock = vi.mocked(getCurrentSession);
const loginMock = vi.mocked(login);

function renderLoginForm() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <LoginForm />
    </QueryClientProvider>,
  );
}

describe("LoginForm", () => {
  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    fetchCsrfTokenMock.mockResolvedValue("csrf-token");
    getCurrentSessionMock.mockResolvedValue({ authenticated: false });
    loginMock.mockResolvedValue(undefined);
  });

  it("submits labelled credentials, then derives the signed-in UI from me", async () => {
    const user = userEvent.setup();
    getCurrentSessionMock
      .mockResolvedValueOnce({ authenticated: false })
      .mockResolvedValueOnce({
        authenticated: true,
        user: { email: "person@example.com" },
      });
    renderLoginForm();

    await user.type(
      await screen.findByLabelText("Email address"),
      "person@example.com",
    );
    await user.type(screen.getByLabelText("Password"), "correct-horse-battery");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() => {
      expect(loginMock).toHaveBeenCalledWith(
        { email: "person@example.com", password: "correct-horse-battery" },
        "csrf-token",
      );
    });
    expect((await screen.findByRole("status")).textContent).toContain(
      "Signed in as person@example.com.",
    );
    expect(screen.queryByLabelText("Password")).toBeNull();
  });

  it("announces a rate limit safely, preserves the email, and clears the password", async () => {
    const user = userEvent.setup();
    loginMock.mockRejectedValue(
      new ApiClientError(429, {
        error: {
          code: "RATE_LIMITED",
          message: "Internal rate-limit detail",
          requestId: "request-123",
          retryAfterSeconds: 60,
        },
      }),
    );
    renderLoginForm();

    const email = await screen.findByLabelText("Email address");
    const password = screen.getByLabelText("Password");
    await user.type(email, "person@example.com");
    await user.type(password, "correct-horse-battery");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect((await screen.findByRole("alert")).textContent).toContain(
      "Too many sign-in attempts. Try again in 60 seconds.",
    );
    expect(screen.queryByText("Internal rate-limit detail")).toBeNull();
    expect((email as HTMLInputElement).value).toBe("person@example.com");
    expect((password as HTMLInputElement).value).toBe("");
  });

  it("renders the signed-out form again when the session has expired", async () => {
    renderLoginForm();

    expect(await screen.findByRole("button", { name: "Sign in" })).toBeTruthy();
    expect(screen.queryByText(/^Signed in as /)).toBeNull();
  });
});
