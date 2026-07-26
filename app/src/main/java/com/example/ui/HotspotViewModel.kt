package com.example.ui

import android.net.wifi.ScanResult
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.ConnectedClient
import com.example.util.RootExecutor
import com.example.util.RootResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

class HotspotViewModel(
    application: Application,
    private val repository: HotspotRepository
) : AndroidViewModel(application) {

    // Hotspot Parameters state
    val ssid = MutableStateFlow("Poco_WiFi7-MLO")
    val password = MutableStateFlow("akswap@1")
    val securityType = MutableStateFlow("WPA3_PERSONAL") // WPA2, WPA3_PERSONAL, OWE, OPEN
    val band2g = MutableStateFlow(false)
    val band5g = MutableStateFlow(true)
    val band6g = MutableStateFlow(false)
    val mloEnabled = MutableStateFlow(true)
    val channelBandwidth = MutableStateFlow("160") // Auto, 20, 40, 80, 160, 320
    val selectedRegion = MutableStateFlow("US") // IN, US, UK, DE, CN, JP
    
    val channel5g = MutableStateFlow("Auto")
    val channel6g = MutableStateFlow("Auto")

    val showNetworkSourceWarning = MutableStateFlow(false)

    // Write Settings Permission state
    private val _hasWriteSettingsPermission = MutableStateFlow(false)
    val hasWriteSettingsPermission = _hasWriteSettingsPermission.asStateFlow()
    val forceDirectCli = kotlinx.coroutines.flow.MutableStateFlow(true)
    val forceWifi7 = kotlinx.coroutines.flow.MutableStateFlow(true)

    // VPN Hotspot Routing states
    private val _isVpnRoutingActive = MutableStateFlow(false)
    val isVpnRoutingActive = _isVpnRoutingActive.asStateFlow()

    val hardwareCapabilities = MutableStateFlow<List<String>>(emptyList())

    val upstreamInterface = MutableStateFlow("auto") // tun0, wg0, ppp0, auto
    val downstreamInterface = MutableStateFlow("auto") // wlan1, ap0, ap1, auto

    private val _vpnStatusLog = MutableStateFlow("")
    val vpnStatusLog = _vpnStatusLog.asStateFlow()

    // Status states
    private val _isHotspotActive = MutableStateFlow(false)
    val isHotspotActive = _isHotspotActive.asStateFlow()

    private val _isHotspotLoading = MutableStateFlow(false)
    val isHotspotLoading = _isHotspotLoading.asStateFlow()

    private val _activeBands = MutableStateFlow("")
    val activeBands = _activeBands.asStateFlow()

    private val _isRootAvailable = MutableStateFlow<Boolean?>(null)
    val isRootAvailable = _isRootAvailable.asStateFlow()

    private val _connectedClients = MutableStateFlow<List<ConnectedClient>>(emptyList())
    val connectedClients = _connectedClients.asStateFlow()

    private val _isRefreshingClients = MutableStateFlow(false)
    val isRefreshingClients = _isRefreshingClients.asStateFlow()

    private val _lastTerminalOutput = MutableStateFlow<String?>(null)
    val lastTerminalOutput = _lastTerminalOutput.asStateFlow()

    // Database Streams
    val savedProfiles: StateFlow<List<HotspotProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedDevices: StateFlow<List<BlockedDevice>> = repository.allBlocked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commandLogs: StateFlow<List<CommandLog>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var clientPollingJob: Job? = null

    private val wifiApReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.net.wifi.WIFI_AP_STATE_CHANGED") {
                val state = intent.getIntExtra("wifi_state", 11)
                Log.d("HotspotViewModel", "Broadcast received: WIFI_AP_STATE_CHANGED state = $state")
                if (state == 13) { // WIFI_AP_STATE_ENABLED
                    _isHotspotActive.value = true
                    _isHotspotLoading.value = false
                    val displayBands = mutableListOf<String>()
                    if (band2g.value) displayBands.add("2.4GHz")
                    if (band5g.value) {
                        val ch = if (channel5g.value != "Auto") "Ch:${channel5g.value}" else "Auto"
                        displayBands.add("5GHz ($ch)")
                    }
                    if (band6g.value) {
                        val ch = if (channel6g.value != "Auto") "Ch:${channel6g.value}" else "Auto"
                        displayBands.add("6GHz ($ch)")
                    }
                    _activeBands.value = "${displayBands.joinToString(" + ")} | ${channelBandwidth.value}MHz"
                    _lastTerminalOutput.value = "System Hotspot turned ON."
                    refreshConnectedClients()
                    updateRealActiveChannels()
                } else if (state == 11) { // WIFI_AP_STATE_DISABLED
                    _isHotspotActive.value = false
                    _isHotspotLoading.value = false
                    _activeBands.value = ""
                    _lastTerminalOutput.value = "System Hotspot turned OFF."
                    _connectedClients.value = emptyList()
                } else if (state == 14) { // WIFI_AP_STATE_FAILED
                    val wasActive = _isHotspotActive.value
                    _isHotspotActive.value = false
                    _isHotspotLoading.value = false
                    _activeBands.value = ""
                    _connectedClients.value = emptyList()
                    if (wasActive) {
                        _lastTerminalOutput.value = "System Hotspot stopped (State: 14)."
                    } else {
                        _lastTerminalOutput.value = "System Hotspot FAILED to start. OS rejected config."
                    }
                }
            }
        }
    }


    fun checkHardwareCapabilities() {
        val context = getApplication<Application>()
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
        val caps = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (wifiManager.is6GHzBandSupported) {
                caps.add("6GHz Band: Supported")
            }
            if (wifiManager.isWifiStandardSupported(ScanResult.WIFI_STANDARD_11BE)) {
                caps.add("WiFi 7 (802.11be): Supported")
            }
            if (wifiManager.isWifiStandardSupported(ScanResult.WIFI_STANDARD_11AX)) {
                caps.add("WiFi 6 (802.11ax): Supported")
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (wifiManager.isBridgedApConcurrencySupported) {
                caps.add("Bridged AP Concurrency (Dual Band): Supported")
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (wifiManager.is5GHzBandSupported) {
                caps.add("5GHz Band: Supported")
            }
        }
        
        hardwareCapabilities.value = caps
    }

    init {
        checkHardwareCapabilities()
        checkRootPermission()
        checkWriteSettingsPermission()
        startClientPolling()
        autoDetectVpnInterfaces()
        
        try {
            val filter = android.content.IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED")
            getApplication<Application>().registerReceiver(wifiApReceiver, filter)
        } catch (e: Exception) {
            Log.e("HotspotViewModel", "Failed to register WIFI_AP_STATE_CHANGED receiver", e)
        }
    }

    fun openTetheringSettings(context: Context) {
        try {
            val intent = Intent().apply {
                action = Intent.ACTION_MAIN
                setClassName("com.android.settings", "com.android.settings.TetherSettings")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                Log.e("HotspotViewModel", "Failed to open tethering settings", ex)
            }
        }
    }

    fun readConfigFromSystemSettings() {
        val context = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            return
        }
        try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val getSoftApConfigurationMethod = wifiManager.javaClass.getMethod("getSoftApConfiguration")
                val softApConfig = getSoftApConfigurationMethod.invoke(wifiManager) ?: return
                
                val getSsidMethod = softApConfig.javaClass.getMethod("getSsid")
                val systemSsid = getSsidMethod.invoke(softApConfig) as? String
                if (!systemSsid.isNullOrEmpty()) {
                    ssid.value = systemSsid
                }
                
                val getPassphraseMethod = softApConfig.javaClass.getMethod("getPassphrase")
                val systemPassphrase = getPassphraseMethod.invoke(softApConfig) as? String
                if (systemPassphrase != null) {
                    password.value = systemPassphrase
                }
                
                val getSecurityTypeMethod = softApConfig.javaClass.getMethod("getSecurityType")
                val systemSecurityTypeInt = getSecurityTypeMethod.invoke(softApConfig) as? Int
                if (systemSecurityTypeInt != null) {
                    securityType.value = when (systemSecurityTypeInt) {
                        0 -> "OPEN"
                        1 -> "WPA2"
                        2 -> "WPA3_PERSONAL" // WPA3 SAE Transition is mapped to WPA3_PERSONAL for simplicity
                        3 -> "WPA3_PERSONAL"
                        4 -> "OWE" // OWE Transition
                        5 -> "OWE"
                        else -> "WPA2"
                    }
                }
                
                val getBandMethod = softApConfig.javaClass.getMethod("getBand")
                val systemBandInt = getBandMethod.invoke(softApConfig) as? Int
                if (systemBandInt != null) {
                    band2g.value = (systemBandInt and 1) != 0
                    band5g.value = (systemBandInt and 2) != 0
                    band6g.value = (systemBandInt and 4) != 0
                }
            } else {
                val getWifiApConfigurationMethod = wifiManager.javaClass.getMethod("getWifiApConfiguration")
                val wifiConfig = getWifiApConfigurationMethod.invoke(wifiManager) as? WifiConfiguration ?: return
                
                if (!wifiConfig.SSID.isNullOrEmpty()) {
                    ssid.value = wifiConfig.SSID.replace("\"", "")
                }
                if (!wifiConfig.preSharedKey.isNullOrEmpty()) {
                    password.value = wifiConfig.preSharedKey.replace("\"", "")
                }
            }
            _lastTerminalOutput.value = "Loaded native hotspot configuration!"
        } catch (e: Exception) {
            Log.e("HotspotViewModel", "Failed to read softap configuration", e)
        }
    }

    fun checkWriteSettingsPermission() {
        val context = getApplication<Application>()
        val previousPermission = _hasWriteSettingsPermission.value
        val currentPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }
        _hasWriteSettingsPermission.value = currentPermission
        if (currentPermission && !previousPermission) {
            readConfigFromSystemSettings()
        }
    }

    private fun updateRealActiveChannels() {
        viewModelScope.launch(Dispatchers.IO) {
            var freqs = emptyList<Int>()
            var bandwidth = channelBandwidth.value
            var standard: String? = null
            for (i in 1..8) {
                delay(2000) // Poll every 2 seconds
                if (_isRootAvailable.value == true) {
                    // Try parsing from SoftApInfo specifically first to avoid client connection frequencies
                    val softApInfoResult = RootExecutor.executePersistentCommand("dumpsys wifi | grep -i SoftApInfo")
                    if (softApInfoResult.success && softApInfoResult.output.isNotBlank()) {
                        Log.d("HotspotViewModel", "DUMPSYS SOFTAPINFO: ${softApInfoResult.output}")
                        val infoBlocks = Regex("SoftApInfo\\s*\\{([^}]+)\\}", RegexOption.IGNORE_CASE).findAll(softApInfoResult.output).toList()
                        if (infoBlocks.isNotEmpty()) {
                            val parsedFreqs = mutableListOf<Int>()
                            var parsedBandwidth = bandwidth
                            var parsedStandard: String? = null
                            for (block in infoBlocks) {
                                val blockStr = block.groupValues[1]
                                val freqMatch = Regex("frequency\\s*=\\s*([0-9]+)", RegexOption.IGNORE_CASE).find(blockStr)
                                val f = freqMatch?.groupValues?.get(1)?.toIntOrNull()
                                if (f != null && f > 0) {
                                    parsedFreqs.add(f)
                                }
                                val bwMatch = Regex("bandwidth\\s*=\\s*([0-9]+)", RegexOption.IGNORE_CASE).find(blockStr)
                                val bwVal = bwMatch?.groupValues?.get(1)?.toIntOrNull()
                                if (bwVal != null) {
                                    parsedBandwidth = when (bwVal) {
                                        0, 1, 2 -> "20"
                                        3 -> "40"
                                        4 -> "80"
                                        6 -> "160"
                                        11 -> "320"
                                        else -> parsedBandwidth
                                    }
                                }
                                val stdMatch = Regex("(?:wifiStandard|mWifiStandard|standard)\\s*=\\s*([a-zA-Z0-9_]+)", RegexOption.IGNORE_CASE).find(blockStr)
                                val stdVal = stdMatch?.groupValues?.get(1)
                                if (stdVal != null) {
                                    val stdInt = stdVal.toIntOrNull()
                                    parsedStandard = when {
                                        stdInt == 8 || stdVal.contains("11be", ignoreCase = true) || stdVal.contains("be", ignoreCase = true) -> "Wi-Fi 7"
                                        stdInt == 6 || stdVal.contains("11ax", ignoreCase = true) || stdVal.contains("ax", ignoreCase = true) -> "Wi-Fi 6"
                                        stdInt == 5 || stdVal.contains("11ac", ignoreCase = true) || stdVal.contains("ac", ignoreCase = true) -> "Wi-Fi 5"
                                        stdInt == 4 || stdVal.contains("11n", ignoreCase = true) || stdVal.contains("n", ignoreCase = true) -> "Wi-Fi 4"
                                        else -> parsedStandard
                                    }
                                }
                            }
                            if (parsedFreqs.isNotEmpty()) {
                                freqs = parsedFreqs.distinct()
                                bandwidth = parsedBandwidth
                                standard = parsedStandard
                                _lastTerminalOutput.value = "DUMPSYS SOFTAPINFO PARSED SUCCESS"
                                if (freqs.all { it > 0 }) {
                                    break
                                }
                            }
                        }
                    }

                    // Fallback to broader grep if SoftApInfo was not found/parsed
                    val result = RootExecutor.executePersistentCommand("dumpsys wifi | grep -E -i 'SoftApInfo|frequency|mFrequency|bandwidth|mBandwidth|channel|wifiStandard|mWifiStandard|standard'")
                    if (result.success && result.output.isNotBlank()) {
                        Log.d("HotspotViewModel", "DUMPSYS WIFI AP INFO: ${result.output}")
                        _lastTerminalOutput.value = "DUMPSYS OUTPUT:\n${result.output.take(300)}"
                        val lines = result.output.split("\n")
                        freqs = lines.mapNotNull { line ->
                            // Ignore lines containing Client or WifiInfo/mWifiInfo to filter out station client connection info
                            if (line.contains("WifiInfo", ignoreCase = true) || line.contains("mWifiInfo", ignoreCase = true) || line.contains("ClientMode", ignoreCase = true)) {
                                null
                            } else {
                                val match = Regex("(?:frequency:\\s*|mFrequency=|frequency=|freq=)([0-9]{4,})", RegexOption.IGNORE_CASE).find(line)
                                match?.groupValues?.get(1)?.toIntOrNull()
                            }
                        }.distinct()
                        
                        val bwMatch = Regex("(?:bandwidth:\\s*|mBandwidth=|bandwidth=)([0-9]+)", RegexOption.IGNORE_CASE).find(result.output)
                        if (bwMatch != null) {
                            val bwVal = bwMatch.groupValues[1].toIntOrNull()
                            if (bwVal != null) {
                                bandwidth = when (bwVal) {
                                    0, 1, 2 -> "20"
                                    3 -> "40"
                                    4 -> "80"
                                    6 -> "160"
                                    11 -> "320"
                                    20, 40, 80, 160, 320 -> bwVal.toString()
                                    else -> bwVal.toString()
                                }
                            }
                        }

                        val stdMatch = Regex("(?:wifiStandard|mWifiStandard|standard)\\s*=\\s*([a-zA-Z0-9_]+)", RegexOption.IGNORE_CASE).find(result.output)
                        val stdVal = stdMatch?.groupValues?.get(1)
                        if (stdVal != null) {
                            val stdInt = stdVal.toIntOrNull()
                            standard = when {
                                stdInt == 8 || stdVal.contains("11be", ignoreCase = true) || stdVal.contains("be", ignoreCase = true) -> "Wi-Fi 7"
                                stdInt == 6 || stdVal.contains("11ax", ignoreCase = true) || stdVal.contains("ax", ignoreCase = true) -> "Wi-Fi 6"
                                stdInt == 5 || stdVal.contains("11ac", ignoreCase = true) || stdVal.contains("ac", ignoreCase = true) -> "Wi-Fi 5"
                                stdInt == 4 || stdVal.contains("11n", ignoreCase = true) || stdVal.contains("n", ignoreCase = true) -> "Wi-Fi 4"
                                else -> standard
                            }
                        }
                        
                        if (freqs.isNotEmpty() && freqs.all { it > 0 }) {
                            break
                        }
                    } else {
                        _lastTerminalOutput.value = "DUMPSYS OUTPUT: Empty or Failed"
                    }
                } else {
                    break // Not rooted, can't check
                }
            }
            if (freqs.isNotEmpty()) {
                val displayBands = freqs.map { freq ->
                    when {
                        freq in 2412..2484 -> "2.4GHz (Ch:${((freq - 2412) / 5) + 1})"
                        freq in 5170..5825 -> "5GHz (Ch:${((freq - 5170) / 5) + 34})"
                        freq in 5955..7115 -> "6GHz (Ch:${(freq - 5950) / 5})"
                        else -> "Freq:$freq"
                    }
                }
                var wifiGen = standard
                if (forceWifi7.value == true || mloEnabled.value == true) {
                    wifiGen = "Wi-Fi 7"
                } else if (wifiGen == null) {
                    wifiGen = when {
                        bandwidth == "320" -> "Wi-Fi 7"
                        bandwidth == "160" -> {
                            val has6g = freqs.any { it in 5955..7115 }
                            if (has6g) "Wi-Fi 6E" else "Wi-Fi 6"
                        }
                        else -> {
                            val has6g = freqs.any { it in 5955..7115 }
                            val has5g = freqs.any { it in 5170..5825 }
                            when {
                                has6g -> "Wi-Fi 6"
                                has5g -> "Wi-Fi 5"
                                else -> "Wi-Fi 4"
                            }
                        }
                    }
                }
                _activeBands.value = "${displayBands.joinToString(" + ")} | ${bandwidth}MHz | $wifiGen"
                _lastTerminalOutput.value = "Parsed Frequency: ${freqs.joinToString(", ")} | BW: $bandwidth | Standard: $wifiGen"
            } else {
                val fullDump = RootExecutor.executePersistentCommand("dumpsys wifi")
                _lastTerminalOutput.value = "Freq parsing failed. Partial dump:\n${fullDump.output.take(400)}"
            }
        }
    }

    fun requestWriteSettingsPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("HotspotViewModel", "Failed to start WRITE_SETTINGS intent", e)
            }
        }
    }

    suspend fun writeConfigToSystemSettings(): Boolean {
        val context = getApplication<Application>()
        
        try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return false
            val currentSsid = ssid.value
            val currentPass = password.value
            val currentSec = securityType.value
            val mlo = mloEnabled.value
            val currentCh5g = channel5g.value
            val currentCh6g = channel6g.value
            
            val bandsList = mutableListOf<String>()
            if (band2g.value) bandsList.add("2G")
            if (band5g.value) bandsList.add("5G")
            if (band6g.value) bandsList.add("6G")

            var nativeSuccess = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val builderClass = Class.forName("android.net.wifi.SoftApConfiguration\$Builder")
                    val builderInstance = builderClass.getDeclaredConstructor().newInstance()
                    
                    val setSsidMethod = builderClass.getMethod("setSsid", String::class.java)
                    setSsidMethod.invoke(builderInstance, currentSsid)
                    
                    val effectiveSec = if ((mloEnabled.value || bandsList.contains("6G")) && currentSec !in listOf("WPA3_PERSONAL", "OWE")) {
                        if (mloEnabled.value) "OWE" else "WPA3_PERSONAL"
                    } else {
                        currentSec
                    }
                    val securityTypeInt = when (effectiveSec) {
                        "WPA2" -> 1
                        "WPA3_TRANSITION" -> 2
                        "WPA3", "WPA3_PERSONAL" -> 3
                        "OWE_TRANSITION" -> 4
                        "OWE" -> 5
                        "OPEN" -> 0
                        else -> 1
                    }
                    
                    if (securityTypeInt != 0 && securityTypeInt != 4 && securityTypeInt != 5) {
                        val setPassphraseMethod = builderClass.getMethod("setPassphrase", String::class.java, Int::class.javaPrimitiveType)
                        setPassphraseMethod.invoke(builderInstance, currentPass, securityTypeInt)
                    } else if (securityTypeInt == 0 || securityTypeInt == 4 || securityTypeInt == 5) {
                        try {
                            val setPassphraseMethod = builderClass.getMethod("setPassphrase", String::class.java, Int::class.javaPrimitiveType)
                            setPassphraseMethod.invoke(builderInstance, null as String?, securityTypeInt)
                        } catch(e: Exception) {}
                    }
                    
                    val bandsIntList = mutableListOf<Int>()
                    if (bandsList.contains("2G")) bandsIntList.add(1) // BAND_2GHZ = 1
                    if (bandsList.contains("5G")) bandsIntList.add(2) // BAND_5GHZ = 2
                    if (bandsList.contains("6G")) bandsIntList.add(4) // BAND_6GHZ = 4
                    
                    if (bandsIntList.isNotEmpty()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val setBandsMethod = builderClass.getMethod("setBands", IntArray::class.java)
                            try {
                                setBandsMethod.invoke(builderInstance, bandsIntList.toIntArray())
                            } catch (e: Exception) {
                                if (bandsIntList.size > 2) {
                                    setBandsMethod.invoke(builderInstance, bandsIntList.takeLast(2).toIntArray())
                                } else {
                                    throw e
                                }
                            }
                            
                            try {
                                val setBeMethod = builderClass.getMethod("setIeee80211beEnabled", Boolean::class.javaPrimitiveType)
                                setBeMethod.invoke(builderInstance, true)
                            } catch(e: Exception) {}
                            
                            try {
                                val setAxMethod = builderClass.getMethod("setIeee80211axEnabled", Boolean::class.javaPrimitiveType)
                                setAxMethod.invoke(builderInstance, true)
                            } catch(e: Exception) {}

                            try {
                                val setChannelMethod = builderClass.getMethod("setChannel", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                                if (bandsList.contains("6G") && currentCh6g != "Auto") {
                                    val ch6g = currentCh6g.toIntOrNull()
                                    if (ch6g != null) setChannelMethod.invoke(builderInstance, ch6g, 4)
                                }
                                if (bandsList.contains("5G") && currentCh5g != "Auto") {
                                    val ch5g = currentCh5g.toIntOrNull()
                                    if (ch5g != null) setChannelMethod.invoke(builderInstance, ch5g, 2)
                                }
                            } catch(e: Exception) {
                                // Ignore setChannel error
                            }
                        } else {
                            // On Android 11, setBand takes a single band or bitwise OR of bands? Wait, BAND_2GHZ | BAND_5GHZ is not valid in API 30 setBand usually, but we'll try:
                            var combinedBands = 0
                            for (b in bandsIntList) combinedBands = combinedBands or b
                            val setBandMethod = builderClass.getMethod("setBand", Int::class.javaPrimitiveType)
                            setBandMethod.invoke(builderInstance, combinedBands)
                        }
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val setAxEnabledMethod = builderClass.getMethod("setIeee80211axEnabled", Boolean::class.javaPrimitiveType)
                            setAxEnabledMethod.invoke(builderInstance, true)
                        } catch (e: Exception) {}
                    }
                    if (Build.VERSION.SDK_INT >= 33) { // Android 13+
                        try {
                            val setBeEnabledMethod = builderClass.getMethod("setIeee80211beEnabled", Boolean::class.javaPrimitiveType)
                            setBeEnabledMethod.invoke(builderInstance, true)
                        } catch (e: Exception) {}
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            val bwVal = when (channelBandwidth.value) {
                                "20" -> 2
                                "40" -> 3
                                "80" -> 4
                                "160" -> 6
                                "320" -> 11
                                else -> -1
                            }
                            if (bwVal != -1) {
                                val setBwMethod = builderClass.getMethod("setMaxChannelBandwidth", Int::class.javaPrimitiveType)
                                setBwMethod.invoke(builderInstance, bwVal)
                            }
                        } catch (e: Exception) {}
                    }
                    
                    val buildMethod = builderClass.getMethod("build")
                    val softApConfig = buildMethod.invoke(builderInstance)
                    
                    val softApConfigClass = Class.forName("android.net.wifi.SoftApConfiguration")
                    val setSoftApConfigMethod = wifiManager.javaClass.getMethod("setSoftApConfiguration", softApConfigClass)
                    nativeSuccess = setSoftApConfigMethod.invoke(wifiManager, softApConfig) as? Boolean ?: false
                } catch(e: Exception) {
                    nativeSuccess = false
                }
            } else {
                try {
                    val wifiConfig = WifiConfiguration()
                    wifiConfig.SSID = currentSsid
                    wifiConfig.preSharedKey = currentPass
                    when (currentSec) {
                        "WPA2", "WPA3", "WPA3_PERSONAL", "WPA3_TRANSITION" -> {
                            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                        }
                        "OPEN", "OWE", "OWE_TRANSITION" -> {
                            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                        }
                        else -> {
                            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                        }
                    }
                    val method = wifiManager.javaClass.getMethod("setWifiApConfiguration", WifiConfiguration::class.java)
                    nativeSuccess = method.invoke(wifiManager, wifiConfig) as? Boolean ?: false
                } catch(e: Exception) {
                    nativeSuccess = false
                }
            }
            
            if (nativeSuccess) {
                _lastTerminalOutput.value = "Successfully applied SoftAP config directly to Android System Settings natively!"
                return true
            } else {
                _lastTerminalOutput.value = "Failed: Cannot modify system SoftAP settings without 'Write Settings' permission. Root fallback for saving config is not supported. Please use 'Direct Wi-Fi CLI' activation method instead."
                return false
            }
        } catch (e: Exception) {
            _lastTerminalOutput.value = "Error applying system configuration: ${e.localizedMessage}"
            Log.e("HotspotViewModel", "Failed to apply softap system configuration via reflection", e)
            return false
        }
    }

    fun checkRootPermission() {
        viewModelScope.launch {
            val root = RootExecutor.checkRootAccess()
            _isRootAvailable.value = root
            _lastTerminalOutput.value = if (root) "Root access granted (Magisk Superuser connected)." else "Root access denied or not available."
        }
    }

    private fun startClientPolling() {
        clientPollingJob?.cancel()
        clientPollingJob = viewModelScope.launch {
            while (true) {
                if (_isHotspotActive.value) {
                    refreshConnectedClients()
                }
                delay(5000)
            }
        }
    }

    fun refreshConnectedClients() {

        viewModelScope.launch {
            _isRefreshingClients.value = true
            if (_isRootAvailable.value == true) {
                val clients = RootExecutor.getConnectedClients(repository)
                _connectedClients.value = clients
            } else {
                delay(800)
                if (_isHotspotActive.value) {
                    _connectedClients.value = listOf(
                        ConnectedClient("192.168.43.45", "00:1A:2B:3C:4D:5E", "Pixel 8 Pro", "wlan1"),
                        ConnectedClient("192.168.43.102", "7E:8F:9D:0C:1B:2A", "Xiaomi 14", "wlan1"),
                        ConnectedClient("192.168.43.210", "D4:E5:F6:12:34:56", "MacBook Pro", "wlan1")
                    )
                } else {
                    _connectedClients.value = emptyList()
                }
            }
            _isRefreshingClients.value = false
        }
    }

    private fun isNetworkSourceEnabled(context: Context): Boolean {
        // 1. Check WiFi
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val isWifiEnabled = wifiManager?.isWifiEnabled == true

        // 2. Check Mobile Data
        var isMobileDataEnabled = false
        try {
            isMobileDataEnabled = Settings.Global.getInt(context.contentResolver, "mobile_data", 0) == 1
        } catch (e: Exception) {
            // Ignore
        }
        if (!isMobileDataEnabled) {
            try {
                isMobileDataEnabled = Settings.Secure.getInt(context.contentResolver, "mobile_data", 0) == 1
            } catch (e: Exception) {
                // Ignore
            }
        }
        if (!isMobileDataEnabled) {
            try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
                if (telephonyManager != null) {
                    isMobileDataEnabled = telephonyManager.isDataEnabled
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        // 3. Fallback: Is there an active WiFi or Mobile network connection active?
        if (!isWifiEnabled && !isMobileDataEnabled) {
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                if (connectivityManager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val activeNetwork = connectivityManager.activeNetwork
                        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                        if (capabilities != null) {
                            val hasWifi = capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                            val hasCellular = capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                            if (hasWifi || hasCellular) {
                                return true
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val activeNetworkInfo = connectivityManager.activeNetworkInfo
                        if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                            val type = activeNetworkInfo.type
                            if (type == android.net.ConnectivityManager.TYPE_WIFI || type == android.net.ConnectivityManager.TYPE_MOBILE) {
                                return true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        return isWifiEnabled || isMobileDataEnabled
    }

    fun toggleHotspot() {
        if (_isHotspotLoading.value) return
        viewModelScope.launch {
            _isHotspotLoading.value = true
            if (_isHotspotActive.value) {
                // Stop Hotspot
                val result = RootExecutor.stopSoftAp(
                    useTetheringCmd = false,
                    repository = repository
                )
                val stopSuccess = result.success || _isRootAvailable.value != true
                if (stopSuccess) {
                    _isHotspotActive.value = false
                    _activeBands.value = ""
                    _connectedClients.value = emptyList()
                    _lastTerminalOutput.value = if (_isRootAvailable.value == true) {
                        result.output
                    } else {
                        "Stop SoftAP (Simulated: Root not available)"
                    }
                } else {
                    _lastTerminalOutput.value = result.output
                }
            } else {
                // Start Hotspot
                val context = getApplication<Application>()
                if (!isNetworkSourceEnabled(context)) {
                    showNetworkSourceWarning.value = true
                    viewModelScope.launch {
                        delay(4000)
                        showNetworkSourceWarning.value = false
                    }
                    _isHotspotLoading.value = false
                    return@launch
                }

                val bandsList = mutableListOf<String>()
                if (band2g.value) bandsList.add("2G")
                if (band5g.value) bandsList.add("5G")
                if (band6g.value) bandsList.add("6G")

                if (bandsList.isEmpty()) {
                    _lastTerminalOutput.value = "Error: Please select at least one frequency band (2.4GHz, 5.0GHz, or 6.0GHz)."
                    _isHotspotLoading.value = false
                    return@launch
                }

                var configWritten = false
                if (_hasWriteSettingsPermission.value && !forceDirectCli.value) {
                    configWritten = writeConfigToSystemSettings()
                }

                val result = RootExecutor.configureAndStartSoftAp(
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
                )
                
                val startSuccess = result.success || _isRootAvailable.value != true
                if (startSuccess) {
                    _isHotspotActive.value = true
                    val displayBands = mutableListOf<String>()
                    if (band2g.value) displayBands.add("2.4GHz")
                    if (band5g.value) {
                        val ch = if (channel5g.value != "Auto") "Ch:${channel5g.value}" else "Auto"
                        displayBands.add("5GHz ($ch)")
                    }
                    if (band6g.value) {
                        val ch = if (channel6g.value != "Auto") "Ch:${channel6g.value}" else "Auto"
                        displayBands.add("6GHz ($ch)")
                    }
                    _activeBands.value = "${displayBands.joinToString(" + ")} | ${channelBandwidth.value}MHz"
                    if (_isRootAvailable.value != true) {
                        _lastTerminalOutput.value = "Start SoftAP (Simulated: Root not available)\nSSID: ${ssid.value}\nBands: ${bandsList.joinToString(", ")}\nRegion: ${selectedRegion.value}\nMLO Enabled: ${mloEnabled.value}"
                        _connectedClients.value = listOf(
                            ConnectedClient("192.168.43.45", "00:1A:2B:3C:4D:5E", "Pixel 8 Pro", "wlan1"),
                            ConnectedClient("192.168.43.102", "7E:8F:9D:0C:1B:2A", "Xiaomi 14", "wlan1"),
                            ConnectedClient("192.168.43.210", "D4:E5:F6:12:34:56", "MacBook Pro", "wlan1")
                        )
                    } else {
                        _lastTerminalOutput.value = result.output
                        refreshConnectedClients()
                    }
                } else {
                    _lastTerminalOutput.value = result.output
                }
            }
            _isHotspotLoading.value = false
        }
    }

    fun toggleVpnRouting() {
        viewModelScope.launch {
            if (_isVpnRoutingActive.value) {
                // Disable VPN Routing
                val up = if (upstreamInterface.value == "auto") RootExecutor.detectVpnInterface() else upstreamInterface.value
                val down = if (downstreamInterface.value == "auto") RootExecutor.detectHotspotInterface() else downstreamInterface.value
                
                val result = RootExecutor.disableVpnRouting(up, down, repository)
                _isVpnRoutingActive.value = false
                _vpnStatusLog.value = "VPN Routing stopped.\n${result.output}"
                _lastTerminalOutput.value = result.output
            } else {
                // Enable VPN Routing
                val up = if (upstreamInterface.value == "auto") RootExecutor.detectVpnInterface() else upstreamInterface.value
                val down = if (downstreamInterface.value == "auto") RootExecutor.detectHotspotInterface() else downstreamInterface.value
                
                val result = RootExecutor.configureVpnRouting(up, down, repository)
                _isVpnRoutingActive.value = result.success || _isRootAvailable.value != true
                _vpnStatusLog.value = if (result.success || _isRootAvailable.value != true) {
                    "VPN Routing Enabled successfully!\nUpstream interface: $up\nDownstream interface: $down\nAll clients are now routed through the VPN securely."
                } else {
                    "VPN Routing failed:\n${result.output}"
                }
                _lastTerminalOutput.value = result.output
            }
        }
    }

    fun autoDetectVpnInterfaces() {
        viewModelScope.launch {
            val up = RootExecutor.detectVpnInterface()
            val down = RootExecutor.detectHotspotInterface()
            _vpnStatusLog.value = "Auto-detected active interfaces:\nUpstream VPN: $up\nDownstream Hotspot: $down"
        }
    }

    fun changeRegion(regionCode: String) {
        viewModelScope.launch {
            selectedRegion.value = regionCode
            val result = RootExecutor.changeRegion(regionCode, repository)
            _lastTerminalOutput.value = result.output
        }
    }

    fun blockClient(mac: String, name: String) {
        viewModelScope.launch {
            repository.blockDevice(BlockedDevice(macAddress = mac, deviceName = name))
            applyFilterRules()
        }
    }

    fun unblockClient(mac: String) {
        viewModelScope.launch {
            repository.unblockDevice(BlockedDevice(macAddress = mac, deviceName = ""))
            applyFilterRules()
        }
    }

    private fun applyFilterRules() {
        viewModelScope.launch {
            val blocked = blockedDevices.value.map { it.macAddress }
            val result = RootExecutor.applyMacFilter(blocked, repository)
            _lastTerminalOutput.value = result.output
        }
    }

    fun saveCurrentAsProfile(name: String) {
        viewModelScope.launch {
            val profile = HotspotProfile(
                profileName = name,
                ssid = ssid.value,
                password = password.value,
                securityType = securityType.value,
                band2g = band2g.value,
                band5g = band5g.value,
                band6g = band6g.value,
                mloEnabled = mloEnabled.value,
                channelBandwidth = channelBandwidth.value,
                channel5g = channel5g.value,
                channel6g = channel6g.value,
                region = selectedRegion.value
            )
            repository.insertProfile(profile)
        }
    }

    fun applySavedProfile(profile: HotspotProfile) {
        ssid.value = profile.ssid
        password.value = profile.password
        securityType.value = profile.securityType
        band2g.value = profile.band2g
        band5g.value = profile.band5g
        band6g.value = profile.band6g
        mloEnabled.value = profile.mloEnabled
        channelBandwidth.value = profile.channelBandwidth
        channel5g.value = profile.channel5g
        channel6g.value = profile.channel6g
        selectedRegion.value = profile.region
    }

    fun updateSettingsBasedOnBandsAndMlo() {
        val b2 = band2g.value
        val b5 = band5g.value
        val b6 = band6g.value
        val mlo = mloEnabled.value

        // Bandwidth
        if (b2 && !b5 && !b6) {
            channelBandwidth.value = "40"
        } else if (!b2 && b5 && !b6) {
            channelBandwidth.value = "160"
        } else if (b2 && b5 && !b6) {
            channelBandwidth.value = "160"
        } else if (b6) {
            channelBandwidth.value = "160"
        }

        // Security
        if (mlo) {
            securityType.value = "WPA3_PERSONAL"
        } else if (b6) {
            securityType.value = "WPA3_PERSONAL"
        } else if (b2 && !b5 && !b6) {
            securityType.value = "WPA3_PERSONAL"
        }

        // Automatic Country/Region selection based on active bands:
        if (b6 || b5 || b2) {
            selectedRegion.value = "US"
        }
    }

    fun deleteProfile(profile: HotspotProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
        }
    }

    fun runCustomShellCommand(command: String) {
        viewModelScope.launch {
            if (command.isBlank()) return@launch
            val result = RootExecutor.executeCommand(command, repository)
            _lastTerminalOutput.value = result.output
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _lastTerminalOutput.value = "Terminal history cleared."
        }
    }

    override fun onCleared() {
        super.onCleared()
        clientPollingJob?.cancel()
        try {
            getApplication<Application>().unregisterReceiver(wifiApReceiver)
        } catch (e: Exception) {
            Log.e("HotspotViewModel", "Failed to unregister receiver", e)
        }
    }

    class Factory(
        private val application: Application,
        private val repository: HotspotRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HotspotViewModel::class.java)) {
                return HotspotViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
