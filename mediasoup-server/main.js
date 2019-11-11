const _ = require('lodash');
const uuid = require('uuid/v4');
const winston = require('winston');
const http = require('http');
const protoo = require('protoo-server');
const mediasoup = require('mediasoup');

const {
  logLevel,
  webSocketServerOptions,
  workerOptions,
  routerOptions,
  webRtcTransportOptions
} = require('./options');

const logger = winston.createLogger({
  level: logLevel,
  transports: [new winston.transports.Console()],
  format: winston.format.simple()
});

const requestHandlers = {
  capabilities ({ router }, request, accept, reject) {
    accept(router.rtpCapabilities);
    logger.log('debug', 'Response', router.rtpCapabilities);
  },

  async createTransport ({ router, peer }, request, accept, reject) {
    const transportType = request.data.type;

    if (transportType !== 'send' && transportType !== 'receive') {
      reject(400, 'Not a valid transport type');
      return;
    }

    if (peer.data.transports[transportType]) {
      reject(409, 'Transport type already exists');
      return;
    }

    const transport = await router.createWebRtcTransport(webRtcTransportOptions);

    peer.data.transports[transportType] = transport;

    const response = _.pick(transport, [
      'id',
      'iceParameters',
      'iceCandidates',
      'dtlsParameters'
    ]);

    accept(response);

    logger.log('debug', 'Response', response);
  },

  async connectTransport ({ peer }, request, accept, reject) {
    const transport = peer.data.transports[request.data.type];

    if (!transport) {
      reject(400, 'Transport type not created');
      return;
    }

    await transport.connect(_.pick(request.data.parameters, ['dtlsParameters']));

    logger.log('debug', 'Connected transport');

    accept();
  },

  async createProducer ({ peer }, request, accept, reject) {
    const transport = peer.data.transports.send;

    if (!transport) {
      reject(400, 'Send transport not created');
      return;
    }

    const producer = await transport.produce(_.pick(request.data.parameters, [
      'kind',
      'rtpParameters'
    ]));

    logger.log('debug', 'Created producer');

    const response = { id: producer.id };

    accept(response);

    logger.log('debug', 'Response', response);
  },

  async createConsumer ({ router, peer }, request, accept, reject) {
    const transport = peer.data.transports.receive;

    if (!transport) {
      reject(400, 'Receive transport not created');
      return;
    }

    const consumer = await transport.consume({
      producerId: request.data.producerId,
      rtpCapabilities: router.rtpCapabilities
    });

    logger.log('debug', 'Created consumer');

    const response = _.pick(consumer, [
      'id',
      'producerId',
      'kind',
      'rtpParameters'
    ]);

    accept(response);

    logger.log('debug', 'Response', response);
  }
};

function handlePeerRequest (env, request, accept, reject) {
  if (request.method in requestHandlers) {
    requestHandlers[request.method](env, request, accept, reject);
  } else {
    reject(400, 'Not a valid request type');
  }
}

async function handleConnectionRequest (
  { router, room },
  info,
  accept,
  reject
) {
  const transport = accept();

  logger.log('debug', 'Accepted connection request');

  const peer = await room.createPeer(uuid(), transport);

  peer.data.transports = {};

  peer.on('request', (request, accept, reject) => {
    logger.log('debug', 'Request received', { request });
    handlePeerRequest({ router, room, peer }, request, accept, reject);
  });

  peer.on('close', () => {
    logger.log('debug', 'Peer closed');
  });
}

async function main () {
  const worker = await mediasoup.createWorker(workerOptions);
  const router = await worker.createRouter(routerOptions);

  const httpServer = http.createServer();
  const protooServer = new protoo.WebSocketServer(httpServer, webSocketServerOptions);

  const room = new protoo.Room();

  protooServer.on('connectionrequest', (info, accept, reject) => {
    logger.log('debug', 'Connection request');
    handleConnectionRequest({ router, room }, info, accept, reject);
  });

  httpServer.listen(3500, () => {
    logger.log('info', 'Listening on port 3500');
    logger.log('debug', 'webRtcTransportOptions', { webRtcTransportOptions });
  });
}

main();
