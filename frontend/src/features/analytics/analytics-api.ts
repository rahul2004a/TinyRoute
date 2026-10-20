import { z } from "zod";
import { ApiClientError, apiRequest } from "../../lib/api-client";
import { refreshSession, type Session } from "../auth/auth-api";

const bucket = z.object({
  kind: z.enum(["VALUE", "UNKNOWN", "DIRECT_OR_UNKNOWN", "OTHER"]),
  label: z.string(),
  count: z.number().int().nonnegative(),
});
export const analyticsSchema = z.object({
  range: z.object({
    from: z.iso.date(),
    to: z.iso.date(),
    timeZone: z.literal("UTC"),
  }),
  generatedAt: z.iso.datetime(),
  recordingDelaySeconds: z.number().int().nonnegative(),
  totalClicks: z.number().int().nonnegative(),
  lifetimeClicks: z.number().int().nonnegative(),
  dailyClicks: z
    .array(
      z.object({ date: z.iso.date(), count: z.number().int().nonnegative() }),
    )
    .max(366),
  breakdowns: z.object({
    referrers: z.array(bucket).max(23),
    devices: z.array(bucket).max(23),
    operatingSystems: z.array(bucket).max(23),
    browsers: z.array(bucket).max(23),
    countries: z.array(bucket).max(23),
    cities: z.array(bucket).max(23),
  }),
  link: z
    .object({
      id: z.uuid(),
      code: z.string(),
      shortUrl: z.url(),
      destinationUrl: z.url(),
      status: z.enum(["ACTIVE", "DISABLED", "EXPIRED"]),
    })
    .optional(),
});
export type Analytics = z.infer<typeof analyticsSchema>;
export type RangeQuery = { from?: string | string[]; to?: string | string[] };
export const analyticsQueryKey = ["analytics"] as const;
export function rangeParameters(query: RangeQuery) {
  const parameters = new URLSearchParams();
  for (const name of ["from", "to"] as const) {
    const value = query[name];
    if (value !== undefined)
      for (const entry of Array.isArray(value) ? value : [value])
        parameters.append(name, entry);
  }
  return parameters;
}
export async function getAnalytics(
  input: RangeQuery & { id?: string; expectedAccount?: string },
  signal?: AbortSignal,
  onSessionRecovered?: (session: Session) => void,
) {
  const parameters = rangeParameters(input).toString();
  const path =
    input.id === undefined
      ? "/api/analytics"
      : `/api/links/${encodeURIComponent(input.id)}/analytics`;
  const read = () =>
    apiRequest(`${path}${parameters ? `?${parameters}` : ""}`, {
      method: "GET",
      cache: "no-store",
      signal,
      responseSchema: analyticsSchema,
    });
  try {
    return await read();
  } catch (error) {
    if (!(error instanceof ApiClientError && error.status === 401)) throw error;
    signal?.throwIfAborted();
    const recovered = await refreshSession();
    signal?.throwIfAborted();
    if (recovered) onSessionRecovered?.(recovered);
    if (
      input.expectedAccount &&
      (!recovered?.authenticated ||
        recovered.user.email !== input.expectedAccount)
    ) {
      throw new Error("Analytics account changed");
    }
    signal?.throwIfAborted();
    return read();
  }
}
