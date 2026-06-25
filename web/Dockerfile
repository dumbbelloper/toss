# gotgan web — React 19 / Vite. 빌드 컨텍스트 = web/
# 멀티스테이지: pnpm build → nginx 정적 서빙(SPA fallback)
FROM node:22-alpine AS build
WORKDIR /src
RUN npm install -g pnpm@9
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

FROM nginx:1.27-alpine
COPY --from=build /src/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
