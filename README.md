# ct.chat

## Prerequisites

* Leiningen
* Docker
* Yarn

## Development

To compile NPM dependencies:

```
$ yarn install
$ yarn webpack
```

To start ejabberd, Postgres, and the media server:

```
$ docker-compose up --build -d
$ ./docker/init_ejabberd.sh

# Make appropriate changes to environment variables in .env
$ cp .env.sample .env
```

To start the Figwheel compiler:

```
$ lein figwheel
```

The server will be available at [http://localhost:3449/](http://localhost:3449/).

### Stylesheets compilation

To compile sass sources and watch for changes:

```
$ lein sass4clj auto
```
