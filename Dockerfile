#########################################################
# Author(s):
#      Juhapekka Piiroinen <juhapekka.piiroinen@csc.fi>
#
# License: GPLv3
#
# (C) 2020 Copyright CSC - IT Center for Science Ltd.
# All Rights Reserved.
#########################################################
FROM maven:3-jdk-11
COPY src /code/src
COPY pom.xml /code/pom.xml
WORKDIR /code
RUN mvn package
RUN mkdir -p /opt/login-download/
RUN cp target/logindownload*.jar /opt/login-download/logindownload.jar
WORKDIR /
COPY secrets /code/secrets
RUN mkdir -p /opt/secrets/
RUN cp /code/secrets/metax.properties /opt/secrets/metax.properties
RUN cp /code/secrets/config.properties /opt/login-download/config.properties
RUN cp /code/secrets/application.properties /opt/login-download/application.properties
# RUN mkdir /code/logs
# VOLUME ./logs /code/logs/

EXPOSE 8433
CMD cd /opt/login-download/ && java -Xmx6g -XX:MaxMetaspaceSize=180M -XX:MaxGCPauseMillis=5000 -jar logindownload.jar