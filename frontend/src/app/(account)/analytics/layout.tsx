import type { Metadata } from "next";
import type { ReactNode } from "react";
import { SettingsSidebar } from "../../../features/auth/settings-sidebar";
import "../../../features/auth/settings.css";
import "../../../features/analytics/analytics.css";

export const metadata: Metadata = { title: "Analytics · TinyRoute" };
export default function AnalyticsLayout({ children }: { children: ReactNode }) {
  return (
    <div className="settings-page analytics-workspace">
      <a className="settings-skip" href="#main-content">
        Skip to content
      </a>
      <SettingsSidebar active="analytics" />
      <main
        className="settings-main auth-page-enter"
        id="main-content"
        tabIndex={-1}
      >
        {children}
      </main>
    </div>
  );
}
