import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */

  reactCompiler: true,
  async rewrites() {
    return [
      {
        source: '/encube-assignment-api/v1/:path*',
        destination: `http://backend:8080/encube-assignment-api/v1/:path*`,
      },
    ]
  },
};

export default nextConfig;
