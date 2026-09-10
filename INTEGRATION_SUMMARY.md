# HARIS Integration Summary (2026-09-10)

## 9Router Integration ✅

### Components
- **Setup Script**: `assets/9router-setup.sh`
- **Provider Installer**: `.../provider/9RouterProviderInstaller.kt`
- **UI**: Added Terminal button to `ProviderListScreen.kt`

### Features
- Local AI gateway on port 20128
- One-click installation from ProviderListScreen
- Auto-registration of OpenCode free models
- MCP integrated via `minis-mcp-cli`

---

## Nano Banana (Gemini Image API) Integration ✅

### Components

#### 1. Secure API Key Storage
**File**: `.../provider/NanoBananaKeyStore.kt`

- Uses Android `EncryptedSharedPreferences` with AES256-GCM
- Key stored in Android Keystore
- Methods: `storeApiKey()`, `getApiKey()`, `hasApiKey()`, `deleteApiKey()`
- Initialized in `MinisApp.onCreate()`

#### 2. Provider Type
**File**: `.../data/model/ProviderConfig.kt`

Added `nanoBanana` enum value:
```kotlin
nanoBanana("Nano Banana (Gemini Image)")
```

#### 3. Image Provider
**File**: `.../provider/NanoBananaProvider.kt`

- Text-to-image generation with Gemini 3.1 Flash
- Image editing with natural language instructions
- Aspect ratio control (1:1, 16:9, 9:16, 4:3, 3:4)
- Resolution options (1K, 2K)
- Output saved to `/var/minis/attachments/`

#### 4. MCP Server
**File**: `shared/nano-banana-mcp/nano_banana_mcp.py`

Tools:
- `generate_image` - Text-to-image generation
- `edit_image` - Image editing

#### 5. Test Script
**File**: `assets/test-nano-banana.sh`

Tests image generation workflow:
```bash
./assets/test-nano-banana.sh
```

### Skills Integration
Uses existing `/var/minis/skills/nano-banana/` skill:
- `scripts/gen.py` - Text to image
- `scripts/edit.py` - Image editing
- `scripts/batch.py` - Batch generation
- `scripts/prompt_crafter.py` - Prompt enhancement

### MCP Integration
Registered via `minis-mcp-cli add`:
```json
{
  "name": "nano-banana",
  "transport": "stdio",
  "command": "python3",
  "args": ["/var/minis/shared/nano-banana-mcp/nano_banana_mcp.py"]
}
```

---

## Provider Flow

### 9Router Flow
1. User taps Terminal button in ProviderListScreen
2. Dialog explains features
3. Click Install
4. Backend:
   - Checks/install Node.js
   - Installs 9Router
   - Starts daemon on port 20128
   - Registers provider
5. Provider appears as "9Router Local"

### Nano Banana Flow
1. User taps Image button in ProviderListScreen
2. Dialog prompts for Gemini API key
3. Click Install
4. Backend:
   - Stores API key securely (EncryptedSharedPreferences)
   - Sets GEMINI_API_KEY in sandbox
   - Registers provider as nanoBanana type
   - Installs google-genai if needed
5. Provider appears as "Nano Banana (Gemini Image)"

---

## Testing

### 9Router Test
```bash
curl http://127.0.0.1:20128/api/health
# Expected: {"ok":true}
```

### Nano Banana Test
```bash
source /etc/profile
./assets/test-nano-banana.sh
# Expected: Image saved to /var/minis/attachments/
```

### MCP Test
```bash
minis-mcp-cli call nano-banana generate_image \
  --input '{"prompt": "a sunset over mountains", "aspect_ratio": "16:9"}'
# Expected: Output path saved
```

---

## Files Modified Summary

| Component | Files |
|-----------|-------|
| **9Router** | `assets/9router-setup.sh`, `.../9RouterProviderInstaller.kt`, `.../ProviderListScreen.kt` |
| **Nano Banana** | `.../NanoBananaKeyStore.kt`, `.../NanoBananaProviderInstaller.kt`, `.../NanoBananaProvider.kt`, `.../ProviderConfig.kt`, `.../MinisApp.kt` |
| **Testing** | `assets/test-nano-banana.sh`, `assets/nano-banana-setup.sh` |
| **MCP** | `shared/nano-banana-mcp/nano_banana_mcp.py` |
| **Docs** | `9ROUTER_INTEGRATION.md`, `NANO_BANANA_INTEGRATION.md`, `BUILD_DEPLOY.md` |

---

## Environment Requirements

### 9Router
- Node.js + npm
- curl

### Nano Banana
- Python 3.x
- google-genai package
- pillow
- Gemini API key

---

## Next Steps

1. **Build HARIS**: Run `./gradlew :app:assembleDebug` in `src/android`
2. **Test**: Install APK on device and verify both integrations
3. **Deploy**: Tag and release on GitHub
4. **Enhance**: Add provider management UI for Nano Banana model selection

---

## API Keys

| Service | Storage | Environment |
|---------|---------|-------------|
| Gemini API | `NanoBananaKeyStore` (EncryptedSharedPreferences) | `GEMINI_API_KEY` |
| 9Router | None (local gateway) | None |

Get Gemini API key: https://aistudio.google.com/apikey
