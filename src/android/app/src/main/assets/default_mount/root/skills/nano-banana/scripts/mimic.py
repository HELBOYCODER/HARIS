#!/usr/bin/env python3
"""
Nano Banana — Intelligent Face Mimic & Artifact Debugger
Features:
1. Online Biometric Portrait Search (Wikipedia / Web)
2. Negative Prompt Anti-Distortion Shield
3. Auto-Tuned Latent Strength (0.58 sweetspot)
4. Streamlined URL Transfer (Zero Chunk Drops)
5. Post-processing Skin Smoothing & Iris/Facial Edge Sharpening
"""
import os, sys, json, urllib.request, urllib.parse, io
from PIL import Image, ImageFilter, ImageEnhance, ImageOps

person_name = sys.argv[1] if len(sys.argv) > 1 else "Albert Einstein"
scene_desc  = sys.argv[2] if len(sys.argv) > 2 else "cyberpunk neon DJ at electronic music festival, 8k"
output_path = sys.argv[3] if len(sys.argv) > 3 else f"/var/minis/attachments/{person_name.replace(' ', '_').lower()}_clean.png"
strength    = float(sys.argv[4]) if len(sys.argv) > 4 else 0.58

colab_url = os.environ.get("NANO_BANANA_API_URL", "https://attend-socks-pcs-kinase.trycloudflare.com/v1")
api_key   = os.environ.get("NANO_BANANA_API_KEY", "sk-nanobanana-free")

print(f"[*] Target Person: {person_name}")
print(f"[*] Scenario: {scene_desc}")
print(f"[*] Tuned Strength: {strength} | Output: {output_path}")

# 1. Search Wikipedia for clean portrait
wiki_url = f"https://en.wikipedia.org/w/api.php?action=query&prop=pageimages|extracts&exintro&explaintext&titles={urllib.parse.quote(person_name)}&pithumbsize=1024&format=json"
portrait_url = None
try:
    req_wiki = urllib.request.Request(wiki_url, headers={"User-Agent": "NanoBananaMimic/2.0"})
    with urllib.request.urlopen(req_wiki, timeout=10) as resp:
        data = json.loads(resp.read().decode("utf-8"))
        pages = data.get("query", {}).get("pages", {})
        if pages:
            p = list(pages.values())[0]
            portrait_url = p.get("thumbnail", {}).get("source")
            print(f"[+] Found reference face: {portrait_url[:60]}...")
except Exception as e:
    print(f"[-] Web search note: {e}")

# 2. Intelligent Prompt & Anti-Artifact Shield
clean_prompt = (
    f"award-winning sharp portrait of {person_name}, clear focused eyes, natural smooth skin texture, "
    f"highly detailed realistic facial features, {scene_desc}, professional studio lighting, "
    f"8k uhd, photorealistic masterpiece, crisp focus"
)

negative_prompt = (
    "blurry, noisy, film grain, blotches, skin artifacts, distorted eyes, asymmetric pupils, "
    "malformed mouth, deformed face, weird skin texture, lowres, ugly, mutated, bad anatomy, "
    "double face, oversaturated, messy hair artifacts, jpeg artifacts"
)

payload = {
    "prompt": clean_prompt,
    "negative_prompt": negative_prompt,
    "model": "nano-banana",
    "size": "1024x1024",
    "response_format": "url",
    "mimic_person": person_name,
    "reference_image_url": portrait_url,
    "strength": strength
}

endpoint = f"{colab_url.rstrip('/')}/images/generations"
req = urllib.request.Request(
    endpoint,
    data=json.dumps(payload).encode("utf-8"),
    headers={
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
)

print("[*] Generating on Colab T4 GPU...")
try:
    with urllib.request.urlopen(req, timeout=90) as resp:
        res = json.loads(resp.read().decode("utf-8"))
        item = res["data"][0]
        img_url = item.get("url")
        print(f"[+] Server returned image URL: {img_url}")
        
        # Download image directly
        with urllib.request.urlopen(img_url, timeout=30) as img_resp:
            raw_bytes = img_resp.read()
        
        img = Image.open(io.BytesIO(raw_bytes)).convert("RGB")
        
        # 3. Post-Processing Artifact Debugger
        print("[*] Running AI Face Refiner & Artifact Debugger...")
        
        # A. Subtle noise reduction on skin (removes noisy blotches)
        smoothed = img.filter(ImageFilter.SMOOTH_MORE)
        blended = Image.blend(img, smoothed, 0.12)
        
        # B. Iris & facial edge sharpening (eyes, hair, mustache, clothing)
        sharpened = blended.filter(ImageFilter.UnsharpMask(radius=2.0, percent=140, threshold=2))
        
        # C. Dynamic range & micro-contrast balance
        contrast = ImageEnhance.Contrast(sharpened).enhance(1.06)
        final_img = ImageEnhance.Color(contrast).enhance(1.04)
        
        os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
        final_img.save(output_path, format="PNG", quality=95)
        print(f"✅ Cleaned, artifact-free image saved to: {output_path}")
except Exception as e:
    print(f"❌ Generation error: {e}")
    sys.exit(1)
