"use client";

import { Menu, X } from "lucide-react";
import Link from "next/link";
import {
  useEffect,
  useId,
  useRef,
  useState,
  type KeyboardEvent,
  type MouseEvent,
} from "react";

import { ThemeSelector } from "../../app/theme-selector";
import { LandingBrand } from "./landing-brand";
import { LANDING_ACCOUNT_LINKS } from "./landing-navigation";

const DESKTOP_MEDIA = "(min-width: 1024px)";

function isPlainClick(event: MouseEvent<HTMLAnchorElement>) {
  return (
    !event.defaultPrevented &&
    event.button === 0 &&
    !event.metaKey &&
    !event.ctrlKey &&
    !event.shiftKey &&
    !event.altKey
  );
}

export function LandingHeader({
  accountLinks = LANDING_ACCOUNT_LINKS,
  currentPath,
}: Readonly<{
  accountLinks?: readonly { href: string; label: string }[];
  currentPath?: string;
}> = {}) {
  const [open, setOpen] = useState(false);
  const panelId = useId();
  const trigger = useRef<HTMLButtonElement>(null);
  const panel = useRef<HTMLElement>(null);
  const desktop = useRef<HTMLElement>(null);
  const desktopHadFocus = useRef(false);
  const triggerHadFocus = useRef(false);

  useEffect(() => {
    const media = window.matchMedia(DESKTOP_MEDIA);
    function onChange(event: MediaQueryListEvent) {
      const active = document.activeElement;
      if (!event.matches) {
        if (
          active &&
          (desktop.current?.contains(active) ||
            (active === document.body && desktopHadFocus.current))
        ) {
          trigger.current?.focus();
        }
        desktopHadFocus.current = false;
        setOpen(false);
        return;
      }
      if (
        active instanceof HTMLAnchorElement &&
        panel.current?.contains(active)
      ) {
        desktop.current
          ?.querySelector<HTMLAnchorElement>(
            `a[href="${active.getAttribute("href")}"]`,
          )
          ?.focus();
      } else if (
        active === trigger.current ||
        (active === document.body && triggerHadFocus.current)
      ) {
        desktop.current?.querySelector<HTMLAnchorElement>("a")?.focus();
      }
      triggerHadFocus.current = false;
      setOpen(false);
    }
    media.addEventListener("change", onChange);
    return () => media.removeEventListener("change", onChange);
  }, []);

  function onEscape(event: KeyboardEvent) {
    if (event.key !== "Escape" || !open) return;
    event.preventDefault();
    setOpen(false);
    trigger.current?.focus();
  }

  return (
    <>
      <a className="landing-skip" href="#main-content">
        Skip to content
      </a>
      <header className="landing-header">
        <div className="landing-header-inner landing-container flex items-center gap-4">
          <LandingBrand />
          <div className="ml-auto flex items-center gap-2 lg:gap-6">
            <ThemeSelector placement="inline" />
            <nav
              aria-label="Primary navigation"
              className="landing-desktop-nav hidden items-center gap-3 lg:flex"
              onFocusCapture={() => {
                desktopHadFocus.current = true;
              }}
              onBlurCapture={() => {
                // CSS can hide and blur the link before the media event fires.
                if (window.matchMedia(DESKTOP_MEDIA).matches)
                  desktopHadFocus.current = false;
              }}
              ref={desktop}
            >
              {accountLinks.map(({ href, label }) => (
                <Link
                  aria-current={href === currentPath ? "page" : undefined}
                  className={
                    href === "/register" ? "landing-action" : "landing-nav-link"
                  }
                  href={href}
                  key={href}
                  prefetch={false}
                >
                  {label}
                </Link>
              ))}
            </nav>
            <button
              aria-controls={panelId}
              aria-expanded={open}
              aria-label="Menu"
              className="grid size-11 place-items-center rounded-lg border border-(--border-strong) lg:hidden"
              onClick={() => setOpen(!open)}
              onFocus={() => {
                triggerHadFocus.current = true;
              }}
              onBlur={() => {
                if (!window.matchMedia(DESKTOP_MEDIA).matches)
                  triggerHadFocus.current = false;
              }}
              onKeyDown={onEscape}
              ref={trigger}
              type="button"
            >
              {open ? (
                <X aria-hidden="true" size={22} />
              ) : (
                <Menu aria-hidden="true" size={22} />
              )}
            </button>
          </div>
        </div>
        <nav
          aria-label="Mobile navigation"
          className="landing-mobile-nav landing-container lg:hidden"
          hidden={!open}
          id={panelId}
          onKeyDown={onEscape}
          ref={panel}
        >
          {accountLinks.map(({ href, label }) => (
            <Link
              aria-current={href === currentPath ? "page" : undefined}
              className={
                href === "/register" ? "landing-action" : "landing-nav-link"
              }
              href={href}
              key={href}
              onClick={(event) => {
                if (isPlainClick(event)) setOpen(false);
              }}
              prefetch={false}
            >
              {label}
            </Link>
          ))}
        </nav>
      </header>
    </>
  );
}
