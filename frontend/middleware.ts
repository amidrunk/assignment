import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

// Protect all routes by default and rely on session cookies set after form login.
// Backend session cookie is path-scoped to /encube-assignment-api/v1, so we also set a
// frontend-readable marker cookie (encube_auth) on successful login.
const SESSION_COOKIES = ["SESSION", "JSESSIONID", "encube_auth"];

export function middleware(req: NextRequest) {
  const { pathname, search } = req.nextUrl;

  // Allow the login page and Next.js internals through without checks to avoid redirect loops.
  if (
    pathname === "/login" ||
    pathname.startsWith("/encube-assignment-api/v1/login") ||
    pathname.startsWith("/_next") ||
    pathname.startsWith("/api/") ||
    pathname.startsWith("/favicon") ||
    pathname.startsWith("/assets") ||
    pathname.startsWith("/public")
  ) {
    return NextResponse.next();
  }

  const isAuthenticated = SESSION_COOKIES.some((name) => req.cookies.has(name));

  if (!isAuthenticated) {
    const loginUrl = new URL("/login", req.url);
    const redirectPath = pathname + (search || "");
    loginUrl.searchParams.set("redirect", redirectPath);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

// Apply to everything except Next.js assets and the login page itself.
export const config = {
  matcher: [
    "/((?!_next|static|favicon\\.ico|login|assets|public|api|encube-assignment-api/v1/login).*)",
  ],
};
