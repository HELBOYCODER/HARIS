#!/usr/bin/env python3
import json, os, sys

import google.genai as genai

class GeminiTranslationMCP:
    def __init__(self):
        self.client = genai.Client(api_key=os.getenv("GEMINI_API_KEY"))

    def translate(self, src, tgt, text):
        prompt = f"Translate the following text from {src} to {tgt}. Return only the translated text: {text}"
        resp = self.client.models.generate_content(model="gemini-1.5-pro", prompt=prompt)
        return resp.text.strip()

    def detect(self, text):
        prompt = f"Detect the ISO language code of this text (e.g., en, fa, de). Return only the code: {text}"
        resp = self.client.models.generate_content(model="gemini-1.5-pro", prompt=prompt)
        return resp.text.strip()

    def call_tool(self, method, params):
        if method == "translate_text":
            return {"translation": self.translate(params["srcLang"], params["tgtLang"], params["text"])}
        if method == "detect_language":
            return {"language": self.detect(params["text"])}
        return {"error": "Unknown method"}

if __name__ == "__main__":
    print = sys.stdout.buffer.write
    for line in sys.stdin:
        try:
            req = json.loads(line)
            if req.get("method") == "initialize":
                print(json.dumps({"jsonrpc": "2.0", "id": req.get("id"), "result": {"capabilities": {"tools": {}}}}).encode() + b'\n')
                continue
            mid = req.get("id")
            params = req.get("params", {})
            try:
                res = GeminiTranslationMCP().call_tool(params.get("name"), params.get("arguments", {}))
                print(json.dumps({"jsonrpc": "2.0", "id": mid, "result": {"content": [{"type": "text", "text": json.dumps(res)}]}}).encode() + b'\n')
            except Exception as e:
                print(json.dumps({"jsonrpc": "2.0", "id": mid, "error": {"code": -32603, "message": str(e)}}).encode() + b'\n')
        except Exception as e:
            print(json.dumps({"error": str(e)}).encode() + b'\n')
            sys.exit(1)
