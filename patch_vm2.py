import re

with open('./app/src/main/java/com/example/ui/HotspotViewModel.kt', 'r') as f:
    content = f.read()

target = """                val result = RootExecutor.configureAndStartSoftAp(
                    ssid = ssid.value,
                    pass = password.value,
                    secType = securityType.value,
                    bands = bandsList,
                    region = selectedRegion.value,
                    channelBandwidth = channelBandwidth.value,
                    channel5g = channel5g.value,
                    channel6g = channel6g.value,
                    mloEnabled = mloEnabled.value,
                    useTetheringCmd = configWritten,
                    repository = repository
                )"""

replacement = """                val result = RootExecutor.configureAndStartSoftAp(
                    ssid = ssid.value,
                    pass = password.value,
                    secType = securityType.value,
                    bands = bandsList,
                    region = selectedRegion.value,
                    channelBandwidth = channelBandwidth.value,
                    channel5g = channel5g.value,
                    channel6g = channel6g.value,
                    mloEnabled = mloEnabled.value,
                    useTetheringCmd = configWritten,
                    forceWifi7 = forceWifi7.value,
                    repository = repository
                )"""

if target in content:
    content = content.replace(target, replacement)
    with open('./app/src/main/java/com/example/ui/HotspotViewModel.kt', 'w') as f:
        f.write(content)
    print("Patched VM2!")
else:
    print("Target not found VM2")
