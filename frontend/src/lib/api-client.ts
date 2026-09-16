import { z } from "zod";

const errorCodeSchema = z.enum([
  "VALIDATION_ERROR",
  "AUTHENTICATION_FAILED",
  "CSRF_INVALID",
  "OAUTH_FAILED",
  "OTP_INVALID",
  "OTP_EXPIRED",
  "RESET_TOKEN_INVALID",
  "RATE_LIMITED",
  "SESSION_UNAVAILABLE",
  "SERVICE_UNAVAILABLE",
]);

const apiErrorSchema = z.object({
  error: z.object({
    code: errorCodeSchema,
    message: z.string(),
    fieldErrors: z.record(z.string(), z.string()).optional(),
    retryAfterSeconds: z.number().int().nonnegative().optional(),
    requestId: z.string(),
  }),
});

export type ApiError = z.infer<typeof apiErrorSchema>;

export class ApiClientError extends Error {
  readonly status: number;
  readonly apiError: ApiError | null;

  constructor(status: number, apiError: ApiError | null) {
    super(
      apiError?.error.message ?? `API request failed with status ${status}`,
    );
    this.name = "ApiClientError";
    this.status = status;
    this.apiError = apiError;
  }
}

type ApiRequestOptions<T> = Omit<RequestInit, "credentials"> & {
  responseSchema: z.ZodType<T>;
};

function resolveApiUrl(path: string): string {
  if (!path.startsWith("/") || path.startsWith("//")) {
    throw new TypeError("API path must start with a single slash");
  }

  const configuredBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;
  if (!configuredBaseUrl) {
    throw new Error("NEXT_PUBLIC_API_BASE_URL is not configured");
  }

  const baseUrl = new URL(configuredBaseUrl);
  if (baseUrl.protocol !== "http:" && baseUrl.protocol !== "https:") {
    throw new Error("NEXT_PUBLIC_API_BASE_URL must use HTTP or HTTPS");
  }

  const requestUrl = new URL(path, baseUrl);
  if (requestUrl.origin !== baseUrl.origin) {
    throw new TypeError("API path must resolve to the configured API origin");
  }

  return requestUrl.toString();
}

async function readJson(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    return undefined;
  }
}

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions<T>,
): Promise<T> {
  const { responseSchema, headers: initialHeaders, ...requestInit } = options;
  const headers = new Headers(initialHeaders);
  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }

  const response = await fetch(resolveApiUrl(path), {
    ...requestInit,
    credentials: "include",
    headers,
  });
  const payload =
    response.status === 204 ? undefined : await readJson(response);

  if (!response.ok) {
    const parsedError = apiErrorSchema.safeParse(payload);
    throw new ApiClientError(
      response.status,
      parsedError.success ? parsedError.data : null,
    );
  }

  return responseSchema.parse(payload);
}
