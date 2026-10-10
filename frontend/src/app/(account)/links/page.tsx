import Link from "next/link";
import { AuthBrand } from "../../../features/auth/auth-brand";
import { LinkCreateForm } from "../../../features/links/link-create-form";

export default function LinksPage() {
  return (
    <main className="auth-shell auth-page-enter mx-auto min-h-[100dvh] max-w-3xl px-4 py-24 sm:px-6 lg:px-8">
      <AuthBrand />
      <LinkCreateForm />
      <nav
        aria-label="Account"
        className="mt-12 flex flex-wrap gap-6 border-t border-(--border) pt-4"
      >
        <Link className="link-navigation" href="/">
          Home
        </Link>
        <Link className="link-navigation" href="/settings">
          Account settings
        </Link>
      </nav>
    </main>
  );
}
