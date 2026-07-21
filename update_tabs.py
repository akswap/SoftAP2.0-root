import re

with open('./app/src/main/java/com/example/ui/MainHotspotScreen.kt', 'r') as f:
    content = f.read()

# Add currentTab state
tab_state = "    var currentTab by remember { mutableStateOf(0) }\n"
content = content.replace("    var customCmdText by remember { mutableStateOf(\"\") }", tab_state + "    var customCmdText by remember { mutableStateOf(\"\") }")

# Add NavigationBar to Scaffold
nav_bar = """        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Config") },
                    label = { Text("Config") },
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Security, contentDescription = "Firewall") },
                    label = { Text("Firewall") },
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.VpnKey, contentDescription = "VPN") },
                    label = { Text("VPN") },
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Terminal, contentDescription = "Console") },
                    label = { Text("Console") },
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background"""
content = content.replace("        containerColor = MaterialTheme.colorScheme.background", nav_bar, 1)

# Add when block for tabs
old_tab_block = """                HotspotConfigTab(
                    isHotspotActive = isHotspotActive,
                    ssid = ssid,
                    onSsidChange = { viewModel.ssid.value = it },
                    password = password,
                    onPasswordChange = { viewModel.password.value = it },
                    securityType = securityType,
                    onSecurityTypeChange = { viewModel.securityType.value = it },
                    band2g = band2g,
                    onBand2gChange = { 
                        viewModel.band2g.value = it 
                        viewModel.updateSettingsBasedOnBandsAndMlo()
                    },
                    band5g = band5g,
                    onBand5gChange = { 
                        viewModel.band5g.value = it 
                        viewModel.updateSettingsBasedOnBandsAndMlo()
                    },
                    band6g = band6g,
                    onBand6gChange = { 
                        viewModel.band6g.value = it 
                        viewModel.updateSettingsBasedOnBandsAndMlo()
                    },
                    mloEnabled = mloEnabled,
                    onMloEnabledChange = { 
                        viewModel.mloEnabled.value = it 
                        viewModel.updateSettingsBasedOnBandsAndMlo()
                    },
                    channelBandwidth = channelBandwidth,
                    onChannelBandwidthChange = { viewModel.channelBandwidth.value = it },
                    channel5g = channel5g,
                    onChannel5gChange = { viewModel.channel5g.value = it },
                    channel6g = channel6g,
                    onChannel6gChange = { viewModel.channel6g.value = it },
                    selectedRegion = selectedRegion,
                    onRegionChange = { viewModel.changeRegion(it) },
                    hasWriteSettingsPermission = hasWriteSettingsPermission,
                    forceDirectCli = forceDirectCli,
                    onForceDirectCliChange = { viewModel.forceDirectCli.value = it },
                    onRequestWriteSettingsPermission = { viewModel.requestWriteSettingsPermission(context) },
                    hardwareCapabilities = hardwareCapabilities,
                    savedProfiles = savedProfiles,
                    onSaveProfileClick = { showProfileSaveDialog = true },
                    onApplyProfile = { viewModel.applySavedProfile(it) },
                    onDeleteProfile = { viewModel.deleteProfile(it) }
                )"""

new_tab_block = """                when (currentTab) {
                    0 -> {
                        HotspotConfigTab(
                            isHotspotActive = isHotspotActive,
                            ssid = ssid,
                            onSsidChange = { viewModel.ssid.value = it },
                            password = password,
                            onPasswordChange = { viewModel.password.value = it },
                            securityType = securityType,
                            onSecurityTypeChange = { viewModel.securityType.value = it },
                            band2g = band2g,
                            onBand2gChange = { 
                                viewModel.band2g.value = it 
                                viewModel.updateSettingsBasedOnBandsAndMlo()
                            },
                            band5g = band5g,
                            onBand5gChange = { 
                                viewModel.band5g.value = it 
                                viewModel.updateSettingsBasedOnBandsAndMlo()
                            },
                            band6g = band6g,
                            onBand6gChange = { 
                                viewModel.band6g.value = it 
                                viewModel.updateSettingsBasedOnBandsAndMlo()
                            },
                            mloEnabled = mloEnabled,
                            onMloEnabledChange = { 
                                viewModel.mloEnabled.value = it 
                                viewModel.updateSettingsBasedOnBandsAndMlo()
                            },
                            channelBandwidth = channelBandwidth,
                            onChannelBandwidthChange = { viewModel.channelBandwidth.value = it },
                            channel5g = channel5g,
                            onChannel5gChange = { viewModel.channel5g.value = it },
                            channel6g = channel6g,
                            onChannel6gChange = { viewModel.channel6g.value = it },
                            selectedRegion = selectedRegion,
                            onRegionChange = { viewModel.changeRegion(it) },
                            hasWriteSettingsPermission = hasWriteSettingsPermission,
                            forceDirectCli = forceDirectCli,
                            onForceDirectCliChange = { viewModel.forceDirectCli.value = it },
                            onRequestWriteSettingsPermission = { viewModel.requestWriteSettingsPermission(context) },
                            hardwareCapabilities = hardwareCapabilities,
                            savedProfiles = savedProfiles,
                            onSaveProfileClick = { showProfileSaveDialog = true },
                            onApplyProfile = { viewModel.applySavedProfile(it) },
                            onDeleteProfile = { viewModel.deleteProfile(it) }
                        )
                    }
                    1 -> {
                        SecurityFirewallTab(
                            connectedClients = connectedClients,
                            blockedDevices = blockedDevices,
                            isRefreshing = isRefreshingClients,
                            onRefresh = { viewModel.refreshConnectedClients() },
                            onBlockDevice = { viewModel.blockDevice(it) },
                            onUnblockDevice = { viewModel.unblockDevice(it) }
                        )
                    }
                    2 -> {
                        val isRoutingActive by viewModel.isRoutingActive.collectAsState()
                        val vpnUpstream by viewModel.vpnUpstream.collectAsState()
                        val hotspotDownstream by viewModel.hotspotDownstream.collectAsState()
                        val vpnStatusLog by viewModel.vpnStatusLog.collectAsState()
                        VpnRouterTab(
                            isRoutingActive = isRoutingActive,
                            upstream = vpnUpstream,
                            onUpstreamChange = { viewModel.updateVpnInterfaces(it, hotspotDownstream) },
                            downstream = hotspotDownstream,
                            onDownstreamChange = { viewModel.updateVpnInterfaces(vpnUpstream, it) },
                            statusLog = vpnStatusLog,
                            onToggleRouting = { viewModel.toggleVpnRouting() },
                            onAutoDetect = { viewModel.autoDetectInterfaces() }
                        )
                    }
                    3 -> {
                        SuConsoleTab(
                            commandLogs = commandLogs,
                            lastOutput = lastTerminalOutput,
                            customCommand = customCmdText,
                            onCommandChange = { customCmdText = it },
                            onRunCommand = { 
                                viewModel.executeCustomCommand(customCmdText)
                                customCmdText = ""
                            },
                            onClearLogs = { viewModel.clearCommandLogs() },
                            onRecheckRoot = { viewModel.checkRootAccess() }
                        )
                    }
                }"""

content = content.replace(old_tab_block, new_tab_block)

# Add Missing Icons to imports
import_icons = """import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey"""
content = content.replace("import androidx.compose.material.icons.filled.Settings", import_icons + "\nimport androidx.compose.material.icons.filled.Settings")

with open('./app/src/main/java/com/example/ui/MainHotspotScreen.kt', 'w') as f:
    f.write(content)

print("Updated tabs")
