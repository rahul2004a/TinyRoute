function googleAuthorizationUrl(): string | undefined {
  const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;
  if (!apiBaseUrl) {
    return undefined;
  }

  try {
    const apiOrigin = new URL(apiBaseUrl);
    return apiOrigin.protocol === "https:"
      ? new URL("/api/auth/google/start", apiOrigin).toString()
      : undefined;
  } catch {
    return undefined;
  }
}

export function GoogleSignInButton() {
  const authorizationUrl = googleAuthorizationUrl();
  if (!authorizationUrl) {
    return null;
  }

  return (
    <a
      className="mt-4 inline-flex min-h-11 w-full items-center justify-center rounded-lg border border-(--auth-border) bg-transparent px-4 py-2 font-medium text-(--auth-ink) transition hover:bg-(--auth-surface-2) active:translate-y-px focus-visible:ring-2 focus-visible:ring-(--auth-primary) focus-visible:ring-offset-2 focus-visible:ring-offset-(--auth-canvas)"
      href={authorizationUrl}
    >
      Continue with Google
    </a>
  );
}
