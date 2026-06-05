#!/bin/bash
# =========================================================
# InfoPouch Backend - One-time Oracle Cloud VM Setup
# Run this ONCE on your Oracle Cloud ARM VM after SSH-ing in
#
# Usage:
#   chmod +x setup.sh && ./setup.sh
# =========================================================

set -e

echo ""
echo "=========================================="
echo " InfoPouch Backend - Server Setup"
echo "=========================================="
echo ""

# ----------------------------------------------------------
# 1. System update
# ----------------------------------------------------------
echo "[1/5] Updating system packages..."
sudo apt update -y && sudo apt upgrade -y

# ----------------------------------------------------------
# 2. Install Java 21
# ----------------------------------------------------------
echo "[2/5] Installing Java 21..."
sudo apt install -y openjdk-21-jdk
java -version
echo "Java installed."

# ----------------------------------------------------------
# 3. Create app directory and placeholder env file
# ----------------------------------------------------------
echo "[3/5] Creating app directory..."
mkdir -p /home/ubuntu/infopouch

if [ ! -f /home/ubuntu/infopouch/app.env ]; then
  cat > /home/ubuntu/infopouch/app.env << 'EOF'
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://REPLACE_WITH_NEON_HOST/REPLACE_WITH_DB_NAME?sslmode=require
DATABASE_USERNAME=REPLACE_WITH_NEON_USERNAME
DATABASE_PASSWORD=REPLACE_WITH_NEON_PASSWORD
JWT_SECRET=REPLACE_WITH_A_STRONG_64_CHAR_SECRET
APP_BASE_URL=http://REPLACE_WITH_YOUR_VM_IP:8080
EOF
  echo "Created /home/ubuntu/infopouch/app.env — fill in your values before starting the app."
else
  echo "app.env already exists, skipping."
fi

# ----------------------------------------------------------
# 4. Create systemd service
# ----------------------------------------------------------
echo "[4/5] Registering systemd service..."
sudo tee /etc/systemd/system/infopouch.service > /dev/null << 'EOF'
[Unit]
Description=InfoPouch Backend API
After=network-online.target
Wants=network-online.target

[Service]
User=ubuntu
WorkingDirectory=/home/ubuntu/infopouch
EnvironmentFile=/home/ubuntu/infopouch/app.env
ExecStart=/usr/bin/java -Xms128m -Xmx512m -jar /home/ubuntu/infopouch/app.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable infopouch
echo "Service registered and enabled."

# ----------------------------------------------------------
# 5. Open port 8080 in the OS firewall
# ----------------------------------------------------------
echo "[5/5] Opening port 8080..."
sudo iptables -I INPUT -p tcp --dport 8080 -j ACCEPT
sudo apt install -y iptables-persistent
sudo netfilter-persistent save
echo "Port 8080 open."

# ----------------------------------------------------------
# Done
# ----------------------------------------------------------
echo ""
echo "=========================================="
echo " Setup complete!"
echo "=========================================="
echo ""
echo "NEXT STEPS:"
echo "  1. Fill in your credentials:"
echo "     nano /home/ubuntu/infopouch/app.env"
echo ""
echo "  2. Deploy the app from your local machine:"
echo "     ./scripts/deploy.sh <your-vm-ip> <path-to-ssh-key>"
echo ""
echo "  NOTE: Also open port 8080 in Oracle Cloud console:"
echo "  Networking > VCN > Security List > Add Ingress Rule > Port 8080"
echo ""
