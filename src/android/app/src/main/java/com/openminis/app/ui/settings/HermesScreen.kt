package com.openminis.app.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.haris.soul.SoulRepository
import com.openminis.app.haris.soul.db.BoardTaskEntity
import com.openminis.app.haris.soul.db.HermesSkillEntity
import com.openminis.app.haris.soul.db.MemoryEntryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HermesScreen(
    onBack: () -> Unit,
    onOpenSkillDetail: (String) -> Unit = {},
    onOpenTerminal: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { SoulRepository.get(context) }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "امکانات و مهارت‌ها" to Icons.Outlined.Extension,
        "دیوان وظایف" to Icons.Outlined.ViewKanban,
        "دفتر حافظه" to Icons.Outlined.Psychology,
        "روح هل‌بوی" to Icons.Outlined.AutoAwesome,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "هرمس (Hermes System)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                        Text(
                            "مرکز مدیریت امکانات، ایجنت‌ها، دیوان وظایف و روح",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                tabs.forEachIndexed { index, (label, icon) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label, fontSize = 13.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }

            when (selectedTab) {
                0 -> HermesSkillsTab(repo = repo, onOpenSkillDetail = onOpenSkillDetail, onOpenTerminal = onOpenTerminal)
                1 -> HermesBoardTab(repo = repo)
                2 -> HermesMemoryTab(repo = repo)
                3 -> HelboySoulTab(onOpenTerminal = onOpenTerminal)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. SKILLS & CAPABILITIES TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HermesSkillsTab(
    repo: SoulRepository,
    onOpenSkillDetail: (String) -> Unit,
    onOpenTerminal: (String) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val skills by repo.observeSkills().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(skills, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) skills else skills.filter { it.name.lowercase().contains(q) }
    }

    if (showAddDialog) {
        AddHermesCapabilityDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, desc, instructions ->
                scope.launch {
                    repo.createSkill(name, desc, instructions)
                    Toast.makeText(context, "مهارت \"$name\" با موفقیت اضافه شد", Toast.LENGTH_SHORT).show()
                    showAddDialog = false
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Banner card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Extension,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "سلاح‌ها و مهارت‌های هرمس",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "هر امکانی که به هرمس اضافه کنی مستقیماً در دیتابیس هوشمند و محیط لینوکس سندباکس ثبت شده و ایجنت در چت به آن دسترسی آنی پیدا می‌کند.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("افزودن امکان / مهارت جدید به هرمس", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick presets
        item {
            Text(
                "دسته‌بندی‌های پیشنهادی هرمس:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetChip("اپراتور اجرایی", "operator") { showAddDialog = true }
                PresetChip("پژوهشگر عمیق", "researcher") { showAddDialog = true }
                PresetChip("برنامه‌ریز سیستم", "planner") { showAddDialog = true }
            }
        }

        // 9Router Local AI Gateway Quick Action
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF00BCD4).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF00BCD4), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("درگاه 9Router Local (پورت 20128)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("مدل‌های رایگان محلی MiMo, Ling, Nemotron", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { onOpenTerminal("9router-setup") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("ترمینال 9Router", fontSize = 11.sp)
                    }
                }
            }
        }

        // Search
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("جستجو در امکانات هرمس...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.ExtensionOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "هنوز امکانی اضافه نشده است",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { showAddDialog = true }) {
                            Text("اولین مهارت را الان اضافه کنید")
                        }
                    }
                }
            }
        } else {
            items(filtered, key = { it.name }) { skill ->
                HermesSkillCard(
                    skill = skill,
                    onDelete = {
                        scope.launch {
                            repo.skillDelete(skill.name)
                            Toast.makeText(context, "حذف شد", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClick = { onOpenSkillDetail(skill.name) }
                )
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, role: String, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        shape = RoundedCornerShape(20.dp),
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
}

@Composable
private fun HermesSkillCard(
    skill: HermesSkillEntity,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    skill.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "وضعیت: فعال · حجم: ${skill.sizeKb}KB",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ADD CAPABILITY DIALOG
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AddHermesCapabilityDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, instructions: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("operator") }
    var instructions by remember { mutableStateOf("") }

    val roles = listOf(
        "operator" to "اپراتور اجرایی (اجرای کد و ابزارها)",
        "researcher" to "پژوهشگر (جستجو و استخراج داده)",
        "planner" to "برنامه‌ریز (تحلیل ساختار و نقشه)",
        "reviewer" to "بازبین و ممیز کیفیت",
        "custom" to "سفارشی / ابزار عمومی",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("افزودن امکان / مهارت به هرمس", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("شناسه مهارت (انگلیسی کوتاه)") },
                    placeholder = { Text("مثال: web-researcher") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات فارسی مهارت") },
                    placeholder = { Text("این مهارت چه کاری برای شما انجام می‌دهد؟") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("دستورالعمل‌ها و پرامپت هرمس") },
                    placeholder = { Text("گام‌ها، ابزارهای لازم، و نحوه انجام تسک...") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, description, instructions)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("ذخیره و فعال‌سازی")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. DIWAN / BOARD TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HermesBoardTab(repo: SoulRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val tasks by repo.observeBoard().collectAsState(initial = emptyList())
    var selectedStatus by remember { mutableStateOf("all") }
    var showNewTaskDialog by remember { mutableStateOf(false) }

    val filteredTasks = remember(tasks, selectedStatus) {
        if (selectedStatus == "all") tasks else tasks.filter { it.status == selectedStatus }
    }

    if (showNewTaskDialog) {
        NewTaskDialog(
            onDismiss = { showNewTaskDialog = false },
            onSave = { title, role, brief ->
                scope.launch {
                    val id = "task-${System.currentTimeMillis()}"
                    repo.boardUpsert(id, title, role, "todo", brief)
                    Toast.makeText(context, "تسک به دیوان هرمس اضافه شد", Toast.LENGTH_SHORT).show()
                    showNewTaskDialog = false
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Header action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("دیوان وظایف و ساب‌ایجنت‌ها", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Button(
                    onClick = { showNewTaskDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تفویض کار جدید", fontSize = 12.sp)
                }
            }
        }

        item {
            // Status filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(selected = selectedStatus == "all", onClick = { selectedStatus = "all" }, label = { Text("همه (${tasks.size})", fontSize = 11.sp) })
                FilterChip(selected = selectedStatus == "todo", onClick = { selectedStatus = "todo" }, label = { Text("در انتظار", fontSize = 11.sp) })
                FilterChip(selected = selectedStatus == "in_progress", onClick = { selectedStatus = "in_progress" }, label = { Text("در حال انجام", fontSize = 11.sp) })
                FilterChip(selected = selectedStatus == "done", onClick = { selectedStatus = "done" }, label = { Text("انجام شد", fontSize = 11.sp) })
            }
        }

        if (filteredTasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("هیچ تسکی در این وضعیت وجود ندارد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(filteredTasks, key = { it.id }) { task ->
                HermesTaskCard(
                    task = task,
                    onMove = { nextStatus ->
                        scope.launch { repo.boardMove(task.id, nextStatus) }
                    },
                    onDelete = {
                        scope.launch { repo.boardDelete(task.id) }
                    }
                )
            }
        }
    }
}

@Composable
private fun HermesTaskCard(
    task: BoardTaskEntity,
    onMove: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val statusColor = when (task.status) {
        "done" -> Color(0xFF4CAF50)
        "in_progress" -> Color(0xFF2196F3)
        "blocked" -> Color(0xFFF44336)
        else -> Color(0xFFFF9800)
    }

    val statusLabel = when (task.status) {
        "done" -> "تکمیل شد"
        "in_progress" -> "در حال انجام"
        "blocked" -> "مسدود"
        else -> "در انتظار"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(20))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (!task.blocker.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    task.blocker,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "نقش: ${task.role}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (task.status != "in_progress" && task.status != "done") {
                        IconButton(onClick = { onMove("in_progress") }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(16.dp))
                        }
                    }
                    if (task.status != "done") {
                        IconButton(onClick = { onMove("done") }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Check, contentDescription = "Done", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NewTaskDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, role: String, brief: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("operator") }
    var brief by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تفویض وظیفه جدید به هرمس", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان تسک") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("ساب‌ایجنت مسئول (operator, researcher, planner)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = brief,
                    onValueChange = { brief = it },
                    label = { Text("توضیحات و بریف کار") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onSave(title, role, brief) }, enabled = title.isNotBlank()) {
                Text("ثبت در دیوان")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. MEMORY & LEDGER TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HermesMemoryTab(repo: SoulRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val memoryEntries by repo.observeMemory().collectAsState(initial = emptyList())
    var keywordQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = remember(memoryEntries, keywordQuery) {
        val q = keywordQuery.trim().lowercase()
        if (q.isEmpty()) memoryEntries else memoryEntries.filter { it.content.lowercase().contains(q) }
    }

    if (showAddDialog) {
        var newKind by remember { mutableStateOf("MEMORY") }
        var newContent by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("ثبت یادداشت در حافظه هرمس", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = newKind == "MEMORY", onClick = { newKind = "MEMORY" }, label = { Text("پروژه") })
                        FilterChip(selected = newKind == "USER", onClick = { newKind = "USER" }, label = { Text("کاربر") })
                        FilterChip(selected = newKind == "LEDGER", onClick = { newKind = "LEDGER" }, label = { Text("تصمیم") })
                    }
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("محتوا") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newContent.isNotBlank()) {
                            scope.launch {
                                repo.memoryWrite(newKind, newContent)
                                Toast.makeText(context, "ثبت شد", Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                            }
                        }
                    },
                    enabled = newContent.isNotBlank()
                ) { Text("ثبت") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("انصراف") } }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("دفتر تصمیمات و حافظه هرمس", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Button(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ثبت یادداشت", fontSize = 12.sp)
                }
            }
        }

        item {
            OutlinedTextField(
                value = keywordQuery,
                onValueChange = { keywordQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("جستجو در حافظه و تصمیمات...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("حافظه خالی است", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(filtered, key = { it.rowId }) { entry ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(entry.kind, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                val d = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(entry.createdAt))
                                Text(d, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(entry.content, fontSize = 13.sp)
                        }
                        IconButton(onClick = { scope.launch { repo.memoryDelete(entry.rowId) } }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. HELBOY SOUL TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HelboySoulTab(onOpenTerminal: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("haris_soul_prefs", android.content.Context.MODE_PRIVATE) }
    var isSoulEnabled by remember { mutableStateOf(prefs.getBoolean("helboy_soul_enabled", true)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("روح هل‌بوی (Helboy Soul)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("PONYTAIL + ارواح هرمس", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isSoulEnabled,
                        onCheckedChange = {
                            isSoulEnabled = it
                            prefs.edit().putBoolean("helboy_soul_enabled", it).apply()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "با فعال بودن این گزینه، مدل در چت با لحن صریح، بدون کدهای اضافی، اولویت‌بندی با روش PONYTAIL و قابلیت‌های کامل هرمس عمل می‌کند.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("قوانین فعال در این حالت:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("۱. اصل YAGNI — کدی که نیاز نیست نوشته نمی‌شود.", fontSize = 12.sp)
                Text("۲. استفاده حداکثری از ابزارهای بومی و کتابخانه‌های استاندارد.", fontSize = 12.sp)
                Text("۳. پشتیبانی از زبان فارسی و راست‌چین در تمام خروجی‌ها.", fontSize = 12.sp)
                Text("۴. تفویض وظایف چندمرحله‌ای به ساب‌ایجنت‌های هرمس.", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onOpenTerminal("omh update && omh list") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("باز کردن ترمینال هرمس (OMH CLI)")
        }
    }
}
