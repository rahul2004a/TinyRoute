"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { logout } from "./auth-api";
import { sessionQueryKey } from "./use-session";

export function LogoutButton() {
  const queryClient = useQueryClient();
  const [error, setError] = useState("");
  const [isSigningOut, setIsSigningOut] = useState(false);
  const [signedOut, setSignedOut] = useState(false);

  async function signOut() {
    setError("");
    setIsSigningOut(true);
    try {
      await logout();
      queryClient.setQueryData(sessionQueryKey, { authenticated: false });
      setSignedOut(true);
    } catch {
      setError("We couldn't sign you out. Please try again.");
    } finally {
      setIsSigningOut(false);
    }
  }

  return (
    <div className="mt-6 space-y-3">
      {error ? (
        <p className="text-sm font-medium text-(--auth-danger)" role="alert">
          {error}
        </p>
      ) : null}
      {signedOut ? (
        <p className="text-sm text-(--auth-ink-muted)" role="status">
          You are signed out.
        </p>
      ) : null}
      <button
        className="min-h-11 rounded-lg border border-(--auth-border) px-4 py-2 font-medium text-(--auth-ink) transition hover:border-(--auth-primary) focus-visible:ring-2 focus-visible:ring-(--auth-primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--auth-canvas) disabled:cursor-not-allowed disabled:opacity-60"
        disabled={isSigningOut}
        onClick={signOut}
        type="button"
      >
        {isSigningOut ? "Signing out…" : "Sign out"}
      </button>
    </div>
  );
}
