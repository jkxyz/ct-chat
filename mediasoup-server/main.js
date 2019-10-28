const express = require('express');
const helmet = require('helmet');
const mediasoup = require('mediasoup');
const _ = require('lodash');

const workerOptions = {
    logLevel: 'debug',
    rtcMinPort: 10000,
    rtcMaxPort: 10100
};

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

const transportAnnouncedIp = process.env.MEDIASOUP_TRANSPORT_ANNOUNCED_IP;

const webRtcTransportOptions = {
    listenIps: [{ ip: '0.0.0.0', announcedIp: transportAnnouncedIp }],
    initialAvailableOutgoingBitrate: 1000000,
    minimumAvailableOutgoingBirtate: 600000,
    maxSctpMessageSize: 262144,
    maxIncomingBitrate: 1500000,
    enableUdp: true,
    enableTcp: true,
    preferUdp: true
};

const transports = new Map();

async function createTransport (router) {
    const transport = await router.createWebRtcTransport(webRtcTransportOptions);
    transports.set(transport.id, transport);
    transport.observer.on('close', () => {
        transports.delete(transport.id);
        console.log('Closed transport ID:', transport.id);
    });
    return transport;
}

async function main () {
    const worker = await mediasoup.createWorker(workerOptions);
    const router = await worker.createRouter(routerOptions);

    const app = express();

    app.use(helmet({ hsts: false }));
    app.use(express.json());

    app.use((req, res, next) => {
        // TODO: Configure for security
        res.header('Access-Control-Allow-Origin', '*');
        res.header('Access-Control-Allow-Headers', '*');
        next();
    });

    app.use((req, res, next) => {
        console.log(req.method, req.originalUrl);
        next();
    });

    app.get('/capabilities', (req, res) => {
        res.status(200).json(router.rtpCapabilities);
    });

    app.post('/transports', async (req, res) => {
        const transport = await createTransport(router);
        res
            .status(201)
            .location(`/transports/${transport.id}`)
            .json(_.pick(transport, [
                'id',
                'iceParameters',
                'iceCandidates',
                'dtlsParameters'
            ]));
    });

    app.post('/transports/:id/connect', async (req, res) => {
        const transport = transports.get(req.params.id);
        if (transport) {
            console.log('Connection parameters:', req.body);
            await transport.connect(_.pick(req.body, ['dtlsParameters']));
            res.status(200).json({});
        } else {
            res.status(404).json({ error: 'Resource not found' });
        }
    });

    app.post('/transports/:id/consumers', async (req, res) => {
        const transport = transports.get(req.params.id);
        if (transport) {
            const consumer = await transport.consume({
                producerId: req.body.producerId,
                rtpCapabilities: router.rtpCapabilities
            });
            res
                .status(201)
                .location(`/transports/${transport.id}/consumers/${consumer.id}`)
                .json(_.pick(consumer, ['id', 'kind', 'rtpParameters']));
        } else {
            res.status(404).json({ error: 'Resource not found' });
        }
    });

    app.post('/transports/:id/producers', async (req, res) => {
        const transport = transports.get(req.params.id);
        if (transport) {
            const producer = await transport.produce(req.body);
            res
                .status(201)
                .location(`/transports/${transport.id}/producers/${producer.id}`)
                .json({ id: producer.id });
        } else {
            res.status(404).json({ error: 'Resource not found' });
        }
    });

    app.listen(3500, () => {
        console.log('Listening on port 3500');
    });
}

main();
