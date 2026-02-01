FROM node:20-alpine AS deps
WORKDIR /app

# Install dependencies based on the lockfile for reproducible builds
COPY package.json package-lock.json ./
RUN npm ci

FROM node:20-alpine AS builder
WORKDIR /app

# Copy source and previously installed node_modules
COPY . .
COPY --from=deps /app/node_modules ./node_modules

# Disable telemetry and build the Next.js app
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

FROM node:20-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000
ENV NEXT_TELEMETRY_DISABLED=1

# Copy built assets and public files
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/public ./public

# Copy manifest files and install only production dependencies
COPY package.json package-lock.json ./
COPY --from=deps /app/node_modules ./node_modules
RUN npm prune --omit=dev

EXPOSE 3000

CMD ["npm", "start"]
