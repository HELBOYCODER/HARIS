# 9Router Local Integration for HARIS

## Overview

This feature integrates **9Router** - a local AI gateway - directly into HARIS (Minis fork). Users can install 9Router with one click from the Providers settings screen, giving them:

- Local AI gateway on port 20128
- Free model access via OpenCode provider
- Unified provider management
- No API key requirements for local use

## Components

### 1. Setup Script (`assets/9router-setup.sh`)

Automates 9Router installation inside the Linux sandbox:
```bash
# Auto-installs dependencies, npm, and 9Router
# Configures provider settings
# Starts daemon on port 20128
./assets/9router-setup.sh
```

### 2. Kotlin Installer (`src/android/app/src/main/java/com/openminis/app/provider/9RouterProviderInstaller.kt`)

Handles the full installation flow:
- Executes setup script via sandbox shell
- Registers 9Router as a provider in the app database
- Verifies health check
- Provides status feedback

### 3. UI Integration (`ProviderListScreen.kt`)

Adds a Terminal icon button to the ProviderListScreen:
- Opens installation dialog
- Shows progress during installation
- Displays success/error messages
- Auto-registers provider after installation

## Installation Flow

1. User taps **Terminal icon** in ProviderListScreen actions
2. Dialog explains 9Router features
3. User taps **Install**
4. Background process runs:
   - Checks for Node.js/npm
   - Installs 9Router globally
   - Creates config file
   - Starts daemon
   - Registers provider in app
5. Provider appears in list as "9Router Local"

## Usage

Once installed:

1. **Access Models**: Navigate to Providers → 9Router Local → Models
2. **Use in Chats**: Select any model from 9Router provider
3. **Dashboard**: Open http://127.0.0.1:20128/dashboard (password: minis123)

## API Endpoints

```
http://127.0.0.1:20128/v1/models          # List models
http://127.0.0.1:20128/v1/chat/completions # Chat API
http://127.0.0.1:20128/api/health        # Health check
```

## Free Models

The integration includes pre-configured access to OpenCode free models:
- `oc/mimo-v2.5-free`
- `oc/ling-3.0-flash-fin-free`
- And others available via OpenCode

## Troubleshooting

**Installation fails:**
- Ensure Node.js can be installed (check internet connection)
- Check `/data/9router/server.log` for daemon errors

**Provider not appearing:**
- Restart HARIS app
- Check that 9Router is running: `curl http://127.0.0.1:20128/api/health`

**Port conflict:**
- Edit `assets/9router-setup.sh` to change `PORT` variable

## Future Enhancements

- Custom port configuration
- Automatic provider updates
- Model catalog sync from 9Router
- Backup/restore of 9Router configuration

## Files Modified

| File | Purpose |
|------|---------|
| `assets/9router-setup.sh` | New - Installation script |
| `src/android/.../provider/9RouterProviderInstaller.kt` | New - Installation logic |
| `src/android/.../ui/settings/ProviderListScreen.kt` | Modified - UI integration |
| `src/android/.../data/model/ProviderType.kt` | May need: Add 9router provider type |

## Dependencies

- Node.js (auto-installed if missing)
- npm (auto-installed if missing)
- 9Router CLI (`npm install -g 9router`)
- curl (for health checks)
