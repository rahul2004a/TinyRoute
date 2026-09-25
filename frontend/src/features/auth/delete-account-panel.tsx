"use client";

import { useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";

import { deleteAccount } from "./auth-api";
import { sessionQueryKey, useSession } from "./use-session";

const buttonClass =
  "min-h-11 rounded-lg px-4 py-2 font-medium transition focus-visible:ring-2 focus-visible:ring-(--primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--canvas) disabled:cursor-not-allowed disabled:opacity-60";

export function DeleteAccountPanel() {
  const session = useSession();
  const queryClient = useQueryClient();
  const [confirming, setConfirming] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [deleted, setDeleted] = useState(false);
  const [error, setError] = useState("");
  const triggerRef = useRef<HTMLButtonElement>(null);
  const confirmationRef = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    if (confirming) confirmationRef.current?.focus();
  }, [confirming]);

  async function confirmDeletion() {
    setError("");
    setSubmitting(true);
    try {
      await deleteAccount();
      queryClient.clear();
      queryClient.setQueryData(sessionQueryKey, { authenticated: false });
      setDeleted(true);
    } catch {
      setError("We couldn't confirm deletion. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  function cancelDeletion() {
    setConfirming(false);
    setError("");
    requestAnimationFrame(() => triggerRef.current?.focus());
  }

  if (deleted) {
    return (
      <section
        className="mx-auto max-w-xl"
        aria-labelledby="account-deleted-title"
      >
        <h1 id="account-deleted-title" className="text-3xl font-semibold">
          Account deleted
        </h1>
        <p className="mt-4" role="status">
          Your account was deleted. Your short links no longer work.
        </p>
        <Link
          className="mt-6 inline-flex min-h-11 items-center underline"
          href="/login"
        >
          Return to sign in
        </Link>
      </section>
    );
  }

  if (session.isPending) return <p role="status">Checking your session.</p>;
  if (session.isError) {
    return (
      <p role="alert">We couldn’t check your session. Refresh and try again.</p>
    );
  }
  if (!session.data?.authenticated) {
    return (
      <p>
        Sign in to manage your account. <Link href="/login">Sign in</Link>
      </p>
    );
  }

  return (
    <section className="mx-auto max-w-xl" aria-labelledby="settings-title">
      <h1 id="settings-title" className="text-3xl font-semibold tracking-tight">
        Account settings
      </h1>
      <p className="mt-3 text-(--ink-muted)">
        Signed in as {session.data.user.email}.
      </p>
      <section
        aria-labelledby="delete-account-title"
        className="mt-12 border-t border-(--border) pt-8"
      >
        <h2 id="delete-account-title" className="text-xl font-semibold">
          Delete your account
        </h2>
        <p className="mt-3 text-(--ink-muted)">
          This permanently removes your account. Your short links will stop
          working, and you’ll be signed out of every session.
        </p>
        {confirming ? (
          <div
            aria-labelledby="delete-confirm-title"
            className="mt-6 space-y-4"
          >
            <h3
              id="delete-confirm-title"
              ref={confirmationRef}
              tabIndex={-1}
              className="text-lg font-semibold focus:outline-none"
            >
              Confirm account deletion
            </h3>
            <p>This action cannot be undone. Delete your account?</p>
            {error ? (
              <p role="alert" className="text-sm text-(--danger)">
                {error}
              </p>
            ) : null}
            <div className="flex flex-wrap gap-3">
              <button
                className={`${buttonClass} bg-(--danger) text-(--canvas)`}
                disabled={submitting}
                onClick={confirmDeletion}
                type="button"
              >
                {submitting ? "Deleting…" : "Confirm deletion"}
              </button>
              <button
                className={`${buttonClass} border border-(--border)`}
                disabled={submitting}
                onClick={cancelDeletion}
                type="button"
              >
                Cancel
              </button>
            </div>
          </div>
        ) : (
          <button
            className={`${buttonClass} mt-6 border border-(--danger) text-(--danger)`}
            onClick={() => setConfirming(true)}
            ref={triggerRef}
            type="button"
          >
            Delete account
          </button>
        )}
      </section>
    </section>
  );
}
