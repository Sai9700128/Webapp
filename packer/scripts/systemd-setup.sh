#!/bin/bash

# Exit on any error
set -e

# Create directory for the application if it doesn't already exist
sudo mkdir -p /opt/csye6225

# Move the JAR file to /opt/csye6225 and set ownership and permissions
sudo mv /tmp/HealthCheck-0.0.1-SNAPSHOT.jar /opt/csye6225/
sudo chown -R csye6225:csye6225 /opt/csye6225/
sudo chmod -R 755 /opt/csye6225/

# Move the service file to /etc/systemd/system/
sudo mv /tmp/csye6225.service /etc/systemd/system/

# Set the ownership of the service file to csye6225
sudo chown csye6225: /etc/systemd/system/csye6225.service

# Reload systemd to recognize the new service
sudo systemctl daemon-reload

# Enable the service to start on boot
sudo systemctl enable csye6225.service


# Create log directory
sudo mkdir -p /var/log/csye6225
sudo chown -R csye6225:csye6225 /var/log/csye6225


echo "Checking if cloudWatch_agent.json exists at /tmp"
ls -l /tmp/cloudWatch_agent.json || echo "File NOT found"

# Move CloudWatch config
sudo mkdir -p /opt/aws/amazon-cloudwatch-agent/etc/
sudo mv /tmp/cloudWatch_agent.json /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json

echo "Verifying it was moved to /opt/aws/amazon-cloudwatch-agent/etc/"
ls -l /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json || echo "Move failed"

# Start and enable the agent
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config -m ec2 -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json -s

sudo systemctl enable amazon-cloudwatch-agent