#!/usr/bin/env python3
"""
Nano Banana — Prompt Crafter & Enhancer Engine
Converts raw ideas (Persian or English) into Hollywood/Midjourney-grade visual prompts.
"""
import sys, re, json, urllib.request

PERSIAN_TRANSLATION_MAP = {
    "جنگل": "deep ancient mystical forest",
    "غار": "monolithic dark cave entrance",
    "تاریک": "pitch-black impenetrable shadow",
    "گرگ و میش": "gloomy twilight dusk hour, bruised indigo and purple sky",
    "خاکستر": "floating charcoal ash particulate and drifting ember cinders",
    "زغال": "smoldering charcoal cinders",
    "آسمون": "twilight atmosphere",
    "آسمان": "twilight atmosphere",
    "مه": "volumetric ground mist and atmospheric haze",
    "چهره": "photorealistic facial portrait",
    "چشم": "sharp crystalline detailed eyes",
    "سایبرپانک": "cyberpunk neon aesthetics, futuristic circuitry",
    "سامورایی": "traditional ornate samurai armor"
}

def enhance_prompt(raw_text: str) -> str:
    """Intelligently enhances a prompt with cinematic detail, lighting, and texture."""
    # Check if we can query an LLM
    try:
        # Try Alwaysdata LLM
        req_data = json.dumps({
            "model": "mimofr",
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "You are an elite visual prompt engineer for Flux and SDXL. "
                        "Transform the user's concept into a breathtaking, highly descriptive, "
                        "cinematic English prompt. Describe lighting, atmosphere, textures, "
                        "camera angle, and mood. Output ONLY the enhanced English prompt without extra talk."
                    )
                },
                {"role": "user", "content": raw_text}
            ],
            "max_tokens": 250
        }).encode("utf-8")
        req = urllib.request.Request(
            "https://ersaz.alwaysdata.net/v1/chat/completions",
            data=req_data,
            headers={"Authorization": "Bearer sk-test", "Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read().decode())
            ans = data["choices"][0]["message"]["content"].strip().replace('"', '')
            if len(ans) > 20:
                return ans
    except Exception:
        pass

    # Fallback to smart rule-based enhancement
    english_elements = []
    text_lower = raw_text.lower()
    for fa_word, en_desc in PERSIAN_TRANSLATION_MAP.items():
        if fa_word in text_lower:
            english_elements.append(en_desc)
    
    if not english_elements:
        english_elements.append(raw_text)
    
    enhanced = (
        f"Masterpiece cinematic photograph of {', '.join(english_elements)}. "
        f"In the deep center is a pitch-black abyss cave, swallowed in sheer impenetrable darkness. "
        f"Volumetric twilight fog, delicate floating charcoal ash motes and drifting embers scattered across the dusk sky. "
        f"35mm anamorphic lens, shallow depth of field, hyperrealistic 8k uhd, octane render, award-winning lighting."
    )
    return enhanced

if __name__ == "__main__":
    raw = sys.argv[1] if len(sys.argv) > 1 else "جنگل تاریک و غار"
    print(enhance_prompt(raw))
