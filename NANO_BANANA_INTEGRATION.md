# Nano Banana (Gemini Image API) Integration for HARIS

## Overview

This feature integrates **Nano Banana** - Google's Gemini image generation API - directly into HARIS as a dedicated image generation provider. Users can:

- Generate images from text prompts
- Edit existing images with natural language instructions
- Control aspect ratios (1:1, 16:9, 9:16, 4:3, 3:4)
- Choose resolutions (1K or 2K)

## Components

### 1. Setup Script (`assets/nano-banana-setup.sh`)

Automates Nano Banana dependency installation:
```bash
# Installs google-genai and pillow
# Configures GEMINI_API_KEY environment
# Verifies installation
./assets/nano-banana-setup.sh
```

### 2. Provider Installer (`.../provider/NanoBananaProviderInstaller.kt`)

Handles the full installation flow:
- Sets GEMINI_API_KEY in sandbox environment
- Registers Nano Banana as a provider in the app database
- Creates model entry for Gemini image generation
- Verifies configuration

### 3. Image Provider (`.../provider/NanoBananaProvider.kt`)

Core image generation functionality:
- Text-to-image generation with custom prompts
- Image-to-image editing with instructions
- Model selection (Nano Banana 2, Pro, or original)
- Output file management

### 4. UI Integration (`ProviderListScreen.kt`)

Adds an Image icon button to the ProviderListScreen:
- Opens installation dialog
- Accepts Gemini API key
- Shows progress during installation
- Displays success/error messages

### 5. MCP Server (`shared/nano-banana-mcp/nano_banana_mcp.py`)

Model Context Protocol server with tools:
- `generate_image`: Text-to-image generation
- `edit_image`: Image editing with instructions

## Installation Flow

1. User taps **Image icon** in ProviderListScreen actions
2. Dialog explains Nano Banana features
3. User enters Gemini API key
4. Background process runs:
   - Installs google-genai and pillow if needed
   - Sets GEMINI_API_KEY environment variable
   - Registers provider in app
5. Provider appears in list as "Nano Banana (Gemini)"

## Usage

### Text-to-Image
```bash
source /etc/profile && python3 /var/minis/skills/nano-banana/scripts/gen.py "a cute panda drinking tea" /var/minis/attachments/panda.png 16:9 2K
```

### Image Editing
```bash
source /etc/profile && python3 /var/minis/skills/nano-banana/scripts/edit.py /var/minis/attachments/photo.jpg "add a wizard hat" /var/minis/attachments/edited.png
```

### MCP Tools (via Minis chat)
```
Use nano-banana MCP:
- generate_image with prompt and aspect ratio
- edit_image with image path and instruction
```

## API Endpoints

```
https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-image-preview:generateImages
https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-image-preview:editImage
```

## Models Available

| Model | ID | Best For |
|-------|----------|----------|
| **Nano Banana 2** (recommended) | `gemini-3.1-flash-image-preview` | Fast, high quality, 2K support |
| **Nano Banana Pro** | `gemini-3-pro-image-preview` | Complex prompts, precise text |
| **Nano Banana** (original) | `gemini-2.5-flash-image` | Low latency, simple tasks |

## MCP Integration

To use Nano Banana in Minis MCP:

```bash
minis-mcp-cli call "nano-banana" generate_image \
  --input '{"prompt": "a sunset over mountains", "aspect_ratio": "16:9", "resolution": "2K"}'
```

Output is saved to `/var/minis/attachments/`

## Environment Setup

```bash
# Set Gemini API key
echo 'export GEMINI_API_KEY="your_api_key"' >> /etc/profile
source /etc/profile

# Verify
echo $GEMINI_API_KEY | head -c 10
```

## Troubleshooting

**API key not set:**
- Run `source /etc/profile` before commands
- Check: `echo $GEMINI_API_KEY`

**google-genai not found:**
- Install: `pip3 install google-genai pillow`

**Rate limit errors:**
- Free tier has limits - wait and retry
- Consider upgrading Gemini API plan

**Image quality issues:**
- Use `gemini-3-pro-image-preview` for complex prompts
- Add descriptive details to prompts

## Future Enhancements

- Batch image generation
- Image style presets
- Model selection UI
- Automatic API key rotation
- Image gallery integration

## Files Modified

| File | Purpose |
|------|---------|
| `assets/nano-banana-setup.sh` | New - Installation script |
| `src/android/.../provider/NanoBananaProviderInstaller.kt` | New - Installation logic |
| `src/android/.../provider/NanoBananaProvider.kt` | New - Image generation |
| `src/android/.../ui/settings/ProviderListScreen.kt` | Modified - UI integration |
| `shared/nano-banana-mcp/nano_banana_mcp.py` | New - MCP server |
| `ProviderType.kt` | May need: Add image provider type |

## Dependencies

- Python 3.x
- `google-genai` package
- `pillow` (PIL)
- Gemini API key

## API Key

Get your key from: https://aistudio.google.com/apikey

Required scopes:
- `https://www.googleapis.com/auth/generativelanguage`

## Testing

Test the installation:
```bash
source /etc/profile
curl -X POST "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-image-preview:generateImages" \
  -H "Content-Type: application/json" \
  -H "X-Goog-Api-Key: $GEMINI_API_KEY" \
  -d '{"prompt": "test image", "aspect_ratio": "16:9", "number_of_images": 1}'
```

Expected response: JSON with base64-encoded image data.
