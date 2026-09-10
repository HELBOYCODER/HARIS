#!/usr/bin/env python3
"""
Nano Banana — Pure Colab GPU Engine (Zero Third-Party Fallback)
Exclusively generates images on user's private Google Colab Tesla T4 GPU.
"""
import os, sys, json, urllib.request, urllib.parse, base64, io
from PIL import Image, ImageFilter, ImageEnhance

# Check for --enhance flag
enhance_mode = False
args = [a for a in sys.argv[1:] if a != "--enhance"]
if "--enhance" in sys.argv:
    enhance_mode = True

raw_prompt  = args[0] if len(args) > 0 else "a beautiful mystical landscape"
output_path = args[1] if len(args) > 1 else "/var/minis/attachments/output.png"
aspect      = args[2] if len(args) > 2 else "16:9"

aspect_to_size = {
    "1:1": (1024, 1024),
    "16:9": (1024, 576),
    "9:16": (576, 1024),
    "4:3": (1024, 768),
    "3:4": (768, 1024)
}
w, h = aspect_to_size.get(aspect, (1024, 576))

# 1. Professional Prompt Enhancement
prompt = raw_prompt
if enhance_mode or any("\u0600" <= c <= "\u06FF" for c in raw_prompt) or len(raw_prompt.split()) < 6:
    print("[*] Enhancing prompt with cinematic details...")
    try:
        from prompt_crafter import enhance_prompt
        prompt = enhance_prompt(raw_prompt)
        print(f"[+] Enhanced: {prompt[:90]}...")
    except Exception as e:
        print(f"[-] Crafter note: {e}")

colab_url = os.environ.get("NANO_BANANA_API_URL", "https://unwrap-oxygen-ion-jerusalem.trycloudflare.com/v1")
api_key   = os.environ.get("NANO_BANANA_API_KEY", "sk-nanobanana-free")

print(f"[*] Target Engine: Google Colab T4 GPU (Exclusive)")
print(f"[*] Endpoint: {colab_url}")
print(f"[*] Prompt: {prompt[:60]}...")
print(f"[*] Target: {output_path} | Aspect: {aspect} ({w}x{h})")

# Exclusive Colab Request
try:
    req_data = json.dumps({
        "prompt": prompt,
        "model": "nano-banana",
        "size": f"{w}x{h}",
        "response_format": "url"
    }).encode("utf-8")
    
    req = urllib.request.Request(
        f"{colab_url.rstrip('/')}/images/generations",
        data=req_data,
        headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        res = json.loads(resp.read().decode("utf-8"))
        item = res["data"][0]
        img_url = item.get("url")
        
        # Download from Colab server directly
        with urllib.request.urlopen(img_url, timeout=30) as dl_resp:
            raw = dl_resp.read()
        
        img = Image.open(io.BytesIO(raw)).convert("RGB")
        # Sharpening & contrast tuning
        sharp = img.filter(ImageFilter.UnsharpMask(radius=1.8, percent=130, threshold=2))
        os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
        sharp.save(output_path, format="PNG", quality=95)
        print(f"✅ Generated 100% on Google Colab T4 GPU: {output_path}")
except Exception as e:
    print(f"❌ خطای اتصال به گوگل کلب: {e}")
    print("⚠️ توجه: سرور کلب خاموش است یا آدرس تانل منقضی شده است.")
    print("👉 لطفاً نوت‌بوک کلب را Run all کنید و لینک جدید trycloudflare را بفرستید.")
    sys.exit(1)
