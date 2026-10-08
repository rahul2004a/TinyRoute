"use client";

import { Copy } from "lucide-react";
import { useRef, useState } from "react";
import type { CreatedLink } from "./link-api";

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
