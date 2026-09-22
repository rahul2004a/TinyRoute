import { z } from "zod";

import { apiRequest } from "./api-client";

const csrfTokenSchema = z.object({
  csrfToken: z.string().min(1),
});

let csrfToken: string | undefined;
let csrfTokenRequest: Promise<string> | undefined;

export function clearCsrfToken() {
  csrfToken = undefined;
  csrfTokenRequest = undefined;
}

export async function getCsrfToken(): Promise<string> {
  if (csrfToken) {
    return csrfToken;
  }

  csrfTokenRequest ??= apiRequest("/api/auth/csrf", {
    method: "GET",
    responseSchema: csrfTokenSchema,
  }).then((response) => {
    csrfToken = response.csrfToken;
    return csrfToken;
  });

  try {
    return await csrfTokenRequest;
  } catch (error) {
    clearCsrfToken();
    throw error;
  }
}
