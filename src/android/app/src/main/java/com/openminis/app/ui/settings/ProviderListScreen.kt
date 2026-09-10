package com.openminis.app.ui.settings

import android.net.Uri
import android.util.Log
import com.openminis.app.provider.RouterProviderInstaller
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import com.openminis.app.provider.NanoBananaKeyStore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material3.Surface
import androidx.compose.runtime.key
import sh.calvin.reorderable.ReorderableColumn
import com.openminis.app.data.model.ProviderInstance
import com.openminis.app.data.model.ProviderType
import com.openminis.app.data.repository.ProviderRepository
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import com.openminis.app.ui.components.MinisAlertDialog
import com.openminis.app.ui.components.SwipeRowAction
import com.openminis.app.ui.components.SwipeRowActions
import com.openminis.app.logging.AppLogger
import com.openminis.app.R

private const val TAG = "ProviderListScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderListScreen(
    providerRepository: ProviderRepository,
    onBack: () -> Unit,
    onAddProvider: () -> Unit,
    onProviderClick: (String) -> Unit,
    onVoiceServiceClick: (String) -> Unit = {},
) {
    val config by providerRepository.config.collectAsState()
    val instances = config.instances
    val groupedInstances = instances.groupBy { it.providerType }
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var show9RouterInstallDialog by remember { mutableStateOf(false) }
    var isInstalling9Router by remember { mutableStateOf(false) }
    var installResultMessage by remember { mutableStateOf<String?>(null) }
    var showNanoBananaDialog by remember { mutableStateOf(false) }
    var isInstallingNanoBanana by remember { mutableStateOf(false) }
    var nanoBananaApiKey by remember { mutableStateOf("") }
    // New state for adding additional API keys to an existing Nano Banana provider
    var showAddKeyDialog by remember { mutableStateOf(false) }
    var newApiKeyInput by remember { mutableStateOf("") }
    var selectedProviderId by remember { mutableStateOf<String?>(null) }
    // [T-android-swipe-row-actions] Pending swipe-delete target. Held here
    // rather than per-row so the confirmation survives the row being
    // recomposed/reordered underneath it.
    var instanceToDelete by remember {
        mutableStateOf<com.openminis.app.data.model.ProviderInstance?>(null)
    }

    // Show add-key dialog when requested
    if (showAddKeyDialog && selectedProviderId != null) {
        ShowAddKeyDialog(
            onDismiss = {
                showAddKeyDialog = false
                newApiKeyInput = ""
            },
            providerId = selectedProviderId,
            apiKeyInput = newApiKeyInput,
            onApiKeyChange = { newApiKeyInput = it },
            onSave = {
                val stored = NanoBananaKeyStore.storeApiKey(context, selectedProviderId!!, newApiKeyInput)
                if (stored) {
                    Log.d(TAG, "Added extra API key for provider $selectedProviderId")
                } else {
                    Log.e(TAG, "Failed to add API key for provider $selectedProviderId")
                }
                showAddKeyDialog = false
                newApiKeyInput = ""
            }
        )
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mime = context.contentResolver.getType(uri).orEmpty()
        val name = ProviderImportZip.queryDisplayName(context, uri).orEmpty()
        val looksLikeZip = mime == "application/zip" ||
            mime == "application/x-zip-compressed" ||
            name.lowercase().endsWith(".zip")
        try {
            if (looksLikeZip) {
                val toastFailed = context.getString(R.string.import_zip_extract_failed)
                val toastNoSupported = context.getString(R.string.import_zip_no_supported)
                ProviderImportZip.importFromZip(
                    context = context,
                    uri = uri,
                    onImportSingle = { jsonStr -> providerRepository.importInstanceJSON(jsonStr) },
                    onExtractFailed = { Toast.makeText(context, toastFailed, Toast.LENGTH_SHORT).show() },
                    onNoSupported = { Toast.makeText(context, toastNoSupported, Toast.LENGTH_SHORT).show() },
                    onSummary = { ok, total ->
                        val msg = context.getString(R.string.import_zip_summary, ok, total)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                )
            } else {
                val jsonStr = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                if (jsonStr != null) {
                    val label = providerRepository.importInstanceJSON(jsonStr)
                    if (label != null) {
                        Toast.makeText(context, "Imported provider \"$label\"", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Invalid provider configuration file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.provider_list_providers),
        onBack = onBack,
        actions = {
            // 9Router installation button
            IconButton(onClick = { show9RouterInstallDialog = true }) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = "Install 9Router",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            // Nano Banana installation button
            IconButton(onClick = { showNanoBananaDialog = true }) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = "Install Nano Banana",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.provider_list_add_provider))
            }
        },
    ) {
        // ─── HARIS Quick Integrations (9Router & Nano Banana) ─────────
        val has9Router = instances.any { it.label.contains("9Router") || (it.customBaseURL ?: "").contains("20128") }
        val hasNanoBanana = instances.any { it.providerType == ProviderType.nanoBanana || it.label.contains("Nano Banana") }
        val coroutineScope = rememberCoroutineScope()

        SettingsSection(
            header = "سرویس‌های لوکال و آماده (HARIS Built-ins)",
            footer = "راه‌اندازی فوری پرووایدرهای داخلی با یک کلیک بدون نیاز به ساخت دستی",
        ) {
            // 9Router Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF00BCD4).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color(0xFF00BCD4),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "9Router Local Gateway",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (has9Router) "متصل روی پورت 20128 • مدل‌های رایگان فعال" else "درگاه مدل‌های رایگان محلی (Port 20128)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    if (has9Router) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = "فعال",
                                color = Color(0xFF2E7D32),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                isInstalling9Router = true
                                coroutineScope.launch {
                                    val res = RouterProviderInstaller.install(context, providerRepository)
                                    isInstalling9Router = false
                                    installResultMessage = res.message
                                    Toast.makeText(context, if (res.success) "9Router با موفقیت فعال شد!" else "خطا: ${res.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isInstalling9Router,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(if (isInstalling9Router) "نصب..." else "اتصال")
                        }
                    }
                }
            }

            // Nano Banana Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFFFFB300).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nano Banana (Gemini)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (hasNanoBanana) "آماده تولید عکس با کیفیت 2K" else "تصویرساز با کلید جمینی و چرخش خودکار",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    if (hasNanoBanana) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = "فعال",
                                color = Color(0xFF2E7D32),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    } else {
                        Button(
                            onClick = { showNanoBananaDialog = true },
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("تنظیم")
                        }
                    }
                }
            }
        }

        if (instances.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.VpnKey,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                )
                Text(
                    text = stringResource(R.string.provider_list_no_providers_configured),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.provider_list_add_a_provider_to_get_started),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        } else {
            groupedInstances.forEach { (providerType, typeInstances) ->
                SettingsSection(header = providerType.displayName) {
                    // [T-android-provider-reorder] Long-press a row to drag it
                    // within its provider-type section (mirrors iOS
                    // ProviderInstancesView's .onMove).
                    //
                    // ReorderableColumn — not the LazyColumn variant used by
                    // ModelGroupsScreen — because this screen renders inside
                    // SettingsScaffold's verticalScroll Column, and the lazy
                    // variant needs a LazyListState. Converting the whole screen
                    // to a LazyColumn would churn the empty state and the Voice
                    // Services section for no user-visible gain.
                    //
                    // `localOrder` holds the live order during the drag so the
                    // rows follow the finger; it re-syncs whenever the repository
                    // emits (keyed on the ids) so an external change — an import,
                    // a delete — is not overwritten by a stale local copy.
                    var localOrder by remember(typeInstances.map { it.id }) {
                        mutableStateOf(typeInstances)
                    }
                    ReorderableColumn(
                        list = localOrder,
                        onSettle = { fromIndex, toIndex ->
                            localOrder = localOrder.toMutableList().apply {
                                add(toIndex, removeAt(fromIndex))
                            }
                            // Commit only THIS section's ids: reorderInstances
                            // keeps every unmentioned instance in its existing
                            // relative position, so other provider-type sections
                            // are untouched. This is the Android answer to the
                            // iOS index-mapping bug (246a8a8e) — there is no
                            // section-local→global index arithmetic to get wrong.
                            providerRepository.reorderInstances(localOrder.map { it.id })
                        },
                    ) { index, instance, isDragging ->
                        key(instance.id) {
                            val modelCount = providerRepository.visibleEntries(instance.id).size
                            val apiKey = providerRepository.loadApiKey(instance.id)
                            // Mirrors iOS `isConfigured` on ProviderInstancesView:
                            // for OAuth providers, having a manual bearer token OR
                            // a stored OAuth credential counts as "configured" — not
                            // just the presence of an API key. Without this, OAuth
                            // instances always show the gray dot even after a
                            // successful sign-in or manual token paste.
                            val isConfigured = if (instance.credentialType ==
                                com.openminis.app.data.model.ProviderCredential.oauth) {
                                val mgr = com.openminis.app.auth.OAuthManager.forInstance(context, instance)
                                mgr?.isAuthenticated() == true
                            } else {
                                // [T-empty-key-compat-endpoints] A keyless
                                // third-party compatible endpoint is
                                // configured-by-definition (mirrors iOS).
                                !apiKey.isNullOrBlank() || instance.allowsEmptyAPIKey
                            }
                            // Lift the dragged row above its neighbours so it
                            // reads as "picked up" (matches ModelGroupsScreen).
                            val elevation by animateDpAsState(
                                targetValue = if (isDragging) 4.dp else 0.dp,
                                label = "provider_drag_elevation",
                            )
                            Surface(
                                shadowElevation = elevation,
                                color = Color.Transparent,
                                modifier = Modifier.longPressDraggableHandle(),
                            ) {
                                // [T-android-swipe-row-actions] Swipe left for
                                // Edit / Delete. Edit reuses the same
                                // onProviderClick the tap already uses, and
                                // Delete routes through the SAME confirmation +
                                // removeInstance() the detail screen's "Delete
                                // Provider" button uses — no second delete path.
                                //
                                // Sits INSIDE longPressDraggableHandle, not
                                // around it: the handle needs to stay attached
                                // to the row the user presses, and the two
                                // gestures separate by axis (see SwipeRowActions).
                                SwipeRowActions(
                                    actions = listOf(
                                        SwipeRowAction(
                                            label = stringResource(R.string.common_edit),
                                            icon = Icons.Filled.Edit,
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            onClick = { onProviderClick(instance.id) },
                                        ),
                                        SwipeRowAction(
                                            label = stringResource(R.string.common_delete),
                                            icon = Icons.Filled.Delete,
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                            onClick = { instanceToDelete = instance },
                                        ),
                                    ),
                                ) {
                                    ProviderInstanceRow(
                                        instance = instance,
                                        modelCount = modelCount,
                                        apiKey = apiKey,
                                        isConfigured = isConfigured,
                                        onClick = { onProviderClick(instance.id) },
                                        onAddKeyClick = { id ->
                                            selectedProviderId = id
                                            showAddKeyDialog = true
                                        }
                                    )
                                }
                            }
                            if (index < localOrder.size - 1) {
                                val divider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 38.dp, end = 14.dp)
                                        .height(0.5.dp)
                                        .background(divider),
                                )
                            }
                        }
                    }
                }
            }
        }

        // [T-android-provider-voice] Voice Services: runtime shadow mirror of
        // every enabled instance that owns audio-modality models (mirrors iOS
        // ProviderInstancesView's Voice Services section). Rows are read-only
        // views onto the underlying instance — no stored entity.
        val shadows = remember(config) { providerRepository.shadowVoiceProviders() }
        if (shadows.isNotEmpty()) {
            SettingsSection(
                header = stringResource(R.string.voice_services_section),
                footer = if (providerRepository.hasFoldedShadowDuplicates()) {
                    stringResource(R.string.voice_services_duplicate_hint)
                } else {
                    null
                },
            ) {
                shadows.forEachIndexed { index, shadow ->
                    ShadowVoiceRow(
                        shadow = shadow,
                        onClick = { onVoiceServiceClick(shadow.instanceId) },
                    )
                    if (index < shadows.size - 1) {
                        val divider = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 38.dp, end = 14.dp)
                                .height(0.5.dp)
                                .background(divider),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }

    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMenu = false
                            onAddProvider()
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.provider_list_add_provider), style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMenu = false
                            importLauncher.launch(
                                arrayOf(
                                    "application/json",
                                    "application/zip",
                                    "application/x-zip-compressed",
                                ),
                            )
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.provider_list_import_provider), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    // [T-android-swipe-row-actions] Same dialog copy and same repository call as
    // ProviderDetailScreen's "Delete Provider" button, so swipe-delete and
    // detail-delete cannot drift apart.
    instanceToDelete?.let { target ->
        MinisAlertDialog(
            onDismissRequest = { instanceToDelete = null },
            title = stringResource(R.string.provider_detail_delete_provider),
            text = stringResource(
                R.string.provider_detail_delete_provider_confirm,
                target.label,
            ),
            confirmText = stringResource(R.string.common_delete),
            isDestructive = true,
            onConfirm = {
                providerRepository.removeInstance(target.id)
                AppLogger.info(
                    "ProviderList",
                    "Deleted provider instance ${target.id} (${target.label}) via swipe",
                )
                instanceToDelete = null
            },
        )
    }
}

@Composable
private fun ProviderInstanceRow(
    instance: ProviderInstance,
    modelCount: Int,
    apiKey: String?,
    isConfigured: Boolean,
    onClick: () -> Unit,
    onAddKeyClick: (String) -> Unit,
) {
    val isActive = isConfigured && instance.isEnabled

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isActive) Color(0xFF34C759) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                    shape = CircleShape,
                ),
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = instance.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.provider_list_api_key),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
                Text(
                    // [T-empty-key-compat-endpoints] Keyless compatible endpoint:
                    // say so instead of the alarming "No API key".
                    text = if (!apiKey.isNullOrBlank()) maskKey(apiKey)
                        else if (instance.allowsEmptyAPIKey) stringResource(R.string.provider_no_key_required)
                        else "No API key",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (modelCount > 0) {
                Text(
                    text = stringResource(R.string.provider_list_models_count, modelCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }

        if (!instance.isEnabled) {
            Text(
                text = stringResource(R.string.provider_list_disabled),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(50),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
        }

        // Add API key button for Nano Banana providers
        if (instance.providerType == ProviderType.nanoBanana) {
            IconButton(onClick = { onAddKeyClick(instance.id) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add API key",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun maskKey(key: String): String {
    if (key.length <= 8) return "****"
    return key.take(6) + "..." + key.takeLast(4)
}

/** One shadow Voice Service row: name + ASR/TTS model counts. */
@Composable
private fun ShadowVoiceRow(
    shadow: ProviderRepository.ShadowVoiceProvider,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.GraphicEq,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = shadow.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val parts = buildList {
                if (shadow.inputModels.isNotEmpty()) {
                    add(stringResource(R.string.voice_services_stt_count, shadow.inputModels.size))
                }
                if (shadow.outputModels.isNotEmpty()) {
                    add(stringResource(R.string.voice_services_tts_count, shadow.outputModels.size))
                }
            }
            if (parts.isNotEmpty()) {
                Text(
                    text = parts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
    }
}

// 9Router Installation Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Show9RouterInstallDialog(
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    isInstalling: Boolean,
    resultMessage: String?
) {
    if (true) { // This would normally be controlled by a state variable
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Install 9Router Local Gateway",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "9Router is a local AI gateway that provides free model access and unified provider management on port 20128.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(onClick = onInstall, modifier = Modifier.weight(1f), enabled = !isInstalling) {
                        Text(if (isInstalling) "Installing..." else "Install")
                    }
                }
            }
        }
    }
}

// extra key dialog
// (was nested, now top-level)
// New dialog to add an additional API key to an existing Nano Banana provider
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowAddKeyDialog(
        onDismiss: () -> Unit,
        providerId: String?,
        apiKeyInput: String,
        onApiKeyChange: (String) -> Unit,
        onSave: () -> Unit,
    ) {
        if (providerId == null) return
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Add Gemini API key",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = onApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("sk-...") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                        Text("Save")
                    }
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowNanoBananaDialog(
    onDismiss: () -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onInstall: () -> Unit,
    isInstalling: Boolean
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Nano Banana (Gemini Image API)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Generate and edit images using Google's Gemini image generation. Supports text-to-image, image editing, and multiple aspect ratios.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini 3.1 Flash Image (2K)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Text-to-image & image editing")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aspect ratio control (1:1, 16:9, etc.)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Gemini API Key",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("sk-...") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Get your key from: https://aistudio.google.com/apikey",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(onClick = onInstall, modifier = Modifier.weight(1f), enabled = !isInstalling && apiKey.isNotBlank()) {
                    Text(if (isInstalling) "Installing..." else "Install")
                }
            }
        }
    }
}