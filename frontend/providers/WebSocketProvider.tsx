'use client';

import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

type MessageHandler = (data: unknown, event: MessageEvent) => void;

type Subscription = {
  dispose: () => void;
};

type WebSocketStatus = "idle" | "connecting" | "open" | "closed" | "error";

type WebSocketContextValue = {
  onMessage: (handler: MessageHandler) => Subscription;
  sendMessage: (payload: unknown) => boolean;
  status: WebSocketStatus;
};

const WebSocketContext = createContext<WebSocketContextValue | null>(null);

const SESSION_COOKIES = ["encube_auth", "SESSION", "JSESSIONID"];

const hasSessionCookie = () => {
  if (typeof document === "undefined") return false;
  const cookieString = document.cookie;
  if (!cookieString) return false;
  return SESSION_COOKIES.some((name) => cookieString.includes(`${name}=`));
};

const serializePayload = (payload: unknown) => {
  if (typeof payload === "string") return payload;
  try {
    return JSON.stringify(payload);
  } catch {
    return String(payload);
  }
};

const parseIncoming = (data: unknown) => {
  if (typeof data === "string") {
    try {
      return JSON.parse(data);
    } catch {
      return data;
    }
  }
  return data;
};

export function WebSocketProvider({ children }: { children: React.ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(hasSessionCookie());
  const [status, setStatus] = useState<WebSocketStatus>("idle");

  const socketRef = useRef<WebSocket | null>(null);
  const listenersRef = useRef(new Set<MessageHandler>());
  const queuedMessagesRef = useRef<unknown[]>([]);
  const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const shouldReconnectRef = useRef(false);
  const retryCountRef = useRef(0);
  const isAuthenticatedRef = useRef(isAuthenticated);
  const connectRef = useRef<() => void>(() => {});

  const clearReconnectTimer = () => {
    if (reconnectTimerRef.current) {
      clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = null;
    }
  };

  useEffect(() => {
    // Poll the readable session marker; backend cookies may be HttpOnly.
    const interval = setInterval(() => {
      const next = hasSessionCookie();
      setIsAuthenticated((prev) => (prev === next ? prev : next));
    }, 5000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    isAuthenticatedRef.current = isAuthenticated;
  }, [isAuthenticated]);

  const flushQueue = useCallback((socket: WebSocket) => {
    if (!queuedMessagesRef.current.length) return;
    const pending = [...queuedMessagesRef.current];
    queuedMessagesRef.current = [];
    pending.forEach((payload) => {
      const message = serializePayload(payload);
      socket.send(message);
    });
  }, []);

  const connect = useCallback(() => {
    if (!isAuthenticatedRef.current) return;

    const existing = socketRef.current;
    if (
      existing &&
      (existing.readyState === WebSocket.OPEN || existing.readyState === WebSocket.CONNECTING)
    ) {
      return;
    }

    clearReconnectTimer();
    shouldReconnectRef.current = true;

    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    const ws = new WebSocket(`${protocol}://${window.location.host}/encube-assignment-api/v1/ws`);
    socketRef.current = ws;

    ws.onopen = () => {
      retryCountRef.current = 0;
      setStatus("open");
      flushQueue(ws);
    };

    ws.onmessage = (event: MessageEvent) => {
      const parsed = parseIncoming(event.data);
      listenersRef.current.forEach((listener) => listener(parsed, event));
    };

    ws.onerror = () => {
      setStatus("error");
    };

    ws.onclose = () => {
      socketRef.current = null;
      setStatus("closed");
      const scheduleReconnect = () => {
        if (!isAuthenticatedRef.current || reconnectTimerRef.current) return;

        const delay = Math.min(30000, 1000 * 2 ** retryCountRef.current);
        reconnectTimerRef.current = setTimeout(() => {
          reconnectTimerRef.current = null;
          retryCountRef.current += 1;
          connectRef.current();
        }, delay);
      };

      if (shouldReconnectRef.current && isAuthenticatedRef.current) {
        scheduleReconnect();
      }
    };
  }, [flushQueue]);

  useEffect(() => {
    connectRef.current = connect;
  }, [connect]);

  const teardownSocket = useCallback(() => {
    shouldReconnectRef.current = false;
    clearReconnectTimer();
    const socket = socketRef.current;
    if (socket) {
      socket.close();
      socketRef.current = null;
    }
    queuedMessagesRef.current = [];
    retryCountRef.current = 0;
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      connect();
    } else {
      teardownSocket();
    }

    return () => {
      teardownSocket();
    };
  }, [connect, isAuthenticated, teardownSocket]);

  const sendMessage = useCallback(
    (payload: unknown) => {
      if (!isAuthenticatedRef.current) {
        console.warn("Attempted to send a message without an active session.");
        return false;
      }

      const socket = socketRef.current;

      if (!socket || socket.readyState === WebSocket.CLOSING || socket.readyState === WebSocket.CLOSED) {
        queuedMessagesRef.current.push(payload);
        connect();
        return false;
      }

      if (socket.readyState === WebSocket.CONNECTING) {
        queuedMessagesRef.current.push(payload);
        return true;
      }

      const message = serializePayload(payload);
      socket.send(message);
      return true;
    },
    [connect],
  );

  const onMessage = useCallback((handler: MessageHandler) => {
    listenersRef.current.add(handler);
    return {
      dispose: () => listenersRef.current.delete(handler),
    };
  }, []);

  const value = useMemo(
    () => ({
      onMessage,
      sendMessage,
      status,
    }),
    [onMessage, sendMessage, status],
  );

  return <WebSocketContext.Provider value={value}>{children}</WebSocketContext.Provider>;
}

export const useWebSocket = () => {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error("useWebSocket must be used within a WebSocketProvider");
  }
  return context;
};

export default WebSocketProvider;
