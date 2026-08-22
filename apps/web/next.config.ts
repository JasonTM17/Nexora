import type { NextConfig } from "next";

const publicCsp = [
  "default-src 'self'",
  // Static App Router output includes Next bootstrap scripts without per-request nonces.
  "script-src 'self' 'unsafe-inline'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data:",
  "font-src 'self'",
  "connect-src 'self'",
  "frame-ancestors 'none'",
  "base-uri 'self'",
  "form-action 'self'",
].join("; ");

const nextConfig: NextConfig = {
  output: "standalone",
  // Turbopack's standalone tracing copies @swc/helpers partially (cjs only) from
  // the pnpm store while the Next server resolves its `esm` subpaths at boot,
  // crashing the container. Force-include the whole package in the trace.
  outputFileTracingIncludes: {
    "/**": ["../../node_modules/.pnpm/@swc+helpers@*/node_modules/@swc/helpers/**"],
  },
  async headers() {
    return [
      {
        source: "/",
        headers: [
          { key: "Content-Security-Policy", value: publicCsp },
          { key: "Cache-Control", value: "public, max-age=0, s-maxage=3600, stale-while-revalidate=86400" },
        ],
      },
    ];
  },
};

export default nextConfig;
