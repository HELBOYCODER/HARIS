package com.openminis.app.provider

import android.content.Context
import android.util.Log
import com.openminis.app.data.model.ProviderInstance
import com.openminis.app.sandbox.ExecutionCoordinator
import com.openminis.app.provider.NanoBananaKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Nano Banana Provider - Handles Google Gemini image generation
 * 
 * Supports:
 * - Text-to-image generation
 * - Image-to-image editing  
 * - Multiple aspect ratios (1:1, 16:9, 9:16, 4:3, 3:4)
 * - 1K and 2K resolution options
 */
class NanoBananaProvider(
    private val context: Context,
    private val instance: ProviderInstance
) {
    companion object {
        private const val TAG = "NanoBananaProvider"
        private const val DEFAULT_OUTPUT_DIR = "/var/minis/attachments"
        
        // Model names
        const val MODEL_NANO_BANANA_2 = "gemini-3.1-flash-image-preview"
        const val MODEL_NANO_BANANA_PRO = "gemini-3-pro-image-preview"
        const val MODEL_NANO_BANANA = "gemini-2.5-flash-image"
    }

    /**
     * Generates an image from text prompt.
     */
    suspend fun generateImage(
        prompt: String,
        outputPath: String = "$DEFAULT_OUTPUT_DIR/generated_${System.currentTimeMillis()}.png",
        aspectRatio: String = "16:9",
        resolution: String = "2K"
    ): ImageGenerationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating image: ${prompt.take(60)}...")

            // Generate the image using Gemini API
            val currentKey = NanoBananaKeyStore.getCurrentKey(instance.id) ?: ""
        if (currentKey.isBlank()) {
            Log.e(TAG, "No API key available for provider ${instance.id}")
            return@withContext ImageGenerationResult(success = false, message = "No API key")
        }
        val result = ExecutionCoordinator.execute(
                sessionId = "nano-banana-gen-${System.currentTimeMillis()}",
                command = """
                    export GEMINI_API_KEY=\"$currentKey\"\n                    source /etc/profile
                    python3 << 'PYTHON_EOF'
import os, sys, subprocess
try:
    import google.genai as genai
    from PIL import Image
    import io
except ImportError:
    subprocess.check_call([sys.executable, "-m", "pip", "install", "google-genai", "pillow", "--break-system-packages"])
    import google.genai as genai
    from PIL import Image
    import io

# Initialize client
api_key = os.environ.get('GEMINI_API_KEY')
if not api_key:
    print("ERROR: GEMINI_API_KEY not set")
    exit(1)

client = genai.Client(api_key=api_key)

prompt = '''${'$'}prompt'''
output_path = '''${'$'}outputPath'''
aspect_ratio = '''${'$'}aspectRatio'''
resolution = '''${'$'}resolution'''

print(f"[*] Generating image...")
print("[*] Model: gemini-3.1-flash-image-preview")
print(f"[*] Prompt: {prompt[:60]}...")
print(f"[*] Aspect: {aspect_ratio}")
print(f"[*] Resolution: {resolution}")

try:
    # Convert resolution to size
    size = "1K" if resolution == "1K" else "2K"
    
    response = client.models.generate_images(
        model='gemini-3.1-flash-image-preview',
        prompt=prompt,
        config={
            'aspect_ratio': aspect_ratio,
            'number_of_images': 1,
            'output_size': size,
        }
    )
    
    # Save the generated image
    image_bytes = response.images[0].image_bytes
    with open(output_path, 'wb') as f:
        f.write(image_bytes)
    
    print(f"[+] Image saved to: {output_path}")
    print("SUCCESS")
    
except Exception as e:
    print(f"ERROR: {e}")
    exit(1)
PYTHON_EOF
                """.trimIndent(),
                timeout = 120_000L // 2 minutes for image generation
            )

            var success = result.exitCode == 0 && result.output.contains("SUCCESS")
            var output = result.output
            // If rate limit hit, rotate key and retry once
            if (!success && (output.contains("Rate limit") || output.contains("quota_exceeded") || output.contains("429"))) {
                Log.w(TAG, "Rate limit hit, rotating Gemini API key")
                NanoBananaKeyStore.rotateKey(instance.id)
                // Re-run with next key
                val newKey = NanoBananaKeyStore.getCurrentKey(instance.id) ?: ""
                if (newKey.isNotBlank()) {
                    val retryResult = ExecutionCoordinator.execute(
                        sessionId = "nano-banana-gen-retry-${System.currentTimeMillis()}",
                        command = """
                            export GEMINI_API_KEY=\"$newKey\"\n                            source /etc/profile
                            python3 << 'PYTHON_EOF'
import os
import google.genai as genai
from PIL import Image
import io

api_key = os.environ.get('GEMINI_API_KEY')
if not api_key:
    print("ERROR: GEMINI_API_KEY not set")
    exit(1)

client = genai.Client(api_key=api_key)

prompt = '''${'$'}prompt'''
output_path = '''${'$'}outputPath'''
aspect_ratio = '''${'$'}aspectRatio'''
resolution = '''${'$'}resolution'''

print(f"[*] Generating image (retry)...")
print(f"[*] Model: gemini-3.1-flash-image-preview")
print(f"[*] Prompt: {prompt[:60]}...")
print(f"[*] Aspect: {aspect_ratio}")
print(f"[*] Resolution: {resolution}")

try:
    size = "1K" if resolution == "1K" else "2K"
    response = client.models.generate_images(
        model='gemini-3.1-flash-image-preview',
        prompt=prompt,
        config={
            'aspect_ratio': aspect_ratio,
            'number_of_images': 1,
            'output_size': size,
        }
    )
    image_bytes = response.images[0].image_bytes
    with open(output_path, 'wb') as f:
        f.write(image_bytes)
    print(f"[+] Image saved to: {output_path}")
    print("SUCCESS")
except Exception as e:
    print(f"ERROR: {e}")
    exit(1)
PYTHON_EOF
                        """.trimIndent(),
                        timeout = 120_000L
                    )
                    success = retryResult.exitCode == 0 && retryResult.output.contains("SUCCESS")
                    output = retryResult.output
                }
            }

            ImageGenerationResult(
                success = success,
                outputPath = if (success) outputPath else null,
                message = output
            )

        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            ImageGenerationResult(
                success = false,
                message = "Exception: ${e.message}"
            )
        }
    }

    /**
     * Edits an existing image with instructions.
     */
    suspend fun editImage(
        inputPath: String,
        instruction: String,
        outputPath: String = "$DEFAULT_OUTPUT_DIR/edited_${System.currentTimeMillis()}.png"
    ): ImageGenerationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Editing image: ${'$'}inputPath")

            val result = ExecutionCoordinator.execute(
                sessionId = "nano-banana-edit-${System.currentTimeMillis()}",
                command = """
                    source /etc/profile
                    python3 << 'PYTHON_EOF'
import os
import google.genai as genai

api_key = os.environ.get('GEMINI_API_KEY')
if not api_key:
    print("ERROR: GEMINI_API_KEY not set")
    exit(1)

client = genai.Client(api_key=api_key)

input_path = '''${'$'}inputPath'''
instruction = '''${'$'}instruction'''
output_path = '''${'$'}outputPath'''

print(f"[*] Editing image: {input_path}")
print(f"[*] Instruction: {instruction[:60]}...")

try:
    with open(input_path, 'rb') as f:
        image_data = f.read()

    response = client.models.edit_image(
        model='gemini-3.1-flash-image-preview',
        image=image_data,
        instruction=instruction,
        config={
            'aspect_ratio': '16:9',
            'number_of_images': 1,
        }
    )

    image_bytes = response.images[0].image_bytes
    with open(output_path, 'wb') as f:
        f.write(image_bytes)

    print(f"[+] Edited image saved to: {output_path}")
    print("SUCCESS")

except Exception as e:
    print(f"ERROR: {e}")
    exit(1)
PYTHON_EOF
                """.trimIndent(),
                timeout = 120_000L
            )

            var success = result.exitCode == 0 && result.output.contains("SUCCESS")
            var output = result.output
            // If rate limit hit, rotate key and retry once
            if (!success && (output.contains("Rate limit") || output.contains("quota_exceeded") || output.contains("429"))) {
                Log.w(TAG, "Rate limit hit, rotating Gemini API key")
                NanoBananaKeyStore.rotateKey(instance.id)
                val newKey = NanoBananaKeyStore.getCurrentKey(instance.id) ?: ""
                if (newKey.isNotBlank()) {
                    val retryResult = ExecutionCoordinator.execute(
                        sessionId = "nano-banana-edit-retry-${System.currentTimeMillis()}",
                        command = """
                            export GEMINI_API_KEY=\"$newKey\"\n                            source /etc/profile
                            python3 << 'PYTHON_EOF'
import os
import google.genai as genai

api_key = os.environ.get('GEMINI_API_KEY')
if not api_key:
    print("ERROR: GEMINI_API_KEY not set")
    exit(1)

client = genai.Client(api_key=api_key)

input_path = '''${'$'}inputPath'''
instruction = '''${'$'}instruction'''
output_path = '''${'$'}outputPath'''

try:
    with open(input_path, 'rb') as f:
        image_data = f.read()

    response = client.models.edit_image(
        model='gemini-3.1-flash-image-preview',
        image=image_data,
        instruction=instruction,
        config={
            'aspect_ratio': '16:9',
            'number_of_images': 1,
        }
    )

    image_bytes = response.images[0].image_bytes
    with open(output_path, 'wb') as f:
        f.write(image_bytes)
    print(f"[+] Edited image saved to: {output_path}")
    print("SUCCESS")
except Exception as e:
    print(f"ERROR: {e}")
    exit(1)
PYTHON_EOF
                        """.trimIndent(),
                        timeout = 120_000L
                    )
                    success = retryResult.exitCode == 0 && retryResult.output.contains("SUCCESS")
                    output = retryResult.output
                }
            }

            ImageGenerationResult(
                success = success,
                outputPath = if (success) outputPath else null,
                message = output
            )

        } catch (e: Exception) {
            Log.e(TAG, "Image editing failed", e)
            ImageGenerationResult(
                success = false,
                message = "Exception: ${e.message}"
            )
        }
    }

    /**
     * Verifies the provider configuration.
     */
    suspend fun verifyConfiguration(): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = ExecutionCoordinator.execute(
                sessionId = "nano-banana-verify",
                command = """
                    source /etc/profile
                    if [ -n "${'$'}GEMINI_API_KEY" ]; then
                        echo "API_KEY_SET"
                    else
                        echo "API_KEY_MISSING"
                        exit 1
                    fi
                """.trimIndent(),
                timeout = 10_000L
            )
            result.exitCode == 0 && result.output.contains("API_KEY_SET")
        } catch (e: Exception) {
            false
        }
    }

    data class ImageGenerationResult(
        val success: Boolean,
        val outputPath: String? = null,
        val message: String
    )
}
