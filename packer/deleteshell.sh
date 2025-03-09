#!/bin/bash

set -e  # Exit immediately if a command exits with a non-zero status.


# Specify varibales
GCP_PROJECT_ID="dev-452121"

AWS_REGION="us-east-1"
AWS_PROFILE="dev"

# $(echo "$ARTIFACT_ID" | awk -F':' '{print $1}')


# Get the latest image name
## GCP
GCP_IMAGE_ID=$(jq -r --arg last_uuid "$(jq -r '.last_run_uuid' manifest.json)" \
  '.builds[] | select(.packer_run_uuid == $last_uuid and .builder_type == "googlecompute") | .artifact_id' manifest.json)

## AWS
AWS_IMAGE_ID=$(jq -r --arg last_uuid "$(jq -r '.last_run_uuid' manifest.json)" \
  '.builds[] | select(.packer_run_uuid == $last_uuid and .builder_type == "amazon-ebs") | .artifact_id' manifest.json)


# Validate IMAGE_ID
# if [[ -z "$GCP_IMAGE_ID" || "$GCP_IMAGE_ID" == "null" ]]; then
#   echo "No valid GCP image found!"
#   exit 1
# fi

# For AWS
# if [[ -z "$AWS_IMAGE_ID" || "$AWS_IMAGE_ID" == "null" ]]; then
#   echo "No valid GCP image & AWS AMI found!"
#   exit 1
# fi

echo "Deleting GCP Image: $GCP_IMAGE_ID"
echo "Deleting AWS AMI Image: $AWS_IMAGE_ID"

# Delete the GCP_image
gcloud compute images delete "$GCP_IMAGE_ID" --quiet --project="$GCP_PROJECT_ID"


# Delete the AWS AMI
aws ec2 deregister-image --image-id $AWS_IMAGE_ID --profile $AWS_PROFILE --region $AWS_REGION


echo "Image deleted successfully!"