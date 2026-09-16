import { render, screen } from "@testing-library/react";
import { useQueryClient } from "@tanstack/react-query";
import { afterEach, describe, expect, it, vi } from "vitest";

import { Providers } from "../app/providers";

function QueryClientProbe() {
  const queryClient = useQueryClient();

  return <span>{queryClient ? "provider-ready" : "provider-missing"}</span>;
}

describe("Providers", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("provides a query client without making an application request", () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    render(
      <Providers>
        <QueryClientProbe />
      </Providers>,
    );

    expect(screen.getByText("provider-ready")).toBeTruthy();
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
