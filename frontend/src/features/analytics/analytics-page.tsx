"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, ArrowUpRight, RefreshCw } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { ApiClientError } from "../../lib/api-client";
import type { Session } from "../auth/auth-api";
import { sessionQueryKey, useSession } from "../auth/use-session";
import { SavedLinkHistory } from "../links/link-create-result";
import {
  analyticsQueryKey,
  getAnalytics,
  rangeParameters,
  type RangeQuery,
} from "./analytics-api";
import { AnalyticsRangeForm } from "./analytics-range-form";
import { AnalyticsReport } from "./analytics-report";

function first(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value;
}
export function AnalyticsPage({
  id,
  rangeQuery,
}: {
  id?: string;
  rangeQuery: RangeQuery;
}) {
  const session = useSession();
  const client = useQueryClient();
  const router = useRouter();
  const pathname = usePathname();
  const account = session.data?.authenticated ? session.data.user.email : null;
  const parameters = rangeParameters(rangeQuery).toString();
  const query = useQuery({
    queryKey: [...analyticsQueryKey, account, id ?? null, parameters],
    queryFn: ({ signal }) =>
      getAnalytics(
        { ...rangeQuery, id, expectedAccount: account ?? undefined },
        signal,
        (recovered) => {
          const current = client.getQueryData<Session>(sessionQueryKey);
          if (!current?.authenticated || current.user.email !== account)
            throw new Error("Analytics account changed");
          client.setQueryData(sessionQueryKey, recovered);
        },
      ),
    enabled: !!account,
    retry: false,
    staleTime: 0,
    refetchOnMount: "always",
  });
  useEffect(() => {
    if (!account) return;
    const key = [...analyticsQueryKey, account, id ?? null, parameters];
    return () => {
      void client.cancelQueries({ queryKey: key, exact: true });
      client.removeQueries({ queryKey: key, exact: true });
    };
  }, [client, account, id, parameters]);
  useEffect(() => {
    if (query.error instanceof ApiClientError && query.error.status === 401)
      client.setQueryData(sessionQueryKey, { authenticated: false });
  }, [client, query.error]);
  const error = query.error instanceof ApiClientError ? query.error : null;
  const report = !query.isError && !query.isFetching ? query.data : undefined;
  const range = report?.range;
  const validatedRange = query.data?.range;
  const suffix = parameters ? `?${parameters}` : "";
  function apply(values: { from: string; to: string }) {
    const next = rangeParameters(values).toString();
    if (next === parameters) void query.refetch();
    else router.push(`${pathname}?${next}`, { scroll: false });
  }
  if (session.isPending) return <p role="status">Checking your session.</p>;
  if (session.isError)
    return (
      <div className="analytics-state">
        <h1 className="settings-title">Analytics</h1>
        <p role="alert">We couldn’t check your session.</p>
        <button
          className="settings-button"
          onClick={() => void session.refetch()}
        >
          Retry session
        </button>
      </div>
    );
  if (!account)
    return (
      <div className="analytics-state">
        <h1 className="settings-title">Sign in to view analytics</h1>
        <p>Your link analytics are private.</p>
        <Link
          className="settings-button"
          href={`/login?returnTo=${encodeURIComponent(pathname + suffix)}`}
        >
          Sign in
        </Link>
      </div>
    );
  if (error?.status === 404)
    return (
      <section
        className="analytics-not-found"
        aria-labelledby="not-found-title"
      >
        <p className="analytics-not-found-status" aria-hidden="true">
          404
        </p>
        <div className="analytics-not-found-message">
          <h1 className="settings-title" id="not-found-title">
            Link not found
          </h1>
          <p>
            This link is unavailable. Return to analytics to view your links.
          </p>
          <Link
            className="settings-button settings-button-primary"
            href={`/analytics${suffix}`}
          >
            <ArrowLeft size={18} aria-hidden="true" />
            Back to analytics
          </Link>
        </div>
      </section>
    );
  return (
    <div className="analytics-content">
      <header className="analytics-heading">
        {id ? (
          <Link className="analytics-back" href={`/analytics${suffix}`}>
            <ArrowLeft size={16} aria-hidden="true" />
            All link analytics
          </Link>
        ) : null}
        <h1 className="settings-title">
          {id ? "Link analytics" : "Analytics"}
        </h1>
        <p className="analytics-muted">
          {id
            ? "A closer look at your short link."
            : "See how your links are performing."}
        </p>
        {report?.link ? (
          <div className="analytics-link-context">
            <p className="font-mono break-all">{report.link.shortUrl}</p>
            <a
              href={report.link.destinationUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="analytics-destination"
            >
              {report.link.destinationUrl}
              <ArrowUpRight size={16} aria-hidden="true" />
            </a>
          </div>
        ) : null}
      </header>
      {validatedRange || parameters ? (
        <AnalyticsRangeForm
          key={`${account}:${parameters}`}
          from={first(rangeQuery.from) ?? validatedRange?.from ?? ""}
          to={first(rangeQuery.to) ?? validatedRange?.to ?? ""}
          serverEnd={validatedRange?.to}
          errors={
            !query.isFetching ? error?.apiError?.error.fieldErrors : undefined
          }
          busy={query.isFetching}
          onApply={apply}
        />
      ) : null}
      <div className="analytics-freshness">
        <p>
          Clicks may take up to one minute to appear.
          {report ? (
            <>
              {" "}
              <span>
                As of{" "}
                <time dateTime={report.generatedAt}>
                  {new Date(report.generatedAt).toLocaleString("en-GB", {
                    timeZone: "UTC",
                    dateStyle: "medium",
                    timeStyle: "short",
                  })}{" "}
                  UTC
                </time>
                .
              </span>
            </>
          ) : null}
        </p>
        <button
          type="button"
          className="settings-button"
          disabled={query.isFetching}
          onClick={() => void query.refetch()}
        >
          <RefreshCw size={15} aria-hidden="true" />
          Refresh
        </button>
      </div>
      {query.isFetching || query.isPending ? (
        <p className="analytics-empty" role="status">
          Loading analytics.
        </p>
      ) : query.isError ? (
        <div className="analytics-state">
          <p className="analytics-error" role="alert">
            {error?.status === 400
              ? "Choose a valid reporting range. Check the date fields."
              : "We couldn’t load analytics. Please try again."}
          </p>
          <button
            className="settings-button"
            onClick={() => void query.refetch()}
          >
            Retry analytics
          </button>
        </div>
      ) : report ? (
        <AnalyticsReport report={report} />
      ) : null}
      {!id ? (
        <SavedLinkHistory
          key={account}
          accountKey={account}
          analyticsRange={range ?? validatedRange}
        />
      ) : null}
    </div>
  );
}
