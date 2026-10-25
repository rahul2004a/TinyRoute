package com.tinyroute.controller;

import com.tinyroute.config.SecurityProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/** Public content uses only fixed copy and the configured frontend origin. */
@Component
public final class RedirectPageRenderer {
    private final String homeUrl;

    public RedirectPageRenderer(SecurityProperties security) {
        homeUrl = HtmlUtils.htmlEscape(security.allowedOrigins().getFirst());
    }

    public String render(int status) {
        String title =
                switch (status) {
                    case 403 -> "Link unavailable";
                    case 404 -> "Link not found";
                    case 429 -> "Too many requests";
                    case 503 -> "Service temporarily unavailable";
                    default -> throw new IllegalArgumentException("Unsupported public status");
                };
        String message =
                switch (status) {
                    case 403 ->
                            "This link is currently unavailable. Ask the sender for another link.";
                    case 404 -> "Check the address for typos, or ask the sender for a new link.";
                    case 429 -> "Please wait a little before opening this link again.";
                    default -> "We couldn’t open this link right now. Please try again shortly.";
                };
        return """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <meta name="robots" content="noindex,nofollow">
              <title>%s · TinyRoute</title>
              <style>
                :root {
                  color-scheme: dark;
                  --canvas: #1e201e;
                  --ink: #ecdfcc;
                  --border: #697565;
                  --primary-hover: #ffffff;
                }
                @media (prefers-color-scheme: light) {
                  :root {
                    color-scheme: light;
                    --canvas: #ecdfcc;
                    --ink: #1e201e;
                    --primary-hover: #3c3d37;
                  }
                }
                * { box-sizing: border-box; }
                body {
                  margin: 0;
                  min-height: 100dvh;
                  display: flex;
                  flex-direction: column;
                  background: var(--canvas);
                  color: var(--ink);
                  font: 16px/1.5 system-ui, sans-serif;
                  -webkit-font-smoothing: antialiased;
                }
                a { color: inherit; text-decoration: none; }
                a:focus-visible {
                  outline: 2px solid var(--ink);
                  outline-offset: 4px;
                }
                header {
                  width: min(1120px, calc(100%% - 64px));
                  margin-inline: auto;
                  min-height: 80px;
                  display: flex;
                  align-items: center;
                }
                .brand {
                  display: inline-flex;
                  align-items: center;
                  gap: 10px;
                  min-height: 44px;
                  border-radius: 8px;
                  font-size: 20px;
                  font-weight: 600;
                  letter-spacing: -.6px;
                  transition: opacity 160ms ease;
                }
                .brand:hover { opacity: .75; }
                .brand:active { opacity: .6; }
                .brand-mark {
                  display: grid;
                  place-items: center;
                  width: 32px;
                  height: 32px;
                  border-radius: 8px;
                  background: var(--ink);
                  color: var(--canvas);
                }
                svg { flex-shrink: 0; }
                main {
                  flex: 1;
                  display: grid;
                  place-items: center;
                  padding: 64px 32px 144px;
                }
                .error {
                  width: 100%%;
                  max-width: 960px;
                  display: grid;
                  grid-template-columns: .85fr 1fr;
                  align-items: center;
                  gap: 64px;
                  animation: content-enter 220ms ease-out both;
                }
                .status {
                  margin: 0;
                  color: color-mix(in srgb, var(--ink) 32%%, var(--canvas));
                  font: 500 clamp(120px, 17vw, 192px)/1 ui-monospace, monospace;
                  letter-spacing: -.09em;
                  font-variant-numeric: tabular-nums;
                }
                .message {
                  border-left: 1px solid var(--border);
                  padding: 16px 0 16px 48px;
                }
                h1 {
                  margin: 0;
                  max-width: 16ch;
                  font-size: clamp(28px, 3vw, 36px);
                  line-height: 1.15;
                  letter-spacing: -.8px;
                  font-weight: 600;
                  text-wrap: balance;
                }
                .description {
                  margin: 20px 0 32px;
                  max-width: 36ch;
                  color: color-mix(in srgb, var(--ink) 78%%, var(--canvas));
                  text-wrap: pretty;
                }
                .home {
                  display: inline-flex;
                  align-items: center;
                  justify-content: center;
                  gap: 10px;
                  min-height: 44px;
                  padding: 12px 20px;
                  border: 1px solid transparent;
                  border-radius: 8px;
                  background: var(--ink);
                  color: var(--canvas);
                  font-size: 14px;
                  font-weight: 600;
                  transition: background 160ms ease, transform 160ms ease;
                }
                .home:hover { background: var(--primary-hover); }
                .home:active { transform: translateY(1px); }
                @keyframes content-enter {
                  from { opacity: 0; transform: translateY(8px); }
                  to { opacity: 1; transform: translateY(0); }
                }
                @media (max-width: 767px) {
                  header { width: calc(100%% - 48px); min-height: 64px; }
                  main { padding: 48px 24px 96px; }
                  .error { max-width: 440px; grid-template-columns: 1fr; gap: 32px; }
                  .status { font-size: clamp(112px, 24vw, 160px); }
                  .message { border-left: 0; border-top: 1px solid var(--border); padding: 32px 0 0; }
                }
                @media (max-width: 479px) {
                  header { width: calc(100%% - 32px); }
                  main { padding-inline: 16px; }
                  .home { width: 100%%; }
                }
                @media (prefers-reduced-motion: reduce) {
                  *, *::before, *::after { animation: none !important; transition: none !important; }
                }
                @media (forced-colors: active) {
                  .brand-mark, .home { border: 1px solid ButtonText; }
                }
              </style>
            </head>
            <body>
              <header>
                <a class="brand" href="%s" aria-label="TinyRoute home">
                  <span class="brand-mark" aria-hidden="true">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                                  <path d="M9 17H7A5 5 0 0 1 7 7h2"/><path d="M15 7h2a5 5 0 1 1 0 10h-2"/><path d="M8 12h8"/>
                    </svg>
                  </span>
                  TinyRoute
                </a>
              </header>
              <main>
                <section class="error" aria-labelledby="error-title">
                  <p class="status" aria-hidden="true">%d</p>
                  <div class="message">
                    <h1 id="error-title">%s</h1>
                    <p class="description">%s</p>
                    <a class="home" href="%s">
                      <svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="m12 19-7-7 7-7M5 12h14"/>
                      </svg>
                      Back to TinyRoute
                    </a>
                  </div>
                </section>
              </main>
            </body>
            </html>
            """
                .formatted(title, homeUrl, status, title, message, homeUrl);
    }
}
