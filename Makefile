#########################################################
# This file contains make targets which will help
# how to use things.
#
# Author(s):
#      Juhapekka Piiroinen <juhapekka.piiroinen@csc.fi>
#
# (C) 2019 Copyright CSC - IT Center for Science Ltd.
# All Rights Reserved.
#########################################################

SHELL:=/bin/bash
NEW_PASSWORD:=$(shell openssl rand -base64 32)

all: logindownload.jar

logindownload.jar: build

up: secrets
	@test -f id_rsa.pub || cp ~/.ssh/id_rsa.pub .
	@vagrant --ssh_key=id_rsa.pub --root_password=$(NEW_PASSWORD) --cache_dir=/tmp/fairdata-download-cache up
	@echo
	@echo "You can login with root:$(NEW_PASSWORD) with:"
	@echo " ssh root@localhost -p2222"
	@echo
	@echo "There should be also public key authentication setup using your public key in ~/.ssh/id_rsa.pub"
	@echo

secrets:
	@test -d secrets || mkdir secrets
	@test -f secrets/metax.properties || (cp service/metax.properties.template secrets/metax.properties && nano secrets/metax.properties)

build: secrets
	@echo
	@echo "Building logindownload.jar"
	@echo
	@rm -f logindownload.jar
	@docker image build -t fairdownload:1.0-dev . -f Dockerfile
	@docker container run --publish 8433:8433 --detach --name fairdata-download fairdownload:1.0-dev
	@docker cp fairdata-download:/opt/login-download/logindownload.jar logindownload.jar
	@docker stop fairdata-download
	@docker rm fairdata-download
	@docker rmi fairdownload:1.0-dev
	@echo
	@echo "logindownload.jar is now built."
	@echo

docker-prod: all
	@echo
	@echo "Creating production docker image.."
	@echo
	@docker rmi fairdownload:1.0
	@docker image build -t fairdownload:1.0 -f Dockerfile.prod .
	@echo
	@echo "Production docker image built."
	@echo

docker-prod-run: secrets
	@docker container run --mount type=bind,source="$(PWD)"/secrets/metax.properties,target=/opt/secrets/metax.properties --mount type=bind,source="$(PWD)"/service/application.properties,target=/opt/login-download/config/application.properties --mount type=bind,source="$(PWD)"/service/config.properties,target=/opt/login-download/config.properties --publish 8433:8433 --detach --name fairdata-download fairdownload:1.0

clean:
	@rm logindownload.jar
	@vagrant destroy
	@rm -rf target
	@rm -rf .vagrant
