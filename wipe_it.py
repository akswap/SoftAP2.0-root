import re

with open('./app/src/main/java/com/example/ui/MainHotspotScreen.kt', 'r') as f:
    content = f.read()

# Let's find exactly the line: `            // Screen Content`
# And the line: `    if (showNoBandDialog) {`
# We'll replace everything in between!

start_idx = content.find("            // Screen Content")
end_idx = content.find("    if (showNoBandDialog) {")

if start_idx != -1 and end_idx != -1:
    new_block = """            // Screen Content
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
                
                if (lastTerminalOutput.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(8.dp).height(150.dp),
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Black),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            item {
                                Text(
                                    text = lastTerminalOutput,
                                    color = androidx.compose.ui.graphics.Color.Green,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

"""
    content = content[:start_idx] + new_block + content[end_idx:]
    with open('./app/src/main/java/com/example/ui/MainHotspotScreen.kt', 'w') as f:
        f.write(content)
else:
    print("Could not find blocks")
