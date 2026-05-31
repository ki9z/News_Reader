FROM node:20-alpine

WORKDIR /app

# Install only production dependencies first for better layer caching.
COPY package.json package-lock.json ./
RUN npm ci --omit=dev

# Copy backend source code.
COPY src ./src
COPY .env.example ./.env.example

ENV NODE_ENV=production
EXPOSE 8080

CMD ["npm", "start"]

