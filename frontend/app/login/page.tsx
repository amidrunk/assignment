'use client';

import { useRouter, useSearchParams } from "next/navigation";
import type { FormEvent } from "react";
import { Suspense, useState } from "react";

import Badge from "@/components/Badge";
import Button from "@/components/Button";
import Card from "@/components/Card";
import Checkbox from "@/components/Checkbox";
import Header from "@/components/Header";
import Input from "@/components/Input";

const highlights = [
  { label: "Live co-editing", tone: "emerald" as const },
  { label: "Secure by default", tone: "sky" as const },
  { label: "Cloud accelerated", tone: "amber" as const },
];

function LoginContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    void submit();
  };

  const submit = async () => {
    if (loading) return;
    setError(null);
    setLoading(true);

    try {
      const body = new URLSearchParams();
      body.append("username", username);
      body.append("password", password);

      const response = await fetch("/encube-assignment-api/v1/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
        },
        body: body.toString(),
        credentials: "include",
        redirect: "manual",
      });

      const success =
        response.ok ||
        response.status === 200 ||
        response.status === 204 ||
        (response.status >= 300 && response.status < 400) ||
        response.type === "opaqueredirect" ||
        (response.redirected && response.status === 404);

      if (success) {
        const redirectParam = searchParams.get("redirect");
        const safeRedirect = redirectParam && redirectParam.startsWith("/") ? redirectParam : "/";

        // Drop a readable cookie for the frontend/middleware; backend session is path-scoped.
        document.cookie = ["encube_auth=1", "Path=/", "SameSite=Lax"].join("; ");

        router.push(safeRedirect);
      } else {
        setError("Invalid username or password. Please try again.");
      }
    } catch (error) {
      console.error(error);
      setError("Unable to sign in right now. Please check your connection and try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative isolate min-h-screen overflow-hidden bg-gradient-to-br from-slate-950 via-slate-900 to-slate-800 px-6 py-12 text-white">
      <div className="pointer-events-none absolute inset-0">
        <div className="absolute left-10 top-16 h-56 w-56 rounded-full bg-sky-400/20 blur-3xl" />
        <div className="absolute bottom-24 right-10 h-64 w-64 rounded-full bg-indigo-500/20 blur-3xl" />
      </div>

      <div className="relative mx-auto grid w-full max-w-6xl grid-cols-1 items-center gap-14 lg:grid-cols-[1.1fr_0.9fr]">
        <section className="space-y-6">
          <div className="inline-flex items-center gap-3 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-xs font-semibold uppercase tracking-[0.12em] text-slate-100 backdrop-blur-sm">
            Encube
            <span className="h-1 w-1 rounded-full bg-sky-300" />
            Collaborative CAD
          </div>

          <div className="space-y-4">
            <Header>Welcome back.</Header>
            <p className="max-w-2xl text-lg text-slate-200/80">
              Sign in to sync designs, review assemblies with your team, and pick up where you left
              off—on any device.
            </p>
          </div>

          <div className="flex flex-wrap gap-3 text-sm text-slate-200/70">
            {highlights.map((item) => (
              <Badge key={item.label} label={item.label} tone={item.tone} />
            ))}
          </div>
        </section>

        <Card>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-4">
              <Input
                label="Username"
                name="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="you@encube.com"
                autoComplete="username"
                required
              />
              <Input
                label="Password"
                name="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                autoComplete="current-password"
                required
              />
            </div>

            {error ? (
              <div className="rounded-xl border border-red-400/30 bg-red-500/10 px-4 py-3 text-sm text-red-100">
                {error}
              </div>
            ) : null}

            <div className="flex items-center justify-between text-sm text-slate-200/80">
              <Checkbox label="Remember me" defaultChecked />
              <a
                href="#"
                className="font-semibold text-sky-300 transition hover:text-sky-200 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-300"
              >
                Forgot password?
              </a>
            </div>

            <Button type="submit" disabled={loading} className={loading ? "opacity-80" : ""}>
              {loading ? "Signing in..." : "Sign in"}
            </Button>
          </form>
        </Card>
      </div>
    </div>
  );
}

export default function Login() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-950 via-slate-900 to-slate-800 text-white">
          <span className="text-sm text-slate-200/80">Preparing sign-in…</span>
        </div>
      }
    >
      <LoginContent />
    </Suspense>
  );
}
