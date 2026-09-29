"use client";

import { useTheme } from "next-themes";
import { useSyncExternalStore } from "react";

const themeOptions = [
  { label: "System", value: "system" },
  { label: "Light", value: "light" },
  { label: "Dark", value: "dark" },
] as const;

const subscribeToHydration = () => () => undefined;

export function ThemeSelector() {
  const { setTheme, theme } = useTheme();
  const mounted = useSyncExternalStore(
    subscribeToHydration,
    () => true,
    () => false,
  );

  return (
    <label className="fixed top-3 right-3 z-50 flex min-h-11 items-center gap-2 rounded-lg border border-(--border) bg-(--surface-1) px-3 text-sm font-medium text-(--ink) shadow-sm">
      Theme
      <select
        className="min-h-11 rounded-md border border-(--border) bg-(--surface-1) px-2 text-(--ink) outline-none focus-visible:ring-2 focus-visible:ring-(--primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--canvas) disabled:cursor-wait"
        disabled={!mounted}
        onChange={(event) => setTheme(event.target.value)}
        value={mounted ? (theme ?? "system") : "system"}
      >
        {themeOptions.map(({ label, value }) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </select>
    </label>
  );
}
