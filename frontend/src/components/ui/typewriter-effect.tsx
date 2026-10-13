"use client";

import { stagger, useAnimate, useInView } from "motion/react";
import { Fragment, useEffect, useRef, useSyncExternalStore } from "react";

import { cn } from "@/lib/utils";

type TypewriterProps = Readonly<{
  words: ReadonlyArray<{ text: string; className?: string }>;
  className?: string;
  cursorClassName?: string;
}>;

const subscribe = () => () => undefined;
const clientSnapshot = () => true;
const serverSnapshot = () => false;

function subscribeToMotionPreference(onChange: () => void) {
  if (typeof window.matchMedia !== "function") return () => undefined;
  const media = window.matchMedia("(prefers-reduced-motion: reduce)");
  media.addEventListener("change", onChange);
  return () => media.removeEventListener("change", onChange);
}

function getMotionPreference() {
  return (
    typeof window.matchMedia !== "function" ||
    window.matchMedia("(prefers-reduced-motion: reduce)").matches
  );
}

function Typewriter({
  words,
  className,
  cursorClassName,
  variant,
}: TypewriterProps & { variant: "smooth" | "characters" }) {
  const [scope, animate] = useAnimate<HTMLSpanElement>();
  const inView = useInView(scope, { once: true, amount: 0.5 });
  const reducedMotion = useSyncExternalStore(
    subscribeToMotionPreference,
    getMotionPreference,
    () => true,
  );
  const ready = useSyncExternalStore(subscribe, clientSnapshot, serverSnapshot);
  const played = useRef(false);

  useEffect(() => {
    if (!ready || !inView || played.current) return;
    played.current = true;
    // Complete the static version too, so enabling motion never re-hides it.
    const timing = {
      duration: reducedMotion ? 0 : 2,
      delay: reducedMotion ? 0 : 0.35,
      ease: "linear" as const,
    };
    const reveal =
      variant === "smooth"
        ? animate(
            ".typewriter-copy",
            { clipPath: ["inset(0% 100% 0% 0%)", "inset(0% 0% 0% 0%)"] },
            timing,
          )
        : animate(
            ".typewriter-character",
            { opacity: [0, 1] },
            {
              duration: reducedMotion ? 0 : 0.1,
              delay: reducedMotion ? 0 : stagger(0.05),
              ease: "easeInOut",
            },
          );
    const cursor = animate(
      ".typewriter-cursor-track",
      { x: ["-100%", "0%"] },
      timing,
    );
    return () => {
      reveal.complete();
      cursor.complete();
    };
  }, [animate, inView, ready, reducedMotion, scope, variant]);

  return (
    <span
      className={cn("typewriter-effect", className)}
      data-active={ready && inView && reducedMotion === false}
      data-ready={ready}
      data-variant={variant}
    >
      <span className="sr-only">
        {words.map((word) => word.text).join(" ")}
      </span>
      <span aria-hidden="true" className="typewriter-visual" ref={scope}>
        <span className="typewriter-copy">
          {words.map((word, index) => (
            <Fragment key={`word-${index}`}>
              <span className={cn("inline-block", word.className)}>
                {Array.from(word.text).map((character, characterIndex) => (
                  <span
                    className="typewriter-character"
                    key={`character-${characterIndex}`}
                  >
                    {character}
                  </span>
                ))}
              </span>
              {index < words.length - 1 ? " " : null}
            </Fragment>
          ))}
        </span>
        <span className="typewriter-cursor-track">
          <span className={cn("typewriter-cursor", cursorClassName)} />
        </span>
      </span>
    </span>
  );
}

export function TypewriterEffect(props: TypewriterProps) {
  return <Typewriter {...props} variant="characters" />;
}

export function TypewriterEffectSmooth(props: TypewriterProps) {
  return <Typewriter {...props} variant="smooth" />;
}
