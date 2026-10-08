import { z } from "zod";

export function localExpiryToInstant(value: string): string | undefined {
  if (!value) return undefined;
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/.exec(
    value,
  );
  if (!match) throw new TypeError("Choose a valid local date and time.");
  const date = new Date(value);
  const parts = [
    date.getFullYear(),
    date.getMonth() + 1,
    date.getDate(),
    date.getHours(),
    date.getMinutes(),
    date.getSeconds(),
  ];
  if (
    !Number.isFinite(date.getTime()) ||
    parts.some((part, index) => part !== Number(match[index + 1] ?? 0))
  )
    throw new TypeError("Choose a valid local date and time.");
  return date.toISOString();
}
export const linkFormSchema = z.object({
  destinationUrl: z
    .string()
    .min(1, "Enter an HTTPS destination.")
    .max(8192, "Use 8192 characters or fewer.")
    .refine((value) => {
      try {
        return (
          /^https:\/\//i.test(value) && new URL(value).protocol === "https:"
        );
      } catch {
        return false;
      }
    }, "Enter a valid HTTPS destination."),
  alias: z
    .string()
    .refine(
      (value) => !value || /^[A-Za-z0-9_-]{3,64}$/.test(value),
      "Use 3–64 letters, numbers, hyphens, or underscores.",
    ),
  expiryLocal: z.string().refine((value) => {
    try {
      const instant = localExpiryToInstant(value);
      return !instant || Date.parse(instant) > Date.now();
    } catch {
      return false;
    }
  }, "Choose a valid future date and time."),
});
export type LinkFormValues = z.infer<typeof linkFormSchema>;
