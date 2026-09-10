#!/bin/bash
# HARIS - MCP & Skill Installation Script
# Installs all keyless MCPs and essential skills by default

set -e

echo "🚀 Installing HARIS MCPs and Skills..."

# ============================================
# MCP Installation (Keyless / No API Key Required)
# ============================================

# Keyless MCPs (no API key needed)
echo "📦 Installing Keyless MCPs..."

# 1. gitmcp - GitHub documentation (keyless)
if ! minis-mcp-cli list 2>/dev/null | grep -q gitmcp; then
    minis-mcp-cli add --name gitmcp --transport http --target "https://gitmcp.io/docs" --note "GitHub docs MCP (keyless)"
    echo "✅ gitmcp installed"
else
    echo "⚪ gitmcp already installed"
fi

# 2. context7 - Up-to-date library docs (keyless)
if ! minis-mcp-cli list 2>/dev/null | grep -q context7; then
    minis-mcp-cli add --name context7 --transport http --target "https://mcp.context7.com/mcp" --note "Library docs MCP (keyless)"
    echo "✅ context7 installed"
else
    echo "⚪ context7 already installed"
fi

# 3. deepwiki - Any GitHub repo knowledge (keyless)
if ! minis-mcp-cli list 2>/dev/null | grep -q deepwiki; then
    minis-mcp-cli add --name deepwiki --transport http --target "https://mcp.deepwiki.com/mcp" --note "GitHub knowledge MCP (keyless)"
    echo "✅ deepwiki installed"
else
    echo "⚪ deepwiki already installed"
fi

# 4. mu - 118 agent tools (keyless)
if ! minis-mcp-cli list 2>/dev/null | grep -q '"mu"'; then
    minis-mcp-cli add --name mu --transport http --target "https://micro.mu/mcp" --note "Agent platform MCP (keyless)"
    echo "✅ mu installed"
else
    echo "⚪ mu already installed"
fi

# 5. seo-mcp - SEO tools (CLI-based, no API key needed)
if ! minis-mcp-cli list 2>/dev/null | grep -q "seo-mcp"; then
    # Check if seo-mcp CLI is installed
    if command -v seo-mcp &>/dev/null; then
        minis-mcp-cli add --name seo-mcp --target "seo-mcp" --note "SEO tools (on-page audit, sitemap, etc.)"
        echo "✅ seo-mcp installed"
    else
        echo "⚠️  seo-mcp CLI not found, skipping"
    fi
else
    echo "⚪ seo-mcp already installed"
fi

# ============================================
# Required MCPs (with API Keys - Auto-Check)
# ============================================

echo "📦 Setting up API-Key MCPs (checking for environment variables)..."

# gemini-translation (needs GEMINI_API_KEY)
if [ -n "$GEMINI_API_KEY" ] && ! minis-mcp-cli list 2>/dev/null | grep -q gemini-translation; then
    minis-mcp-cli add --name gemini-translation --command python3 --args "/var/minis/shared/gemini-translation-mcp/gemini_translation_mcp.py" --note "Realtime translation via Gemini"
    echo "✅ gemini-translation installed"
else
    echo "⚪ gemini-translation requires GEMINI_API_KEY"
fi

# nano-banana (needs GEMINI_API_KEY)
if [ -n "$GEMINI_API_KEY" ] && ! minis-mcp-cli list 2>/dev/null | grep -q nano-banana; then
    minis-mcp-cli add --name nano-banana --command python3 --args "/var/minis/shared/nano-banana-mcp/nano_banana_mcp.py" --note "Nano Banana (Gemini) image generation"
    echo "✅ nano-banana installed"
else
    echo "⚪ nano-banana requires GEMINI_API_KEY"
fi

# 9router (local gateway)
if ! minis-mcp-cli list 2>/dev/null | grep -q "9router"; then
    # Check if 9router is installed
    if command -v 9router &>/dev/null; then
        minis-mcp-cli add --name 9router --transport http --target "http://127.0.0.1:20128/v1" --note "Local AI gateway (free models)"
        echo "✅ 9router installed"
    else
        echo "⚠️  9router not installed, run: ./assets/9router-setup.sh"
    fi
else
    echo "⚪ 9router already installed"
fi

# ============================================
# Essential Skills Integration
# ============================================

echo "📚 Verifying Essential Skills..."

# Key skills that should be present
ESSENTIAL_SKILLS=(
    "nano-banana"
    "9router"
    "9router-stt"
    "9router-tts"
    "image-to-code-skill"
    "design-taste-frontend"
    "vercel-web-design-guidelines"
    "i-have-adhd"
    "kept"
    "ponytail"
)

for skill in "${ESSENTIAL_SKILLS[@]}"; do
    if [ -d "/var/minis/skills/$skill" ]; then
        echo "✅ $skill found"
    else
        echo "⚠️  $skill not found"
    fi
done

echo ""
echo "✅ HARIS MCP & Skill installation complete!"
echo ""
echo "Next steps:"
echo "1. Set API keys in environment (for Gemini features):"
echo "   export GEMINI_API_KEY='your_key_here'"
echo ""
echo "2. Start 9router for local gateway:"
echo "   9router"
echo ""
echo "3. Verify MCPs:"
echo "   minis-mcp-cli list"
echo ""