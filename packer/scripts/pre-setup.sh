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

# Installing MySQL
sudo apt install -y mysql-server

# Start and enable MySQL service
sudo systemctl start mysql
sudo systemctl enable mysql




# Set root password and change authentication plugin to mysql_native_password
sudo mysql -e "ALTER USER '${DB_USERNAME}'@'localhost' IDENTIFIED WITH mysql_native_password BY '${DB_PASSWORD}';FLUSH PRIVILEGES;"

# Create the database if it doesn't exist

mysql -u root -p"${DB_PASSWORD}" -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME};"



# Setup environment variables
echo "DB_URL=${DB_URL}" | sudo tee -a /etc/environment
echo "DB_USRNAME=${DB_USERNAME}" | sudo tee -a /etc/environment
echo "DB_PASSWORD=${DB_PASSWORD}" | sudo tee -a /etc/environment

# Make sure the changes take effect
source /etc/environment

echo "MySQL root password has been set and environment variables have been configured."
