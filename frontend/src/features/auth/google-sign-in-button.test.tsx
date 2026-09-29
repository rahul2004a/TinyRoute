import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";

import { GoogleSignInButton } from "./google-sign-in-button";

const originalApiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;

afterEach(() => {
  cleanup();
  if (originalApiBaseUrl === undefined) {
    delete process.env.NEXT_PUBLIC_API_BASE_URL;
  } else {
    process.env.NEXT_PUBLIC_API_BASE_URL = originalApiBaseUrl;
  }
});

describe("GoogleSignInButton", () => {
  it("navigates directly to the configured backend authorization endpoint", () => {
    process.env.NEXT_PUBLIC_API_BASE_URL = "https://api.tinyroute.test";

    render(<GoogleSignInButton />);

    expect(
      screen
        .getByRole("link", { name: "Continue with Google" })
        .getAttribute("href"),
    ).toBe("https://api.tinyroute.test/api/auth/google/start");
  });

  it("does not prevent password sign-in when the public API origin is unavailable", () => {
    delete process.env.NEXT_PUBLIC_API_BASE_URL;

    render(<GoogleSignInButton />);

    expect(
      screen.queryByRole("link", { name: "Continue with Google" }),
    ).toBeNull();
  });
});
