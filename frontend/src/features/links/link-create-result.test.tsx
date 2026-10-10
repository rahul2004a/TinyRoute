import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import { LinkCreateResult } from "./link-create-result";

const link = {
  id: "fixture",
  code: "Abc12345",
  shortUrl: "https://go.tinyroute.test/Abc12345",
  destinationUrl: "https://example.com/docs?q=java#setup",
  createdAt: "2026-10-08T00:00:00Z",
  expiresAt: null,
};

it("displays the returned expiry with its exact instant", () => {
  const expiry = "2030-10-08T12:30:00Z";
  render(<LinkCreateResult link={{ ...link, expiresAt: expiry }} />);
  expect(document.querySelector("time")?.getAttribute("datetime")).toBe(expiry);
  expect(screen.queryByText("No expiry")).toBeNull();
});
afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});
it("shows a selectable server URL and only announces copy after the clipboard succeeds", async () => {
  const user = userEvent.setup();
  let resolve!: () => void;
  const write = vi.fn(
    () =>
      new Promise<void>((r) => {
        resolve = r;
      }),
  );
  Object.defineProperty(navigator, "clipboard", {
    configurable: true,
    value: { writeText: write },
  });
  render(<LinkCreateResult link={link} />);
  expect((screen.getByLabelText("Short URL") as HTMLInputElement).value).toBe(
    link.shortUrl,
  );
  expect(screen.getByText("No expiry")).toBeTruthy();
  await user.click(screen.getByRole("button", { name: "Copy short link" }));
  expect(write).toHaveBeenCalledWith(link.shortUrl);
  expect(screen.queryByText("Copied to clipboard.")).toBeNull();
  resolve();
  await waitFor(() =>
    expect(screen.getByRole("status").textContent).toBe("Copied to clipboard."),
  );
});
it.each(["denied", "missing"])(
  "keeps manual copying available when clipboard is %s",
  async (mode) => {
    const user = userEvent.setup();
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value:
        mode === "missing"
          ? undefined
          : { writeText: vi.fn().mockRejectedValue(new Error("denied")) },
    });
    render(<LinkCreateResult link={link} />);
    await user.click(screen.getByRole("button", { name: "Copy short link" }));
    expect((await screen.findByRole("status")).textContent).toContain(
      "Select the URL and copy it manually.",
    );
    expect(screen.queryByText("Copied to clipboard.")).toBeNull();
    expect((screen.getByLabelText("Short URL") as HTMLInputElement).value).toBe(
      link.shortUrl,
    );
  },
);
