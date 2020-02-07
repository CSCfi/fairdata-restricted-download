#########################################################
# This file contains the Vagrant configuration
# for the Download Service which will enable easy setup
# and the clean up processes.
#
# Author(s):
#      Juhapekka Piiroinen <juhapekka.piiroinen@csc.fi>
#
# (C) 2019 Copyright CSC - IT Center for Science Ltd.
# All Rights Reserved.
#########################################################


###################################
# Lets parse the command line arguments for vagrant up
require 'optparse'
OptionParser.new do |o|
  o.on('--root_password PASSWORD') { |root_password| $root_password = root_password }
  o.on('--ssh_key SSH_KEY') { |ssh_key| $ssh_key = ssh_key }
  o.on('--cache_dir DIRECTORY') { |cache_dir| $cache_dir = cache_dir }
  o.on('-h') { puts o; exit }
  o.parse!
end

###################################
# Lets create our machine
Vagrant.configure("2") do |config|
    config.ssh.password = "#{$root_password}"
    config.ssh.username = "root"

    config.vm.synced_folder "#{$cache_dir}", "/cache", :mount_options => ["dmode=777,fmode=777"], create: true
    config.vm.network :forwarded_port, guest: 22, host: 2222

    config.vm.provider "docker" do |d|
        d.build_dir = "."
        d.dockerfile = "Dockerfile.dev"
        d.has_ssh = true
        d.build_args = [
            '--build-arg', "ssh_key=#{$ssh_key}",
            '--build-arg', "root_password=#{$root_password}",
        ]
        d.create_args = [
            '-v', '/sys/fs/cgroup:/sys/fs/cgroup:ro',
            '--privileged=true','--cap-add=NET_ADMIN'
        ]
    end
end