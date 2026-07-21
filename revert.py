with open('./app/src/main/java/com/example/ui/MainHotspotScreen.kt', 'r') as f:
    content = f.read()

import re

# Remove currentTab
content = content.replace("    var currentTab by remember { mutableStateOf(0) }\n", "")

# Remove NavigationBar
nav_regex = re.compile(r"        bottomBar = \{\s*NavigationBar\([\s\S]*?containerColor = MaterialTheme.colorScheme.background", re.MULTILINE)
content = nav_regex.sub("        containerColor = MaterialTheme.colorScheme.background", content)

# Remove when block
when_start = content.find("                when (currentTab) {")
when_end = content.find("                }")
when_end = content.find("                }", when_end + 1)
when_end = content.find("                }", when_end + 1)
when_end = content.find("                }", when_end + 1)
when_end = content.find("                }", when_end + 1)
# Actually, the original HotspotConfigTab string is easier to put back
old_hotspot = """                HotspotConfigTab(
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

content = re.sub(r"                when \(currentTab\) \{[\s\S]*?            \}", old_hotspot, content)

with open('./app/src/main/java/com/example/ui/MainHotspotScreen.kt', 'w') as f:
    f.write(content)

