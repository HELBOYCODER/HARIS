#!/bin/sh
# Nano Banana (Gemini Image API) Setup Script
# This script installs dependencies and configures the image generation provider

set -e

OUTPUT_DIR=${OUTPUT_DIR:-/var/minis/attachments}
SKILL_DIR=${SKILL_DIR:-/var/minis/skills/nano-banana}

echo "🍌 Setting up Nano Banana (Gemini Image API)..."

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Check for API key
if [ -z "$GEMINI_API_KEY" ]; then
    echo "⚠️  GEMINI_API_KEY not set"
    echo "Please set it via:"
    echo "  echo 'export GEMINI_API_KEY=\"your_api_key\"' >> /etc/profile"
    echo "  source /etc/profile"
    exit 1
fi

echo "✅ API key configured"

# Install Python dependencies if needed
if ! command -v python3 >/dev/null 2>&1; then
    echo "📦 Installing Python3..."
    apk add --no-cache python3 py3-pip
fi

# Install google-genai and pillow
if ! pip3 show google-genai >/dev/null 2>&1; then
    echo "📦 Installing google-genai..."
    pip3 install google-genai pillow
else
    echo "✅ google-genai already installed"
fi

# Copy skill scripts to standard location
if [ -d "$SKILL_DIR/scripts" ]; then
    echo "📁 Found Nano Banana scripts in $SKILL_DIR/scripts"
    # Scripts already available
else
    echo "⚠️  Nano Banana scripts not found at $SKILL_DIR/scripts"
    echo "Make sure the skill is properly installed"
fi

echo "🍌 Nano Banana setup complete!"
echo "
 Usage:"
echo "  source /etc/profile && python3 -m google.genai generate-image \"your prompt\""
echo "  # Or use the scripts in $SKILL_DIR/scripts/"
