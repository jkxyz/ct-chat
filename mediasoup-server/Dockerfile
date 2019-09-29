FROM node:10-jessie
WORKDIR /opt/mediasoup-server
RUN npm install -g --unsafe-perm mediasoup@3.1.5
COPY package*.json ./
RUN npm install
RUN npm link mediasoup
COPY main.js ./
CMD ["node", "main.js"]
