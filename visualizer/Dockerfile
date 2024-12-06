FROM node:lts-slim

WORKDIR /usr/src/app

COPY package*.json ./

RUN npm install

COPY . .

RUN npm run build_frontend

EXPOSE 8888

CMD ["node", "server.js"]