#!/bin/bash
pushd /opt/login-download
java -Xmx6g -XX:MaxMetaspaceSize=180M -XX:MaxGCPauseMillis=5000 -jar logindownload.jar
popd