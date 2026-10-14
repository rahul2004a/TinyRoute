import { z } from "zod";
import { ApiClientError, apiRequest } from "../../lib/api-client";
import { clearCsrfToken, getCsrfToken } from "../../lib/csrf";
import { refreshSession } from "../auth/auth-api";

const createdLinkSchema = z.object({
  id: z.uuid(),
  code: z.string().min(1),
  shortUrl: z.url(),
  destinationUrl: z.string(),
  createdAt: z.iso.datetime(),
  expiresAt: z.iso.datetime().nullable(),
});
export type CreatedLink = z.infer<typeof createdLinkSchema>;
const savedLinkSchema = createdLinkSchema.extend({
  status: z.enum(["ACTIVE", "DISABLED", "EXPIRED"]),
  clickCount: z.number().int().nonnegative(),
});
const linkPageSchema = z.object({
  items: z.array(savedLinkSchema).max(50),
  nextCursor: z.string().min(1).nullable(),
});
export type SavedLink = z.infer<typeof savedLinkSchema>;
export const linkHistoryQueryKey = ["links", "history"] as const;

export async function getLinks(cursor?: string, signal?: AbortSignal) {
  const params = new URLSearchParams({ size: "10" });
  if (cursor !== undefined) params.set("cursor", cursor);
  const read = () =>
    apiRequest(`/api/links?${params}`, {
      method: "GET",
      signal,
      responseSchema: linkPageSchema,
    });
  try {
    return await read();
  } catch (error) {
    if (!(error instanceof ApiClientError && error.status === 401)) throw error;
    await refreshSession();
    return read();
  }
}
export type CreateLinkInput = {
  destinationUrl: string;
  alias?: string;
  expiresAt?: string;
};

/** Spring owns validation and policy; this request is never automatically retried. */
export async function createLink(input: CreateLinkInput): Promise<CreatedLink> {
  // Existing authentication may rotate the cookie on authenticated requests.
  // Bootstrap each manual mutation; never retry an ambiguous POST.
  clearCsrfToken();
  try {
    const csrf = await getCsrfToken();
    return await apiRequest("/api/links", {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-CSRF-TOKEN": csrf },
      body: JSON.stringify(input),
      responseSchema: createdLinkSchema,
    });
  } finally {
    clearCsrfToken();
  }
}
