const http = require('http');
const express = require('express');
const protoo = require('protoo-server');
const mediasoup = require('mediasoup');
const uuid = require('uuid/v4');

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

async function main () {

    const worker = await mediasoup.createWorker({
        logLevel: 'debug',
        rtcMinPort: 10000,
        rtcMaxPort: 10100
    });

    const router = await worker.createRouter({
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
    });

    const app = express();

    const httpServer = http.createServer(app);

    const protooServer = new protoo.WebSocketServer(httpServer);

    const room = new protoo.Room();

    protooServer.on('connectionrequest', async (info, accept, reject) => {
        // TODO: Verify user details from info.request -- use ejabberd API
        const transport = accept();

        const peer = await room.createPeer(uuid(), transport);

        // The transport used by the client to receive media
        let receiveTransport;

        // The transport used by the client to send media
        let sendTransport;

        let videoProducer;

        peer.on('request', async (request, accept, reject) => {

            switch (request.method) {

            case 'capabilities':
                accept(router.rtpCapabilities);
                return;

            case 'create-receive-transport':
                receiveTransport = await router.createWebRtcTransport(
                    webRtcTransportOptions
                );
                accept({
                    receiveTransport: {
                        id: receiveTransport.id,
                        iceParameters: receiveTransport.iceParameters,
                        iceCandidates: receiveTransport.iceCandidates,
                        dtlsParameters: receiveTransport.dtlsParameters
                    }
                });
                return;

            case 'connect-receive-transport':
                if (receiveTransport) {
                    await receiveTransport.connect(request.data);
                    accept();
                } else {
                    reject();
                }
                return;

            case 'create-send-transport':
                // TODO: Authorize the user using ejabberd API
                sendTransport = await router.createWebRtcTransport(
                    webRtcTransportOptions
                );
                accept({
                    sendTransport: {
                        id: sendTransport.id,
                        iceParameters: sendTransport.iceParameters,
                        iceCandidates: sendTransport.iceCandidates,
                        dtlsParameters: sendTransport.dtlsParameters
                    }
                });
                return;

            case 'connect-send-transport':
                if (sendTransport) {
                    await sendTransport.connect(request.data);
                    accept();
                } else {
                    reject();
                }
                return;

            case 'create-producer':
                if (sendTransport) {
                    videoProducer = await sendTransport.produce(request.data);
                    accept({ videoProducer: { id: videoProducer.id }});
                } else {
                    reject();
                }
                return;

            case 'create-consumer':
                if (receiveTransport) {
                    const videoConsumer = await receiveTransport.consume({
                        producerId: request.data.producerId,
                        rtpCapabilities: router.rtpCapabilities
                    });
                    accept({
                        id: videoConsumer.id,
                        producerId: request.data.producerId,
                        kind: videoConsumer.kind,
                        rtpParameters: videoConsumer.rtpParameters
                    });
                } else {
                    reject();
                }
                return;

            default:
                reject();

            }

        });

        await peer.notify('ready');

    });

    const port = 3500;

    httpServer.listen(port, () => {
        console.log(`Listening on port ${port}`);
        console.log(`MEDIASOUP_TRANSPORT_ANNOUNCED_IP: ${transportAnnouncedIp}`);
    });

}

main();
