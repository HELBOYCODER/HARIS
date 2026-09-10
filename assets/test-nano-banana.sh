#!/bin/sh
# Nano Banana Image Generation Test Script
# Tests the Gemini API image generation

set -e

echo "🍌 Nano Banana Image Generation Test"
echo "=================================="

# Check API key
if [ -z "$GEMINI_API_KEY" ]; then
    echo "❌ GEMINI_API_KEY not set"
    echo "Please set it via: echo 'export GEMINI_API_KEY=your_key' >> /etc/profile"
    exit 1
fi

echo "✅ API key configured"
echo "📍 API key prefix: $(echo "$GEMINI_API_KEY" | head -c 20)..."

# Check dependencies
echo "🔍 Checking dependencies..."

if ! command -v python3 >/dev/null 2>&1; then
    echo "❌ Python3 not found"
    exit 1
fi

echo "✅ Python3 installed"

if ! pip3 show google-genai >/dev/null 2>&1; then
    echo "❌ google-genai not installed"
    echo "Please install: pip3 install google-genai pillow"
    exit 1
fi

echo "✅ google-genai installed"

# Test image generation
echo ""
echo "🎨 Testing image generation..."
echo "Prompt: a cute panda drinking tea in a bamboo forest"
echo "Aspect: 16:9"
echo "Resolution: 2K"

OUTPUT_FILE="/var/minis/attachments/test_nano_banana_${$(date +%s)}.png"

source /etc/profile

python3 << PYTHON_EOF
import os
import google.genai as genai

api_key = os.environ.get('GEMINI_API_KEY')
if not api_key:
    print("ERROR: GEMINI_API_KEY not set")
    exit(1)

try:
    client = genai.Client(api_key=api_key)
    
    prompt = "a cute panda drinking tea in a bamboo forest, high quality, 2K"
    print(f"[*] Generating image...")
    
    response = client.models.generate_images(
        model='gemini-3.1-flash-image-preview',
        prompt=prompt,
        config={
            'aspect_ratio': '16:9',
            'number_of_images': 1,
            'output_size': '2K',
        }
    )
    
    output_path = "$OUTPUT_FILE"
    with open(output_path, 'wb') as f:
        f.write(response.images[0].image_bytes)
    
    print(f"[+] SUCCESS: Image saved to {output_path}")
    print("✅ Test passed!")
except Exception as e:
    print(f"❌ ERROR: {e}")
    exit(1)
PYTHON_EOF

if [ $? -eq 0 ] && [ -f "$OUTPUT_FILE" ]; then
    echo ""
    echo "=================================="
    echo "✅ All tests passed!"
    echo "📁 Generated image: $OUTPUT_FILE"
    echo "📝 View in Minis: minis://attachments/$(basename "$OUTPUT_FILE")"
else
    echo ""
    echo "=================================="
    echo "❌ Test failed"
    exit 1
fi
