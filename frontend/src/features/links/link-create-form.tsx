"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { ApiClientError } from "../../lib/api-client";
import { clearCsrfToken } from "../../lib/csrf";
import { sessionQueryKey, useSession } from "../auth/use-session";
import { createLink, type CreatedLink } from "./link-api";
import {
  linkFormSchema,
  localExpiryToInstant,
  type LinkFormValues,
} from "./link-form-schema";
import { LinkCreateResult } from "./link-create-result";

export function LinkCreateForm() {
  const queryClient = useQueryClient();
  const session = useSession();
  const mutation = useMutation({ mutationFn: createLink, retry: false });
  const locked = useRef(false);
  const [submissionError, setSubmissionError] = useState("");
  const [recovering, setRecovering] = useState(false);
  const [lastResult, setLastResult] = useState<CreatedLink>();
  const form = useForm<LinkFormValues>({
    resolver: zodResolver(linkFormSchema),
    defaultValues: { destinationUrl: "", alias: "", expiryLocal: "" },
  });
  async function submit(values: LinkFormValues) {
    if (locked.current) return;
    locked.current = true;
    setSubmissionError("");
    try {
      const created = await mutation.mutateAsync({
        destinationUrl: values.destinationUrl,
        ...(values.alias ? { alias: values.alias } : {}),
        ...(values.expiryLocal
          ? { expiresAt: localExpiryToInstant(values.expiryLocal) }
          : {}),
      });
      setLastResult(created);
    } catch (error) {
      if (error instanceof ApiClientError) {
        const detail = error.apiError?.error;
        if (error.status === 401) {
          setRecovering(true);
          await queryClient.invalidateQueries({ queryKey: sessionQueryKey });
          setSubmissionError(
            "Your session was rechecked. Submit again to create your link, or sign in if needed.",
          );
        } else if (error.status === 403 && detail?.code === "CSRF_INVALID") {
          clearCsrfToken();
          setSubmissionError(
            "Your security token needs refreshing. Submit again to create your link.",
          );
        } else if (detail?.code === "VALIDATION_ERROR") {
          const mapping = {
            destinationUrl: "destinationUrl",
            alias: "alias",
            expiresAt: "expiryLocal",
          } as const;
          for (const [serverField, field] of Object.entries(mapping)) {
            const message = detail.fieldErrors?.[serverField];
            if (message) form.setError(field, { type: "server", message });
          }
          setSubmissionError("Check the highlighted fields and try again.");
        } else if (detail?.code === "ALIAS_UNAVAILABLE") {
          form.setError("alias", {
            type: "server",
            message: "This alias is already used. Choose another.",
          });
          setSubmissionError("Choose a different custom alias and try again.");
        } else if (detail?.code === "CODE_ALLOCATION_FAILED")
          setSubmissionError(
            "We couldn’t allocate a code. Submit again to try once more.",
          );
        else if (error.status === 429)
          setSubmissionError(
            detail?.retryAfterSeconds !== undefined
              ? `Creation limit reached. Try again in ${detail.retryAfterSeconds} seconds.`
              : "Creation limit reached. Please try again later.",
          );
        else if (error.status === 503)
          setSubmissionError(
            "Creation is temporarily unavailable. Keep your inputs and try again shortly.",
          );
        else
          setSubmissionError(
            "We couldn’t confirm creation. Creation may have succeeded. Check your last result before trying again.",
          );
      } else
        setSubmissionError(
          "Creation may have succeeded. Check your last result before trying again.",
        );
    } finally {
      locked.current = false;
      setRecovering(false);
    }
  }
  if (session.isPending) return <p role="status">Checking your session.</p>;
  if (session.isError)
    return (
      <div>
        <p role="alert">We couldn’t check your session. Please try again.</p>
        <button
          type="button"
          className="link-secondary mt-4"
          onClick={() => void session.refetch()}
        >
          Retry session check
        </button>
      </div>
    );
  if (!session.data?.authenticated)
    return (
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">
          Create a short link
        </h1>
        <p className="mt-3">
          Sign in to turn an HTTPS destination into a short link.
        </p>
        <Link
          href="/login"
          className="link-action mt-6 inline-flex items-center justify-center"
        >
          Sign in
        </Link>
      </div>
    );
  const fields = [
    {
      name: "destinationUrl",
      label: "HTTPS destination",
      helper: "Keep the full path, query, and fragment.",
      type: "url",
      placeholder: "https://example.com/docs?q=java#setup",
    },
    {
      name: "alias",
      label: "Custom alias (optional)",
      helper: "3–64 letters, numbers, hyphens, or underscores. Case matters.",
      type: "text",
      placeholder: "e.g. Docs2026",
    },
    {
      name: "expiryLocal",
      label: "Expiry (optional)",
      helper: "Your local date and time. Leave empty for no expiry.",
      type: "datetime-local",
      placeholder: undefined,
    },
  ] as const;
  return (
    <section aria-labelledby="create-link-title">
      <h1
        id="create-link-title"
        className="text-3xl font-semibold tracking-[-0.8px] sm:text-4xl"
      >
        Create a short link
      </h1>
      <p className="mt-3 max-w-prose text-(--ink-muted)">
        An HTTPS destination, ready to share in one click.
      </p>
      <form
        noValidate
        className="mt-8 space-y-6"
        onSubmit={(event) => {
          void form.handleSubmit(submit)(event);
        }}
      >
        {fields.map((field) => {
          const error = form.formState.errors[field.name];
          return (
            <div key={field.name}>
              <label className="text-sm font-semibold" htmlFor={field.name}>
                {field.label}
              </label>
              <p
                id={`${field.name}-help`}
                className="mt-1 text-sm leading-6 text-(--ink-muted)"
              >
                {field.helper}
              </p>
              <input
                id={field.name}
                type={field.type}
                placeholder={field.placeholder}
                className="link-input mt-2 w-full"
                aria-invalid={Boolean(error)}
                aria-describedby={`${field.name}-help${error ? ` ${field.name}-error` : ""}`}
                autoComplete="off"
                {...form.register(field.name)}
              />
              {error ? (
                <p
                  id={`${field.name}-error`}
                  className="mt-2 text-sm text-(--danger)"
                >
                  {error.message}
                </p>
              ) : null}
            </div>
          );
        })}
        {submissionError ? (
          <p role="alert" className="text-sm text-(--danger)">
            {submissionError}
          </p>
        ) : null}
        <button
          type="submit"
          className="link-action w-full sm:w-auto"
          disabled={
            mutation.isPending || form.formState.isSubmitting || recovering
          }
        >
          {mutation.isPending
            ? "Creating…"
            : recovering
              ? "Checking session…"
              : "Create short link"}
        </button>
      </form>
      {lastResult ? (
        <LinkCreateResult key={lastResult.id} link={lastResult} />
      ) : null}
    </section>
  );
}
