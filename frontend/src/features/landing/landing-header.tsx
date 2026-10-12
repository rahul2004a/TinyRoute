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
import {
  LANDING_ACCOUNT_LINKS,
  LANDING_SECTION_LINKS,
} from "./landing-navigation";

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

export function LandingHeader() {
  const [open, setOpen] = useState(false);
  const panelId = useId();
  const trigger = useRef<HTMLButtonElement>(null);
  const panel = useRef<HTMLElement>(null);
  const desktop = useRef<HTMLElement>(null);

  useEffect(() => {
    const media = window.matchMedia("(min-width: 1024px)");
    function onChange(event: MediaQueryListEvent) {
      if (!event.matches) return;
      const active = document.activeElement;
      if (
        active instanceof HTMLAnchorElement &&
        panel.current?.contains(active)
      ) {
        desktop.current
          ?.querySelector<HTMLAnchorElement>(
            `a[href="${active.getAttribute("href")}"]`,
          )
          ?.focus();
      } else if (active === trigger.current) {
        desktop.current?.querySelector<HTMLAnchorElement>("a")?.focus();
      }
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

  function onSectionClick(event: MouseEvent<HTMLAnchorElement>) {
    if (!isPlainClick(event)) return;
    setOpen(false);
    const id = event.currentTarget.hash.slice(1);
    document.getElementById(id)?.focus({ preventScroll: true });
  }

  return (
    <>
      <a className="landing-skip" href="#main-content">
        Skip to content
      </a>
      <header className="landing-header border-b border-(--border)">
        <div className="landing-container flex h-14 items-center gap-4">
          <LandingBrand />
          <nav
            aria-label="Primary navigation"
            className="landing-desktop-nav ml-auto hidden items-center gap-3 lg:flex"
            ref={desktop}
          >
            {LANDING_SECTION_LINKS.map(({ href, label }) => (
              <a
                className="landing-nav-link"
                href={href}
                key={href}
                onClick={onSectionClick}
              >
                {label}
              </a>
            ))}
            {LANDING_ACCOUNT_LINKS.map(({ href, label }) => (
              <Link
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
          <div className="ml-auto flex items-center gap-2 lg:ml-0">
            <ThemeSelector placement="inline" />
            <button
              aria-controls={panelId}
              aria-expanded={open}
              aria-label="Menu"
              className="grid size-11 place-items-center rounded-lg border border-(--border-strong) lg:hidden"
              onClick={() => setOpen(!open)}
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
          {LANDING_SECTION_LINKS.map(({ href, label }) => (
            <a
              className="landing-nav-link"
              href={href}
              key={href}
              onClick={onSectionClick}
            >
              {label}
            </a>
          ))}
          {LANDING_ACCOUNT_LINKS.map(({ href, label }) => (
            <Link
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
