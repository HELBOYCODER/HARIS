# HARIS MCP & Skills Configuration

## 🔌 MCP Servers

### Keyless MCPs (No API Key Required)

| MCP | Purpose | Status |
|-----|---------|--------|
| `gitmcp` | GitHub repo documentation & search | ✅ Ready |
| `context7` | Up-to-date library documentation | ✅ Ready |
| `deepwiki` | GitHub repo knowledge & Q&A | ✅ Ready |
| `mu` | 118 agent tools (web, mail, weather, etc.) | ✅ Ready |

### API-Key MCPs (Environment Variables Required)

| MCP | Purpose | API Key |
|-----|---------|---------|
| `gemini-translation` | Realtime translation | `GEMINI_API_KEY` |
| `nano-banana` | Image generation & editing | `GEMINI_API_KEY` |

### Local Gateway MCPs

| MCP | Purpose | Port |
|-----|---------|------|
| `9router` | Local AI gateway (OpenCode free models) | 20128 |

---

## 📚 Essential Skills

### AI & Code Generation
- `nano-banana` - Gemini image generation
- `9router-image` - Image generation via local gateway
- `image-to-code-skill` - Convert images to UI code

### UI/Design Systems (70+ styles)
- `awesome-brutalism` - Brutalist design
- `awesome-bento` - Bento box layout
- `awesome-glassmorphism` - Glass effect
- `awesome-claymorphism` - Clay effect
- `awesome-design-taste-frontend` - Premium frontend design

### Translation & Speech
- `9router-stt` - Speech-to-text
- `9router-tts` - Text-to-speech
- `gemini-translation` - Translation MCP

### Development Tools
- `agent-browser` - Browser automation
- `kept` - Local chat archive (ChatGPT, Claude, Gemini)
- `ponytail` - Code optimization rules
- `vercel-web-design-guidelines` - Web design audit (190 rules)

### ADHD-Friendly
- `i-have-adhd` - Output formatting for ADHD users

---

## 🚀 Quick Setup

```bash
# Install all MCPs and verify skills
./assets/install-mcp-skill.sh

# Set API keys (required for translation & image features)
export GEMINI_API_KEY="your_api_key_here"

# Start local gateway
9router

# Verify installation
minis-mcp-cli list
```

---

## 📖 MCP Usage Examples

### Translation
```bash
minis-mcp-cli call gemini-translation translate_text \
  --input '{"srcLang":"fa","tgtLang":"en","text":"سلام"}'
```

### Image Generation
```bash
minis-mcp-cli call nano-banana generate_image \
  --input '{"prompt":"a beautiful sunset", "aspect_ratio":"16:9"}'
```

### Documentation Search
```bash
minis-mcp-cli call gitmcp fetch_documentation \
  --input '{"repo":"OpenMinis/OpenMinis","query":"speech recognition"}'
```

---

## ✅ Installation Status

| Category | Count | Ready |
|----------|-------|-------|
| Keyless MCPs | 4 | ✅ 4/4 |
| API-Key MCPs | 2 | ⚠️ Requires GEMINI_API_KEY |
| Local Gateway | 1 | ✅ 9router |
| Design Skills | 70+ | ✅ All available |
| Speech Tools | 3 | ✅ 9router-stt/tts, gemini |
| Dev Tools | 20+ | ✅ All available |

---

## 📁 File Locations

```
/var/minis/
├── skills/                # All skills (296+)
├── shared/
│   ├── gemini-translation-mcp/
│   └── nano-banana-mcp/
└── HARIS-base/
    └── assets/
        └── install-mcp-skill.sh
```

---

## 🔐 Security Notes

- All API keys stored in environment variables (not in code)
- Use `minis-mcp-cli` to manage MCPs
- Personal tokens never saved in project files
- MCP servers run locally via stdio