"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { logout } from "./auth-api";
import { sessionQueryKey } from "./use-session";

export function LogoutButton({ className = "" }: { className?: string }) {
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
    <div className={`mt-6 space-y-3 ${className}`}>
      {error ? (
        <p className="text-sm font-medium text-(--danger)" role="alert">
          {error}
        </p>
      ) : null}
      {signedOut ? (
        <p className="text-sm text-(--ink-muted)" role="status">
          You are signed out.
        </p>
      ) : null}
      <button
        className="min-h-11 rounded-lg border border-(--border) px-4 py-2 font-medium text-(--ink) transition hover:border-(--primary) focus-visible:ring-2 focus-visible:ring-(--primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--canvas) disabled:cursor-not-allowed disabled:opacity-60"
        disabled={isSigningOut}
        onClick={signOut}
        type="button"
      >
        {isSigningOut ? "Signing out…" : "Sign out"}
      </button>
    </div>
  );
}
