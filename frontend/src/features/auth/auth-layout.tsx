import type { ReactNode } from "react";
import { UrlShorteningDemo } from "./url-shortening-demo";

export function AuthLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <main className="auth-shell grid min-h-[100dvh] lg:grid-cols-2">
      <section className="auth-page-enter flex min-h-[100dvh] items-center justify-center px-6 py-12 sm:px-10 lg:px-16 xl:px-24">
        {children}
      </section>
      <aside className="hidden border-l border-(--auth-border) bg-(--auth-surface) lg:flex">
        <figure className="m-auto w-full max-w-xl px-8 py-10 xl:px-12">
          <UrlShorteningDemo />
          <figcaption className="mx-auto mt-6 max-w-md text-center">
            <h2 className="text-3xl font-semibold tracking-[-0.8px] text-(--auth-ink)">
              Short links. Smarter sharing.
            </h2>
            <p className="mt-3 text-base leading-7 text-(--auth-ink-muted)">
              Create, manage, and track your links from one place.
            </p>
          </figcaption>
        </figure>
      </aside>
    </main>
  );
}
