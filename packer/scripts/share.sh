#!/bin/bash

# Fetch the image name, project IDs from arguments
IMAGE_NAME=$(jq -r --arg last_uuid "$(jq -r '.last_run_uuid' manifest.json)" \
  '.builds[] | select(.packer_run_uuid == $last_uuid and .builder_type == "googlecompute") | .artifact_id' manifest.json)
DEV_PROJECT_ID=dev-452121
DEMO_PROJECT_ID=pioneering-tome-453017-h5

# Share the image with the DEMO project
gcloud compute images add-iam-policy-binding "$IMAGE_NAME" \
  --project="$DEV_PROJECT_ID" \
  --member="project:$DEMO_PROJECT_ID" \
  --role="roles/compute.imageUser"
