# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS build
WORKDIR /app

ENV npm_config_fetch_retries=5 \
    npm_config_fetch_retry_mintimeout=20000 \
    npm_config_fetch_retry_maxtimeout=120000

COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    sh -ec 'for attempt in 1 2 3; do \
      npm ci && exit 0; \
      echo "npm ci failed on attempt ${attempt}, retrying..." >&2; \
      sleep $((attempt * 5)); \
    done; \
    exit 1'

ARG VITE_API_BASE_URL=/api
ARG VITE_WS_URL=
ARG VITE_AUTH_ENABLED=false
ARG VITE_OIDC_AUTHORITY=
ARG VITE_OIDC_CLIENT_ID=
ARG VITE_OIDC_SCOPE=openid profile email
ARG VITE_OIDC_AUDIENCE=
ARG VITE_OIDC_REDIRECT_URI=
ARG VITE_OIDC_SILENT_REDIRECT_URI=
ARG VITE_OIDC_POST_LOGOUT_REDIRECT_URI=

ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WS_URL=$VITE_WS_URL
ENV VITE_AUTH_ENABLED=$VITE_AUTH_ENABLED
ENV VITE_OIDC_AUTHORITY=$VITE_OIDC_AUTHORITY
ENV VITE_OIDC_CLIENT_ID=$VITE_OIDC_CLIENT_ID
ENV VITE_OIDC_SCOPE=$VITE_OIDC_SCOPE
ENV VITE_OIDC_AUDIENCE=$VITE_OIDC_AUDIENCE
ENV VITE_OIDC_REDIRECT_URI=$VITE_OIDC_REDIRECT_URI
ENV VITE_OIDC_SILENT_REDIRECT_URI=$VITE_OIDC_SILENT_REDIRECT_URI
ENV VITE_OIDC_POST_LOGOUT_REDIRECT_URI=$VITE_OIDC_POST_LOGOUT_REDIRECT_URI

COPY frontend/ ./
RUN npm run build

FROM nginx:1.27-alpine

COPY infra/docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
