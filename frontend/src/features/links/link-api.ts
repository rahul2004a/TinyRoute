import { z } from "zod";
import { apiRequest } from "../../lib/api-client";
import { clearCsrfToken, getCsrfToken } from "../../lib/csrf";

const createdLinkSchema = z.object({
  id: z.uuid(),
  code: z.string().min(1),
  shortUrl: z.url(),
  destinationUrl: z.string(),
  createdAt: z.iso.datetime(),
  expiresAt: z.iso.datetime().nullable(),
});
export type CreatedLink = z.infer<typeof createdLinkSchema>;
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
