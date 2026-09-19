"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { ApiClientError } from "../../lib/api-client";
import {
  fetchCsrfToken,
  resendRegistrationOtp,
  startRegistration,
  verifyRegistration,
} from "./auth-api";

const registrationSchema = z.object({
  email: z.string().trim().email("Enter a valid email address."),
  password: z.string().min(12, "Use at least 12 characters."),
});

const otpSchema = z.object({
  otp: z.string().regex(/^\d{6}$/, "Enter the six-digit code from your email."),
});

type RegistrationValues = z.infer<typeof registrationSchema>;
type OtpValues = z.infer<typeof otpSchema>;
type RegistrationStep = "register" | "verify";

const genericRegistrationMessage =
  "If the address can receive a TinyRoute verification email, a code is on its way.";

function errorMessage(error: unknown): string {
  if (error instanceof ApiClientError && error.apiError) {
    return error.apiError.error.message;
  }
  return "We couldn't complete that request. Please try again.";
}

function inputClassName(hasError: boolean) {
  return `mt-2 block min-h-11 w-full rounded-lg border bg-white px-3 py-2 text-base text-(--foreground) outline-none transition placeholder:text-(--muted-text) focus-visible:ring-2 focus-visible:ring-(--accent) focus-visible:ring-offset-2 ${
    hasError ? "border-red-700" : "border-(--border)"
  }`;
}

export function RegistrationForm({
  initialStep = "register",
}: Readonly<{ initialStep?: RegistrationStep }>) {
  const [step, setStep] = useState<RegistrationStep>(initialStep);
  const [notice, setNotice] = useState(
    initialStep === "verify" ? genericRegistrationMessage : "",
  );
  const [submissionError, setSubmissionError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const registrationForm = useForm<RegistrationValues>({
    defaultValues: { email: "", password: "" },
    resolver: zodResolver(registrationSchema),
  });
  const otpForm = useForm<OtpValues>({
    defaultValues: { otp: "" },
    resolver: zodResolver(otpSchema),
  });

  async function submitRegistration(values: RegistrationValues) {
    setIsSubmitting(true);
    setSubmissionError("");
    try {
      const csrfToken = await fetchCsrfToken();
      await startRegistration(values, csrfToken);
      setNotice(genericRegistrationMessage);
      setStep("verify");
    } catch (error) {
      setSubmissionError(errorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function submitOtp(values: OtpValues) {
    setIsSubmitting(true);
    setSubmissionError("");
    try {
      const csrfToken = await fetchCsrfToken();
      await verifyRegistration(values.otp, csrfToken);
      setNotice("Your email is verified. You are signed in.");
    } catch (error) {
      setSubmissionError(errorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function resendOtp() {
    setIsSubmitting(true);
    setSubmissionError("");
    try {
      const csrfToken = await fetchCsrfToken();
      await resendRegistrationOtp(csrfToken);
      setNotice("A new verification code is on its way.");
    } catch (error) {
      setSubmissionError(errorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  if (step === "verify") {
    return (
      <section
        aria-labelledby="verify-email-title"
        className="w-full max-w-md rounded-xl border border-(--border) bg-white p-6 sm:p-8"
      >
        <h1
          id="verify-email-title"
          className="text-3xl font-semibold tracking-tight"
        >
          Verify your email
        </h1>
        <p className="mt-3 text-sm leading-6 text-(--muted-text)">
          Enter the six-digit code from your TinyRoute email.
        </p>
        {notice ? (
          <p className="mt-4 text-sm text-(--accent)" role="status">
            {notice}
          </p>
        ) : null}
        {submissionError ? (
          <p className="mt-4 text-sm text-red-700" role="alert">
            {submissionError}
          </p>
        ) : null}
        <form
          className="mt-6 space-y-5"
          onSubmit={otpForm.handleSubmit(submitOtp)}
        >
          <div>
            <label className="text-sm font-medium" htmlFor="otp">
              Verification code
            </label>
            <input
              aria-describedby={
                otpForm.formState.errors.otp ? "otp-error" : undefined
              }
              aria-invalid={Boolean(otpForm.formState.errors.otp)}
              autoComplete="one-time-code"
              className={inputClassName(Boolean(otpForm.formState.errors.otp))}
              id="otp"
              inputMode="numeric"
              maxLength={6}
              {...otpForm.register("otp")}
            />
            {otpForm.formState.errors.otp ? (
              <p className="mt-2 text-sm text-red-700" id="otp-error">
                {otpForm.formState.errors.otp.message}
              </p>
            ) : null}
          </div>
          <button
            className="min-h-11 w-full rounded-lg bg-(--accent) px-4 py-2 font-medium text-white focus-visible:ring-2 focus-visible:ring-(--accent) focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={isSubmitting}
            type="submit"
          >
            Verify email
          </button>
        </form>
        <button
          className="mt-4 min-h-11 rounded-lg px-2 py-2 text-sm font-medium text-(--accent) underline underline-offset-4 focus-visible:ring-2 focus-visible:ring-(--accent) focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          onClick={resendOtp}
          type="button"
        >
          Resend code
        </button>
      </section>
    );
  }

  return (
    <section
      aria-labelledby="register-title"
      className="w-full max-w-md rounded-xl border border-(--border) bg-white p-6 sm:p-8"
    >
      <h1 id="register-title" className="text-3xl font-semibold tracking-tight">
        Create your account
      </h1>
      <p className="mt-3 text-sm leading-6 text-(--muted-text)">
        Use your email and a secure password to manage TinyRoute links.
      </p>
      {submissionError ? (
        <p className="mt-4 text-sm text-red-700" role="alert">
          {submissionError}
        </p>
      ) : null}
      <form
        className="mt-6 space-y-5"
        onSubmit={registrationForm.handleSubmit(submitRegistration)}
      >
        <div>
          <label className="text-sm font-medium" htmlFor="email">
            Email address
          </label>
          <input
            aria-describedby={
              registrationForm.formState.errors.email
                ? "email-error"
                : undefined
            }
            aria-invalid={Boolean(registrationForm.formState.errors.email)}
            autoComplete="email"
            className={inputClassName(
              Boolean(registrationForm.formState.errors.email),
            )}
            id="email"
            type="email"
            {...registrationForm.register("email")}
          />
          {registrationForm.formState.errors.email ? (
            <p className="mt-2 text-sm text-red-700" id="email-error">
              {registrationForm.formState.errors.email.message}
            </p>
          ) : null}
        </div>
        <div>
          <label className="text-sm font-medium" htmlFor="password">
            Password
          </label>
          <input
            aria-describedby={
              registrationForm.formState.errors.password
                ? "password-help password-error"
                : "password-help"
            }
            autoComplete="new-password"
            className={inputClassName(
              Boolean(registrationForm.formState.errors.password),
            )}
            id="password"
            type="password"
            {...registrationForm.register("password")}
          />
          <p className="mt-2 text-sm text-(--muted-text)" id="password-help">
            Use at least 12 characters.
          </p>
          {registrationForm.formState.errors.password ? (
            <p className="mt-2 text-sm text-red-700" id="password-error">
              {registrationForm.formState.errors.password.message}
            </p>
          ) : null}
        </div>
        <button
          className="min-h-11 w-full rounded-lg bg-(--accent) px-4 py-2 font-medium text-white focus-visible:ring-2 focus-visible:ring-(--accent) focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          Create account
        </button>
      </form>
    </section>
  );
}
