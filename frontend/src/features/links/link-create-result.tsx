"use client";

import Link from "next/link";
import { Copy } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useRef, useState } from "react";
import {
  getLinks,
  linkHistoryQueryKey,
  type CreatedLink,
  type SavedLink,
} from "./link-api";

export function LinkCreateResult({ link }: Readonly<{ link: CreatedLink }>) {
  const [message, setMessage] = useState("Short link created.");
  const [copying, setCopying] = useState(false);
  const url = useRef<HTMLInputElement>(null);
  async function copy() {
    setCopying(true);
    try {
      if (!navigator.clipboard) throw new Error("Clipboard unavailable");
      await navigator.clipboard.writeText(link.shortUrl);
      setMessage("Copied to clipboard.");
    } catch {
      url.current?.focus();
      url.current?.select();
      setMessage("Select the URL and copy it manually.");
    } finally {
      setCopying(false);
    }
  }
  return (
    <section
      aria-labelledby="link-result-title"
      className="mt-12 border-t border-(--border) pt-8"
    >
      <h2
        id="link-result-title"
        className="text-2xl font-semibold tracking-tight"
      >
        Last created short link
      </h2>
      <div className="mt-4 flex min-w-0 flex-col gap-3 sm:flex-row">
        <input
          ref={url}
          aria-label="Short URL"
          className="link-input min-w-0 flex-1 font-mono"
          value={link.shortUrl}
          readOnly
          onFocus={(event) => event.currentTarget.select()}
        />
        <button
          type="button"
          className="link-secondary flex items-center justify-center gap-2"
          disabled={copying}
          onClick={copy}
        >
          <Copy aria-hidden="true" size={18} />
          {copying ? "Copying…" : "Copy short link"}
        </button>
      </div>
      <p className="mt-3 text-sm text-(--ink-muted)">
        {link.expiresAt ? (
          <>
            <span>Expires </span>
            <time dateTime={link.expiresAt}>
              {new Date(link.expiresAt).toLocaleString(undefined, {
                dateStyle: "medium",
                timeStyle: "long",
              })}
            </time>
          </>
        ) : (
          "No expiry"
        )}
      </p>
      <p className="mt-2 min-h-6 text-sm" role="status" aria-live="polite">
        {message}
      </p>
    </section>
  );
}

function SavedLinkCopy({ link }: Readonly<{ link: SavedLink }>) {
  const [copying, setCopying] = useState(false);
  const [message, setMessage] = useState("");
  async function copy() {
    setCopying(true);
    try {
      if (!navigator.clipboard) throw new Error("Clipboard unavailable");
      await navigator.clipboard.writeText(link.shortUrl);
      setMessage("Copied.");
    } catch {
      setMessage("Select the short URL and copy it manually.");
    } finally {
      setCopying(false);
    }
  }
  return (
    <div>
      <button
        type="button"
        className="link-secondary inline-flex items-center justify-center gap-2"
        aria-label={`Copy ${link.code}`}
        disabled={copying}
        onClick={copy}
      >
        <Copy aria-hidden="true" size={18} />
        <span className="md:sr-only">Copy</span>
      </button>
      {message ? (
        <p className="mt-2 text-xs text-(--ink-muted)" aria-live="polite">
          {message}
        </p>
      ) : null}
    </div>
  );
}

function SavedLinkDetails({ link }: Readonly<{ link: SavedLink }>) {
  return (
    <>
      <span className="text-sm font-medium">
        {link.status === "ACTIVE"
          ? "Active"
          : link.status === "DISABLED"
            ? "Disabled"
            : "Expired"}
      </span>
      <time
        className="mt-1 block text-xs text-(--ink-muted)"
        dateTime={link.createdAt}
      >
        {new Date(link.createdAt).toLocaleString(undefined, {
          dateStyle: "medium",
          timeStyle: "short",
        })}
      </time>
      <p className="mt-2 text-xs text-(--ink-muted)">
        Lifetime redirects:{" "}
        <span className="font-mono">
          {link.clickCount.toLocaleString("en")}
        </span>
      </p>
    </>
  );
}

function LinkAnalyticsAction({
  id,
  range,
}: {
  id: string;
  range?: { from: string; to: string };
}) {
  const parameters = range
    ? `?${new URLSearchParams({ from: range.from, to: range.to })}`
    : "";
  return (
    <Link
      href={`/analytics/${id}${parameters}`}
      prefetch={false}
      className="mt-2 inline-flex min-h-11 items-center text-xs underline underline-offset-4"
    >
      View analytics
    </Link>
  );
}

export function SavedLinkHistory({
  accountKey,
  analyticsRange,
}: Readonly<{
  accountKey: string;
  analyticsRange?: { from: string; to: string };
}>) {
  const [cursors, setCursors] = useState<(string | undefined)[]>([undefined]);
  const cursor = cursors[cursors.length - 1];
  const history = useQuery({
    queryKey: [...linkHistoryQueryKey, accountKey, cursor ?? null],
    queryFn: ({ signal }) => getLinks(cursor, signal),
    retry: false,
    staleTime: 0,
    refetchOnMount: "always",
  });
  const items = history.data?.items ?? [];
  const nextCursor = history.data?.nextCursor;
  return (
    <section
      aria-labelledby="saved-links-title"
      className="mt-12 border-t border-(--border) pt-8"
    >
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h2
          id="saved-links-title"
          className="text-2xl font-semibold tracking-tight"
        >
          Your links
        </h2>
        <button
          type="button"
          className="link-secondary"
          disabled={history.isFetching}
          onClick={() => void history.refetch()}
        >
          Refresh links
        </button>
      </div>
      {history.isPending ? (
        <p className="mt-4 text-sm text-(--ink-muted)" aria-live="polite">
          Loading your links.
        </p>
      ) : history.isError ? (
        <p className="mt-4 text-sm text-(--danger)" role="alert">
          We couldn’t load your links. Use Refresh links to try again.
        </p>
      ) : items.length === 0 ? (
        <p className="mt-4 text-sm text-(--ink-muted)">
          {cursors.length === 1 ? (
            <>
              No links yet.{" "}
              <Link href="/links" className="underline underline-offset-4">
                Create your first short link.
              </Link>
            </>
          ) : (
            "No saved links on this page."
          )}
        </p>
      ) : (
        <>
          <table className="mt-6 hidden w-full table-fixed text-left md:table">
            <caption className="sr-only">
              Your saved short links, newest first
            </caption>
            <thead>
              <tr className="border-b border-(--border) text-sm text-(--ink-muted)">
                <th scope="col" className="w-[39%] pb-3 pr-4 font-medium">
                  Short link
                </th>
                <th scope="col" className="w-[29%] pb-3 pr-4 font-medium">
                  Destination
                </th>
                <th scope="col" className="w-[23%] pb-3 pr-4 font-medium">
                  Status / created
                </th>
                <th scope="col" className="w-[9%] pb-3 font-medium">
                  <span className="sr-only">Copy</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {items.map((link) => (
                <tr
                  key={link.id}
                  className="border-b border-(--border) align-top"
                >
                  <td className="py-4 pr-4">
                    <a
                      href={link.shortUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="block break-all font-mono text-xs underline underline-offset-4 select-all"
                    >
                      {link.shortUrl}
                    </a>
                    <LinkAnalyticsAction id={link.id} range={analyticsRange} />
                  </td>
                  <td className="py-4 pr-4">
                    <p className="break-all text-sm text-(--ink-muted)">
                      {link.destinationUrl}
                    </p>
                  </td>
                  <td className="py-4 pr-4">
                    <SavedLinkDetails link={link} />
                  </td>
                  <td className="py-4">
                    <SavedLinkCopy link={link} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <ul
            className="mt-6 space-y-4 md:hidden"
            aria-label="Saved short links"
          >
            {items.map((link) => (
              <li
                key={link.id}
                className="min-w-0 rounded-lg border border-(--border) p-4"
              >
                <a
                  href={link.shortUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="break-all font-mono text-sm underline underline-offset-4 select-all"
                >
                  {link.shortUrl}
                </a>
                <div>
                  <LinkAnalyticsAction id={link.id} range={analyticsRange} />
                </div>
                <p className="mt-3 break-all text-sm text-(--ink-muted)">
                  {link.destinationUrl}
                </p>
                <div className="mt-4 flex items-start justify-between gap-4">
                  <div>
                    <SavedLinkDetails link={link} />
                  </div>
                  <SavedLinkCopy link={link} />
                </div>
              </li>
            ))}
          </ul>
        </>
      )}
      <nav
        aria-label="Link history pages"
        className="mt-6 flex flex-wrap items-center justify-between gap-3"
      >
        <button
          type="button"
          className="link-secondary"
          disabled={cursors.length === 1 || history.isFetching}
          onClick={() => setCursors((current) => current.slice(0, -1))}
        >
          Previous
        </button>
        <p className="text-sm text-(--ink-muted)" aria-live="polite">
          Page {cursors.length}
        </p>
        <button
          type="button"
          className="link-secondary"
          disabled={!nextCursor || history.isFetching || history.isError}
          onClick={() => {
            if (nextCursor) setCursors((current) => [...current, nextCursor]);
          }}
        >
          Next
        </button>
      </nav>
    </section>
  );
}
