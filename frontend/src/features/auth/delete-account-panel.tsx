"use client";

import { useQueryClient } from "@tanstack/react-query";
import { Check, LogOut, Trash2, UserRound } from "lucide-react";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";

import { deleteAccount } from "./auth-api";
import { LogoutButton } from "./logout-button";
import { sessionQueryKey, useSession } from "./use-session";

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
      // Keep the session query's observers connected so sidebar identity clears
      // immediately, while removing every other private query and mutation.
      queryClient.removeQueries({
        predicate: ({ queryKey }) =>
          queryKey.length !== sessionQueryKey.length ||
          queryKey.some((part, index) => part !== sessionQueryKey[index]),
      });
      queryClient.getMutationCache().clear();
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
        className="settings-state"
        aria-labelledby="account-deleted-title"
      >
        <span aria-hidden="true" className="settings-state-icon">
          <Check size={24} />
        </span>
        <h1 id="account-deleted-title" className="settings-title">
          Account deleted
        </h1>
        <p className="mt-4" role="status">
          Your account was deleted. Your short links no longer work.
        </p>
        <Link
          className="settings-button settings-button-primary mt-6"
          href="/login"
        >
          Return to sign in
        </Link>
      </section>
    );
  }

  if (session.isPending)
    return (
      <div className="settings-state" role="status">
        <div aria-hidden="true" className="settings-skeleton" />
        <p>Checking your session.</p>
      </div>
    );
  if (session.isError) {
    return (
      <section className="settings-state" aria-labelledby="session-error-title">
        <h1 className="settings-title" id="session-error-title">
          Account settings
        </h1>
        <p className="mt-4" role="alert">
          We couldn’t check your session. Refresh and try again.
        </p>
        <button
          className="settings-button mt-6"
          onClick={() => void session.refetch()}
          type="button"
        >
          Try again
        </button>
      </section>
    );
  }
  if (!session.data?.authenticated) {
    return (
      <section className="settings-state" aria-labelledby="signed-out-title">
        <span aria-hidden="true" className="settings-state-icon">
          <UserRound size={24} />
        </span>
        <h1 className="settings-title" id="signed-out-title">
          Account settings
        </h1>
        <p className="mt-4">Sign in to manage your account.</p>
        <Link
          className="settings-button settings-button-primary mt-6"
          href="/login"
        >
          Sign in
        </Link>
      </section>
    );
  }

  return (
    <div className="settings-content">
      <header className="settings-heading">
        <h1 id="settings-title" className="settings-title">
          Account settings
        </h1>
        <p>Manage your account and your current session.</p>
      </header>
      <section className="settings-section" aria-labelledby="profile-title">
        <div className="settings-section-heading">
          <UserRound aria-hidden="true" size={19} />
          <h2 id="profile-title">Your account</h2>
        </div>
        <div className="settings-profile">
          <span
            aria-hidden="true"
            className="settings-avatar settings-avatar-large"
          >
            {session.data.user.email.charAt(0).toUpperCase()}
          </span>
          <div className="min-w-0">
            <p className="settings-field-label">Email address</p>
            <p className="settings-profile-email">{session.data.user.email}</p>
          </div>
          <span className="settings-status">
            <Check aria-hidden="true" size={14} />
            Signed in
          </span>
        </div>
      </section>
      <section
        className="settings-section settings-session"
        aria-labelledby="session-title"
      >
        <div>
          <div className="settings-section-heading">
            <LogOut aria-hidden="true" size={19} />
            <h2 id="session-title">Current session</h2>
          </div>
          <p>Sign out of TinyRoute on this browser.</p>
        </div>
        <LogoutButton className="settings-logout" />
      </section>
      <section
        aria-labelledby="delete-account-title"
        className="settings-danger"
      >
        <div className="settings-section-heading">
          <Trash2 aria-hidden="true" size={19} />
          <h2 id="delete-account-title">Delete your account</h2>
          <span className="settings-danger-label">Permanent action</span>
        </div>
        <p className="settings-danger-description">
          This permanently removes your account. Your short links will stop
          working, and you’ll be signed out of every session.
        </p>
        {confirming ? (
          <div
            aria-labelledby="delete-confirm-title"
            className="settings-confirmation"
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
            <div className="settings-confirm-actions">
              <button
                className="settings-button settings-button-danger-filled"
                disabled={submitting}
                onClick={confirmDeletion}
                type="button"
              >
                {submitting ? "Deleting…" : "Confirm deletion"}
              </button>
              <button
                className="settings-button"
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
            className="settings-button settings-button-danger mt-6"
            onClick={() => setConfirming(true)}
            ref={triggerRef}
            type="button"
          >
            Delete account
          </button>
        )}
      </section>
    </div>
  );
}
