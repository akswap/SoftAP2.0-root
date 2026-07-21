import re

with open('./app/src/main/java/com/example/util/RootExecutor.kt', 'r') as f:
    content = f.read()

# Let's insert the patch in configureAndStartSoftAp, just before `commands.add("cmd wifi stop-softap")`
target = """        // Apply country code first
        changeRegion(region, repository)

        // Android SoftAP standard CLI parameters and config setups:"""

replacement = """        // Apply country code first
        changeRegion(region, repository)
        
        // Force WiFi 7 (802.11be) in WCNSS_qcom_cfg.ini (to fix Magisk module reset issue)
        executeCommand("sed -i 's/^enable_11be=0/enable_11be=1/g' /mnt/vendor/persist/wlan/WCNSS_qcom_cfg.ini", repository)
        executeCommand("sed -i 's/^gEnable11be=0/gEnable11be=1/g' /mnt/vendor/persist/wlan/WCNSS_qcom_cfg.ini", repository)
        executeCommand("sed -i 's/^BandCapability=/#BandCapabilityMOD=/g' /mnt/vendor/persist/wlan/WCNSS_qcom_cfg.ini", repository)
        executeCommand("sed -i 's/^enable_11be=0/enable_11be=1/g' /vendor/etc/wifi/WCNSS_qcom_cfg.ini", repository)
        executeCommand("sed -i 's/^gEnable11be=0/gEnable11be=1/g' /vendor/etc/wifi/WCNSS_qcom_cfg.ini", repository)
        
        // Also force IEEE 802.11be in hostapd confs if possible
        executeCommand("sed -i 's/ieee80211ax=1/ieee80211ax=1\\nieee80211be=1\\neht_oper_chwidth=1/g' /data/vendor/wifi/hostapd/hostapd.conf", repository)

        // Android SoftAP standard CLI parameters and config setups:"""

if target in content:
    content = content.replace(target, replacement)
    with open('./app/src/main/java/com/example/util/RootExecutor.kt', 'w') as f:
        f.write(content)
    print("Patched RootExecutor!")
else:
    print("Target not found")
