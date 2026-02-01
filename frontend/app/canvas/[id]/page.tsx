'use client';

import Link from "next/link";
import { useParams } from "next/navigation";
import { DragEvent, useCallback, useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import Badge from "@/components/Badge";
import Button from "@/components/Button";
import Card from "@/components/Card";
import Header from "@/components/Header";
import { Canvas, CanvasFile, fetchCanvases, fetchFiles, uploadFile } from "@/lib/api/canvases";

const formatDateTime = (value?: string) => {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
};

const humanSize = (bytes?: number) => {
  if (!bytes && bytes !== 0) return null;
  if (bytes < 1024) return `${bytes} B`;
  const units = ["KB", "MB", "GB", "TB"];
  let size = bytes / 1024;
  let unitIndex = 0;
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex += 1;
  }
  return `${size.toFixed(size >= 10 ? 0 : 1)} ${units[unitIndex]}`;
};

const kindTone: Record<NonNullable<CanvasFile["kind"]>, { label: string; tone: Parameters<typeof Badge>[0]["tone"] }> =
  {
    document: { label: "Document", tone: "sky" },
    model: { label: "Model", tone: "emerald" },
    image: { label: "Image", tone: "amber" },
    data: { label: "Data", tone: "slate" },
    other: { label: "File", tone: "slate" },
  };

function FileRow({ file }: { file: CanvasFile }) {
  const updated = formatDateTime(file.updatedAt);
  const meta = [file.owner, updated].filter(Boolean).join(" • ");
  const size = humanSize(file.size);
  const badge = file.kind ? kindTone[file.kind] : { label: "File", tone: "slate" as const };
  const displayName = file.name ?? file.fileName ?? "Untitled file";

  return (
    <div className="flex flex-wrap items-center gap-3 rounded-xl border border-white/10 bg-white/5 px-4 py-3 shadow-sm">
      <div className="flex min-w-0 flex-1 items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-white/10 text-sm font-semibold text-white">
          {displayName.slice(0, 2).toUpperCase()}
        </div>
        <div className="min-w-0 space-y-1">
          <p className="truncate text-sm font-semibold text-white">{displayName}</p>
          <p className="truncate text-xs text-slate-200/80">{meta || "Synced"}</p>
        </div>
      </div>
      <div className="flex items-center gap-3 text-sm text-slate-100">
        {size ? <span className="text-xs text-slate-200/80">{size}</span> : null}
        <Badge label={badge.label} tone={badge.tone} />
      </div>
    </div>
  );
}

function FilesSkeleton() {
  return (
    <div className="space-y-3">
      {Array.from({ length: 4 }).map((_, index) => (
        <div
          key={`skeleton-${index}`}
          className="h-14 rounded-xl border border-white/10 bg-white/5"
        >
          <div className="h-full w-full animate-pulse rounded-xl bg-white/5" />
        </div>
      ))}
    </div>
  );
}

export default function CanvasDetail() {
  const params = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const canvasId = params.id;
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [dragging, setDragging] = useState(false);

  const {
    data: canvases = [],
    isLoading: canvasesLoading,
    isError: canvasesError,
  } = useQuery({
    queryKey: ["canvases"],
    queryFn: fetchCanvases,
    staleTime: 1000 * 60 * 5,
  });

  const canvas = useMemo<Canvas | undefined>(
    () => canvases.find((item) => item.id === canvasId),
    [canvasId, canvases],
  );

  const {
    data: files = [],
    isLoading: filesLoading,
    isError: filesError,
    error: filesErrorObj,
    refetch: refetchFiles,
  } = useQuery({
    queryKey: ["canvas-files", canvasId],
    queryFn: () => fetchFiles(canvasId),
    enabled: Boolean(canvasId),
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) => uploadFile(canvasId, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["canvas-files", canvasId] });
    },
  });

  const handleFiles = useCallback(
    (fileList: FileList | null) => {
      if (!fileList || !fileList.length) return;

      const files = Array.from(fileList);
      files.forEach((file) => uploadMutation.mutate(file));
    },
    [uploadMutation],
  );

  const onDrop = (event: DragEvent<HTMLLabelElement>) => {
    event.preventDefault();
    event.stopPropagation();
    setDragging(false);
    handleFiles(event.dataTransfer.files);
  };

  const onDragOver = (event: DragEvent<HTMLLabelElement>) => {
    event.preventDefault();
    event.stopPropagation();
    if (!dragging) setDragging(true);
  };

  const onDragLeave = (event: DragEvent<HTMLLabelElement>) => {
    event.preventDefault();
    event.stopPropagation();
    setDragging(false);
  };

  const title = canvas?.name || "Canvas";
  const subtitle = canvas
    ? "Files inside this collaborative workspace."
    : canvasesLoading
      ? "Loading canvas details..."
      : canvasesError
        ? "Unable to load canvas details."
        : "Workspace overview";

  return (
    <div className="relative isolate min-h-screen overflow-hidden bg-gradient-to-br from-slate-950 via-slate-900 to-slate-800 px-6 py-12 text-white">
      <div className="pointer-events-none absolute inset-0">
        <div className="absolute left-12 top-10 h-72 w-72 rounded-full bg-indigo-400/18 blur-3xl" />
        <div className="absolute bottom-24 right-6 h-80 w-80 rounded-full bg-sky-400/15 blur-3xl" />
      </div>

      <div className="relative mx-auto flex max-w-6xl flex-col gap-8">
        <div className="flex items-center justify-between gap-3">
          <div className="space-y-2">
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-300">Canvas</p>
            <Header size="lg">{title}</Header>
            <p className="text-sm text-slate-200/80">{subtitle}</p>
          </div>
          <div className="flex gap-3">
            <Button
              variant="ghost"
              fullWidth={false}
              onClick={() => refetchFiles()}
              className="w-auto px-3 py-2 text-sm"
              disabled={filesLoading}
            >
              Refresh files
            </Button>
            <Link href="/" className="inline-block">
              <Button variant="primary" fullWidth={false} className="w-auto px-3 py-2 text-sm">
                All canvases
              </Button>
            </Link>
          </div>
        </div>

        <Card padding="lg" className="border-white/10 bg-white/5">
          <div className="mb-6">
            <label
              onDrop={onDrop}
              onDragOver={onDragOver}
              onDragLeave={onDragLeave}
              className={`flex cursor-pointer flex-col items-center justify-center gap-3 rounded-2xl border-2 border-dashed px-6 py-7 text-center transition ${
                dragging
                  ? "border-sky-300 bg-sky-400/10 shadow-[0_0_0_3px_rgba(56,189,248,0.25)]"
                  : "border-white/15 bg-white/5 hover:border-white/30"
              }`}
            >
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-white/10 text-lg font-semibold text-white">
                ⇪
              </div>
              <div className="space-y-1">
                <p className="text-base font-semibold text-white">Drop files to upload</p>
                <p className="text-sm text-slate-200/80">
                  We’ll attach them to <span className="font-semibold">{title}</span> and keep them in sync.
                </p>
              </div>
              <div className="text-xs text-slate-300/80">
                {uploadMutation.isPending ? "Uploading…" : "Click to browse or drag files here"}
              </div>
              <input
                ref={fileInputRef}
                type="file"
                multiple
                className="hidden"
                onChange={(event) => handleFiles(event.target.files)}
              />
            </label>
            {uploadMutation.isError ? (
              <div className="mt-3 rounded-xl border border-red-400/30 bg-red-500/10 px-4 py-2 text-xs text-red-100">
                {(uploadMutation.error as Error)?.message || "Upload failed. Please try again."}
              </div>
            ) : null}
          </div>

          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.12em] text-slate-300">Files</p>
              <p className="text-xs text-slate-200/70">
                {files.length} item{files.length === 1 ? "" : "s"} in this canvas
              </p>
            </div>
            {canvas ? <Badge label="Live workspace" tone="emerald" /> : null}
          </div>

          {filesError ? (
            <div className="rounded-xl border border-red-400/30 bg-red-500/10 px-4 py-3 text-sm text-red-100">
              {(filesErrorObj as Error)?.message || "Unable to load files right now."}
            </div>
          ) : null}

          {filesLoading ? <FilesSkeleton /> : null}

          {!filesLoading && files.length === 0 && !filesError ? (
            <div className="rounded-xl border border-white/10 bg-white/5 px-4 py-6 text-center text-sm text-slate-200/80">
              No files in this canvas yet. Drag something in or create a new doc to get started.
            </div>
          ) : null}

          {!filesLoading && files.length > 0 ? (
            <div className="space-y-3">
              {files.map((file) => (
                <FileRow key={file.id} file={file} />
              ))}
            </div>
          ) : null}
        </Card>
      </div>
    </div>
  );
}
