package com.example.util

import android.util.Log
import com.example.data.CommandLog
import com.example.data.HotspotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object RootExecutor {
    private const val TAG = "RootExecutor"

    /**
     * Checks if root access is available by trying to request 'su'.
     */
    suspend fun checkRootAccess(): Boolean = withContext(Dispatchers.IO) {
        var process: Process? = null
        var os: DataOutputStream? = null
        try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            os.writeBytes("exit\n")
            os.flush()
            val exitValue = process.waitFor()
            return@withContext (exitValue == 0)
        } catch (e: Exception) {
            Log.w(TAG, "Root not available: ${e.message}")
            false
        } finally {
            try { os?.close() } catch (ignored: Exception) {}
            try { process?.destroy() } catch (ignored: Exception) {}
        }
    }

    private var persistentProcess: Process? = null
    private var persistentOs: DataOutputStream? = null
    private var persistentReader: BufferedReader? = null

    @Synchronized
    fun executePersistentCommand(command: String): RootResult {
        try {
            if (persistentProcess == null) {
                persistentProcess = Runtime.getRuntime().exec("su")
                persistentOs = DataOutputStream(persistentProcess!!.outputStream)
                persistentReader = BufferedReader(InputStreamReader(persistentProcess!!.inputStream))
            }

            persistentOs!!.writeBytes("$command 2>&1\n")
            persistentOs!!.writeBytes("echo '---EOF---'\n")
            persistentOs!!.flush()

            val output = StringBuilder()
            var line: String?
            while (true) {
                line = persistentReader!!.readLine()
                if (line == null || line == "---EOF---") {
                    break
                }
                output.append(line).append("\n")
            }
            return RootResult(true, output.toString().trim())
        } catch (e: Exception) {
            persistentProcess?.destroy()
            persistentProcess = null
            return RootResult(false, e.message ?: "Error")
        }
    }

    /**
     * Executes a command in root shell and returns stdout/stderr combined.
     */
    suspend fun executeCommand(
        command: String,
        repository: HotspotRepository? = null
    ): RootResult = withContext(Dispatchers.IO) {
        var process: Process? = null
        var os: DataOutputStream? = null
        var reader: BufferedReader? = null
        var errorReader: BufferedReader? = null
        val output = StringBuilder()
        var isSuccess = false

        try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()

            reader = BufferedReader(InputStreamReader(process.inputStream))
            errorReader = BufferedReader(InputStreamReader(process.errorStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            var errLine: String?
            while (errorReader.readLine().also { errLine = it } != null) {
                output.append("[ERR] ").append(errLine).append("\n")
            }

            val exitCode = process.waitFor()
            isSuccess = (exitCode == 0)
        } catch (e: Exception) {
            output.append("Execution Exception: ").append(e.localizedMessage)
            isSuccess = false
        } finally {
            try { os?.close() } catch (ignored: Exception) {}
            try { reader?.close() } catch (ignored: Exception) {}
            try { errorReader?.close() } catch (ignored: Exception) {}
            try { process?.destroy() } catch (ignored: Exception) {}
        }

        val resultStr = output.toString().trim()
        val finalResult = RootResult(isSuccess, resultStr)

        // Log to Room db asynchronously if repository is provided
        repository?.let { repo ->
            try {
                repo.addLog(
                    CommandLog(
                        command = command,
                        output = if (resultStr.isEmpty()) "[No Output]" else resultStr,
                        isSuccess = isSuccess
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed logging command to DB", e)
            }
        }

        return@withContext finalResult
    }

    /**
     * Change Wi-Fi Region/Country Code via multiple fallback CLI tricks
     */
    suspend fun changeRegion(countryCode: String, repository: HotspotRepository): RootResult {
        val commands = listOf(
            "iw reg set ${countryCode.uppercase()}",
            "setprop ro.boot.wificountrycode ${countryCode.uppercase()}",
            "setprop wifi.countrycode ${countryCode.uppercase()}",
            "cmd wifi force-country-code enabled ${countryCode.uppercase()}"
        )
        val fullCmd = commands.joinToString(" ; ")
        val result = executeCommand(fullCmd, repository)
        return RootResult(result.success, "$ $fullCmd\n${result.output}".trim())
    }

    /**
     * Start softap hotspot with custom settings.
     */
    suspend fun configureAndStartSoftAp(
        ssid: String,
        pass: String,
        secType: String,
        bands: List<String>,
        region: String,
        channelBandwidth: String,
        channel5g: String,
        channel6g: String,
        mloEnabled: Boolean,
        useTetheringCmd: Boolean,
        forceWifi7: Boolean = true,
        repository: HotspotRepository
    ): RootResult {
        val bandProps = when {
            bands.isEmpty() -> "2G"
            else -> bands.joinToString("_")
        }

        // Apply country code first
        changeRegion(region, repository)
        
        if (forceWifi7) {
            // Force WiFi 7 (802.11be) in WCNSS_qcom_cfg.ini (to fix Magisk module reset issue)
            executeCommand("sed -i 's/^enable_11be=0/enable_11be=1/g' /mnt/vendor/persist/wlan/WCNSS_qcom_cfg.ini", repository)
            executeCommand("sed -i 's/^gEnable11be=0/gEnable11be=1/g' /mnt/vendor/persist/wlan/WCNSS_qcom_cfg.ini", repository)
            executeCommand("sed -i 's/^BandCapability=/#BandCapabilityMOD=/g' /mnt/vendor/persist/wlan/WCNSS_qcom_cfg.ini", repository)
            executeCommand("sed -i 's/^enable_11be=0/enable_11be=1/g' /vendor/etc/wifi/WCNSS_qcom_cfg.ini", repository)
            executeCommand("sed -i 's/^gEnable11be=0/gEnable11be=1/g' /vendor/etc/wifi/WCNSS_qcom_cfg.ini", repository)
            
            // Also force IEEE 802.11be in hostapd confs if possible
            executeCommand("sed -i 's/ieee80211ax=1/ieee80211ax=1\nieee80211be=1\neht_oper_chwidth=1/g' /data/vendor/wifi/hostapd/hostapd.conf", repository)
        }

        // Android SoftAP standard CLI parameters and config setups:
        val commands = mutableListOf<String>()
        commands.add("cmd wifi stop-softap")

        // Setting Android SoftAP specific System Properties
        commands.add("setprop wifi.active_band $bandProps")
        commands.add("setprop wifi.softap.ssid \"$ssid\"")
        commands.add("setprop wifi.softap.passphrase \"$pass\"")
        commands.add("setprop wifi.softap.security $secType")

        if (mloEnabled) {
            commands.add("setprop wifi.softap.mlo.enabled 1")
            commands.add("setprop persist.sys.wifi.softap.mlo 1")
            commands.add("setprop persist.vendor.wifi.softap.mlo 1")
            val mloBands = mutableListOf<String>()
            if (bands.contains("2G")) mloBands.add("2.4G")
            if (bands.contains("5G")) mloBands.add("5G")
            if (bands.contains("6G")) mloBands.add("6G")
            val mloBandsStr = if (mloBands.isNotEmpty()) mloBands.joinToString(",") else "2.4G,5G,6G"
            commands.add("setprop wifi.softap.mlo.bands \"$mloBandsStr\"")
        } else {
            commands.add("setprop wifi.softap.mlo.enabled 0")
        }
        
        commands.add("setprop wifi.softap.ieee80211be.enabled 1")
        commands.add("setprop wifi.softap.be.enabled 1")
        commands.add("setprop persist.sys.wifi.softap.be 1")
        commands.add("setprop persist.vendor.wifi.softap.be 1")

        if (useTetheringCmd) {
            // Standard system tethering activation via root CLI connectivity service
            commands.add("cmd tether start wifi")
            commands.add("cmd connectivity tether start-tethering wifi")
            commands.add("service call tethering 3 i32 0") // Try tethering service
            commands.add("service call connectivity 34 i32 0") // Older connectivity
            commands.add("service call connectivity 24 i32 0")
        } else {
            // Trigger Android's SoftAP Command utility directly
            // Under modern versions of android: "cmd wifi start-softap <ssid> <security_type> <passphrase> [-b <bands>]"
            
            // 6GHz strictly requires pure WPA3_SAE (no transition mode, no wpa2).
            val effectiveSecType = if ((mloEnabled || bands.contains("6G")) && secType !in listOf("WPA3_PERSONAL", "OWE")) {
                if (mloEnabled) "OWE" else "WPA3_PERSONAL"
            } else {
                secType
            }

            val securityArg = when (effectiveSecType) {
                "WPA2" -> "wpa2"
                "WPA3", "WPA3_PERSONAL" -> "wpa3"
                "WPA3_TRANSITION" -> "wpa3_transition"
                "OWE" -> "owe"
                "OWE_TRANSITION" -> "owe_transition"
                "OPEN" -> "open"
                else -> "wpa2"
            }

            // Configure bands via CLI arguments if available
            val bandArg = when {
                mloEnabled && bands.contains("2G") && bands.contains("5G") && bands.contains("6G") -> "-b bridged_5_6"
                mloEnabled && bands.contains("2G") && bands.contains("5G") -> "-b bridged_2_5"
                mloEnabled && bands.contains("2G") && bands.contains("6G") -> "-b bridged_2_6"
                mloEnabled && bands.contains("5G") && bands.contains("6G") -> "-b bridged_5_6"
                bands.contains("2G") && bands.contains("5G") && bands.contains("6G") -> "-b bridged_2_5" // Android CLI prefers dual-bridge
                bands.contains("2G") && bands.contains("5G") -> "-b bridged_2_5"
                bands.contains("2G") && bands.contains("6G") -> "-b bridged_2_6"
                bands.contains("5G") && bands.contains("6G") -> "-b bridged_5_6"
                bands.contains("2G") -> "-b 2"
                bands.contains("5G") -> "-b 5"
                bands.contains("6G") -> "-b 6"
                else -> "-b 2"
            }

            val bwArg = if (channelBandwidth != "Auto") "-w $channelBandwidth" else ""

            // Calculate frequencies from channels ONLY if explicitly set
            val freqs = mutableListOf<Int>()
            val explicitCh5g = channel5g != "Auto" && channel5g.toIntOrNull() != null
            val explicitCh6g = channel6g != "Auto" && channel6g.toIntOrNull() != null

            if (explicitCh5g || explicitCh6g) {
                // If any channel is explicitly set, we MUST use -f. 
                // When using -f in Bridged mode, we must provide frequencies for ALL bands.
                if (bands.contains("2G")) {
                    freqs.add(2437) // Default Ch 6 for 2.4GHz
                }
                if (bands.contains("5G")) {
                    if (explicitCh5g) {
                        freqs.add(5000 + (channel5g.toInt() * 5))
                    } else {
                        freqs.add(5200) // Default Ch 40 for 5GHz
                    }
                }
                if (bands.contains("6G")) {
                    if (explicitCh6g) {
                        freqs.add(5950 + (channel6g.toInt() * 5))
                    } else {
                        freqs.add(6115) // Default Ch 33 for 6GHz
                    }
                }
            }

            val freqArg = if (freqs.isNotEmpty()) "-f ${freqs.joinToString(" ")}" else ""

            // Generate actual SoftAP runner script or command
            val cmdWifiArgs = if (securityArg == "open" || securityArg == "owe" || securityArg == "owe_transition") {
                "cmd wifi start-softap \"$ssid\" $securityArg $bandArg $bwArg $freqArg".trim().replace("  ", " ")
            } else {
                "cmd wifi start-softap \"$ssid\" $securityArg \"$pass\" $bandArg $bwArg $freqArg".trim().replace("  ", " ")
            }
            commands.add(cmdWifiArgs)
        }

        // Executing sequence of commands
        val fullCmd = commands.joinToString(" ; ")
        val res = executeCommand(fullCmd, repository)
        val totalOutput = StringBuilder().append("$ $fullCmd\n").append(res.output).append("\n\n")

        val hasActivationCommands = commands.any { it.contains("start-") || it.contains("tether start") || it.contains("service call") }
        val anyStartCommandSucceeded = hasActivationCommands && res.success
        val success = if (hasActivationCommands) anyStartCommandSucceeded else true

        return RootResult(success, totalOutput.toString().trim())
    }

    /**
     * Stop SoftAP
     */
    suspend fun stopSoftAp(useTetheringCmd: Boolean, repository: HotspotRepository): RootResult {
        if (useTetheringCmd) {
            val commands = listOf(
                "cmd tether stop wifi",
                "cmd connectivity tether stop-tethering wifi",
                "service call tethering 4 i32 0",
                "service call connectivity 35 i32 0",
                "service call connectivity 25 i32 0"
            )
            val totalOutput = StringBuilder()
            var anySucceeded = false
            for (cmd in commands) {
                val res = executeCommand(cmd, repository)
                totalOutput.append("$ $cmd\n").append(res.output).append("\n\n")
                if (res.success) {
                    anySucceeded = true
                }
            }
            return RootResult(anySucceeded, totalOutput.toString().trim())
        } else {
            return executeCommand("cmd wifi stop-softap", repository)
        }
    }

    /**
     * Read /proc/net/arp and dnsmasq.leases to trace connected clients
     */
    suspend fun getConnectedClients(repository: HotspotRepository): List<ConnectedClient> = withContext(Dispatchers.IO) {
        val clients = mutableListOf<ConnectedClient>()
        val macsAdded = mutableSetOf<String>()

        // 1. Check arp table
        try {
            val arpResult = executePersistentCommand("cat /proc/net/arp")
            val lines = arpResult.output.split("\n")
            for (line in lines) {
                val parts = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
                if (parts.size >= 4 && parts[3].contains(":")) {
                    val ip = parts[0]
                    val mac = parts[3].uppercase()
                    val iface = parts.last()
                    if (mac != "00:00:00:00:00:00" && !macsAdded.contains(mac)) {
                        clients.add(ConnectedClient(ipAddress = ip, macAddress = mac, deviceName = "Client [ARP]", interfaceName = iface))
                        macsAdded.add(mac)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing arp", e)
        }

        // 2. Check dnsmasq.leases
        try {
            val leasesResult = executePersistentCommand("cat /data/misc/dhcp/dnsmasq.leases")
            if (leasesResult.success && leasesResult.output.isNotBlank()) {
                val lines = leasesResult.output.split("\n")
                for (line in lines) {
                    val parts = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
                    if (parts.size >= 4) {
                        val mac = parts[1].uppercase()
                        val ip = parts[2]
                        val name = parts[3]
                        val existingIndex = clients.indexOfFirst { it.macAddress == mac }
                        if (existingIndex >= 0) {
                            clients[existingIndex] = clients[existingIndex].copy(deviceName = name, ipAddress = ip)
                        } else if (!macsAdded.contains(mac)) {
                            clients.add(ConnectedClient(ipAddress = ip, macAddress = mac, deviceName = name, interfaceName = "wlan1"))
                            macsAdded.add(mac)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing leases", e)
        }

        // 3. Fallback: query neighbor cache
        try {
            val ipNeighResult = executePersistentCommand("ip neigh show")
            val lines = ipNeighResult.output.split("\n")
            for (line in lines) {
                val parts = line.split("\\s+").filter { it.isNotBlank() }
                if (parts.contains("lladdr")) {
                    val lladdrIndex = parts.indexOf("lladdr")
                    if (lladdrIndex + 1 < parts.size) {
                        val ip = parts[0]
                        val mac = parts[lladdrIndex + 1].uppercase()
                        if (!macsAdded.contains(mac) && mac.contains(":")) {
                            clients.add(ConnectedClient(ipAddress = ip, macAddress = mac, deviceName = "Connected Device", interfaceName = "wlan1"))
                            macsAdded.add(mac)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error run ip neigh", e)
        }

        return@withContext clients
    }

    /**
     * Apply access control block list using root iptables
     */
    suspend fun applyMacFilter(
        blockedDevices: List<String>,
        repository: HotspotRepository
    ): RootResult {
        val commands = mutableListOf<String>()
        // Flush custom filtering rules or clean existing blocks
        commands.add("iptables -F FORWARD")
        commands.add("iptables -P FORWARD ACCEPT")

        // Block all specified MAC addresses
        for (mac in blockedDevices) {
            commands.add("iptables -A FORWARD -m mac --mac-source $mac -j DROP")
            commands.add("iptables -A FORWARD -m mac --mac-destination $mac -j DROP")
        }

        val totalOutput = StringBuilder()
        var overallSuccess = true
        for (cmd in commands) {
            val res = executeCommand(cmd, repository)
            totalOutput.append("$ $cmd\n").append(res.output).append("\n\n")
            if (!res.success) {
                overallSuccess = false
            }
        }
        return RootResult(overallSuccess, totalOutput.toString().trim())
    }

    /**
     * Auto-detect active VPN interfaces (e.g. tun0, wg0, ppp0)
     */
    suspend fun detectVpnInterface(): String {
        val res = executeCommand("ip link show", null)
        val lines = res.output.split("\n")
        for (line in lines) {
            val parts = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
            for (part in parts) {
                val clean = part.replace(":", "")
                if (clean.startsWith("tun") || clean.startsWith("wg") || clean.startsWith("ppp") || clean.startsWith("tap") || clean.startsWith("rawip")) {
                    return clean
                }
            }
        }
        return "tun0" // Default fallback
    }

    /**
     * Auto-detect active hotspot interface (e.g. wlan1, ap0, ap1, swlan0)
     */
    suspend fun detectHotspotInterface(): String {
        val res = executeCommand("ip link show", null)
        val lines = res.output.split("\n")
        for (line in lines) {
            val parts = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
            for (part in parts) {
                val clean = part.replace(":", "")
                if (clean.startsWith("ap") || clean == "wlan1" || clean == "wlan2" || clean.startsWith("swlan")) {
                    return clean
                }
            }
        }
        return "wlan1" // Default fallback
    }

    /**
     * Setup VPN Hotspot routing using root rules
     */
    suspend fun configureVpnRouting(
        upstream: String,
        downstream: String,
        repository: HotspotRepository
    ): RootResult {
        val commands = mutableListOf<String>()
        
        // 1. Enable IP forwarding in kernel
        commands.add("echo 1 > /proc/sys/net/ipv4/ip_forward")
        commands.add("ndc ipf forward enable 2>/dev/null || true")
        
        // 2. Clear old state / NAT rules to avoid duplicates
        commands.add("iptables -t nat -D POSTROUTING -o $upstream -j MASQUERADE 2>/dev/null || true")
        commands.add("iptables -D FORWARD -i $downstream -o $upstream -j ACCEPT 2>/dev/null || true")
        commands.add("iptables -D FORWARD -i $upstream -o $downstream -m state --state ESTABLISHED,RELATED -j ACCEPT 2>/dev/null || true")
        
        // 3. Add fresh packet forwarding rules
        commands.add("iptables -t nat -I POSTROUTING 1 -o $upstream -j MASQUERADE")
        commands.add("iptables -I FORWARD 1 -i $downstream -o $upstream -j ACCEPT")
        commands.add("iptables -I FORWARD 1 -i $upstream -o $downstream -m state --state ESTABLISHED,RELATED -j ACCEPT")
        
        // 4. Bypass Android default tethering routing table by looking up the main routing table
        commands.add("ip rule del iif $downstream lookup main 2>/dev/null || true")
        commands.add("ip rule add iif $downstream lookup main")
        
        // Try to forward ipv6 if present
        commands.add("echo 1 > /proc/sys/net/ipv6/conf/all/forwarding 2>/dev/null || true")
        
        val totalOutput = StringBuilder()
        var overallSuccess = true
        for (cmd in commands) {
            val res = executeCommand(cmd, repository)
            totalOutput.append("$ $cmd\n").append(res.output).append("\n\n")
            // Ignore minor deletion or ipv6 errors, but forwarding configuration must succeed
            if (!res.success && !cmd.contains("del") && !cmd.contains("true") && !cmd.contains("echo 1 > /proc/sys/net/ipv6")) {
                overallSuccess = false
            }
        }
        return RootResult(overallSuccess, totalOutput.toString().trim())
    }

    /**
     * Disable VPN Hotspot routing
     */
    suspend fun disableVpnRouting(
        upstream: String,
        downstream: String,
        repository: HotspotRepository
    ): RootResult {
        val commands = mutableListOf<String>()
        commands.add("iptables -t nat -D POSTROUTING -o $upstream -j MASQUERADE 2>/dev/null || true")
        commands.add("iptables -D FORWARD -i $downstream -o $upstream -j ACCEPT 2>/dev/null || true")
        commands.add("iptables -D FORWARD -i $upstream -o $downstream -m state --state ESTABLISHED,RELATED -j ACCEPT 2>/dev/null || true")
        commands.add("ip rule del iif $downstream lookup main 2>/dev/null || true")
        
        val totalOutput = StringBuilder()
        for (cmd in commands) {
            val res = executeCommand(cmd, repository)
            totalOutput.append("$ $cmd\n").append(res.output).append("\n\n")
        }
        return RootResult(true, totalOutput.toString().trim())
    }
}

data class RootResult(val success: Boolean, val output: String)

data class ConnectedClient(
    val ipAddress: String,
    val macAddress: String,
    val deviceName: String,
    val interfaceName: String
)
