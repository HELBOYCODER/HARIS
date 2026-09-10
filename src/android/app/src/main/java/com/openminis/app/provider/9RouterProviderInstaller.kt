package com.openminis.app.provider

import android.content.Context
import android.util.Log
import com.openminis.app.data.model.*
import com.openminis.app.data.repository.ProviderRepository
import com.openminis.app.sandbox.ExecutionCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Handles installation and configuration of 9Router as a local provider.
 * 
 * This utility:
 * 1. Runs the 9Router setup script in the sandbox
 * 2. Registers 9Router as a provider in the app database
 * 3. Configures free model access (OpenCode, etc.)
 */
object RouterProviderInstaller {

    private const val TAG = "9RouterInstaller"
    private const val PROVIDER_LABEL = "9Router Local"
    private const val PROVIDER_TYPE = "9router"
    private const val DEFAULT_PORT = 20128

    data class InstallationResult(
        val success: Boolean,
        val message: String,
        val providerId: String? = null
    )

    /**
     * Installs 9Router and registers it as a provider.
     */
    suspend fun install(
        context: Context,
        providerRepository: ProviderRepository
    ): InstallationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting 9Router installation...")

            // Step 1: Run self-contained setup command in sandbox
            val session = "9router-setup"
            val setupResult = ExecutionCoordinator.execute(
                sessionId = session,
                command = """
                    mkdir -p /data/9router /root/.9router
                    if ! command -v node >/dev/null 2>&1; then
                        apk add --no-cache nodejs npm curl
                    fi
                    if ! command -v 9router >/dev/null 2>&1; then
                        npm install -g 9router@latest
                    fi
                    pkill -f "9router" || true
                    nohup 9router --port $DEFAULT_PORT --data-dir /data/9router > /data/9router/server.log 2>&1 &
                    sleep 2
                    echo "ok"
                """.trimIndent(),
                timeout = 300_000L // 5 minutes
            )

            if (setupResult.exitCode != 0) {
                Log.e(TAG, "Setup script failed: ${setupResult.output}")
                return@withContext InstallationResult(
                    success = false,
                    message = "Setup failed: ${setupResult.output.take(200)}"
                )
            }

            Log.d(TAG, "Setup successful: ${setupResult.output}")

            // Step 2: Register 9Router as a provider
            val providerId = UUID.randomUUID().toString()
            val instance = ProviderInstance(
                id = providerId,
                label = PROVIDER_LABEL,
                providerType = ProviderType.openAI, // 9Router is OpenAI-compatible
                credentialType = ProviderCredential.apiKey,
                isEnabled = true,
                customBaseURL = "http://127.0.0.1:$DEFAULT_PORT",
                appendV1Suffix = false
            )

            // Save to repository
            providerRepository.importInstanceJSON(
                """
                {
                    "id": "$providerId",
                    "label": "$PROVIDER_LABEL",
                    "providerType": "openAI",
                    "credentialType": "apiKey",
                    "isEnabled": true,
                    "customBaseURL": "http://127.0.0.1:$DEFAULT_PORT",
                    "appendV1Suffix": false
                }
                """.trimIndent()
            )

            // Pre-seed 100% free models from Antigravity/OpenCode into 9Router
            val freeModels = listOf(
                LLMModel(id = "oc/mimo-v2.5-free", displayName = "MiMo v2.5 (Free)", provider = "9router"),
                LLMModel(id = "oc/ling-3.0-flash-fin-free", displayName = "Ling 3.0 Flash (Free)", provider = "9router"),
                LLMModel(id = "oc/nemotron-3.5-lightning-free", displayName = "Nemotron 3.5 Lightning (Free)", provider = "9router"),
                LLMModel(id = "oc/big-pickle", displayName = "Big Pickle (Free)", provider = "9router")
            )
            providerRepository.replaceEntries(providerId, freeModels)

            Log.d(TAG, "Provider registered with ID: $providerId and ${freeModels.size} free models")

            // Step 3: Verify health
            val healthResult = ExecutionCoordinator.execute(
                sessionId = "9router-health",
                command = "curl -s http://127.0.0.1:$DEFAULT_PORT/api/health",
                timeout = 10_000L
            )

            if (healthResult.exitCode != 0 || !healthResult.output.contains("ok")) {
                Log.w(TAG, "Health check not fully successful: ${healthResult.output}")
            }

            InstallationResult(
                success = true,
                message = "9Router installed successfully on port $DEFAULT_PORT",
                providerId = providerId
            )

        } catch (e: Exception) {
            Log.e(TAG, "Installation failed", e)
            InstallationResult(
                success = false,
                message = "Exception: ${e.message}"
            )
        }
    }

    /**
     * Checks if 9Router is already installed and running.
     */
    suspend fun isRunning(): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = ExecutionCoordinator.execute(
                sessionId = "9router-check",
                command = "curl -s --connect-timeout 2 http://127.0.0.1:$DEFAULT_PORT/api/health",
                timeout = 5_000L
            )
            result.exitCode == 0 && result.output.contains("ok")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets the default provider configuration for 9Router.
     */
    fun getDefaultProviderConfig(): ProviderInstance {
        return ProviderInstance(
            id = UUID.randomUUID().toString(),
            label = PROVIDER_LABEL,
            providerType = ProviderType.openAI,
            credentialType = ProviderCredential.apiKey,
            isEnabled = true,
            customBaseURL = "http://127.0.0.1:$DEFAULT_PORT",
            appendV1Suffix = false
        )
    }
}
