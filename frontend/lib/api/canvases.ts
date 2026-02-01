export type Canvas = {
  id: string;
  name: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  participantCount?: number;
};

export type CanvasFile = {
  id: string;
  name?: string;
  fileName?: string;
  size?: number;
  updatedAt?: string;
  kind?: "document" | "model" | "image" | "data" | "other";
  owner?: string;
};

type CreateFileDescriptor = {
  fileName: string;
  contentType: string;
  attributes: Record<string, string>;
};

const API_BASE = "/encube-assignment-api/v1";

const parseJsonSafely = async (response: Response) => {
  const text = await response.text();
  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
};

export const fetchCanvases = async (): Promise<Canvas[]> => {
  const response = await fetch(`${API_BASE}/canvases`, {
    credentials: "include",
  });

  if (!response.ok) {
    const message = await response.text().catch(() => "");
    throw new Error(message || "Failed to load canvases.");
  }

  const data = await parseJsonSafely(response);
  if (!data) return [];

  return Array.isArray(data) ? data : [];
};

export const createCanvas = async (name: string): Promise<Canvas | null> => {
  const response = await fetch(`${API_BASE}/canvases`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ name }),
    credentials: "include",
  });

  if (!response.ok) {
    const message = await response.text().catch(() => "");
    throw new Error(message || "Unable to create canvas.");
  }

  const data = await parseJsonSafely(response);
  return (data as Canvas | null) ?? null;
};

export const fetchFiles = async (canvasId: string): Promise<CanvasFile[]> => {
  const response = await fetch(`${API_BASE}/files?canvasId=${encodeURIComponent(canvasId)}`, {
    credentials: "include",
  });

  if (!response.ok) {
    const message = await response.text().catch(() => "");
    throw new Error(message || "Failed to load files for this canvas.");
  }

  const data = await parseJsonSafely(response);
  if (!data) return [];
  return Array.isArray(data) ? data : [];
};

export const uploadFile = async (canvasId: string, file: File): Promise<CanvasFile | null> => {
  const descriptor: CreateFileDescriptor = {
    fileName: file.name,
    contentType: file.type || "application/octet-stream",
    attributes: {
      canvasId,
    },
  };

  const form = new FormData();
  form.append("descriptor", new Blob([JSON.stringify(descriptor)], { type: "application/json" }));
  form.append("file", file, file.name);

  const response = await fetch(`${API_BASE}/files`, {
    method: "POST",
    credentials: "include",
    body: form,
  });

  if (!response.ok) {
    const message = await response.text().catch(() => "");
    throw new Error(message || "File upload failed.");
  }

  const data = await parseJsonSafely(response);
  return (data as CanvasFile | null) ?? null;
};
