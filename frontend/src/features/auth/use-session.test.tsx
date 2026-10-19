import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, cleanup, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { ApiClientError } from "../../lib/api-client";
import { getCurrentSession } from "./auth-api";
import { useSession } from "./use-session";

vi.mock("./auth-api", () => ({ getCurrentSession: vi.fn() }));

afterEach(cleanup);

describe("useSession", () => {
  it.each([404, 503])(
    "finishes a failed %s check without inherited retry delays, then permits explicit recovery",
    async (status) => {
      const getSession = vi.mocked(getCurrentSession);
      getSession.mockRejectedValue(new ApiClientError(status, null));
      // Preserve automatic retries for other queries, as the real app does.
      const client = new QueryClient({
        defaultOptions: { queries: { retry: 3, retryDelay: 1, gcTime: 0 } },
      });
      const wrapper = ({ children }: { children: ReactNode }) => (
        <QueryClientProvider client={client}>{children}</QueryClientProvider>
      );
      const { result } = renderHook(useSession, { wrapper });

      await waitFor(() => expect(result.current.isError).toBe(true));
      expect(getSession).toHaveBeenCalledOnce();

      getSession.mockResolvedValue({ authenticated: false });
      await act(async () => {
        await result.current.refetch();
      });
      await waitFor(() =>
        expect(result.current.data).toEqual({ authenticated: false }),
      );
      expect(getSession).toHaveBeenCalledTimes(2);
      client.clear();
    },
  );
});
