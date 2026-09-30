"use client";

import { Moon, Sun } from "lucide-react";
import { useTheme } from "next-themes";
import { useSyncExternalStore } from "react";

const subscribeToHydration = () => () => undefined;

export function ThemeSelector() {
  const { setTheme, theme } = useTheme();
  const mounted = useSyncExternalStore(
    subscribeToHydration,
    () => true,
    () => false,
  );
  const isDark = !mounted || theme !== "light";

  return (
    <button
      aria-label={isDark ? "Switch to light mode" : "Switch to dark mode"}
      className="fixed top-3 right-3 z-50 grid size-12 place-items-center rounded-xl border-2 border-(--border-strong) bg-(--canvas) text-(--ink) transition-colors hover:bg-(--surface-2) focus-visible:ring-2 focus-visible:ring-(--primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--canvas) disabled:cursor-wait motion-reduce:transition-none"
      disabled={!mounted}
      onClick={() => setTheme(isDark ? "light" : "dark")}
      type="button"
    >
      {isDark ? (
        <Sun aria-hidden="true" size={22} strokeWidth={1.8} />
      ) : (
        <Moon aria-hidden="true" size={22} strokeWidth={1.8} />
      )}
    </button>
  );
}
