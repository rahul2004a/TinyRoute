package com.tinyroute.controller;

import org.springframework.stereotype.Component;

/** Fixed public content: no destination, code, owner, or exception interpolation. */
@Component
public final class RedirectPageRenderer {
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
                    case 403 -> "This link is currently unavailable.";
                    case 404 -> "This link cannot be opened.";
                    case 429 -> "Please wait before trying again.";
                    default -> "Please try again shortly.";
                };
        return """
            <!doctype html><html lang="en"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <title>%s · TinyRoute</title><style>
            :root{color-scheme:dark;--canvas:#1e201e;--ink:#ecdfcc;--border:#697565}
            @media(prefers-color-scheme:light){:root{color-scheme:light;--canvas:#ecdfcc;--ink:#1e201e}}
            *{box-sizing:border-box}body{margin:0;min-height:100dvh;display:grid;place-items:center;background:var(--canvas);color:var(--ink);font:16px/1.5 system-ui,sans-serif;padding:24px}
            main{width:100%%;max-width:560px;border-top:1px solid var(--border);padding-top:32px}h1{font-size:clamp(28px,5vw,36px);line-height:1.15;letter-spacing:-.8px;font-weight:600}p{max-width:65ch}
            </style></head><body><main><p>TinyRoute</p><h1>%s</h1><p>%s</p></main></body></html>
            """
                .formatted(title, title, message);
    }
}
