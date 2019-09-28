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

To start ejabberd, Postgres, and the media server:

```
# Make appropriate changes to environment variables in .env
$ cp .env.sample .env

$ docker-compose up --build -d
$ ./docker/init_ejabberd.sh
```

To start the Figwheel compiler:

```
$ lein figwheel
```

The server will be available at http://localhost:3449/

### Stylesheets compilation

To compile sass sources and watch for changes:

```
$ lein sass4clj auto
```
