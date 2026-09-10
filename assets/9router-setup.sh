#!/bin/sh
# 9Router Setup Script for HARIS (Minis Fork)
# This script installs and configures 9Router as a local AI gateway

set -e

PORT=${PORT:-20128}
DATA_DIR=${DATA_DIR:-/data/9router}
DASHBOARD_PASS=${DASHBOARD_PASS:-minis123}

echo "🚀 Setting up 9Router on port $PORT..."

# Create data directory
mkdir -p "$DATA_DIR"

# Check if npm/node available
if command -v node >/dev/null 2>&1; then
    echo "✅ Node.js found: $(node --version)"
else
    echo "⚠️ Node.js not found - installing..."
    # This would typically install Node.js via apk or similar
    # For now, skip installation and let user install manually
fi

# Install 9Router if not already installed
if ! command -v 9router >/dev/null 2>&1; then
    echo "📦 Installing 9Router..."
    if command -v npm >/dev/null 2>&1; then
        npm install -g 9router@latest
    else
        echo "❌ npm not found - please install Node.js first"
        exit 1
    fi
else
    echo "✅ 9Router already installed: $(9router --version)"
fi

# Configure 9Router
echo "⚙️ Configuring 9Router..."
mkdir -p "$DATA_DIR"

# Create config file
cat > "$DATA_DIR/config.json" << 'EOF'
{
  "port": 20128,
  "dataDir": "/data/9router",
  "dashboardPassword": "minis123",
  "requireApiKey": false,
  "providers": {
    "opencode": {
      "authType": "noAuth",
      "isActive": true
    }
  }
}
EOF

# Start 9Router daemon
echo "🔄 Starting 9Router daemon on port $PORT..."
nohup 9router --port "$PORT" --data-dir "$DATA_DIR" > /data/9router/server.log 2>&1 &
sleep 2

# Health check
echo "🏥 Checking health..."
if command -v curl >/dev/null 2>&1; then
    if curl -s "http://127.0.0.1:$PORT/api/health" | grep -q "ok"; then
        echo "✅ 9Router is running!"
        echo "📊 Dashboard: http://127.0.0.1:$PORT/dashboard"
        echo "🔑 API Base: http://127.0.0.1:$PORT/v1"
        echo "📚 Models: http://127.0.0.1:$PORT/v1/models"
    else
        echo "❌ Health check failed. Check /data/9router/server.log"
        exit 1
    fi
else
    echo "⚠️ curl not available for health check"
fi

echo "
�️ 9Router setup complete!"
