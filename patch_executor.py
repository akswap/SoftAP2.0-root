import re

with open('./app/src/main/java/com/example/util/RootExecutor.kt', 'r') as f:
    content = f.read()

target = """        channel5g: String,
        channel6g: String,
        mloEnabled: Boolean,
        useTetheringCmd: Boolean,
        repository: HotspotRepository"""

replacement = """        channel5g: String,
        channel6g: String,
        mloEnabled: Boolean,
        useTetheringCmd: Boolean,
        forceWifi7: Boolean = true,
        repository: HotspotRepository"""

if target in content:
    content = content.replace(target, replacement)
    
    # Also wrap the sed commands in `if (forceWifi7) { ... }`
    target2 = """        // Force WiFi 7 (802.11be) in WCNSS_qcom_cfg.ini (to fix Magisk module reset issue)
        executeCommand("sed -i 's/^enable_11be=0/enable_11be=1/g' /mnt/vendor/persist/wlan/WCNSS_qcom_cfg.ini", repository)
        executeCommand("sed -i 's/^gEnable11be=0/gEnable11be=1/g' /mnt/vendor/persist/wlan/WCNSS_qcom_cfg.ini", repository)
        executeCommand("sed -i 's/^BandCapability=/#BandCapabilityMOD=/g' /mnt/vendor/persist/wlan/WCNSS_qcom_cfg.ini", repository)
        executeCommand("sed -i 's/^enable_11be=0/enable_11be=1/g' /vendor/etc/wifi/WCNSS_qcom_cfg.ini", repository)
        executeCommand("sed -i 's/^gEnable11be=0/gEnable11be=1/g' /vendor/etc/wifi/WCNSS_qcom_cfg.ini", repository)
        
        // Also force IEEE 802.11be in hostapd confs if possible
        executeCommand("sed -i 's/ieee80211ax=1/ieee80211ax=1\\nieee80211be=1\\neht_oper_chwidth=1/g' /data/vendor/wifi/hostapd/hostapd.conf", repository)"""

    replacement2 = """        if (forceWifi7) {
            // Force WiFi 7 (802.11be) in WCNSS_qcom_cfg.ini (to fix Magisk module reset issue)
            executeCommand("sed -i 's/^enable_11be=0/enable_11be=1/g' /mnt/vendor/persist/wlan/WCNSS_qcom_cfg.ini", repository)
            executeCommand("sed -i 's/^gEnable11be=0/gEnable11be=1/g' /mnt/vendor/persist/wlan/WCNSS_qcom_cfg.ini", repository)
            executeCommand("sed -i 's/^BandCapability=/#BandCapabilityMOD=/g' /mnt/vendor/persist/wlan/WCNSS_qcom_cfg.ini", repository)
            executeCommand("sed -i 's/^enable_11be=0/enable_11be=1/g' /vendor/etc/wifi/WCNSS_qcom_cfg.ini", repository)
            executeCommand("sed -i 's/^gEnable11be=0/gEnable11be=1/g' /vendor/etc/wifi/WCNSS_qcom_cfg.ini", repository)
            
            // Also force IEEE 802.11be in hostapd confs if possible
            executeCommand("sed -i 's/ieee80211ax=1/ieee80211ax=1\\nieee80211be=1\\neht_oper_chwidth=1/g' /data/vendor/wifi/hostapd/hostapd.conf", repository)
        }"""
    
    content = content.replace(target2, replacement2)
    
    with open('./app/src/main/java/com/example/util/RootExecutor.kt', 'w') as f:
        f.write(content)
    print("Patched Executor!")
else:
    print("Target not found Executor")
