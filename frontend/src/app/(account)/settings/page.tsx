import type { Metadata } from "next";
import { DeleteAccountPanel } from "../../../features/auth/delete-account-panel";
import { SettingsSidebar } from "../../../features/auth/settings-sidebar";
import "../../../features/auth/settings.css";

export const metadata: Metadata = {
  title: "Account settings · TinyRoute",
};

export default function SettingsPage() {
  return (
    <div className="settings-page">
      <a className="settings-skip" href="#main-content">
        Skip to content
      </a>
      <SettingsSidebar />
      <main
        className="settings-main auth-page-enter"
        id="main-content"
        tabIndex={-1}
      >
        <DeleteAccountPanel />
      </main>
    </div>
  );
}
