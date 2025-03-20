packer {
  required_plugins {
    amazon = {
      version = ">= 1.0.0, <2.0.0"
      source  = "github.com/hashicorp/amazon"
    }
  }
}

# # DB Configuration

# variable "DB_URL" {
#   type = string
# }

# variable "DB_USERNAME" {
#   type = string
# }

# variable "DB_PASSWORD" {
#   type = string
# }

# variable "DB_NAME" {
#   type = string
# }

# variable "PRT_NBR" {
#   type = string
# }

variable "SSH_USERNAME" {
  type    = string
  default = "ubuntu"
}

# aws configurations

# Default subnet id
variable "SUBNET_ID" {
  type = string
  # default = "subnet-0b41b887118645e4b"
}

variable "INSTANCE_TYPE" {
  type    = string
  default = "t2.micro"
}

variable "REGION" {
  type    = string
  default = "us-east-1"
}

variable "AMI_NAME" {
  type    = string
  default = "csye6225_health_checker"
}

variable "AMI_DESCRIPTION" {
  type    = string
  default = "AMI for CSYE6225 Assignment 4"
}

variable "OWNER_ID" {
  type = string
}

variable "AMI_USER" {
  type = string
}


# Amazon AMI Source Configuration
source "amazon-ebs" "AWS_AMI" {
  profile         = "dev"
  ami_name        = "${var.AMI_NAME}_${formatdate("YYYY_MM_DD_HH_MM_ss", timestamp())}"
  ami_description = var.AMI_DESCRIPTION
  instance_type   = var.INSTANCE_TYPE
  region          = var.REGION
  subnet_id       = var.SUBNET_ID


  # Base Image Selection
  source_ami_filter {
    filters = {
      name                = "ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-*"
      root-device-type    = "ebs"
      virtualization-type = "hvm"
    }
    most_recent = true
    owners      = [var.OWNER_ID] # Canonical’s official AWS account
  }

  ssh_username = var.SSH_USERNAME

  # Storage Configuration
  launch_block_device_mappings {
    delete_on_termination = true        # Deletes the volume when the instance is terminated.
    device_name           = "/dev/sda1" # Specifies the root volume device name.
    # volume_size           = var.VOLUME_SIZE # Specifies the volume size in GiB.
    # volume_type           = var.VOLUME_TYPE # Specifies the volume type (e.g., "gp2" for General Purpose SSD).
  }

  ami_users = [var.AMI_USER]
}


# Build Configuration
# Step 1
build {
  sources = ["source.amazon-ebs.AWS_AMI"]


  # Step 2
  # Pre-Setup Script
  provisioner "shell" {
    # environment_vars = [
    #   "DB_URL=${var.DB_URL}",
    #   "DB_USERNAME=${var.DB_USERNAME}",
    #   "DB_PASSWORD=${var.DB_PASSWORD}",
    #   "DB_NAME=${var.DB_NAME}",
    #   "PRT_NBR=${var.PRT_NBR}"
    # ]
    script = "scripts/pre-setup.sh"
  }

  provisioner "file" {
    source      = "../HealthCheck/target/HealthCheck-0.0.1-SNAPSHOT.jar"
    destination = "/tmp/"
  }

  provisioner "file" {
    source      = "csye6225.service"
    destination = "/tmp/"
  }

  provisioner "shell" {
    script = "scripts/systemd-setup.sh"
  }

  # Step 3

  post-processor "manifest" {
    output = "manifest.json"
  }


}
