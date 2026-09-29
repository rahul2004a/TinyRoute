import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClientError } from "../../lib/api-client";
import {
  confirmPasswordReset,
  fetchCsrfToken,
  requestPasswordReset,
} from "./auth-api";
import {
  PasswordResetConfirmForm,
  PasswordResetRequestForm,
} from "./password-reset-form";

vi.mock("./auth-api", () => ({
  confirmPasswordReset: vi.fn(),
  fetchCsrfToken: vi.fn(),
  requestPasswordReset: vi.fn(),
}));

const fetchCsrfTokenMock = vi.mocked(fetchCsrfToken);
const requestPasswordResetMock = vi.mocked(requestPasswordReset);
const confirmPasswordResetMock = vi.mocked(confirmPasswordReset);

describe("password-reset UI", () => {
  beforeEach(() => {
    window.history.replaceState(null, "", "/password-reset");
    fetchCsrfTokenMock.mockResolvedValue("csrf-fixture");
    requestPasswordResetMock.mockResolvedValue({ status: "ACCEPTED" });
    confirmPasswordResetMock.mockResolvedValue(undefined);
  });

  afterEach(() => cleanup());

  it("shows a generic request outcome for an email address", async () => {
    const user = userEvent.setup();
    render(<PasswordResetRequestForm />);

    await user.type(
      screen.getByLabelText("Email address"),
      "person@example.com",
    );
    await user.click(screen.getByRole("button", { name: "Send reset link" }));

    await waitFor(() =>
      expect(requestPasswordResetMock).toHaveBeenCalledWith(
        "person@example.com",
        "csrf-fixture",
      ),
    );
    expect(screen.getByRole("status").textContent).toMatch(
      /if.*account.*email/i,
    );
    expect(screen.queryByText(/account exists/i)).toBeNull();
  });

  it("removes the link fragment and confirms matching passwords without exposing the token in the URL", async () => {
    const user = userEvent.setup();
    const token = "opaque-reset-fixture";
    window.history.replaceState(
      null,
      "",
      `/password-reset/confirm#token=${token}`,
    );
    render(<PasswordResetConfirmForm />);

    await waitFor(() => expect(window.location.hash).toBe(""));
    await user.type(
      screen.getByLabelText("New password"),
      "correct-horse-battery",
    );
    await user.type(
      screen.getByLabelText("Confirm new password"),
      "correct-horse-battery",
    );
    await user.click(screen.getByRole("button", { name: "Reset password" }));

    await waitFor(() =>
      expect(confirmPasswordResetMock).toHaveBeenCalledWith(
        token,
        "correct-horse-battery",
        "csrf-fixture",
      ),
    );
    expect(window.location.href).not.toContain(token);
    expect(screen.getByRole("status").textContent).toMatch(/password.*reset/i);
  });

  it("rejects mismatched passwords before sending a request", async () => {
    const user = userEvent.setup();
    window.history.replaceState(
      null,
      "",
      "/password-reset/confirm#token=fixture",
    );
    render(<PasswordResetConfirmForm />);

    await user.type(
      screen.getByLabelText("New password"),
      "correct-horse-battery",
    );
    await user.type(
      screen.getByLabelText("Confirm new password"),
      "another-long-password",
    );
    await user.click(screen.getByRole("button", { name: "Reset password" }));

    expect(await screen.findByText("Passwords do not match.")).toBeTruthy();
    expect(confirmPasswordResetMock).not.toHaveBeenCalled();
  });

  it("shows the same safe recovery message for expired or used links", async () => {
    const user = userEvent.setup();
    window.history.replaceState(
      null,
      "",
      "/password-reset/confirm#token=fixture",
    );
    confirmPasswordResetMock.mockRejectedValue(
      new ApiClientError(400, {
        error: {
          code: "RESET_TOKEN_INVALID",
          message: "Internal token detail",
          requestId: "request-fixture",
        },
      }),
    );
    render(<PasswordResetConfirmForm />);

    await user.type(
      screen.getByLabelText("New password"),
      "correct-horse-battery",
    );
    await user.type(
      screen.getByLabelText("Confirm new password"),
      "correct-horse-battery",
    );
    await user.click(screen.getByRole("button", { name: "Reset password" }));

    expect((await screen.findByRole("alert")).textContent).toMatch(
      /invalid or expired/i,
    );
    expect(screen.queryByText("Internal token detail")).toBeNull();
  });

  it("does not submit a confirmation without a token fragment", async () => {
    render(<PasswordResetConfirmForm />);

    expect(screen.getByRole("alert").textContent).toMatch(
      /invalid or expired/i,
    );
    expect(screen.queryByRole("button", { name: "Reset password" })).toBeNull();
    expect(confirmPasswordResetMock).not.toHaveBeenCalled();
  });

  it("accepts a new reset link opened while the confirmation page is already mounted", async () => {
    render(<PasswordResetConfirmForm />);
    expect(screen.getByRole("alert").textContent).toMatch(
      /invalid or expired/i,
    );

    window.location.hash = "token=second-reset-fixture";

    expect(await screen.findByLabelText("New password")).toBeTruthy();
    expect(window.location.hash).toBe("");
  });
});
