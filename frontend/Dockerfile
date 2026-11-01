# ---------- Build ----------
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# ---------- Serve ----------
FROM nginx:1.27-alpine
# Templates in this folder are filled from environment variables at startup (envsubst)
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

# Where nginx forwards /api requests. In docker compose the API service is called "app".
ENV API_URL=http://app:8080
EXPOSE 80
