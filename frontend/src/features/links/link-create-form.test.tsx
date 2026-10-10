import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { LinkCreateForm } from "./link-create-form";
import { localExpiryToInstant } from "./link-form-schema";
import { createLink } from "./link-api";
import { getCurrentSession } from "../auth/auth-api";
import { clearCsrfToken } from "../../lib/csrf";
import { ApiClientError, type ApiError } from "../../lib/api-client";

vi.mock("./link-api", () => ({ createLink: vi.fn() }));
vi.mock("../auth/auth-api", () => ({ getCurrentSession: vi.fn() }));
vi.mock("../../lib/csrf", () => ({ clearCsrfToken: vi.fn() }));
const link = {
  id: "fixture",
  code: "Abc12345",
  shortUrl: "https://go.tinyroute.test/Abc12345",
  destinationUrl: "https://example.com/docs?q=java#setup",
  createdAt: "2026-10-08T00:00:00Z",
  expiresAt: null,
};
function mount() {
  return render(
    <QueryClientProvider
      client={
        new QueryClient({
          defaultOptions: {
            queries: { retry: false },
            mutations: { retry: 3 },
          },
        })
      }
    >
      <LinkCreateForm />
    </QueryClientProvider>,
  );
}
beforeEach(() => {
  vi.mocked(getCurrentSession).mockResolvedValue({
    authenticated: true,
    user: { email: "fixture@example.com" },
  });
  vi.mocked(createLink).mockResolvedValue(link);
});
afterEach(() => cleanup());
async function fill() {
  const user = userEvent.setup();
  await user.type(
    await screen.findByLabelText("HTTPS destination"),
    link.destinationUrl,
  );
  await user.type(screen.getByLabelText("Custom alias (optional)"), "MyAlias");
  return user;
}
function error(
  status: number,
  code: ApiError["error"]["code"],
  extra: Partial<ApiError["error"]> = {},
) {
  return new ApiClientError(status, {
    error: {
      code,
      message: "Safe API message",
      requestId: "fixture",
      ...extra,
    },
  });
}
it("checks the session and prompts signed-out visitors to sign in", async () => {
  vi.mocked(getCurrentSession).mockResolvedValue({ authenticated: false });
  mount();
  expect(
    (await screen.findByRole("link", { name: "Sign in" })).getAttribute("href"),
  ).toBe("/login");
  expect(createLink).not.toHaveBeenCalled();
  expect(screen.queryByLabelText("HTTPS destination")).toBeNull();
});
it("keeps creation unavailable while session state is pending or unknown", async () => {
  vi.mocked(getCurrentSession).mockImplementation(() => new Promise(() => {}));
  mount();
  expect(screen.getByRole("status").textContent).toBe("Checking your session.");
  cleanup();
  vi.mocked(getCurrentSession).mockRejectedValue(new Error("unknown"));
  mount();
  expect((await screen.findByRole("alert")).textContent).toContain(
    "check your session",
  );
  expect(screen.queryByLabelText("HTTPS destination")).toBeNull();
});
it("submits optional inputs once, retains the inputs, and renders the authoritative result", async () => {
  mount();
  const user = await fill();
  await user.click(screen.getByRole("button", { name: "Create short link" }));
  await waitFor(() =>
    expect(vi.mocked(createLink).mock.calls[0]?.[0]).toEqual({
      destinationUrl: link.destinationUrl,
      alias: "MyAlias",
    }),
  );
  expect(
    ((await screen.findByLabelText("Short URL")) as HTMLInputElement).value,
  ).toBe(link.shortUrl);
  expect(
    (screen.getByLabelText("HTTPS destination") as HTMLInputElement).value,
  ).toBe(link.destinationUrl);
});
it("prevents duplicate pending requests", async () => {
  vi.mocked(createLink).mockImplementation(() => new Promise(() => {}));
  mount();
  const user = await fill();
  await user.dblClick(
    screen.getByRole("button", { name: "Create short link" }),
  );
  expect(createLink).toHaveBeenCalledTimes(1);
  expect(
    screen.getByRole("button", { name: "Creating…" }).hasAttribute("disabled"),
  ).toBe(true);
});
it.each([
  [400, "VALIDATION_ERROR"],
  [409, "ALIAS_UNAVAILABLE"],
  [429, "RATE_LIMITED"],
  [503, "SERVICE_UNAVAILABLE"],
] as const)(
  "retains inputs after %s and never retries automatically",
  async (status, code) => {
    vi.mocked(createLink).mockRejectedValue(
      error(status, code, {
        retryAfterSeconds: 31,
        fieldErrors:
          status === 400 ? { destinationUrl: "Use an HTTPS URL." } : undefined,
      }),
    );
    mount();
    const user = await fill();
    await user.click(screen.getByRole("button", { name: "Create short link" }));
    expect(await screen.findByRole("alert")).toBeTruthy();
    expect(
      (screen.getByLabelText("HTTPS destination") as HTMLInputElement).value,
    ).toBe(link.destinationUrl);
    expect(
      (screen.getByLabelText("Custom alias (optional)") as HTMLInputElement)
        .value,
    ).toBe("MyAlias");
    expect(createLink).toHaveBeenCalledTimes(1);
    if (status === 429)
      expect(screen.getByRole("alert").textContent).toContain("31 seconds");
    if (status === 400)
      expect(
        screen
          .getByLabelText("HTTPS destination")
          .getAttribute("aria-describedby"),
      ).toContain("destinationUrl-error");
  },
);
it("rechecks a rejected session and requires deliberate resubmission", async () => {
  vi.mocked(createLink).mockRejectedValue(error(401, "AUTHENTICATION_FAILED"));
  mount();
  const user = await fill();
  await user.click(screen.getByRole("button", { name: "Create short link" }));
  expect((await screen.findByRole("alert")).textContent).toContain(
    "Submit again",
  );
  expect(getCurrentSession).toHaveBeenCalledTimes(2);
  expect(createLink).toHaveBeenCalledTimes(1);
});
it("clears rejected CSRF without retrying creation", async () => {
  vi.mocked(createLink).mockRejectedValue(error(403, "CSRF_INVALID"));
  mount();
  const user = await fill();
  await user.click(screen.getByRole("button", { name: "Create short link" }));
  expect((await screen.findByRole("alert")).textContent).toContain(
    "Submit again",
  );
  expect(clearCsrfToken).toHaveBeenCalledOnce();
  expect(createLink).toHaveBeenCalledTimes(1);
});
it("warns about ambiguous network failure without retrying", async () => {
  vi.mocked(createLink).mockRejectedValue(new TypeError("network"));
  mount();
  const user = await fill();
  await user.click(screen.getByRole("button", { name: "Create short link" }));
  expect((await screen.findByRole("alert")).textContent).toContain(
    "Creation may have succeeded",
  );
  expect(createLink).toHaveBeenCalledTimes(1);
});
it("converts local expiry to a UTC instant and rejects invalid local times", () => {
  expect(localExpiryToInstant("")).toBeUndefined();
  expect(localExpiryToInstant("2030-10-08T12:30")).toBe(
    new Date(2030, 9, 8, 12, 30).toISOString(),
  );
  expect(() => localExpiryToInstant("2030-02-30T12:30")).toThrow();
});

it("keeps the last successful URL available after a later ambiguous failure", async () => {
  mount();
  const user = await fill();
  await user.click(screen.getByRole("button", { name: "Create short link" }));
  await screen.findByLabelText("Short URL");
  vi.mocked(createLink).mockRejectedValueOnce(new TypeError("network"));
  await user.click(screen.getByRole("button", { name: "Create short link" }));
  await screen.findByRole("alert");
  expect((screen.getByLabelText("Short URL") as HTMLInputElement).value).toBe(
    link.shortUrl,
  );
  expect(createLink).toHaveBeenCalledTimes(2);
});

it("submits a local expiry as the exact UTC instant", async () => {
  mount();
  const user = await fill();
  fireEvent.change(screen.getByLabelText("Expiry (optional)"), {
    target: { value: "2030-10-08T12:30" },
  });
  await user.click(screen.getByRole("button", { name: "Create short link" }));
  await waitFor(() =>
    expect(vi.mocked(createLink).mock.calls[0]?.[0]).toEqual({
      destinationUrl: link.destinationUrl,
      alias: "MyAlias",
      expiresAt: new Date(2030, 9, 8, 12, 30).toISOString(),
    }),
  );
});

it("provides associated client format feedback before submitting", async () => {
  mount();
  const user = userEvent.setup();
  await user.type(
    await screen.findByLabelText("HTTPS destination"),
    "http://example.com",
  );
  await user.click(screen.getByRole("button", { name: "Create short link" }));
  expect(
    await screen.findByText("Enter a valid HTTPS destination."),
  ).toBeTruthy();
  expect(
    screen.getByLabelText("HTTPS destination").getAttribute("aria-invalid"),
  ).toBe("true");
  expect(createLink).not.toHaveBeenCalled();
});
