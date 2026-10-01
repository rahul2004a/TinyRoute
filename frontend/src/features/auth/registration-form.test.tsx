import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
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
import { sessionQueryKey } from "./use-session";

const replaceMock = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: replaceMock }),
}));

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
const originalApiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;

function renderRegistrationForm() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return {
    queryClient,
    ...render(
      <QueryClientProvider client={queryClient}>
        <RegistrationForm />
      </QueryClientProvider>,
    ),
  };
}

describe("RegistrationForm", () => {
  afterEach(() => {
    cleanup();
    if (originalApiBaseUrl === undefined) {
      delete process.env.NEXT_PUBLIC_API_BASE_URL;
    } else {
      process.env.NEXT_PUBLIC_API_BASE_URL = originalApiBaseUrl;
    }
  });

  beforeEach(() => {
    fetchCsrfTokenMock.mockResolvedValue("csrf-token");
    startRegistrationMock.mockResolvedValue({ status: "PENDING_VERIFICATION" });
    resendRegistrationOtpMock.mockResolvedValue({
      status: "PENDING_VERIFICATION",
    });
    verifyRegistrationMock.mockResolvedValue({
      authenticated: true,
      user: { email: "person@example.com" },
    });
  });

  it("offers Google account creation through the backend authorization flow", () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "https://api.tinyroute.test";
    renderRegistrationForm();

    expect(
      screen
        .getByRole("link", { name: "Continue with Google" })
        .getAttribute("href"),
    ).toBe("https://api.tinyroute.test/api/auth/google/start");
  });

  it("links existing account holders to sign in", () => {
    renderRegistrationForm();

    expect(screen.getByText(/Already have an account/)).toBeTruthy();
    expect(
      screen.getByRole("link", { name: "Sign in" }).getAttribute("href"),
    ).toBe("/login");
  });

  it("submits accessible email and password fields with a CSRF token and shows the same generic success message", async () => {
    const user = userEvent.setup();
    renderRegistrationForm();

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
    renderRegistrationForm();

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
    renderRegistrationForm();

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

  it("stores the verified session and leaves the OTP screen", async () => {
    const user = userEvent.setup();
    const { queryClient } = renderRegistrationForm();

    await user.type(
      screen.getByLabelText("Email address"),
      "person@example.com",
    );
    await user.type(screen.getByLabelText("Password"), "correct-horse-battery");
    await user.click(screen.getByRole("button", { name: "Create account" }));
    await user.type(
      await screen.findByLabelText("Verification code"),
      "123456",
    );
    await user.click(screen.getByRole("button", { name: "Verify email" }));

    await waitFor(() => {
      expect(queryClient.getQueryData(sessionQueryKey)).toEqual({
        authenticated: true,
        user: { email: "person@example.com" },
      });
      expect(replaceMock).toHaveBeenCalledWith("/settings");
    });
    expect(screen.queryByLabelText("Verification code")).toBeNull();
    expect(screen.queryByRole("button", { name: "Resend code" })).toBeNull();
  });
});
