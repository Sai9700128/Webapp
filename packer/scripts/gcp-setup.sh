#!/bin/bash

# Exit on any error
set -eux

# Set DEBIAN_FRONTEND to noninteractive to prevent interactive prompts during package installation.
  # This is necessary for automated builds in non-interactive environments like Packer.
export DEBIAN_FRONTEND=noninteractive

#!/bin/bash
set -eux  # Enables debugging and stops on the first error

# Ensure APT lists and cache are up to date
sudo rm -rf /var/lib/apt/lists/*
sudo apt-get clean
sudo apt-get update

# Reconfigure and reinstall command-not-found database
sudo dpkg --configure -a || true
sudo apt-get install --reinstall -y command-not-found || true

# Upgrade the system
sudo apt-get upgrade -y
sudo apt-get dist-upgrade -y

# Ensure package sources are set correctly
echo "deb http://archive.ubuntu.com/ubuntu $(lsb_release -cs) main universe" | sudo tee /etc/apt/sources.list.d/custom.list
sudo apt-get update

# Try installing libasound2
sudo apt-get install -y libasound2 || (sudo apt-get update && sudo apt-get install -y libasound2)

# Clean up
sudo apt-get clean




# Installing JDK 17...
sudo apt install -y openjdk-21-jdk

# Installing Maven
sudo apt install -y maven

# Create the group csye6225 if it does not already exist
sudo groupadd -f csye6225

# Create the user csye6225 with the primary group csye6225 and no login shell
sudo useradd -m -g csye6225 -s /usr/sbin/nologin csye6225

# Installing MySQL
sudo apt install -y mysql-server

# Start and enable MySQL service
sudo systemctl start mysql
sudo systemctl enable mysql




# Set root password and change authentication plugin to mysql_native_password
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'S@i112002';FLUSH PRIVILEGES;"

# Create the database if it doesn't exist

mysql -u root -p"${DB_PASSWORD}" -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME};"



# Setup environment variables
echo "export DB_URL=${DB_URL}" | sudo tee -a /etc/environment
echo "export DB_USERNAME=${DB_USRNAME}" | sudo tee -a /etc/environment
echo "export DB_PASSWORD=${DB_PASSWORD}" | sudo tee -a /etc/environment
echo "export PRT_NBR=${PRT_NBR}" | sudo tee -a /etc/environment

# Make sure the changes take effect
source /etc/environment

echo "MySQL root password has been set and environment variables have been configured."
