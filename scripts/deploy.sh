#!/bin/bash
# =========================================================
# InfoPouch Backend - Deploy Script
# Run this locally every time you want to push a new version
#
# Usage:
#   chmod +x scripts/deploy.sh
#   ./scripts/deploy.sh <vm-ip> <path-to-ssh-key>
#
# Example:
#   ./scripts/deploy.sh 129.154.56.78 ~/Downloads/oracle-key.pem
# =========================================================

set -e

VM_IP=$1
SSH_KEY=$2

# ----------------------------------------------------------
# Validate args
# ----------------------------------------------------------
if [ -z "$VM_IP" ] || [ -z "$SSH_KEY" ]; then
  echo "Usage: ./scripts/deploy.sh <vm-ip> <path-to-ssh-key>"
  echo "Example: ./scripts/deploy.sh 129.154.56.78 ~/Downloads/oracle-key.pem"
  exit 1
fi

if [ ! -f "$SSH_KEY" ]; then
  echo "SSH key not found at: $SSH_KEY"
  exit 1
fi

chmod 400 "$SSH_KEY"

echo ""
echo "=========================================="
echo " InfoPouch Backend - Deploying"
echo " Target: ubuntu@$VM_IP"
echo "=========================================="
echo ""

# ----------------------------------------------------------
# 1. Build the JAR locally
# ----------------------------------------------------------
echo "[1/3] Building JAR..."
./mvnw clean package -DskipTests -q
echo "Build complete."

# ----------------------------------------------------------
# 2. Upload JAR to server
# ----------------------------------------------------------
echo "[2/3] Uploading JAR to server..."
scp -i "$SSH_KEY" \
    -o StrictHostKeyChecking=no \
    target/backend-0.0.1-SNAPSHOT.jar \
    ubuntu@$VM_IP:/home/ubuntu/infopouch/app.jar
echo "Upload complete."

# ----------------------------------------------------------
# 3. Restart the service
# ----------------------------------------------------------
echo "[3/3] Restarting service..."
ssh -i "$SSH_KEY" \
    -o StrictHostKeyChecking=no \
    ubuntu@$VM_IP \
    "sudo systemctl restart infopouch && sudo systemctl status infopouch --no-pager"

echo ""
echo "=========================================="
echo " Deployed successfully!"
echo " API is live at: http://$VM_IP:8080"
echo " Swagger UI:     http://$VM_IP:8080/swagger-ui.html"
echo "=========================================="
echo ""
