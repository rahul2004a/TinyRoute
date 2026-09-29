import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { AuthLayout } from "./auth-layout";

describe("AuthLayout", () => {
  it("keeps the form focused and provides a URL-shortening companion on desktop", () => {
    render(
      <AuthLayout>
        <p>Form content</p>
      </AuthLayout>,
    );

    expect(screen.getByText("Form content")).toBeTruthy();
    expect(
      screen.getByRole("heading", { name: "Short links. Smarter sharing." }),
    ).toBeTruthy();
    expect(screen.getByTestId("auth-url-shortening")).toBeTruthy();
    expect(
      screen.getByText("Create, manage, and track your links from one place."),
    ).toBeTruthy();
  });
});
