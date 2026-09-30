"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useQueryClient } from "@tanstack/react-query";
import { Eye, EyeOff } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { ApiClientError } from "../../lib/api-client";
import { AuthBrand } from "./auth-brand";
import { fetchCsrfToken, login } from "./auth-api";
import { GoogleSignInButton } from "./google-sign-in-button";
import { LogoutButton } from "./logout-button";
import { sessionQueryKey, useSession } from "./use-session";

const loginSchema = z.object({
  email: z.string().trim().email("Enter a valid email address."),
  password: z.string().min(1, "Enter your password."),
});

type LoginValues = z.infer<typeof loginSchema>;

function loginErrorMessage(error: unknown): string {
  if (
    error instanceof ApiClientError &&
    error.status === 429 &&
    error.apiError?.error.retryAfterSeconds !== undefined
  ) {
    return `Too many sign-in attempts. Try again in ${error.apiError.error.retryAfterSeconds} seconds.`;
  }

  return "We couldn't sign you in. Check your email and password and try again.";
}

function inputClassName(hasError: boolean, hasTopMargin = true) {
  return `${hasTopMargin ? "mt-2 " : ""}block min-h-11 w-full rounded-lg border bg-(--auth-surface) px-3 py-2 text-base text-(--auth-ink) outline-none transition placeholder:text-(--auth-ink-muted) focus-visible:border-(--auth-primary) focus-visible:ring-2 focus-visible:ring-(--auth-primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--auth-canvas) ${
    hasError ? "border-(--auth-danger)" : "border-(--auth-border)"
  }`;
}

export function LoginForm({
  oauthFailed = false,
}: Readonly<{ oauthFailed?: boolean }>) {
  const queryClient = useQueryClient();
  const session = useSession();
  const [submissionError, setSubmissionError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const form = useForm<LoginValues>({
    defaultValues: { email: "", password: "" },
    resolver: zodResolver(loginSchema),
  });

  async function submit(values: LoginValues) {
    setIsSubmitting(true);
    setSubmissionError("");
    try {
      const csrfToken = await fetchCsrfToken();
      await login(values, csrfToken);
      await queryClient.invalidateQueries({ queryKey: sessionQueryKey });
    } catch (error) {
      form.setValue("password", "");
      setSubmissionError(loginErrorMessage(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  if (session.isPending) {
    return <p role="status">Checking your session.</p>;
  }

  if (session.data?.authenticated) {
    return (
      <section aria-labelledby="signed-in-title" className="w-full max-w-md">
        <AuthBrand />
        <h1
          className="text-3xl font-semibold tracking-[-0.8px] text-(--auth-ink) sm:text-4xl"
          id="signed-in-title"
        >
          You’re signed in
        </h1>
        <p
          className="mt-3 text-sm leading-6 text-(--auth-ink-muted)"
          role="status"
        >
          Signed in as {session.data.user.email}.
        </p>
        <LogoutButton />
        <Link
          className="mt-4 inline-flex min-h-11 items-center text-sm underline underline-offset-4 focus-visible:ring-2 focus-visible:ring-(--auth-primary)"
          href="/settings"
        >
          Account settings
        </Link>
      </section>
    );
  }

  return (
    <section aria-labelledby="login-title" className="w-full max-w-md">
      <AuthBrand />
      <h1
        className="text-3xl font-semibold tracking-[-0.8px] text-(--auth-ink) sm:text-4xl"
        id="login-title"
      >
        Sign in to TinyRoute
      </h1>
      <p className="mt-3 max-w-sm text-sm leading-6 text-(--auth-ink-muted)">
        Manage your links and see how they perform.
      </p>
      {session.isError ? (
        <p
          className="mt-5 text-sm font-medium text-(--auth-danger)"
          role="alert"
        >
          We couldn’t check your session. Please refresh and try again.
        </p>
      ) : null}
      {oauthFailed ? (
        <p
          className="mt-5 text-sm font-medium text-(--auth-danger)"
          role="alert"
        >
          Google sign-in could not be completed. Please try again.
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
      <form className="mt-7 space-y-5" onSubmit={form.handleSubmit(submit)}>
        <div>
          <label
            className="text-sm font-semibold text-(--auth-ink)"
            htmlFor="email"
          >
            Email address
          </label>
          <input
            aria-describedby={
              form.formState.errors.email ? "email-error" : undefined
            }
            aria-invalid={Boolean(form.formState.errors.email)}
            autoComplete="email"
            className={inputClassName(Boolean(form.formState.errors.email))}
            id="email"
            type="email"
            {...form.register("email")}
          />
          {form.formState.errors.email ? (
            <p className="mt-2 text-sm text-(--auth-danger)" id="email-error">
              {form.formState.errors.email.message}
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
                form.formState.errors.password ? "password-error" : undefined
              }
              aria-invalid={Boolean(form.formState.errors.password)}
              autoComplete="current-password"
              className={`${inputClassName(Boolean(form.formState.errors.password), false)} pr-12`}
              id="password"
              type={isPasswordVisible ? "text" : "password"}
              {...form.register("password")}
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
          {form.formState.errors.password ? (
            <p
              className="mt-2 text-sm text-(--auth-danger)"
              id="password-error"
            >
              {form.formState.errors.password.message}
            </p>
          ) : null}
        </div>
        <button
          className="min-h-11 w-full rounded-lg bg-(--auth-primary) px-4 py-2 font-medium text-(--auth-on-primary) transition hover:bg-(--auth-primary-hover) active:translate-y-px focus-visible:ring-2 focus-visible:ring-(--auth-primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--auth-canvas) disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          Sign in
        </button>
      </form>
      <Link
        className="mt-4 inline-flex min-h-11 items-center text-sm underline underline-offset-4 focus-visible:ring-2 focus-visible:ring-(--auth-primary)"
        href="/password-reset"
      >
        Forgot password?
      </Link>
      <GoogleSignInButton />
      <p className="mt-6 text-center text-sm text-(--auth-ink-muted)">
        New to TinyRoute?{" "}
        <Link
          className="rounded-sm font-semibold text-(--auth-ink) underline underline-offset-4 focus-visible:ring-2 focus-visible:ring-(--auth-primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--auth-canvas)"
          href="/register"
        >
          Create an account
        </Link>
      </p>
    </section>
  );
}
