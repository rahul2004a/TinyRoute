import { z } from "zod";

import { ApiClientError, apiRequest } from "../../lib/api-client";

const csrfTokenSchema = z.object({
  csrfToken: z.string().min(1),
});

const pendingRegistrationSchema = z.object({
  status: z.literal("PENDING_VERIFICATION"),
});

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
  const response = await apiRequest("/api/auth/csrf", {
    method: "GET",
    responseSchema: csrfTokenSchema,
  });
  return response.csrfToken;
}

export function startRegistration(input: RegistrationInput, csrfToken: string) {
  return apiRequest("/api/auth/register", {
    ...mutationOptions(csrfToken, input),
    responseSchema: pendingRegistrationSchema,
  });
}

export function verifyRegistration(otp: string, csrfToken: string) {
  return apiRequest("/api/auth/register/verify", {
    ...mutationOptions(csrfToken, { otp }),
    responseSchema: sessionSchema,
  });
}

export async function login(
  input: LoginInput,
  csrfToken: string,
): Promise<void> {
  await apiRequest("/api/auth/login", {
    ...mutationOptions(csrfToken, input),
    responseSchema: sessionSchema,
  });
}

export async function getCurrentSession(): Promise<Session> {
  try {
    return await apiRequest("/api/auth/me", {
      method: "GET",
      responseSchema: sessionSchema,
    });
  } catch (error) {
    if (error instanceof ApiClientError && error.status === 401) {
      return { authenticated: false };
    }
    throw error;
  }
}

export function resendRegistrationOtp(csrfToken: string) {
  return apiRequest("/api/auth/register/resend-otp", {
    ...mutationOptions(csrfToken),
    responseSchema: pendingRegistrationSchema,
  });
}
