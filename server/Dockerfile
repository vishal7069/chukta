FROM node:24-alpine
WORKDIR /app
COPY package.json ledger.mjs server.mjs ./
ENV NODE_ENV=production PORT=8787 DB_PATH=/data/chukta.sqlite DEV_AUTH=false
RUN mkdir /data && chown node:node /data
USER node
EXPOSE 8787
CMD ["node", "server.mjs"]
