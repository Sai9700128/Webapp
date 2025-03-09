#!/bin/bash

# Ensure jq is installed (required for parsing JSON)
if ! command -v jq &> /dev/null
then
    echo "jq command not found. Installing..."
    sudo apt-get install jq -y
fi

# Parse AWS AMI ID from manifest.json
AWS_AMI_ID=$(jq -r '.builds[] | select(.name=="aws-image") | .artifact_id' manifest.json | cut -d':' -f2)

# Parse GCP Image Name from manifest.json
GCP_IMAGE_NAME=$(jq -r '.builds[] | select(.name=="gcp-image") | .artifact_id' manifest.json | awk -F'/' '{print $NF}')

# Check if AWS AMI ID was found and delete it
if [ -n "$AWS_AMI_ID" ]; then
    echo "Deregistering AWS AMI: $AWS_AMI_ID"
    aws ec2 deregister-image --image-id $AWS_AMI_ID
    echo "Deleted AWS AMI: $AWS_AMI_ID"

    # Find and delete the associated snapshot
    AWS_SNAPSHOT_ID=$(aws ec2 describe-images --image-ids $AWS_AMI_ID --query 'Images[*].BlockDeviceMappings[*].Ebs.SnapshotId' --output text)
    if [ -n "$AWS_SNAPSHOT_ID" ]; then
        echo "Deleting AWS Snapshot: $AWS_SNAPSHOT_ID"
        aws ec2 delete-snapshot --snapshot-id $AWS_SNAPSHOT_ID
        echo "Deleted AWS Snapshot: $AWS_SNAPSHOT_ID"
    fi
else
    echo "AWS AMI ID not found in manifest.json"
fi

# Check if GCP Image Name was found and delete it
if [ -n "$GCP_IMAGE_NAME" ]; then
    echo "Deleting GCP Image: $GCP_IMAGE_NAME"
    gcloud compute images delete $GCP_IMAGE_NAME --quiet
    echo "Deleted GCP Image: $GCP_IMAGE_NAME"
else
    echo "GCP Image Name not found in manifest.json"
fi

echo "Image cleanup completed!"
