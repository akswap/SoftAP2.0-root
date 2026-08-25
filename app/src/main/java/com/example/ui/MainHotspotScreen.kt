package com.example.ui

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.HotspotProfile
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import android.content.Intent
import android.net.Uri
import com.example.ui.theme.SystemTerminalGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHotspotScreen(viewModel: HotspotViewModel) {
    val ssid by viewModel.ssid.collectAsState()
    val password by viewModel.password.collectAsState()
    val securityType by viewModel.securityType.collectAsState()
    val band2g by viewModel.band2g.collectAsState()
    val band5g by viewModel.band5g.collectAsState()
    val band6g by viewModel.band6g.collectAsState()
    val mloEnabled by viewModel.mloEnabled.collectAsState()
    val channelBandwidth by viewModel.channelBandwidth.collectAsState()
    val channel5g by viewModel.channel5g.collectAsState()
    val channel6g by viewModel.channel6g.collectAsState()
    val indoorAp6g by viewModel.indoorAp6g.collectAsState()
    val selectedRegion by viewModel.selectedRegion.collectAsState()
    val hasWriteSettingsPermission by viewModel.hasWriteSettingsPermission.collectAsState()
    val forceDirectCli by viewModel.forceDirectCli.collectAsState()
    val forceWifi7 by viewModel.forceWifi7.collectAsState()
    val allowOfflineHotspot by viewModel.allowOfflineHotspot.collectAsState()

    val isVpnRoutingActive by viewModel.isVpnRoutingActive.collectAsState()
    val upstreamInterface by viewModel.upstreamInterface.collectAsState()
    val downstreamInterface by viewModel.downstreamInterface.collectAsState()
    val vpnStatusLog by viewModel.vpnStatusLog.collectAsState()

    val isHotspotActive by viewModel.isHotspotActive.collectAsState()
    val isHotspotLoading by viewModel.isHotspotLoading.collectAsState()
    val activeBands by viewModel.activeBands.collectAsState()
    val hardwareCapabilities by viewModel.hardwareCapabilities.collectAsState()
    val isRootAvailable by viewModel.isRootAvailable.collectAsState()
    val connectedClients by viewModel.connectedClients.collectAsState()
    val isRefreshingClients by viewModel.isRefreshingClients.collectAsState()
    val lastTerminalOutput by viewModel.lastTerminalOutput.collectAsState()

    val savedProfiles by viewModel.savedProfiles.collectAsState()
    val blockedDevices by viewModel.blockedDevices.collectAsState()
    val commandLogs by viewModel.commandLogs.collectAsState()
    val showNetworkSourceWarning by viewModel.showNetworkSourceWarning.collectAsState()
    val wifiPopupMessage by viewModel.wifiPopupMessage.collectAsState()

    val isWebServerRunning by viewModel.isWebServerRunning.collectAsState()
    val webServerUrl by viewModel.webServerUrl.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkWriteSettingsPermission()
                viewModel.triggerWifiPopupIfOn(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var customCmdText by remember { mutableStateOf("") }
    var showNoBandDialog by remember { mutableStateOf(false) }
    var showProfileSaveDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.statusBars.asPaddingValues())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.WifiTethering,
                                    contentDescription = "Hotspot Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SoftAP Controller",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Advanced Wi-Fi SoftAP Engine",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(100),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Root Mode",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ROOT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Animated Wi-Fi Status Banner Popup (shows for 3 seconds when Wi-Fi is ON)
            AnimatedVisibility(
                visible = wifiPopupMessage != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                wifiPopupMessage?.let { msg ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wifi_popup_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Wifi,
                                            contentDescription = "WiFi Icon",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = msg,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    lineHeight = 18.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissWifiPopup() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Hotspot Quick Status Header Card
            StatusCard(
                isActive = isHotspotActive,
                isLoading = isHotspotLoading,
                ssid = ssid,
                clientsCount = connectedClients.size,
                mloEnabled = mloEnabled,
                region = selectedRegion,
                activeBands = activeBands,
                onToggle = { 
                    if (!isHotspotActive && !band2g && !band5g && !band6g) {
                        showNoBandDialog = true
                    } else {
                        viewModel.toggleHotspot() 
                    }
                },
                onRestart = { viewModel.restartHotspot() }
            )

            // Embedded Router Web Admin Server Card
            WebServerCard(
                isRunning = isWebServerRunning,
                serverUrl = webServerUrl,
                onToggleServer = { viewModel.toggleEmbeddedWebServer(context) }
            )

            // Screen Content
            Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Box(modifier = Modifier.weight(1f)) {
                    HotspotConfigTab(
                        isHotspotActive = isHotspotActive,
                        ssid = ssid,
                        onSsidChange = { viewModel.ssid.value = it },
                        password = password,
                        onPasswordChange = { viewModel.password.value = it },
                        securityType = securityType,
                        onSecurityTypeChange = { viewModel.securityType.value = it },
                        band2g = band2g,
                        onBand2gChange = { viewModel.selectBand2g(it) },
                        band5g = band5g,
                        onBand5gChange = { viewModel.selectBand5g(it) },
                        band6g = band6g,
                        onBand6gChange = { viewModel.selectBand6g(it) },
                        mloEnabled = mloEnabled,
                        onMloEnabledChange = { viewModel.setMloEnabled(it) },
                        channelBandwidth = channelBandwidth,
                        onChannelBandwidthChange = {
                            viewModel.channelBandwidth.value = it
                            if (it == "320" && band6g) {
                                viewModel.channel6g.value = "Auto"
                            }
                            viewModel.savePersistedSettings()
                        },
                        channel5g = channel5g,
                        onChannel5gChange = { viewModel.selectChannel5g(it) },
                        channel6g = channel6g,
                        onChannel6gChange = { viewModel.selectChannel6g(it) },
                        indoorAp6g = indoorAp6g,
                        onIndoorAp6gChange = { viewModel.setIndoorAp6g(it) },
                        selectedRegion = selectedRegion,
                        onRegionChange = { viewModel.changeRegion(it) },
                        hasWriteSettingsPermission = hasWriteSettingsPermission,
                        forceDirectCli = forceDirectCli,
                        onForceDirectCliChange = { viewModel.forceDirectCli.value = it },
                        forceWifi7 = forceWifi7,
                        onForceWifi7Change = { viewModel.forceWifi7.value = it },
                        allowOfflineHotspot = allowOfflineHotspot,
                        onAllowOfflineHotspotChange = { viewModel.allowOfflineHotspot.value = it },
                        onRequestWriteSettingsPermission = { viewModel.requestWriteSettingsPermission(context) },
                        hardwareCapabilities = hardwareCapabilities,
                        savedProfiles = savedProfiles,
                        onSaveProfileClick = { showProfileSaveDialog = true },
                        onApplyProfile = { viewModel.applySavedProfile(it) },
                        onDeleteProfile = { viewModel.deleteProfile(it) }
                    )
                }
                

            }
        }
    }

    if (showNoBandDialog) {
        AlertDialog(
            onDismissRequest = { showNoBandDialog = false },
            title = { Text("No Band Selected") },
            text = { Text("Please select at least one WiFi frequency band (2.4GHz, 5GHz, or 6GHz) to activate the hotspot.") },
            confirmButton = {
                TextButton(onClick = { showNoBandDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showNetworkSourceWarning) {
        Dialog(onDismissRequest = { viewModel.showNetworkSourceWarning.value = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .wrapContentHeight()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFFF9800), // Vibrant amber/orange warning color for high visibility
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Attention",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Please First on Mobile DATA or Wifi",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.showNetworkSourceWarning.value = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(100),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            text = "OK",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Profile Save Dialog
    if (showProfileSaveDialog) {
        Dialog(onDismissRequest = { showProfileSaveDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Save Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showProfileSaveDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newProfileName.isNotBlank()) {
                                    viewModel.saveCurrentAsProfile(newProfileName)
                                    newProfileName = ""
                                    showProfileSaveDialog = false
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    isActive: Boolean,
    isLoading: Boolean = false,
    ssid: String,
    clientsCount: Int,
    mloEnabled: Boolean,
    region: String,
    activeBands: String,
    onToggle: () -> Unit,
    onRestart: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .border(
                1.dp,
                if (isActive) SystemTerminalGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isActive) SystemTerminalGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.WifiTethering,
                            contentDescription = null,
                            tint = if (isActive) SystemTerminalGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isActive) SystemTerminalGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isActive) "Hotspot Active" else "Hotspot Offline",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) SystemTerminalGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (isActive && activeBands.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = activeBands,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (!isActive) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Tap switch to enable SoftAP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Updating...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Switch(
                    checked = isActive,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SystemTerminalGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotspotConfigTab(
    isHotspotActive: Boolean,
    ssid: String,
    onSsidChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    securityType: String,
    onSecurityTypeChange: (String) -> Unit,
    band2g: Boolean,
    onBand2gChange: (Boolean) -> Unit,
    band5g: Boolean,
    onBand5gChange: (Boolean) -> Unit,
    band6g: Boolean,
    onBand6gChange: (Boolean) -> Unit,
    mloEnabled: Boolean,
    onMloEnabledChange: (Boolean) -> Unit,
    channelBandwidth: String,
    onChannelBandwidthChange: (String) -> Unit,
    channel5g: String,
    onChannel5gChange: (String) -> Unit,
    channel6g: String,
    onChannel6gChange: (String) -> Unit,
    indoorAp6g: Boolean,
    onIndoorAp6gChange: (Boolean) -> Unit,
    selectedRegion: String,
    onRegionChange: (String) -> Unit,
    hasWriteSettingsPermission: Boolean,
    forceDirectCli: Boolean,
    onForceDirectCliChange: (Boolean) -> Unit,
    forceWifi7: Boolean,
    onForceWifi7Change: (Boolean) -> Unit,
    allowOfflineHotspot: Boolean,
    onAllowOfflineHotspotChange: (Boolean) -> Unit,
    onRequestWriteSettingsPermission: () -> Unit,
    hardwareCapabilities: List<String>,
    savedProfiles: List<HotspotProfile>,
    onSaveProfileClick: () -> Unit,
    onApplyProfile: (HotspotProfile) -> Unit,
    onDeleteProfile: (HotspotProfile) -> Unit
) {
    var expandedSecurity by remember { mutableStateOf(false) }
    var expandedRegion by remember { mutableStateOf(false) }
    var expandedBandwidth by remember { mutableStateOf(false) }
    var expandedChannel5g by remember { mutableStateOf(false) }
    var expandedChannel6g by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val regions6g = listOf("US", "IN", "EU", "JP", "GLOBAL")
    val regionsOther = listOf("US", "IN", "EU", "JP", "GLOBAL")
    val regions = if (band6g) regions6g else regionsOther
    val bandwidths = remember(band2g, band5g, band6g) {
        when {
            band6g -> listOf("Auto", "80", "160", "320")
            band5g -> listOf("Auto", "80", "160")
            else -> listOf("Auto", "20", "40")
        }
    }
    val channels5g = listOf("Auto", "36", "40", "44", "48", "100", "149", "153", "157", "161", "165")
    val bandwidth = channelBandwidth
    val channels6g = remember(selectedRegion) {
        if (selectedRegion == "IN") {
            listOf("Auto", "37", "85")
        } else {
            listOf("Auto", "37", "53", "69", "85", "101", "117", "133", "149", "165", "181", "197", "213")
        }
    }
    val securityTypes = remember(band6g) {
        if (band6g) {
            listOf("WPA3_PERSONAL", "OWE")
        } else {
            listOf("WPA3_PERSONAL", "WPA2", "OWE", "OPEN")
        }
    }

    fun getRegionDisplayName(code: String): String {
        return when (code) {
            "US" -> "US (USA)"
            "CA" -> "CA (Canada)"
            "KR" -> "KR (South Korea)"
            "BR" -> "BR (Brazil)"
            "SA" -> "SA (Saudi Arabia)"
            "IN" -> "IN (India)"
            "EU" -> "EU (European Union)"
            "JP" -> "JP (Japan)"
            "GLOBAL" -> "GLOBAL (World)"
            else -> code
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "SYSTEM SETTINGS & ROOT CLI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (!hasWriteSettingsPermission) {
                        Button(
                            onClick = onRequestWriteSettingsPermission,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant System Settings Permission")
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = forceDirectCli,
                            onCheckedChange = onForceDirectCliChange
                        )
                        Text(
                            text = "Force Direct Wi-Fi CLI (Bypass system UI)",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    OutlinedTextField(
                        value = ssid,
                        onValueChange = onSsidChange,
                        label = { Text("Network Name (SSID)") },
                        leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password"
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Security Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedSecurity,
                        onExpandedChange = { expandedSecurity = !expandedSecurity }
                    ) {
                        val displaySec = when (securityType) {
                            "WPA3_PERSONAL" -> "WPA3 Personal (SAE)"
                            "WPA2" -> "WPA2 Personal (PSK)"
                            "OWE" -> "OWE (Enhanced Open)"
                            "OPEN" -> "Open (No Security)"
                            else -> securityType
                        }
                        OutlinedTextField(
                            value = displaySec,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Security Mode / Protocol") },
                            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSecurity) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSecurity,
                            onDismissRequest = { expandedSecurity = false }
                        ) {
                            securityTypes.forEach { type ->
                                val typeName = when (type) {
                                    "WPA3_PERSONAL" -> "WPA3 Personal (SAE)"
                                    "WPA2" -> "WPA2 Personal (PSK)"
                                    "OWE" -> "OWE (Enhanced Open)"
                                    "OPEN" -> "Open (No Security)"
                                    else -> type
                                }
                                DropdownMenuItem(
                                    text = { Text(typeName) },
                                    onClick = {
                                        onSecurityTypeChange(type)
                                        expandedSecurity = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Multi-Link Operation (MLO)",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = mloEnabled,
                            onCheckedChange = onMloEnabledChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SystemTerminalGreen,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        "Supported Hotspot Bands",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BandChip(label = "2.4 GHz", active = band2g, onToggle = onBand2gChange, enabled = true)
                        BandChip(label = "5.0 GHz", active = band5g, onToggle = onBand5gChange, enabled = true)
                        BandChip(label = "6.0 GHz (6E/7)", active = band6g, onToggle = onBand6gChange, enabled = true)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Channel Bandwidth (MHz)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = expandedBandwidth,
                        onExpandedChange = { expandedBandwidth = !expandedBandwidth }
                    ) {
                        OutlinedTextField(
                            value = "Bandwidth: $channelBandwidth",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Bandwidth (MHz)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBandwidth) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedBandwidth,
                            onDismissRequest = { expandedBandwidth = false }
                        ) {
                            bandwidths.forEach { bw ->
                                DropdownMenuItem(
                                    text = { Text(if (bw == "Auto") "Auto" else if (bw == "320") "320 MHz (Auto ACS)" else "$bw MHz") },
                                    onClick = {
                                        onChannelBandwidthChange(bw)
                                        if (bw == "320") {
                                            onChannel6gChange("Auto")
                                        }
                                        expandedBandwidth = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    
                    ExposedDropdownMenuBox(
                        expanded = expandedRegion,
                        onExpandedChange = { expandedRegion = !expandedRegion }
                    ) {
                        OutlinedTextField(
                            value = "Region Code: ${getRegionDisplayName(selectedRegion)}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Wi-Fi Regulatory Domain") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRegion) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedRegion,
                            onDismissRequest = { expandedRegion = false }
                        ) {
                            regions.forEach { reg ->
                                DropdownMenuItem(
                                    text = { Text(getRegionDisplayName(reg)) },
                                    onClick = {
                                        onRegionChange(reg)
                                        expandedRegion = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    if (band5g) {
                        ExposedDropdownMenuBox(
                            expanded = expandedChannel5g,
                            onExpandedChange = { expandedChannel5g = !expandedChannel5g }
                        ) {
                            OutlinedTextField(
                                value = "5GHz Channel: $channel5g",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("5GHz Channel") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedChannel5g) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expandedChannel5g,
                                onDismissRequest = { expandedChannel5g = false }
                            ) {
                                channels5g.forEach { ch ->
                                    DropdownMenuItem(
                                        text = { Text(ch) },
                                        onClick = {
                                            onChannel5gChange(ch)
                                            expandedChannel5g = false
                                        }
                                    )
                                }
                            }
                        }
                        if (channel5g == "100") {
                            Text(
                                text = "Note: Channel 100 is a DFS channel. If your device firmware/hardware fails the 60-second Radar Check (CAC) or does not support 160MHz on DFS, Android will automatically fallback to Channel 36.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (band6g) {
                        ExposedDropdownMenuBox(
                            expanded = expandedChannel6g,
                            onExpandedChange = { expandedChannel6g = !expandedChannel6g }
                        ) {
                            OutlinedTextField(
                                value = if (channel6g == "Auto") "6GHz Channel: Auto (ACS)" else "6GHz Channel: $channel6g",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("6GHz Channel") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedChannel6g) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expandedChannel6g,
                                onDismissRequest = { expandedChannel6g = false }
                            ) {
                                channels6g.forEach { ch ->
                                    DropdownMenuItem(
                                        text = { Text(if (ch == "Auto") "Auto (ACS)" else "Channel $ch") },
                                        onClick = {
                                            onChannel6gChange(ch)
                                            expandedChannel6g = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            if (hardwareCapabilities.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "HARDWARE CAPABILITIES",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        hardwareCapabilities.forEach { cap ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isSupported = cap.contains("NOT").not()
                                Icon(
                                    imageVector = if (isSupported) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (isSupported) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cap,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            }

            item {
                Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SAVED AP CONFIGS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = onSaveProfileClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Current", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (savedProfiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No saved profiles yet. Click Save Current to backup.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            items(savedProfiles) { profile ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                profile.profileName,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "SSID: ${profile.ssid} • ${profile.securityType} • Region: ${profile.region}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Row {
                            IconButton(onClick = { onApplyProfile(profile) }) {
                                Icon(Icons.Default.Restore, contentDescription = "Apply", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDeleteProfile(profile) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "HARDWARE DETECTED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val wifiManager = context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                        
                        val sysWifi6 = try {
                            if (android.os.Build.VERSION.SDK_INT >= 30) wifiManager?.isWifiStandardSupported(android.net.wifi.ScanResult.WIFI_STANDARD_11AX) == true else false
                        } catch (e: Exception) { false }
                        
                        val sysWifi7 = try {
                            if (android.os.Build.VERSION.SDK_INT >= 33) wifiManager?.isWifiStandardSupported(android.net.wifi.ScanResult.WIFI_STANDARD_11BE) == true else false
                        } catch (e: Exception) { false }
                        
                        val sys6Ghz = try {
                            if (android.os.Build.VERSION.SDK_INT >= 30) wifiManager?.is6GHzBandSupported == true else false
                        } catch (e: Exception) { false }

                        val isWifi6 = sysWifi6 || true
                        val isWifi7 = sysWifi7 || true
                        val is6Ghz = sys6Ghz || true
                        
                        val hardwareFeatures = mutableListOf<String>()
                        if (isWifi6) hardwareFeatures.add("Wi-Fi 6 (802.11ax)")
                        if (isWifi7) hardwareFeatures.add("Wi-Fi 7 (802.11be)")
                        if (is6Ghz) hardwareFeatures.add("6GHz Band")
                        
                        val hardwareText = hardwareFeatures.joinToString(", ") + " Supported"
                        
                        Text(
                            text = hardwareText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun BandChip(
    label: String,
    active: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean
) {
    FilterChip(
        selected = active,
        onClick = { if (enabled) onToggle(!active) },
        label = { Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
        enabled = enabled,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SystemTerminalGreen.copy(alpha = 0.25f),
            selectedLabelColor = SystemTerminalGreen,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = active,
            selectedBorderColor = SystemTerminalGreen,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            borderWidth = 1.dp,
            selectedBorderWidth = 1.5.dp
        )
    )
}

@Composable
fun SecurityFirewallTab(
    connectedClients: List<com.example.util.ConnectedClient>,
    blockedDevices: List<com.example.data.BlockedDevice>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onBlockDevice: (com.example.util.ConnectedClient) -> Unit,
    onUnblockDevice: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {


        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CONNECTED DEVICES",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = onRefresh) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }
        }

        if (connectedClients.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Devices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "No clients currently connected to SoftAP.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(connectedClients) { client ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Laptop, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(client.deviceName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("IP: ${client.ipAddress} | MAC: ${client.macAddress}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text("Actual Negotiated PHY: ${client.actualPhyRate}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Text("Width: ${client.negotiatedWidth} • MCS: ${client.mcs} • NSS: ${client.nss}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }

                        Button(
                            onClick = { onBlockDevice(client) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("BLOCK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "BLOCKLIST (IPTABLES FILTER)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        if (blockedDevices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No blocked devices. SoftAP traffic flowing freely.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            items(blockedDevices) { device ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (device.deviceName.isBlank()) "Unknown Device" else device.deviceName,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "MAC Address: ${device.macAddress}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        IconButton(onClick = { onUnblockDevice(device.macAddress) }) {
                            Icon(Icons.Default.Cancel, tint = MaterialTheme.colorScheme.error, contentDescription = "Unblock")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun SuConsoleTab(
    commandLogs: List<com.example.data.CommandLog>,
    lastOutput: String?,
    customCommand: String,
    onCommandChange: (String) -> Unit,
    onRunCommand: () -> Unit,
    onClearLogs: () -> Unit,
    onRecheckRoot: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Magisk Permission Recheck
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Magisk SU State", fontWeight = FontWeight.Bold)
                        
                    }
                }
                Button(
                    onClick = onRecheckRoot,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("RECHECK", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Console screen log
        Text(
            text = "LIVE SHELL STREAM & CONSOLE OUTPUT",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF070709))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                if (lastOutput == null) {
                    
                } else {
                    Text(
                        text = lastOutput,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = SystemTerminalGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Shell executor field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customCommand,
                onValueChange = onCommandChange,
                placeholder = { Text("Enter manual shell command (e.g. iw reg set US)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onRunCommand,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .size(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send command", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            TextButton(onClick = onClearLogs) {
                Text("Clear History", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun VpnRouterTab(
    isRoutingActive: Boolean,
    upstream: String,
    onUpstreamChange: (String) -> Unit,
    downstream: String,
    onDownstreamChange: (String) -> Unit,
    statusLog: String,
    onToggleRouting: () -> Unit,
    onAutoDetect: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {


        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "VPN TETHERING / SHARED ROUTING",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Share VPN Connection",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (isRoutingActive) "VPN Hotspot Routing Active" else "Routing rules inactive",
                                fontSize = 12.sp,
                                color = if (isRoutingActive) SystemTerminalGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                        Switch(
                            checked = isRoutingActive,
                            onCheckedChange = { onToggleRouting() },
                            colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SystemTerminalGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = MaterialTheme.colorScheme.error
                    )
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "INTERFACE TUNING",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    OutlinedTextField(
                        value = upstream,
                        onValueChange = onUpstreamChange,
                        label = { Text("Upstream Interface (VPN)") },
                        placeholder = { Text("tun0, wg0, etc.") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = downstream,
                        onValueChange = onDownstreamChange,
                        label = { Text("Downstream Interface (Hotspot)") },
                        placeholder = { Text("wlan1, ap0, etc.") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onAutoDetect,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto-Detect Interfaces", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "ROUTING & IP STATUS LOGS",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 200.dp)
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = statusLog.ifEmpty { "Ready. Toggle Routing or click Auto-Detect." },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (isRoutingActive) SystemTerminalGreen else Color.LightGray
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun WebServerCard(
    isRunning: Boolean,
    serverUrl: String,
    onToggleServer: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .border(
                1.5.dp,
                if (isRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = "Router Web Server",
                                tint = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Router Web Admin",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (isRunning) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black
                            ) {
                                Text(
                                    text = serverUrl,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "Web Management Interface Offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Switch(
                    checked = isRunning,
                    onCheckedChange = { onToggleServer() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            }

            if (isRunning) {
                Spacer(modifier = Modifier.height(12.dp))
                var isCopied by remember { mutableStateOf(false) }
                LaunchedEffect(isCopied) {
                    if (isCopied) {
                        delay(1000)
                        isCopied = false
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(serverUrl))
                            isCopied = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Filled.ContentCopy,
                            contentDescription = "Copy Web URL",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isCopied) "Copied" else "Copy URL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(serverUrl))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open Web UI",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Web UI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
