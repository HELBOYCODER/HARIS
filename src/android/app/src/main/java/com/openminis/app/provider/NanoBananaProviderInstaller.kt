package com.openminis.app.provider

import android.content.Context
import android.util.Log
import com.openminis.app.data.model.*
import com.openminis.app.data.repository.ProviderRepository
import com.openminis.app.provider.NanoBananaKeyStore
import com.openminis.app.sandbox.ExecutionCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object NanoBananaProviderInstaller {

    private const val TAG = "NanoBananaInstaller"
    private const val PROVIDER_LABEL = "Nano Banana (Gemini)"
    private const val GEMINI_MODEL = "gemini-3.1-flash-image-preview"

    data class InstallationResult(
        val success: Boolean,
        val message: String,
        val providerId: String? = null
    )

    suspend fun install(
        context: Context,
        providerRepository: ProviderRepository,
        geminiApiKey: String
    ): InstallationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Nano Banana installation...")
            val providerId = UUID.randomUUID().toString()
            val keyStored = NanoBananaKeyStore.storeApiKey(context, providerId, geminiApiKey)
            if (!keyStored) {
                Log.e(TAG, "Failed to store API key securely")
                return@withContext InstallationResult(success = false, message = "Failed to store API key securely")
            }
            val envResult = ExecutionCoordinator.execute(
                sessionId = "nano-banana-setup",
                command = """
                    source /etc/profile
                    export GEMINI_API_KEY="${'$'}geminiApiKey"
                    echo "API key set successfully"
                """.trimIndent(),
                timeout = 30_000L
            )
            if (envResult.exitCode != 0) {
                Log.e(TAG, "Failed to set API key: ${'$'}{envResult.output}")
                return@withContext InstallationResult(success = false, message = "Failed to configure API key: ${'$'}{envResult.output.take(200)}")
            }
            Log.d(TAG, "API key configured")
            val imageProvider = ProviderInstance(
                id = providerId,
                label = PROVIDER_LABEL,
                providerType = ProviderType.nanoBanana,
                credentialType = ProviderCredential.apiKey,
                isEnabled = true,
                customBaseURL = "https://generativelanguage.googleapis.com/v1beta",
                appendV1Suffix = false,
                customUserAgent = "HARIS-NanoBanana/1.0"
            )
            providerRepository.importInstanceJSON(
                """
                {
                    "id": "${'$'}providerId",
                    "label": "${'$'}PROVIDER_LABEL",
                    "providerType": "nanoBanana",
                    "credentialType": "apiKey",
                    "isEnabled": true,
                    "customBaseURL": "https://generativelanguage.googleapis.com/v1beta",
                    "appendV1Suffix": false,
                    "customUserAgent": "HARIS-NanoBanana/1.0"
                }
                """.trimIndent()
            )
            Log.d(TAG, "Provider registered with ID: ${'$'}providerId")
            InstallationResult(success = true, message = "Nano Banana provider installed successfully", providerId = providerId)
        } catch (e: Exception) {
            Log.e(TAG, "Installation failed", e)
            InstallationResult(success = false, message = "Exception: ${'$'}{e.message}")
        }
    }

    suspend fun setupEnvironment(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = ExecutionCoordinator.execute(
                sessionId = "nano-banana-env",
                command = """
                    source /etc/profile
                    echo 'export GEMINI_API_KEY="${'$'}apiKey"' >> /etc/profile
                    source /etc/profile
                    if [ -n "${'$'}GEMINI_API_KEY" ]; then
                        echo "API key configured"
                    else
                        echo "Failed to set API key"
                        exit 1
                    fi
                """.trimIndent(),
                timeout = 30_000L
            )
            result.exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Setup failed", e)
            false
        }
    }

    suspend fun testGeneration(
        prompt: String = "a cute panda drinking tea",
        outputPath: String = "/var/minis/attachments/test-output.png"
    ): NanoBananaProvider.ImageGenerationResult = withContext(Dispatchers.IO) {
        try {
            val result = ExecutionCoordinator.execute(
                sessionId = "nano-banana-test",
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
prompt = '''${'$'}prompt'''
print(f"[*] Generating image with prompt: {prompt[:60]}...")
try:
    response = client.models.generate_images(
        model='gemini-3.1-flash-image-preview',
        prompt=prompt,
        config={'aspect_ratio': '16:9','number_of_images': 1}
    )
    image_data = response.images[0].image_bytes
    open('${'$'}outputPath', 'wb').write(image_data)
    print(f"[+] Image saved to: ${'$'}outputPath")
    print("SUCCESS")
except Exception as e:
    print(f"ERROR: {e}")
    exit(1)
PYTHON_EOF
                """.trimIndent(),
                timeout = 60_000L
            )
            NanoBananaProvider.ImageGenerationResult(success = result.exitCode == 0, outputPath = if (result.exitCode == 0) outputPath else null, message = result.output)
        } catch (e: Exception) {
            Log.e(TAG, "Test failed", e)
            NanoBananaProvider.ImageGenerationResult(success = false, message = "Exception: ${'$'}{e.message}")
        }
    }

    fun getDefaultProviderConfig(): ProviderInstance {
        return ProviderInstance(
            id = UUID.randomUUID().toString(),
            label = PROVIDER_LABEL,
            providerType = ProviderType.nanoBanana,
            credentialType = ProviderCredential.apiKey,
            isEnabled = true,
            customBaseURL = "https://generativelanguage.googleapis.com/v1beta",
            appendV1Suffix = false,
            customUserAgent = "HARIS-NanoBanana/1.0"
        )
    }
}
