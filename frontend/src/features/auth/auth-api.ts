import { z } from "zod";

import { ApiClientError, apiRequest } from "../../lib/api-client";
import { clearCsrfToken, getCsrfToken } from "../../lib/csrf";

const pendingRegistrationSchema = z.object({
  status: z.literal("PENDING_VERIFICATION"),
});

const acceptedSchema = z.object({ status: z.literal("ACCEPTED") });

const sessionSchema = z.object({
  authenticated: z.literal(true),
  user: z.object({
    email: z.string().email(),
  }),
});

export type Session =
  { authenticated: false } | { authenticated: true; user: { email: string } };

export type RegistrationInput = {
  email: string;
  password: string;
};

export type LoginInput = RegistrationInput;

const emptyResponseSchema = z.undefined();

function mutationOptions(csrfToken: string, body?: object) {
  return {
    body: body === undefined ? undefined : JSON.stringify(body),
    headers: {
      "Content-Type": "application/json",
      "X-CSRF-TOKEN": csrfToken,
    },
    method: "POST",
  };
}

export async function fetchCsrfToken(): Promise<string> {
  return getCsrfToken();
}

export function startRegistration(input: RegistrationInput, csrfToken: string) {
  return apiRequest("/api/auth/register", {
    ...mutationOptions(csrfToken, input),
    responseSchema: pendingRegistrationSchema,
  });
}

export async function verifyRegistration(otp: string, csrfToken: string) {
  const response = await apiRequest("/api/auth/register/verify", {
    ...mutationOptions(csrfToken, { otp }),
    responseSchema: sessionSchema,
  });
  clearCsrfToken();
  return response;
}

export async function login(
  input: LoginInput,
  csrfToken: string,
): Promise<void> {
  await apiRequest("/api/auth/login", {
    ...mutationOptions(csrfToken, input),
    responseSchema: sessionSchema,
  });
  clearCsrfToken();
}

export async function getCurrentSession(): Promise<Session> {
  try {
    return await apiRequest("/api/auth/me", {
      method: "GET",
      responseSchema: sessionSchema,
    });
  } catch (error) {
    if (error instanceof ApiClientError && error.status === 401) {
      try {
        return await refreshSession();
      } catch (refreshError) {
        if (
          refreshError instanceof ApiClientError &&
          refreshError.status === 401
        ) {
          clearCsrfToken();
          return { authenticated: false };
        }
        if (
          refreshError instanceof ApiClientError &&
          refreshError.status === 403
        ) {
          clearCsrfToken();
        }
        throw refreshError;
      }
    }
    throw error;
  }
}

export async function refreshSession(): Promise<Session> {
  const csrfToken = await getCsrfToken();
  return apiRequest("/api/auth/refresh", {
    ...mutationOptions(csrfToken),
    responseSchema: sessionSchema,
  });
}

export async function logout(): Promise<void> {
  try {
    const csrfToken = await getCsrfToken();
    await apiRequest("/api/auth/logout", {
      ...mutationOptions(csrfToken),
      responseSchema: emptyResponseSchema,
    });
  } catch (error) {
    if (error instanceof ApiClientError && error.status === 401) {
      clearCsrfToken();
      return;
    }
    if (error instanceof ApiClientError && error.status === 403) {
      clearCsrfToken();
    }
    throw error;
  }
  clearCsrfToken();
}

export { clearCsrfToken };

export function resendRegistrationOtp(csrfToken: string) {
  return apiRequest("/api/auth/register/resend-otp", {
    ...mutationOptions(csrfToken),
    responseSchema: pendingRegistrationSchema,
  });
}

export function requestPasswordReset(email: string, csrfToken: string) {
  return apiRequest("/api/auth/password-reset", {
    ...mutationOptions(csrfToken, { email }),
    responseSchema: acceptedSchema,
  });
}

export function confirmPasswordReset(
  token: string,
  newPassword: string,
  csrfToken: string,
) {
  return apiRequest("/api/auth/password-reset/confirm", {
    ...mutationOptions(csrfToken, { token, newPassword }),
    responseSchema: emptyResponseSchema,
  });
}
