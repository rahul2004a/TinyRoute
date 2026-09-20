"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { Eye, EyeOff, Link2 } from "lucide-react";
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

function inputClassName(hasError: boolean, hasTopMargin = true) {
  return `${hasTopMargin ? "mt-2 " : ""}block min-h-11 w-full rounded-lg border bg-(--auth-surface) px-3 py-2 text-base text-(--auth-ink) outline-none transition placeholder:text-(--auth-ink-muted) focus-visible:border-(--auth-primary) focus-visible:ring-2 focus-visible:ring-(--auth-primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--auth-canvas) ${
    hasError ? "border-(--auth-danger)" : "border-(--auth-border)"
  }`;
}

function AuthBrand() {
  return (
    <div className="mb-14 flex items-center gap-2.5 text-sm font-semibold tracking-tight text-(--auth-ink)">
      <span className="grid size-8 place-items-center rounded-md bg-(--auth-ink) text-(--auth-canvas)">
        <Link2 aria-hidden="true" size={17} strokeWidth={2.25} />
      </span>
      TinyRoute
    </div>
  );
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
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const registrationForm = useForm<RegistrationValues>({
    defaultValues: { email: "", password: "" },
    resolver: zodResolver(registrationSchema),
  });
  const otpForm = useForm<OtpValues>({
    defaultValues: { otp: "" },
    resolver: zodResolver(otpSchema),
  });

  async function runSubmission(submission: () => Promise<void>) {
    setIsSubmitting(true);
    setSubmissionError("");
    try {
      await submission();
    } catch (error) {
      setSubmissionError(errorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function submitRegistration(values: RegistrationValues) {
    await runSubmission(async () => {
      const csrfToken = await fetchCsrfToken();
      await startRegistration(values, csrfToken);
      setNotice(genericRegistrationMessage);
      setStep("verify");
    });
  }

  async function submitOtp(values: OtpValues) {
    await runSubmission(async () => {
      const csrfToken = await fetchCsrfToken();
      await verifyRegistration(values.otp, csrfToken);
      setNotice("Your email is verified. You are signed in.");
    });
  }

  async function resendOtp() {
    await runSubmission(async () => {
      const csrfToken = await fetchCsrfToken();
      await resendRegistrationOtp(csrfToken);
      setNotice("A new verification code is on its way.");
    });
  }

  if (step === "verify") {
    return (
      <section aria-labelledby="verify-email-title" className="w-full max-w-md">
        <AuthBrand />
        <h1
          id="verify-email-title"
          className="text-3xl font-semibold tracking-[-0.8px] text-(--auth-ink) sm:text-4xl"
        >
          Verify your email
        </h1>
        <p className="mt-3 max-w-sm text-sm leading-6 text-(--auth-ink-muted)">
          Enter the six-digit code from your TinyRoute email.
        </p>
        {notice ? (
          <p
            className="mt-5 border-l-2 border-(--auth-primary) bg-(--auth-surface-2) px-4 py-3 text-sm leading-6 text-(--auth-ink)"
            role="status"
          >
            {notice}
          </p>
        ) : null}
        {submissionError ? (
          <p
            className="mt-5 text-sm font-medium text-(--auth-danger)"
            role="alert"
          >
            {submissionError}
          </p>
        ) : null}
        <form
          className="mt-7 space-y-5"
          onSubmit={otpForm.handleSubmit(submitOtp)}
        >
          <div>
            <label
              className="text-sm font-semibold text-(--auth-ink)"
              htmlFor="otp"
            >
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
              <p className="mt-2 text-sm text-(--auth-danger)" id="otp-error">
                {otpForm.formState.errors.otp.message}
              </p>
            ) : null}
          </div>
          <button
            className="min-h-11 w-full rounded-lg bg-(--auth-primary) px-4 py-2 font-medium text-(--auth-on-primary) transition hover:bg-(--auth-primary-hover) active:translate-y-px focus-visible:ring-2 focus-visible:ring-(--auth-primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--auth-canvas) disabled:cursor-not-allowed disabled:opacity-60"
            disabled={isSubmitting}
            type="submit"
          >
            Verify email
          </button>
        </form>
        <button
          className="mt-4 inline-flex min-h-11 w-full items-center justify-center rounded-lg border border-(--auth-border) bg-transparent px-4 py-2 text-sm font-medium text-(--auth-ink) transition hover:bg-(--auth-surface-2) active:translate-y-px focus-visible:ring-2 focus-visible:ring-(--auth-primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--auth-canvas) disabled:cursor-not-allowed disabled:opacity-60"
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
    <section aria-labelledby="register-title" className="w-full max-w-md">
      <AuthBrand />
      <h1
        id="register-title"
        className="text-3xl font-semibold tracking-[-0.8px] text-(--auth-ink) sm:text-4xl"
      >
        Create your account
      </h1>
      <p className="mt-3 max-w-sm text-sm leading-6 text-(--auth-ink-muted)">
        Use your email and a secure password to manage TinyRoute links.
      </p>
      {submissionError ? (
        <p
          className="mt-5 text-sm font-medium text-(--auth-danger)"
          role="alert"
        >
          {submissionError}
        </p>
      ) : null}
      <form
        className="mt-7 space-y-5"
        onSubmit={registrationForm.handleSubmit(submitRegistration)}
      >
        <div>
          <label
            className="text-sm font-semibold text-(--auth-ink)"
            htmlFor="email"
          >
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
            <p className="mt-2 text-sm text-(--auth-danger)" id="email-error">
              {registrationForm.formState.errors.email.message}
            </p>
          ) : null}
        </div>
        <div>
          <label
            className="text-sm font-semibold text-(--auth-ink)"
            htmlFor="password"
          >
            Password
          </label>
          <div className="relative mt-2">
            <input
              aria-describedby={
                registrationForm.formState.errors.password
                  ? "password-help password-error"
                  : "password-help"
              }
              autoComplete="new-password"
              className={`${inputClassName(
                Boolean(registrationForm.formState.errors.password),
                false,
              )} pr-12`}
              id="password"
              type={isPasswordVisible ? "text" : "password"}
              {...registrationForm.register("password")}
            />
            <button
              aria-label={isPasswordVisible ? "Hide password" : "Show password"}
              aria-pressed={isPasswordVisible}
              className="absolute top-1/2 right-1 grid size-10 -translate-y-1/2 place-items-center rounded-md text-(--auth-ink-muted) transition hover:text-(--auth-ink) focus-visible:ring-2 focus-visible:ring-(--auth-primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--auth-canvas)"
              onClick={() => setIsPasswordVisible((visible) => !visible)}
              type="button"
            >
              {isPasswordVisible ? (
                <EyeOff aria-hidden="true" size={18} />
              ) : (
                <Eye aria-hidden="true" size={18} />
              )}
            </button>
          </div>
          <p
            className="mt-2 text-sm text-(--auth-ink-muted)"
            id="password-help"
          >
            Use at least 12 characters.
          </p>
          {registrationForm.formState.errors.password ? (
            <p
              className="mt-2 text-sm text-(--auth-danger)"
              id="password-error"
            >
              {registrationForm.formState.errors.password.message}
            </p>
          ) : null}
        </div>
        <button
          className="min-h-11 w-full rounded-lg bg-(--auth-primary) px-4 py-2 font-medium text-(--auth-on-primary) transition hover:bg-(--auth-primary-hover) active:translate-y-px focus-visible:ring-2 focus-visible:ring-(--auth-primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--auth-canvas) disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          Create account
        </button>
      </form>
    </section>
  );
}
