"use client";

import { useQuery } from "@tanstack/react-query";

import { getCurrentSession } from "./auth-api";

export const sessionQueryKey = ["auth", "session"] as const;

export function useSession() {
  return useQuery({
    queryFn: getCurrentSession,
    queryKey: sessionQueryKey,
    // The API lifecycle already bounds CSRF/rotation recovery. Replaying it
    // adds backoff delays and can reuse a refresh token after an ambiguous error.
    retry: false,
  });
}
