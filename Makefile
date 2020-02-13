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
DOWNLOAD_VERSION:=$(shell python -c "import xml.etree.ElementTree as ET;print ET.parse('pom.xml').getroot().find('{http://maven.apache.org/POM/4.0.0}version').text")

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
	make secrets/.htpasswd
	make secrets/ssl.key
	make secrets/ssl.crt
	make secrets/metax.properties
	make secrets/application.properties
	make secrets/config.properties

secrets/metax.properties:
	@test -f secrets/metax.properties || (cp service/metax.properties.template secrets/metax.properties && nano secrets/metax.properties)

secrets/application.properties:
	@test -f secrets/application.properties || (cp service/application.properties secrets/application.properties && nano secrets/application.properties)

secrets/config.properties:
	@test -f secrets/config.properties || (cp service/config.properties secrets/config.properties && nano secrets/config.properties)

build: secrets
	@echo
	@echo "Building logindownload.jar v$(DOWNLOAD_VERSION)"
	@echo
	@rm -f logindownload.jar
	-@docker rmi -f fairdownload:$(DOWNLOAD_VERSION)-dev
	@docker image build -t fairdownload:$(DOWNLOAD_VERSION)-dev . -f Dockerfile
	@docker container run --publish 8433:8433 --detach --name fairdata-download fairdownload:$(DOWNLOAD_VERSION)-dev
	@docker cp fairdata-download:/opt/login-download/logindownload.jar logindownload.jar
	@docker stop fairdata-download
	@docker rm fairdata-download
	@docker rmi fairdownload:$(DOWNLOAD_VERSION)-dev
	@echo
	@echo "logindownload.jar v$(DOWNLOAD_VERSION) is now built."
	@echo

docker-prod: all
	@echo
	@echo "Creating production docker image (fairdownload:$(DOWNLOAD_VERSION)).."
	@echo
	-@docker rmi -f fairdownload:$(DOWNLOAD_VERSION)
	@docker image build -t fairdownload:$(DOWNLOAD_VERSION) -f Dockerfile.prod .
	@echo
	@echo "Production docker image built."
	@echo

docker-prod-run: secrets
	@docker container run --mount type=bind,source="$(PWD)"/secrets/metax.properties,target=/opt/secrets/metax.properties --mount type=bind,source="$(PWD)"/service/application.properties,target=/opt/login-download/config/application.properties --mount type=bind,source="$(PWD)"/service/config.properties,target=/opt/login-download/config.properties --publish 8433:8433 --detach --name fairdata-download fairdownload:$(DOWNLOAD_VERSION)

secrets/.htpasswd:
	@htpasswd -db .htpasswd user password

secrets/ssl.crt: certs
secrets/ssl.key: certs

certs:
	@test -f secrets/ssl.crt || openssl req -x509 -days 365 -nodes -subj '/C=FI/ST=Uusimaa/L=Espoo/O=CSC - Tieteen tietotekniikan keskus Oy/CN=fairdata-download.csc.local' -addext "subjectAltName = DNS:fairdata-download.csc.local" -addext "extendedKeyUsage = serverAuth" -newkey rsa:2048 -out secrets/ssl.crt -keyout secrets/ssl.key

swarm-deploy: secrets/ssl.crt secrets/ssl.key secrets/metax.properties secrets/application.properties secrets/config.properties
	@docker stack deploy --compose-file docker-compose.yml fairdownload

swarm-rm:
	@docker stack rm fairdownload

swarm-init:
	@docker swarm init

swarm-leave:
	@docker swarm leave --force

swarm-ps:
	@docker stack services fairdownload

clean:
	@rm -f logindownload.jar
	@rm -rf target
	@rm -rf .vagrant
	-@vagrant destroy 2> /dev/null > /dev/null
