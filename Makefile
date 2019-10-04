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
NEW_PASSWORD:=$(shell cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)

all:
	export JAVA_HOME=`/usr/libexec/java_home -v 11` && mvn package

up:
	@test -f secrets/metax.properties || (cp service/metax.properties.template secrets/metax.properties && nano secrets/metax.properties)
	@test -f id_rsa.pub || cp ~/.ssh/id_rsa.pub .
	@vagrant --ssh_key=id_rsa.pub --root_password=$(NEW_PASSWORD) --cache_dir=/tmp/fairdata-download-cache up
	@echo
	@echo "You can login with root:$(NEW_PASSWORD) with:"
	@echo " ssh root@localhost -p2222"
	@echo
	@echo "There should be also public key authentication setup using your public key in ~/.ssh/id_rsa.pub"
	@echo

clean:
	vagrant destroy
