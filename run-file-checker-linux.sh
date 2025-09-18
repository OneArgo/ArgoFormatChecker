#!/bin/bash

# TODO : add when container can run as non root
# export DOCKER_UID=$UID
# export DOCKER_GID=$(id -g $UID)

docker compose --env-file .env.demo.bodc down
docker compose --env-file .env.demo.bodc up