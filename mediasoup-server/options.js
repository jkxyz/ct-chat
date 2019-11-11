const assert = require('assert');

const logLevel = process.env.LOG_LEVEL || 'info';

module.exports.logLevel = logLevel;

const workerOptions = {
  logLevel: logLevel,
  // TODO: Specify a wider port range in production
  rtcMinPort: 10000,
  rtcMaxPort: 10100
};

module.exports.workerOptions = workerOptions;

const routerOptions = {
  mediaCodecs: [
    {
      kind: 'audio',
      mimeType: 'audio/opus',
      clockRate: 48000,
      channels: 2
    },
    {
      kind: 'video',
      mimeType: 'video/VP8',
      clockRate: 90000,
      parameters: {
        'x-google-start-bitrate': 1000
      }
    },
    {
      kind: 'video',
      mimeType: 'video/VP9',
      clockRate: 90000,
      parameters: {
        'profile-id': 2,
        'x-google-start-bitrate': 1000
      }
    },
    {
      kind: 'video',
      mimeType: 'video/h264',
      clockRate: 90000,
      parameters: {
        'packetization-mode': 1,
        'profile-level-id': '42e01f',
        'level-asymmetry-allowed': 1,
        'x-google-start-bitrate': 1000
      }
    }
  ]
};

module.exports.routerOptions = routerOptions;

const transportAnnouncedIp = process.env.MEDIASOUP_TRANSPORT_ANNOUNCED_IP;

assert(
  transportAnnouncedIp,
  'MEDIASOUP_TRANSPORT_ANNOUNCED_IP environment variable is not set'
);

const webRtcTransportOptions = {
  // TODO: Listen on correct IP address in production
  listenIps: [{ ip: '0.0.0.0', announcedIp: transportAnnouncedIp }],
  initialAvailableOutgoingBitrate: 1000000,
  minimumAvailableOutgoingBirtate: 600000,
  maxSctpMessageSize: 262144,
  maxIncomingBitrate: 1500000,
  enableUdp: true,
  enableTcp: true,
  preferUdp: true
};

module.exports.webRtcTransportOptions = webRtcTransportOptions;

const webSocketServerOptions = {
  maxReceivedFrameSize: 960000,
  maxReceivedMessageSize: 960000,
  fragmentOutgoingMessages: true,
  fragmentationThreshold: 960000
};

module.exports.webSocketServerOptions = webSocketServerOptions;
