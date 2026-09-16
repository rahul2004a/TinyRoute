export default function HomePage() {
  return (
    <main className="mx-auto flex min-h-screen max-w-5xl items-center px-6 py-16">
      <div className="max-w-2xl">
        <p className="mb-3 text-sm font-semibold tracking-wide text-[var(--accent)] uppercase">
          TinyRoute
        </p>
        <h1 className="text-4xl font-semibold tracking-tight sm:text-6xl">
          Short links, with a clear path back to you.
        </h1>
        <p className="mt-6 max-w-xl text-lg leading-8 text-[var(--muted-text)]">
          The client foundation is ready for secure account access and link
          management.
        </p>
      </div>
    </main>
  );
}
