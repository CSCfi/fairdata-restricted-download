#!/bin/bash
pushd /opt/login-download
java -Xmx6g -XX:MaxMetaspaceSize=180M -verbose -jar logindownload-0.1.5.jar --trace
popd
