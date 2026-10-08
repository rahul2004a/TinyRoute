import { z } from "zod";
import { apiRequest } from "../../lib/api-client";
import { getCsrfToken } from "../../lib/csrf";

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
  const csrf = await getCsrfToken();
  return apiRequest("/api/links", {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-CSRF-TOKEN": csrf },
    body: JSON.stringify(input),
    responseSchema: createdLinkSchema,
  });
}
