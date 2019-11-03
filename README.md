# ct.chat

## Prerequisites

* Leiningen
* Docker
* Yarn

The following ports need to be available:

* 3449 (Figwheel)
* 5222 (ejabberd)
* 5280 (ejabberd)
* 5443 (ejabberd)
* 3500 (media server)
* 10000-10100 (media server)

## Development

To compile NPM dependencies:

```
$ yarn install
$ yarn webpack
```

To setup and start ejabberd, Postgres, and the media server:

```
# Make appropriate changes to environment variables in .env
$ cp .env.sample .env

# Building the mediasoup image will take some time
$ docker-compose up --build -d

$ ./docker/init_ejabberd.sh
```

To compile sass sources:

```
$ lein sass4clj once
```

To start the Figwheel ClojureScript compiler:

```
$ lein figwheel
```

The server will be available at http://localhost:3449/

### Stylesheets compilation

To compile sass sources and watch for changes:

```
$ lein sass4clj auto
```

### MediaSoup

[MediaSoup][mediasoup] is a WebRTC Selective Forwarding Unit and media server.

It's recommended to run the MediaSoup server in a Docker container. This is
because installing the `mediasoup` package compiles a C++ extension which
requires a large number of system dependencies. The `mediasoup` package is
omitted from `mediasoup-server/package.json` and will be installed when building
the Docker image.

To install new Node packages:

```
$ docker-compose exec mediasoup npm install --save ...PACKAGES
$ docker-compose exec mediasoup cat package.json > mediasoup-server/package.json
$ docker-compose exec mediasoup cat package-lock.json > mediasoup-server/package-lock.json
```

[mediasoup]: https://mediasoup.org/
