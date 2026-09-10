# Building and Deploying HARIS

## Prerequisites

### Android Build
- JDK 17
- Android SDK (compileSdk 36, minSdk 26)
- Android NDK r28+
- CMake 3.22.1

### iOS Build
- macOS with Xcode
- Homebrew packages: ninja, llvm, libarchive, pkg-config

## Build Steps

### 1. Clone Repository
```bash
git clone --recurse-submodules https://github.com/YOUR_USERNAME/HARIS.git
cd HARIS
```

### 2. Build Native Dependencies
```bash
# Android
./deps/build_proot.sh
./scripts/prepare_android_sandbox.sh

# iOS
./deps/build_lame.sh
./deps/build_ffmpeg.sh
./deps/build_ish.sh
./deps/prepare_alpine_rootfs.sh
```

### 3. Build APK
```bash
cd src/android
./gradlew :app:assembleDebug
```

The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions CI/CD

Create `.github/workflows/build-apk.yml`:

```yaml
name: Build Android APK

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
        with:
          submodules: recursive
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Setup Android SDK
        uses: android-actions/setup-android@v3
      
      - name: Setup NDK
        uses: nttld/setup-ndk@v1
        with:
          ndk-version: r28.0.12433566
      
      - name: Build dependencies
        run: |
          ./deps/build_proot.sh
          ./scripts/prepare_android_sandbox.sh
      
      - name: Build APK
        run: |
          cd src/android
          chmod +x gradlew
          ./gradlew :app:assembleDebug
      
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: haris-debug-apk
          path: src/android/app/build/outputs/apk/debug/app-debug.apk
```

## 9Router Integration Setup

### First Run Requirements
When 9Router installation is triggered, the sandbox needs:

1. **Node.js** - Installs via `apk add nodejs npm` if missing
2. **curl** - For health checks (usually already present)
3. **Write access** to `/data/` directory

### Testing 9Router Locally

To test 9Router integration without full app build:

```bash
# In the sandbox
curl -s http://127.0.0.1:20128/api/health
# Expected: {"ok":true}
```

## Release Process

### 1. Version Tag
```bash
git tag v1.0.0
git push origin v1.0.0
```

### 2. Create GitHub Release
1. Go to https://github.com/YOUR_USERNAME/HARIS/releases/new
2. Select tag `v1.0.0`
3. Title: `HARIS v1.0.0`
4. Description: Add release notes
5. Upload APK artifact
6. Publish

## Known Issues

### 9Router Installation Fails
- Ensure internet connectivity in sandbox
- Check npm registry access: `npm ping`
- May need proxy configuration in corporate networks

### Build Fails with NDK Errors
- Ensure NDK r28+ is installed
- Set `ANDROID_NDK_HOME` environment variable
- Clean and rebuild: `./gradlew clean`

## Deployment Checklist

- [ ] All native dependencies build successfully
- [ ] APK builds without errors
- [ ] 9Router setup script works in sandbox
- [ ] Provider registration completes
- [ ] Health check passes
- [ ] Models are discoverable
- [ ] Test on physical device
- [ ] Update CHANGELOG.md
- [ ] Tag release
- [ ] Publish to GitHub Releases
