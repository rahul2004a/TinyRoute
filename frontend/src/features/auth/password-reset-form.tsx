"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { ApiClientError } from "../../lib/api-client";
import {
  confirmPasswordReset,
  fetchCsrfToken,
  requestPasswordReset,
} from "./auth-api";

const requestSchema = z.object({
  email: z.string().trim().email("Enter a valid email address."),
});

const confirmSchema = z
  .object({
    newPassword: z
      .string()
      .min(12, "Use at least 12 characters.")
      .max(128, "Use no more than 128 characters."),
    confirmPassword: z.string(),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    message: "Passwords do not match.",
    path: ["confirmPassword"],
  });

type RequestValues = z.infer<typeof requestSchema>;
type ConfirmValues = z.infer<typeof confirmSchema>;

const invalidLinkMessage =
  "This reset link is invalid or expired. Request a new link.";
const genericRequestMessage =
  "If an account uses that email address, a reset link is on its way.";
const inputClass =
  "mt-2 block min-h-11 w-full rounded-lg border border-(--auth-border) bg-(--auth-surface) px-3 py-2 text-base text-(--auth-ink) outline-none focus-visible:border-(--auth-primary) focus-visible:ring-2 focus-visible:ring-(--auth-primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--auth-canvas) aria-invalid:border-(--auth-danger)";
const buttonClass =
  "min-h-11 w-full rounded-lg bg-(--auth-primary) px-4 py-2 font-medium text-(--auth-on-primary) transition hover:bg-(--auth-primary-hover) active:translate-y-px focus-visible:ring-2 focus-visible:ring-(--auth-primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--auth-canvas) disabled:cursor-not-allowed disabled:opacity-60";

function errorMessage(error: unknown): string {
  if (error instanceof ApiClientError) {
    if (error.apiError?.error.code === "RESET_TOKEN_INVALID")
      return invalidLinkMessage;
    if (error.status === 429)
      return "Too many attempts. Please wait and try again.";
  }
  return "We couldn't complete that request. Please try again.";
}

export function PasswordResetRequestForm() {
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const form = useForm<RequestValues>({
    defaultValues: { email: "" },
    resolver: zodResolver(requestSchema),
  });

  async function submit(values: RequestValues) {
    setSubmitting(true);
    setError("");
    try {
      const csrfToken = await fetchCsrfToken();
      await requestPasswordReset(values.email, csrfToken);
      setStatus(genericRequestMessage);
    } catch (cause) {
      setError(errorMessage(cause));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section aria-labelledby="reset-request-title" className="w-full max-w-md">
      <h1
        id="reset-request-title"
        className="text-3xl font-semibold tracking-[-0.8px] text-(--auth-ink) sm:text-4xl"
      >
        Reset your password
      </h1>
      <p className="mt-3 text-sm leading-6 text-(--auth-ink-muted)">
        Enter your email address to receive a reset link.
      </p>
      {status ? (
        <p className="mt-5 text-sm text-(--auth-ink)" role="status">
          {status}
        </p>
      ) : null}
      {error ? (
        <p className="mt-5 text-sm text-(--auth-danger)" role="alert">
          {error}
        </p>
      ) : null}
      <form className="mt-7 space-y-5" onSubmit={form.handleSubmit(submit)}>
        <div>
          <label
            className="text-sm font-semibold text-(--auth-ink)"
            htmlFor="reset-email"
          >
            Email address
          </label>
          <input
            id="reset-email"
            type="email"
            autoComplete="email"
            aria-invalid={Boolean(form.formState.errors.email)}
            aria-describedby={
              form.formState.errors.email ? "reset-email-error" : undefined
            }
            className={inputClass}
            {...form.register("email")}
          />
          {form.formState.errors.email ? (
            <p
              id="reset-email-error"
              className="mt-2 text-sm text-(--auth-danger)"
            >
              {form.formState.errors.email.message}
            </p>
          ) : null}
        </div>
        <button className={buttonClass} disabled={submitting} type="submit">
          Send reset link
        </button>
      </form>
      <Link
        className="mt-5 inline-flex min-h-11 items-center text-sm underline underline-offset-4 focus-visible:ring-2 focus-visible:ring-(--auth-primary)"
        href="/login"
      >
        Back to sign in
      </Link>
    </section>
  );
}

export function PasswordResetConfirmForm() {
  const [token, setToken] = useState<string | null | undefined>(undefined);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const fragmentRead = useRef(false);
  const form = useForm<ConfirmValues>({
    defaultValues: { newPassword: "", confirmPassword: "" },
    resolver: zodResolver(confirmSchema),
  });
  const resetForm = form.reset;

  useEffect(() => {
    function readFragment() {
      const fragment = window.location.hash.slice(1);
      if (!fragment && fragmentRead.current) return;
      fragmentRead.current = true;
      window.history.replaceState(null, "", window.location.pathname);
      setToken(new URLSearchParams(fragment).get("token") || null);
      setStatus("");
      setError("");
      resetForm();
    }

    readFragment();
    window.addEventListener("hashchange", readFragment);
    return () => window.removeEventListener("hashchange", readFragment);
  }, [resetForm]);

  async function submit(values: ConfirmValues) {
    if (!token) return;
    setSubmitting(true);
    setError("");
    try {
      const csrfToken = await fetchCsrfToken();
      await confirmPasswordReset(token, values.newPassword, csrfToken);
      setToken(null);
      form.reset();
      setStatus(
        "Your password has been reset. Sign in with your new password.",
      );
    } catch (cause) {
      if (
        cause instanceof ApiClientError &&
        cause.apiError?.error.code === "RESET_TOKEN_INVALID"
      ) {
        setToken(null);
      } else {
        setError(errorMessage(cause));
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section aria-labelledby="reset-confirm-title" className="w-full max-w-md">
      <h1
        id="reset-confirm-title"
        className="text-3xl font-semibold tracking-[-0.8px] text-(--auth-ink) sm:text-4xl"
      >
        Choose a new password
      </h1>
      {status ? (
        <p className="mt-5 text-sm text-(--auth-ink)" role="status">
          {status}
        </p>
      ) : null}
      {token === null && !status ? (
        <p className="mt-5 text-sm text-(--auth-danger)" role="alert">
          {invalidLinkMessage}
        </p>
      ) : null}
      {error ? (
        <p className="mt-5 text-sm text-(--auth-danger)" role="alert">
          {error}
        </p>
      ) : null}
      {token ? (
        <form className="mt-7 space-y-5" onSubmit={form.handleSubmit(submit)}>
          <div>
            <label
              className="text-sm font-semibold text-(--auth-ink)"
              htmlFor="new-password"
            >
              New password
            </label>
            <input
              id="new-password"
              type="password"
              autoComplete="new-password"
              aria-invalid={Boolean(form.formState.errors.newPassword)}
              aria-describedby={
                form.formState.errors.newPassword
                  ? "new-password-error"
                  : "new-password-help"
              }
              className={inputClass}
              {...form.register("newPassword")}
            />
            <p
              id="new-password-help"
              className="mt-2 text-sm text-(--auth-ink-muted)"
            >
              Use 12 to 128 characters.
            </p>
            {form.formState.errors.newPassword ? (
              <p
                id="new-password-error"
                className="mt-2 text-sm text-(--auth-danger)"
              >
                {form.formState.errors.newPassword.message}
              </p>
            ) : null}
          </div>
          <div>
            <label
              className="text-sm font-semibold text-(--auth-ink)"
              htmlFor="confirm-password"
            >
              Confirm new password
            </label>
            <input
              id="confirm-password"
              type="password"
              autoComplete="new-password"
              aria-invalid={Boolean(form.formState.errors.confirmPassword)}
              aria-describedby={
                form.formState.errors.confirmPassword
                  ? "confirm-password-error"
                  : undefined
              }
              className={inputClass}
              {...form.register("confirmPassword")}
            />
            {form.formState.errors.confirmPassword ? (
              <p
                id="confirm-password-error"
                className="mt-2 text-sm text-(--auth-danger)"
              >
                {form.formState.errors.confirmPassword.message}
              </p>
            ) : null}
          </div>
          <button className={buttonClass} disabled={submitting} type="submit">
            Reset password
          </button>
        </form>
      ) : null}
      <Link
        className="mt-5 inline-flex min-h-11 items-center text-sm underline underline-offset-4 focus-visible:ring-2 focus-visible:ring-(--auth-primary)"
        href={status ? "/login" : "/password-reset"}
      >
        {status ? "Sign in" : "Request a new link"}
      </Link>
    </section>
  );
}
