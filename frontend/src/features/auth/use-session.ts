"use client";

import { useQuery } from "@tanstack/react-query";

import { getCurrentSession } from "./auth-api";

export const sessionQueryKey = ["auth", "session"] as const;

export function useSession() {
  return useQuery({
    queryFn: getCurrentSession,
    queryKey: sessionQueryKey,
  });
}
