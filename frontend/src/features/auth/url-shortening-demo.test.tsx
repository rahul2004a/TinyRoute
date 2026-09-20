import { act, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { LONG_URL, SHORT_URL, UrlShorteningDemo } from "./url-shortening-demo";

describe("UrlShorteningDemo", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("types the full source URL before transforming it into the TinyRoute URL", async () => {
    vi.useFakeTimers();
    render(<UrlShorteningDemo />);

    expect(screen.getByTestId("auth-url-source").textContent).toBe("");

    act(() => {
      vi.advanceTimersByTime(28);
    });
    expect(screen.getByTestId("auth-url-source").textContent).toBe("h");

    for (let character = 1; character < LONG_URL.length; character += 1) {
      await act(async () => {
        await vi.advanceTimersToNextTimerAsync();
      });
    }
    expect(screen.getByTestId("auth-url-source").textContent).toBe(LONG_URL);

    await act(async () => {
      await vi.advanceTimersToNextTimerAsync();
    });
    await act(async () => {
      await vi.advanceTimersToNextTimerAsync();
    });
    await act(async () => {
      await vi.advanceTimersToNextTimerAsync();
    });
    expect(screen.getByTestId("auth-url-result").textContent).toBe(SHORT_URL);
  });
});
