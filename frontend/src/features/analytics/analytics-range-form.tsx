"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";

const schema = z.object({
  from: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Choose a valid start date."),
  to: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Choose a valid end date."),
});
type Values = z.infer<typeof schema>;
export function AnalyticsRangeForm({
  from,
  to,
  serverEnd,
  onApply,
  errors = {},
  busy = false,
}: {
  from: string;
  to: string;
  serverEnd?: string;
  onApply: (values: Values) => void;
  errors?: Record<string, string>;
  busy?: boolean;
}) {
  const form = useForm<Values>({
    defaultValues: { from, to },
    resolver: zodResolver(schema),
  });
  function preset(days: number) {
    if (!serverEnd) return;
    const date = new Date(`${serverEnd}T00:00:00Z`);
    date.setUTCDate(date.getUTCDate() - days + 1);
    const values = { from: date.toISOString().slice(0, 10), to: serverEnd };
    form.reset(values);
    onApply(values);
  }
  return (
    <section aria-label="Reporting dates" className="analytics-filter">
      <div className="analytics-presets" aria-label="Date range presets">
        {[7, 30, 90].map((days) => (
          <button
            type="button"
            className="settings-button"
            key={days}
            disabled={busy || !serverEnd}
            aria-pressed={
              !!serverEnd &&
              from ===
                new Date(
                  Date.parse(`${serverEnd}T00:00:00Z`) - (days - 1) * 86400000,
                )
                  .toISOString()
                  .slice(0, 10) &&
              to === serverEnd
            }
            onClick={() => preset(days)}
          >
            Last {days} days
          </button>
        ))}
      </div>
      <form
        onSubmit={form.handleSubmit((values) => onApply(values))}
        noValidate
      >
        {(["from", "to"] as const).map((field) => {
          const error = form.formState.errors[field]?.message ?? errors[field];
          return (
            <div className="analytics-date-field" key={field}>
              <label htmlFor={`analytics-${field}`}>
                {field === "from" ? "From" : "To"} (UTC)
              </label>
              <input
                id={`analytics-${field}`}
                type="date"
                {...form.register(field)}
                aria-invalid={!!error}
                aria-describedby={
                  error ? `analytics-${field}-error` : undefined
                }
              />
              {error ? (
                <p id={`analytics-${field}-error`} className="analytics-error">
                  {error}
                </p>
              ) : null}
            </div>
          );
        })}
        <button
          className="settings-button settings-button-primary"
          disabled={busy}
          type="submit"
        >
          Apply range
        </button>
      </form>
    </section>
  );
}
