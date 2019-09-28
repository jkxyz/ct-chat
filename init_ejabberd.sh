#!/bin/bash

set -e

docker-compose exec ejabberd bin/ejabberdctl create_room test-room chat localhost
docker-compose exec ejabberd bin/ejabberdctl set_room_affiliation test-room chat josh@localhost owner
