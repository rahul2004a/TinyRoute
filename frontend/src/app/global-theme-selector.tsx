"use client";

import { usePathname } from "next/navigation";

import { ThemeSelector } from "./theme-selector";

export function GlobalThemeSelector() {
  const pathname = usePathname();
  return pathname === "/" ||
    pathname === "/links" ||
    pathname === "/settings" ||
    pathname === "/analytics" ||
    pathname.startsWith("/analytics/") ? null : (
    <ThemeSelector />
  );
}
