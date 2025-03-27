#!/bin/bash

# Exit on any error
set -e

# Set DEBIAN_FRONTEND to noninteractive to prevent interactive prompts during package installation.
  # This is necessary for automated builds in non-interactive environments like Packer.
export DEBIAN_FRONTEND=noninteractive


# Updating package lists...
sudo apt update -y

# Installing JDK 17...
sudo apt install -y openjdk-21-jdk

# Installing Maven
sudo apt install -y maven

# Create the group csye6225 if it does not already exist
sudo groupadd -f csye6225

# Create the user csye6225 with the primary group csye6225 and no login shell
sudo useradd -m -g csye6225 -s /usr/sbin/nologin csye6225

echo "JDK setup done"


echo "Installing Amazon-ClouWatch-Agent,,,...."

wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb

sudo dpkg -i amazon-cloudwatch-agent.deb

rm amazon-cloudwatch-agent.deb
