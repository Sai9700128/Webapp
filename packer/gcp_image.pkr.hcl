packer {
  required_plugins {
    googlecompute = {
      version = ">= 1.0.0, <2.0.0"
      source  = "github.com/hashicorp/googlecompute"
    }
  }
}

# # General Configuration

# variable "DB_URL" {
#   type = string
# }

# variable "DB_USRNAME" {
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

# variable "SSH_USERNAME" {
#   type    = string
#   # default = "ubuntu"
# }

# # gcp configurations

variable "GCP_PROJECT_ID" {
  type = string
  #   default = "gcp-dev-452117"
}
# variable "GCP_ZONE" {
#   type    = string
#   default = "us-central1-a"
# }
# variable "GCP_MACHINE_TYPE" {
#   type = string
#   default = "n1-standard-1"
# }
# variable "GCP_IMAGE_NAME" {
#   type    = string
#   default = "csye6225-health-checker-gcp"
# }
# variable "GCP_IMAGE_DESCRIPTION" {
#   type    = string
#   default = "Custom GCP Image for CSYE6225"
# }
# variable "GCP_DISK_SIZE" {
#   type    = number
#   default = 25
# }
# variable "GCP_DISK_TYPE" {
#   type    = string
#   default = "pd-balanced"
# }
# # variable "GCP_DEMO_PROJECT_ID" {
# #   type = string
# # }




# Google Cloud Image Source Configuration
source "googlecompute" "gcp_custom_ubuntu" {
  project_id        = var.GCP_PROJECT_ID
  zone              = var.GCP_ZONE
  machine_type      = var.GCP_MACHINE_TYPE
  image_name        = "${var.GCP_IMAGE_NAME}-${formatdate("YYYY-MM-DD-HH-mm-ss", timestamp())}"
  image_family      = "csye6225-health-checker-family"
  image_description = var.GCP_IMAGE_DESCRIPTION

  disk_size = var.GCP_DISK_SIZE
  disk_type = var.GCP_DISK_TYPE


  # Use the latest Ubuntu 24.04 LTS image
  source_image_family = "ubuntu-2204-lts"



  ssh_username = var.SSH_USERNAME
}


# Build Configuration
# Step 1
build {
  sources = ["source.googlecompute.gcp_custom_ubuntu"]


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
    script = "scripts/gcp-setup.sh"
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
