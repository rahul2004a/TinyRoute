import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { LinkCreateResult, SavedLinkHistory } from "./link-create-result";
import { getLinks } from "./link-api";
vi.mock("./link-api", () => ({
  getLinks: vi.fn(),
  linkHistoryQueryKey: ["links", "history"],
}));
beforeEach(() => vi.mocked(getLinks).mockReset());

const link = {
  id: "fixture",
  code: "Abc12345",
  shortUrl: "https://go.tinyroute.test/Abc12345",
  destinationUrl: "https://example.com/docs?q=java#setup",
  createdAt: "2026-10-08T00:00:00Z",
  expiresAt: null,
};

function history(accountKey = "first@example.com") {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <SavedLinkHistory accountKey={accountKey} />
    </QueryClientProvider>,
  );
}

it("loads saved pages and navigates forward and backward using server cursors", async () => {
  const first = { ...link, status: "ACTIVE" as const, clickCount: 0 };
  const second = {
    ...first,
    id: "second",
    code: "Older",
    shortUrl: "https://go.tinyroute.test/Older",
  };
  vi.mocked(getLinks).mockImplementation(async (cursor) =>
    cursor === "next-page"
      ? { items: [second], nextCursor: null }
      : { items: [first], nextCursor: "next-page" },
  );
  const user = userEvent.setup();
  history();
  await screen.findAllByRole("link", { name: first.shortUrl });
  expect(
    (screen.getByRole("button", { name: "Previous" }) as HTMLButtonElement)
      .disabled,
  ).toBe(true);
  await user.click(screen.getByRole("button", { name: "Next" }));
  await screen.findAllByRole("link", { name: second.shortUrl });
  expect(screen.getByText("Page 2")).toBeTruthy();
  expect(
    (screen.getByRole("button", { name: "Next" }) as HTMLButtonElement)
      .disabled,
  ).toBe(true);
  await user.click(screen.getByRole("button", { name: "Previous" }));
  await screen.findAllByRole("link", { name: first.shortUrl });
  expect(screen.getByText("Page 1")).toBeTruthy();
  expect(getLinks).toHaveBeenCalledWith("next-page", expect.any(AbortSignal));
});

it("shows loading, a safe failure, and a retryable empty result", async () => {
  let reject!: (reason: Error) => void;
  vi.mocked(getLinks).mockReturnValueOnce(
    new Promise((_resolve, failure) => {
      reject = failure;
    }),
  );
  const user = userEvent.setup();
  history();
  expect(screen.getByText("Loading your links.")).toBeTruthy();
  reject(new Error("private datastore details"));
  expect((await screen.findByRole("alert")).textContent).toContain(
    "We couldn’t load your links.",
  );
  expect(screen.queryByText("private datastore details")).toBeNull();
  vi.mocked(getLinks).mockResolvedValue({ items: [], nextCursor: null });
  await user.click(screen.getByRole("button", { name: "Refresh links" }));
  expect(await screen.findByText("No saved links on this page.")).toBeTruthy();
});

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
