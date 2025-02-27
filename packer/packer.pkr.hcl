packer {
  required_plugins {
    amazon = {
      version = ">= 1.0.0, <2.0.0"
      source  = "github.com/hashicorp/amazon"
    }
    google = {
      version = ">= 1.0.0, <2.0.0"
      source  = "github.com/hashicorp/googlecompute"
    }

  }
}

# General Configuration

variable "DB_URL" {
  type = string
}

variable "DB_USRNAME" {
  type = string
}

variable "DB_PASSWORD" {
  type = string
}

variable "DB_NAME" {
  type = string
}

variable "PRT_NBR" {
  type = string
}

variable "ssh_username" {
  type    = string
  default = "ubuntu"
}

# aws configurations

# Default subnet id
variable "subnet_id" {
  type = string
  # default = "subnet-060513e6e25a58f21"
}

variable "instance_type" {
  type    = string
  default = "t2.micro"
}

variable "region" {
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

# gcp configure

variable "GCP_ZONE" {
  type    = string
  default = "us-central1-a"
}

variable "gcp_project_id" {
  type = string
  # default = "dev-452121"
}


variable "OWNER_ID" {
  type    = string
  default = "099720109477"
}


variable "gcp_IMAGE_FAM_NAME" {
  type    = string
  default = "csye6225-health-checker"
}

variable "gcp_machine_type" {
  type    = string
  default = "n1-standard-1"
}


# Amazon AMI Source Configuration
source "amazon-ebs" "my-ami" {
  profile         = "dev"
  ami_name        = "${var.AMI_NAME}_${formatdate("YYYY_MM_DD_HH_MM_ss", timestamp())}"
  ami_description = var.AMI_DESCRIPTION
  instance_type   = var.instance_type
  region          = var.region
  subnet_id       = var.subnet_id


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

  ssh_username = var.ssh_username

  # Storage Configuration
  launch_block_device_mappings {
    delete_on_termination = true        # Deletes the volume when the instance is terminated.
    device_name           = "/dev/sda1" # Specifies the root volume device name.
    # volume_size           = var.VOLUME_SIZE # Specifies the volume size in GiB.
    # volume_type           = var.VOLUME_TYPE # Specifies the volume type (e.g., "gp2" for General Purpose SSD).
  }
}


# Google Cloud Image Source Configuration
source "googlecompute" "my-image" {
  project_id   = var.gcp_project_id
  source_image = "ubuntu-2204-lts"
  zone         = var.GCP_ZONE
  machine_type = var.gcp_machine_type
  image_name   = "${var.gcp_IMAGE_FAM_NAME}-${formatdate("YYYY-MM-DD-HH-MM-ss", timestamp())}"
  image_family = var.gcp_IMAGE_FAM_NAME
  ssh_username = var.ssh_username
}

# Build Configuration
# Step 1
build {
  sources = ["source.amazon-ebs.my-ami",
    "source.googlecompute.my-image"
  ]


  # Step 2
  # Pre-Setup Script
  provisioner "shell" {
    environment_vars = [
      "DB_URL=${var.DB_URL}",
      "DB_USRNAME=${var.DB_USRNAME}",
      "DB_PASSWORD=${var.DB_PASSWORD}",
      "DB_NAME=${var.DB_NAME}",
      "PRT_NBR=${var.PRT_NBR}"
    ]
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
