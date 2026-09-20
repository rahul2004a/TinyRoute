import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { ApiClientError } from "../../lib/api-client";
import { RegistrationForm } from "./registration-form";
import {
  fetchCsrfToken,
  resendRegistrationOtp,
  startRegistration,
  verifyRegistration,
} from "./auth-api";

vi.mock("./auth-api", () => ({
  fetchCsrfToken: vi.fn(),
  resendRegistrationOtp: vi.fn(),
  startRegistration: vi.fn(),
  verifyRegistration: vi.fn(),
}));

const fetchCsrfTokenMock = vi.mocked(fetchCsrfToken);
const resendRegistrationOtpMock = vi.mocked(resendRegistrationOtp);
const startRegistrationMock = vi.mocked(startRegistration);
const verifyRegistrationMock = vi.mocked(verifyRegistration);

describe("RegistrationForm", () => {
  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    fetchCsrfTokenMock.mockResolvedValue("csrf-token");
    startRegistrationMock.mockResolvedValue({ status: "PENDING_VERIFICATION" });
    resendRegistrationOtpMock.mockResolvedValue({
      status: "PENDING_VERIFICATION",
    });
  });

  it("submits accessible email and password fields with a CSRF token and shows the same generic success message", async () => {
    const user = userEvent.setup();
    render(<RegistrationForm />);

    expect(screen.getByText("TinyRoute")).toBeTruthy();

    await user.type(
      screen.getByLabelText("Email address"),
      "person@example.com",
    );
    await user.type(screen.getByLabelText("Password"), "correct-horse-battery");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    await waitFor(() => {
      expect(startRegistrationMock).toHaveBeenCalledWith(
        { email: "person@example.com", password: "correct-horse-battery" },
        "csrf-token",
      );
    });
    expect(screen.getByRole("status").textContent).toContain(
      "If the address can receive a TinyRoute verification email, a code is on its way.",
    );
    expect(screen.queryByText(/already exists/i)).toBeNull();
  });

  it("lets a person reveal and re-mask their password without changing it", async () => {
    const user = userEvent.setup();
    render(<RegistrationForm />);

    const password = screen.getByLabelText("Password");
    await user.type(password, "correct-horse-battery");

    expect(password.getAttribute("type")).toBe("password");
    expect(
      screen
        .getByRole("button", { name: "Show password" })
        .getAttribute("aria-pressed"),
    ).toBe("false");

    await user.click(screen.getByRole("button", { name: "Show password" }));

    expect(password.getAttribute("type")).toBe("text");
    expect((password as HTMLInputElement).value).toBe("correct-horse-battery");
    expect(
      screen
        .getByRole("button", { name: "Hide password" })
        .getAttribute("aria-pressed"),
    ).toBe("true");

    await user.keyboard("{Enter}");
    expect(password.getAttribute("type")).toBe("password");
  });

  it("shows an invalid OTP response and allows a keyboard user to resend a code", async () => {
    const user = userEvent.setup();
    verifyRegistrationMock.mockRejectedValue(
      new ApiClientError(400, {
        error: {
          code: "OTP_INVALID",
          message: "The verification code is invalid.",
          requestId: "request-123",
        },
      }),
    );
    render(<RegistrationForm />);

    await user.type(
      screen.getByLabelText("Email address"),
      "person@example.com",
    );
    await user.type(screen.getByLabelText("Password"), "correct-horse-battery");
    await user.click(screen.getByRole("button", { name: "Create account" }));

    const otp = await screen.findByLabelText("Verification code");
    await user.type(otp, "000000");
    await user.keyboard("{Enter}");

    await waitFor(() => {
      expect(verifyRegistrationMock).toHaveBeenCalledWith(
        "000000",
        "csrf-token",
      );
    });
    expect(screen.getByRole("alert").textContent).toContain(
      "The verification code is invalid.",
    );

    await user.click(screen.getByRole("button", { name: "Resend code" }));
    await waitFor(() => {
      expect(resendRegistrationOtpMock).toHaveBeenCalledWith("csrf-token");
    });
    expect(screen.getByRole("status").textContent).toContain(
      "A new verification code is on its way.",
    );
  });
});
