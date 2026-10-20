import { LandingHeader } from "../../../features/landing/landing-header";
import { LandingFooter } from "../../../features/landing/landing-page";
import "../../../features/landing/landing.css";
import { LinkCreateForm } from "../../../features/links/link-create-form";

const accountLinks = [
  { href: "/links", label: "Your links" },
  { href: "/analytics", label: "Analytics" },
  { href: "/settings", label: "Account settings" },
] as const;

export default function LinksPage() {
  return (
    <div className="flex min-h-[100dvh] flex-col">
      <div className="landing-page">
        <LandingHeader accountLinks={accountLinks} currentPath="/links" />
      </div>
      <main
        className="auth-shell auth-page-enter mx-auto w-full max-w-3xl flex-1 px-4 py-12 sm:px-6 sm:py-16 lg:px-8"
        id="main-content"
        tabIndex={-1}
      >
        <LinkCreateForm />
      </main>
      <div className="landing-page">
        <LandingFooter accountLinks={accountLinks} />
      </div>
    </div>
  );
}
