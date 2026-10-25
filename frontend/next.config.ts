import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  devIndicators: false,
  async rewrites() {
    const backendOrigin = process.env.BACKEND_ORIGIN;
    if (!backendOrigin) return [];

    const backend = new URL(backendOrigin);
    if (
      !["http:", "https:"].includes(backend.protocol) ||
      backend.username ||
      backend.password ||
      backend.pathname !== "/" ||
      backend.search ||
      backend.hash
    ) {
      throw new Error("BACKEND_ORIGIN must be an HTTP(S) origin");
    }

    return {
      beforeFiles: [
        {
          source: "/api/:path*",
          destination: `${backend.origin}/api/:path*`,
        },
      ],
      fallback: [
        {
          source: "/:code((?!actuator$|error$|_next$)[A-Za-z0-9_-]{3,64})",
          destination: `${backend.origin}/:code`,
        },
      ],
    };
  },
  async headers() {
    return [
      {
        source: "/password-reset/confirm",
        headers: [{ key: "Referrer-Policy", value: "no-referrer" }],
      },
    ];
  },
};

export default nextConfig;
