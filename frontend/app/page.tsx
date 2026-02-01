'use client';

import Link from "next/link";
import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import Badge from "@/components/Badge";
import Button from "@/components/Button";
import Card from "@/components/Card";
import Header from "@/components/Header";
import Input from "@/components/Input";
import { Canvas, createCanvas, fetchCanvases } from "@/lib/api/canvases";

const accentPalette = [
  { bg: "from-sky-400/20 via-sky-300/5 to-white/0", outline: "outline-sky-300/30", badge: "sky" as const },
  {
    bg: "from-emerald-400/20 via-emerald-300/5 to-white/0",
    outline: "outline-emerald-300/30",
    badge: "emerald" as const,
  },
  { bg: "from-indigo-400/15 via-indigo-300/5 to-white/0", outline: "outline-indigo-300/30", badge: "slate" as const },
  { bg: "from-amber-300/20 via-amber-200/5 to-white/0", outline: "outline-amber-300/30", badge: "amber" as const },
];

const formatDate = (value?: string) => {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
};

const initialsFromName = (name: string) =>
  name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((segment) => segment[0])
    .join("")
    .toUpperCase() || "CV";

function CanvasCard({ canvas, index }: { canvas: Canvas; index: number }) {
  const accent = accentPalette[index % accentPalette.length];
  const updatedAt = formatDate(canvas.updatedAt ?? canvas.createdAt);
  const detailLine = updatedAt ? `Updated ${updatedAt}` : "Shared workspace";

  return (
    <Link href={`/canvas/${canvas.id}`} className="block focus-visible:outline-none">
      <Card
        padding="md"
        className={`border-white/10 bg-gradient-to-br ${accent.bg} outline outline-1 ${accent.outline} transition duration-200 hover:-translate-y-0.5 hover:border-white/20 focus-visible:ring-2 focus-visible:ring-sky-300/60`}
      >
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white/10 text-sm font-semibold uppercase text-white">
              {initialsFromName(canvas.name)}
            </div>
            <div className="space-y-1">
              <p className="text-lg font-semibold text-white">{canvas.name}</p>
              <p className="text-xs text-slate-200/80">{detailLine}</p>
            </div>
          </div>
          <Badge label="Canvas" tone={accent.badge} />
        </div>
      </Card>
    </Link>
  );
}

function CanvasSkeleton() {
  return (
    <Card padding="md" className="border-white/5 bg-white/5">
      <div className="flex items-center gap-4 animate-pulse">
        <div className="h-12 w-12 rounded-xl bg-white/10" />
        <div className="flex-1 space-y-3">
          <div className="h-4 w-1/3 rounded-full bg-white/10" />
          <div className="h-3 w-1/2 rounded-full bg-white/10" />
        </div>
      </div>
    </Card>
  );
}

function EmptyState() {
  return (
    <Card padding="md" className="border-dashed border-white/15 bg-white/5 text-center">
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full border border-white/10 bg-white/5 text-sm font-semibold text-slate-100">
        +
      </div>
      <div className="mt-4 space-y-2">
        <p className="text-lg font-semibold text-white">No canvases yet</p>
        <p className="text-sm text-slate-200/80">
          Start a canvas to collaborate with your team in real time. You can add as many as you need.
        </p>
      </div>
    </Card>
  );
}

export default function Home() {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  const {
    data: canvases = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["canvases"],
    queryFn: fetchCanvases,
  });

  const canvasCount = useMemo(() => canvases.length, [canvases]);

  const createMutation = useMutation({
    mutationFn: createCanvas,
    onMutate: async (newName: string) => {
      setFormError(null);
      await queryClient.cancelQueries({ queryKey: ["canvases"] });

      const previous = queryClient.getQueryData<Canvas[]>(["canvases"]);
      const optimistic: Canvas = {
        id: `temp-${Date.now()}`,
        name: newName,
        createdAt: new Date().toISOString(),
      };

      queryClient.setQueryData<Canvas[]>(["canvases"], (old = []) => [...old, optimistic]);
      setName("");
      return { previous };
    },
    onError: (mutationError, _variables, context) => {
      setFormError(mutationError instanceof Error ? mutationError.message : "Unable to create canvas.");
      if (context?.previous) {
        queryClient.setQueryData(["canvases"], context.previous);
      }
    },
    onSuccess: (created) => {
      if (created) {
        queryClient.setQueryData<Canvas[]>(["canvases"], (existing = []) => {
          const filtered = existing.filter((item) => !item.id.startsWith("temp-"));
          return [...filtered, created];
        });
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["canvases"] });
    },
  });

  const handleCreate = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmed = name.trim();

    if (!trimmed) {
      setFormError("Please give your canvas a name.");
      return;
    }

    createMutation.mutate(trimmed);
  };

  return (
    <div className="relative isolate min-h-screen overflow-hidden bg-gradient-to-br from-slate-950 via-slate-900 to-slate-800 px-6 py-12 text-white">
      <div className="pointer-events-none absolute inset-0">
        <div className="absolute left-12 top-10 h-72 w-72 rounded-full bg-sky-400/15 blur-3xl" />
        <div className="absolute bottom-24 right-6 h-80 w-80 rounded-full bg-emerald-400/15 blur-3xl" />
      </div>

      <div className="relative mx-auto flex max-w-6xl flex-col gap-8">
        <header className="flex flex-wrap items-start justify-between gap-4">
          <div className="space-y-3">
            <div className="inline-flex items-center gap-3 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-xs font-semibold uppercase tracking-[0.12em] text-slate-100 backdrop-blur-sm">
              Encube
              <span className="h-1 w-1 rounded-full bg-sky-300" />
              Canvas workspace
            </div>
            <div className="space-y-2">
              <Header size="lg">Your canvases, all in one place.</Header>
              <p className="max-w-2xl text-base text-slate-200/80">
                Browse every workspace you have access to, spin up a fresh canvas in seconds, and jump
                back into collaborative mode without friction.
              </p>
            </div>
            <div className="flex flex-wrap gap-2 text-sm text-slate-200/90">
              <Badge label="Real-time notifications" tone="emerald" />
              <Badge label="File upload" tone="sky" />
              <Badge label="Not much more..." tone="slate" />
            </div>
          </div>

          <Card className="w-full max-w-xs border-white/10 bg-white/5 text-right" padding="md">
            <p className="text-sm font-semibold text-slate-200/90">Total canvases</p>
            <p className="mt-2 text-4xl font-semibold text-white">{canvasCount}</p>
            <p className="mt-1 text-xs text-slate-300/70">Updated live as your team creates.</p>
          </Card>
        </header>

        <div className="grid gap-6 lg:grid-cols-[minmax(0,1.55fr)_minmax(320px,0.85fr)]">
          <section className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-sm font-semibold uppercase tracking-[0.16em] text-slate-300">
                  Active canvases
                </p>
                <p className="text-xs text-slate-300/80">
                  Sorted by last update to keep what&apos;s fresh within reach.
                </p>
              </div>
              <Button
                type="button"
                variant="ghost"
                fullWidth={false}
                onClick={() => refetch()}
                className="w-auto px-3 py-2 text-sm"
                disabled={isLoading}
              >
                Refresh
              </Button>
            </div>

            {isError ? (
              <Card padding="md" className="border-red-400/30 bg-red-500/10 text-red-50">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-base font-semibold">We hit a snag.</p>
                    <p className="mt-1 text-sm text-red-100/90">
                      {(error as Error)?.message || "Unable to load canvases right now."}
                    </p>
                  </div>
                  <Button
                    type="button"
                    variant="primary"
                    fullWidth={false}
                    onClick={() => refetch()}
                    className="w-auto px-3 py-2 text-sm"
                  >
                    Try again
                  </Button>
                </div>
              </Card>
            ) : null}

            {isLoading ? (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                {Array.from({ length: 4 }).map((_, index) => (
                  <CanvasSkeleton key={`skeleton-${index}`} />
                ))}
              </div>
            ) : null}

            {!isLoading && !isError && canvases.length === 0 ? <EmptyState /> : null}

            {!isLoading && canvases.length > 0 ? (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                {canvases
                  .slice()
                  .sort((a, b) => {
                    const aDate = new Date(a.updatedAt ?? a.createdAt ?? 0).getTime();
                    const bDate = new Date(b.updatedAt ?? b.createdAt ?? 0).getTime();
                    return bDate - aDate;
                  })
                  .map((canvas, index) => (
                    <CanvasCard key={canvas.id || `${canvas.name}-${index}`} canvas={canvas} index={index} />
                  ))}
              </div>
            ) : null}
          </section>

          <Card padding="lg" className="border-white/10 bg-white/5">
            <form onSubmit={handleCreate} className="space-y-6">
              <div className="space-y-2">
                <p className="text-sm font-semibold uppercase tracking-[0.16em] text-slate-300">New canvas</p>
                <Header size="md">Create a fresh workspace.</Header>
                <p className="text-sm text-slate-200/80">
                  Name it, hit create, and you&apos;ll land in an empty canvas ready for realtime collaboration.
                </p>
              </div>

              <Input
                label="Canvas name"
                name="canvasName"
                placeholder="e.g. My latest product"
                value={name}
                onChange={(event) => setName(event.target.value)}
                required
              />

              {formError ? (
                <div className="rounded-lg border border-red-400/30 bg-red-500/10 px-3 py-2 text-sm text-red-100">
                  {formError}
                </div>
              ) : null}

              <Button type="submit" disabled={createMutation.isPending} className="mt-1">
                {createMutation.isPending ? "Creating..." : "Create canvas"}
              </Button>

              <div className="flex items-center gap-3 text-xs text-slate-200/75">
                <span className="h-px flex-1 bg-white/10" />
                <span>Everything stays in sync with your team.</span>
                <span className="h-px flex-1 bg-white/10" />
              </div>
            </form>
          </Card>
        </div>
      </div>
    </div>
  );
}
