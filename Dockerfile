#########################################################
# This file contains the Docker image configuration
# which will enable us to run the download service
# inside a container.
#
# Author(s):
#      Juhapekka Piiroinen <juhapekka.piiroinen@csc.fi>
#
# (C) 2019 Copyright CSC - IT Center for Science Ltd.
# All Rights Reserved.
#########################################################

################################################
# We base our container on centos with systemd.
FROM centos/systemd
ENV SYSTEMD_IGNORE_CHROOT=1

################################################
# These are our arguments which we use later on
# ensure that you have defined these when building.
ARG ssh_key=""
ARG root_password="changeme"
ARG metax_secrets="secrets/metax.properties"

################################################
# These are optional build time variables
ARG EXTRA_PACKAGES=""

################################################
# These are the default internal variables
ARG BASE_PACKAGES="epel-release open-vm-tools openssh-server passwd rsyslog sudo cronie openssl git curl java-11 java-11-devel java-11-openjdk java-11-openjdk-devel unzip which maven"
ARG YUM_ARGS="--setopt=tsflags=nodocs -y"
ARG GIT_USER="download-service"
ARG GIT_EMAIL="noreply@csc.fi"

################################################
# Lets prepare the environment

# we will update packages
RUN yum update ${YUM_ARGS}

# then we will reinstall this package to get the proper
# localizations as we will need to have that finnish support
RUN yum -y -q reinstall glibc-common

# now that everything is ok, we will install those actual
# required packages which we need for running and compiling
# our software
RUN yum install ${YUM_ARGS} ${BASE_PACKAGES} ${EXTRA_PACKAGES}

# Show what we have prior to the changes below
RUN alternatives --list

# Select correct Java version
RUN echo $(alternatives --display java | grep 'family java-11-openjdk' | cut -d' ' -f1) | xargs -I{} alternatives --set java {}
RUN echo $(alternatives --display javac | grep 'family java-11-openjdk' | cut -d' ' -f1) | xargs -I{} alternatives --set javac {}
RUN echo $(alternatives --display jre_openjdk | grep 'family java-11-openjdk' | cut -d' ' -f1) | xargs -I{} alternatives --set jre_openjdk {}
RUN echo $(alternatives --display java_sdk_openjdk | grep 'family java-11-openjdk' | cut -d' ' -f1) | xargs -I{} alternatives --set java_sdk_openjdk {}

# Configure Java home env
ENV JAVA_HOME /etc/alternatives/jre_openjdk

# Show what we have now in use
RUN alternatives --list

################################################
# Lets clean up after installing all those files
RUN yum clean all

################################################
# Lets enable the services which we want to be there enabled
RUN systemctl enable sshd
RUN systemctl enable dbus
RUN systemctl enable rsyslog
RUN systemctl enable crond

################################################
# Configure the locale to Finnish
# as we have special characters in file names etc.
ENV LANG fi_FI.UTF-8
ENV LC_ALL fi_FI.UTF-8
RUN localedef -c -f UTF-8 -i fi_FI fi_FI.UTF-8

################################################
# Configure git client
RUN git config --global user.name ${GIT_USER}
RUN git config --global user.email ${GIT_EMAIL}

################################################
# Configure SSH server
RUN mkdir /var/run/sshd
RUN ssh-keygen -t rsa -f /etc/ssh/ssh_host_rsa_key -N ''

################################################
# Configure SSH authentication
ADD ${ssh_key} /root/.ssh/authorized_keys

################################################
# Install Fairdata download service
RUN mkdir -p /opt/login-download/config /var/log/fairdata-download/
ADD service/config.properties /opt/login-download/config.properties
ADD service/application.properties /opt/login-download/config/application.properties
ADD service/start-fairdata-download.sh /opt/login-download/start-fairdata-download.sh
ADD service/fairdata-download.service /etc/systemd/system/fairdata-download.service
RUN chmod +x /opt/login-download/start-fairdata-download.sh

################################################
# Copy source code and Build the jar
ADD pom.xml /build/pom.xml
ADD src /build/src
RUN export JAVA_HOME=/etc/alternatives/jre_openjdk && cd /build && mvn -X package

################################################
# Copy the jar file
RUN cp /build/target/logindownload-*.jar /opt/login-download/logindownload.jar
RUN chmod +x /opt/login-download/logindownload.jar

################################################
# Copy the metax.properties secrets file
RUN mkdir -p /opt/secrets/
ADD ${metax_secrets} /opt/secrets/metax.properties

################################################
# Enable the fairdata download service
RUN systemctl enable fairdata-download

################################################
# Set temporary account for SSH use
RUN echo root:${root_password} | chpasswd

################################################
# Port configurations
EXPOSE 22
EXPOSE 8433

################################################
# What to run there inside the container
CMD /usr/sbin/init
