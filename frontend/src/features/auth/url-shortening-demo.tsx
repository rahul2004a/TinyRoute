"use client";

import { useEffect, useState, useSyncExternalStore } from "react";

export const LONG_URL = "https://example.com/blog/how-to-shorten-long-urls";
export const SHORT_URL = "tinyroute.in/8z5hdy5";

const TYPING_DELAY_MS = 28;
const SOURCE_PAUSE_MS = 560;
const SHORTENING_MS = 520;
const RESULT_PAUSE_MS = 2_200;

type AnimationPhase = "typing" | "pause" | "shortening" | "result";

function subscribeToMotionPreference(onChange: () => void) {
  if (typeof window.matchMedia !== "function") {
    return () => undefined;
  }

  const mediaQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
  mediaQuery.addEventListener("change", onChange);
  return () => mediaQuery.removeEventListener("change", onChange);
}

function getMotionPreference() {
  return (
    typeof window.matchMedia === "function" &&
    window.matchMedia("(prefers-reduced-motion: reduce)").matches
  );
}

export function UrlShorteningDemo() {
  const [phase, setPhase] = useState<AnimationPhase>("typing");
  const [typedLength, setTypedLength] = useState(0);
  const prefersReducedMotion = useSyncExternalStore(
    subscribeToMotionPreference,
    getMotionPreference,
    () => false,
  );

  useEffect(() => {
    if (prefersReducedMotion) {
      return;
    }

    if (phase === "typing") {
      const timer = window.setTimeout(() => {
        if (typedLength < LONG_URL.length) {
          setTypedLength((length) => length + 1);
        } else {
          setPhase("pause");
        }
      }, TYPING_DELAY_MS);
      return () => window.clearTimeout(timer);
    }

    const delay =
      phase === "pause"
        ? SOURCE_PAUSE_MS
        : phase === "shortening"
          ? SHORTENING_MS
          : RESULT_PAUSE_MS;
    const timer = window.setTimeout(() => {
      if (phase === "pause") {
        setPhase("shortening");
      } else if (phase === "shortening") {
        setPhase("result");
      } else {
        setTypedLength(0);
        setPhase("typing");
      }
    }, delay);

    return () => window.clearTimeout(timer);
  }, [phase, prefersReducedMotion, typedLength]);

  const displayPhase = prefersReducedMotion ? "result" : phase;
  const visibleSource = prefersReducedMotion
    ? LONG_URL
    : LONG_URL.slice(0, typedLength);
  const showsCursor =
    !prefersReducedMotion && (phase === "typing" || phase === "pause");

  return (
    <div
      aria-hidden="true"
      className="auth-url-shortening"
      data-phase={displayPhase}
      data-testid="auth-url-shortening"
    >
      <div className="auth-url-source-slot">
        <p
          className="auth-url-source font-mono text-(--auth-ink-muted)"
          data-testid="auth-url-source"
        >
          {visibleSource}
          {showsCursor ? <span className="auth-url-cursor" /> : null}
        </p>
      </div>

      <div className="auth-url-transition">
        <span className="auth-url-arrow">
          <span className="auth-url-arrow-stem" />
          <span className="auth-url-arrow-head" />
        </span>
        <span className="auth-url-compression">
          <span />
          <span />
          <span />
        </span>
      </div>

      <div className="auth-url-result-slot">
        <p
          className="auth-url-result font-mono text-(--auth-ink)"
          data-testid="auth-url-result"
        >
          {displayPhase === "result" ? SHORT_URL : ""}
        </p>
      </div>
    </div>
  );
}
