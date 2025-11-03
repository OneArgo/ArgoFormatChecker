#!/bin/bash

export DOCKER_UID=$UID
export DOCKER_GID=$(id -g $UID)

# BODC file format checker
echo "----- File format checker for BODC 3901945 -----"
docker compose --env-file .env.demo.bodc down
docker compose --env-file .env.demo.bodc up

# Coriolis file format checker
echo "----- File format checker for COriolis 2903996 -----"
docker compose --env-file .env.demo.coriolis down
docker compose --env-file .env.demo.coriolis up