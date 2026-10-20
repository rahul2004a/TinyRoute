"use client";

import {
  ArrowLeft,
  ChartNoAxesCombined,
  LayoutDashboard,
  Link2,
  PanelLeft,
  Settings2,
  X,
} from "lucide-react";
import Link from "next/link";
import { useEffect, useId, useRef, useState } from "react";

import { ThemeSelector } from "../../app/theme-selector";
import { useSession } from "./use-session";

function SidebarContent({
  email,
  onNavigate,
  active,
}: {
  email: string | null;
  onNavigate?: () => void;
  active: "settings" | "analytics";
}) {
  return (
    <>
      <div className="settings-sidebar-top">
        <Link
          className="settings-brand"
          href="/"
          onClick={onNavigate}
          prefetch={false}
        >
          <span aria-hidden="true" className="settings-brand-mark">
            <Link2 size={19} />
          </span>
          TinyRoute
        </Link>
        <Link
          className="settings-return"
          href="/"
          onClick={onNavigate}
          prefetch={false}
        >
          <ArrowLeft aria-hidden="true" size={17} />
          Back to home
        </Link>
        <p className="settings-nav-label">Account</p>
        <nav aria-label="Settings navigation">
          <Link
            className="settings-nav-link"
            href="/links"
            onClick={onNavigate}
            prefetch={false}
          >
            <LayoutDashboard aria-hidden="true" size={18} />
            {active === "analytics" ? "Your links" : "Dashboard"}
          </Link>
          <Link
            aria-current={active === "analytics" ? "page" : undefined}
            className="settings-nav-link"
            href="/analytics"
            onClick={onNavigate}
            prefetch={false}
          >
            <ChartNoAxesCombined aria-hidden="true" size={18} />
            Analytics
          </Link>
          <Link
            aria-current={active === "settings" ? "page" : undefined}
            className="settings-nav-link"
            href="/settings"
            onClick={onNavigate}
            prefetch={false}
          >
            <Settings2 aria-hidden="true" size={18} />
            Account settings
          </Link>
        </nav>
      </div>
      <div className="settings-sidebar-bottom">
        <div className="settings-appearance">
          <span>Appearance</span>
          <ThemeSelector placement="inline" />
        </div>
        {email ? (
          <div className="settings-sidebar-account">
            <span aria-hidden="true" className="settings-avatar">
              {email.charAt(0).toUpperCase()}
            </span>
            <div className="min-w-0">
              <p className="settings-account-label">Personal account</p>
              <p className="settings-account-email" title={email}>
                {email}
              </p>
            </div>
          </div>
        ) : null}
      </div>
    </>
  );
}

export function SettingsSidebar({
  active = "settings",
}: {
  active?: "settings" | "analytics";
}) {
  const session = useSession();
  const email = session.data?.authenticated ? session.data.user.email : null;
  const [open, setOpen] = useState(false);
  const drawerId = useId();
  const drawer = useRef<HTMLDialogElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  const desktop = useRef<HTMLElement>(null);

  useEffect(() => {
    const media = window.matchMedia("(min-width: 1024px)");
    function onChange(event: MediaQueryListEvent) {
      if (event.matches && drawer.current?.open) {
        drawer.current.close();
        desktop.current
          ?.querySelector<HTMLAnchorElement>('[aria-current="page"]')
          ?.focus();
      } else if (
        !event.matches &&
        desktop.current?.contains(document.activeElement)
      ) {
        trigger.current?.focus();
      }
    }
    media.addEventListener("change", onChange);
    return () => media.removeEventListener("change", onChange);
  }, []);

  return (
    <>
      <aside
        className="settings-sidebar"
        aria-label="Account navigation"
        ref={desktop}
      >
        <SidebarContent email={email} active={active} />
      </aside>
      <button
        aria-controls={drawerId}
        aria-expanded={open}
        aria-haspopup="dialog"
        aria-label={
          active === "analytics"
            ? "Open analytics navigation"
            : "Open settings navigation"
        }
        className="settings-menu-button"
        onClick={() => {
          drawer.current?.showModal();
          setOpen(true);
        }}
        ref={trigger}
        type="button"
      >
        <PanelLeft aria-hidden="true" size={21} />
      </button>
      <dialog
        aria-label="Settings menu"
        className="settings-drawer"
        id={drawerId}
        onClick={(event) => {
          if (event.target === event.currentTarget) drawer.current?.close();
        }}
        onClose={() => setOpen(false)}
        onKeyDown={(event) => {
          if (event.key !== "Tab") return;
          const controls = event.currentTarget.querySelectorAll<HTMLElement>(
            'a[href], button:not(:disabled), [tabindex="0"]',
          );
          const first = controls[0];
          const last = controls[controls.length - 1];
          if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last?.focus();
          } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first?.focus();
          }
        }}
        ref={drawer}
      >
        <div className="settings-drawer-content">
          <button
            aria-label="Close settings navigation"
            className="settings-drawer-close"
            onClick={() => drawer.current?.close()}
            type="button"
          >
            <X aria-hidden="true" size={20} />
          </button>
          <SidebarContent
            active={active}
            email={email}
            onNavigate={() => drawer.current?.close()}
          />
        </div>
      </dialog>
    </>
  );
}
