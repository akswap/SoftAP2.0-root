# 📶 SoftAP & SoftAP Router APK- Advanced WiFi 7 & 6GHz Hotspot Controller & WEBUI

**SoftAP Controller** is a powerful Android application designed for advanced hotspot (SoftAP) control, multi-band frequency unlocking (2.4GHz, 5GHz, 6GHz), Wi-Fi 7 (802.11be / EHT / MLO) forcing, bandwidth tuning (up to 320 MHz), region code bypass, and offline hotspot tethering.

---

## 🔥 Key Features

- **📶 Multi-Band SoftAP Configuration**:
  - Independent or simultaneous selection of **2.4 GHz**, **5 GHz**, and **6 GHz** (Wi-Fi 6E & Wi-Fi 7).
- **🚀 Wi-Fi 7 & MLO Support**:
  - Force **IEEE 802.11be (EHT)** mode and **MLO (Multi-Link Operation)** across 2.4GHz, 5GHz, and 6GHz bands.
- **⚡ Ultra-Wide Bandwidth Tuning**:
  - Custom channel bandwidth support: **20 MHz, 40 MHz, 80 MHz, 160 MHz, and 320 MHz**.
- **🌐 Offline Hotspot & Data Bypass**:
  - Option to start hotspot without active mobile data or Wi-Fi uplink checks.
- **🌍 Regional Regulatory Code Bypass**:
  - Change Wi-Fi country code on the fly (`US`, `JP`, `IN`, `DE`, `UK`, `CN`) to unlock restricted 6GHz / 5GHz channels via `iw`, `setprop`, `settings`, and Qualcomm config tweaks (`WCNSS_qcom_cfg.ini`).
- **🛡️ Root / Shell CLI Execution Engine**:
  - Direct shell script execution for root-enabled devices to bypass vendor limitations.
- **🔀 VPN Tethering & Interface Routing**:
  - Route connected device traffic through active VPN interfaces (`tun0`, `wlan0`, etc.).

---

## 🛠️ Prerequisite & Requirements

1. **Android Version**: Android 11+ (Android 13/14/15 recommended for 6GHz and Wi-Fi 7 MLO).
2. **Root Access (Recommended for 6GHz / Wi-Fi 7 / Custom Bandwidth)**: Magisk or KernelSU required to execute direct root commands (`iw`, `sed` on Qualcomm config, `cmd wifi`).
3. **Hardware Capability**: Device Wi-Fi chipset must support 6GHz (Wi-Fi 6E/7) at hardware level for 6GHz operation.

---

## 🚀 Quick Setup & Usage

1. **Grant Permissions**:
   - Grant Root Access (Magisk/KernelSU prompt) when requested.
   - Grant Write Settings permission if prompted.
2. **Configure Hotspot Settings**:
   - Set Hotspot SSID and Password.
   - Select Band(s): `2.4 GHz`, `5 GHz`, or `6 GHz`.
   - Select Channel & Bandwidth (`160MHz` / `320MHz`).
   - Toggle **Force Wi-Fi 7 (802.11be)** and **Bypass Offline Check** as needed.
3. **Start Hotspot**:
   - Tap **Start Hotspot**.

---

## 📄 License & Disclaimer

- **Disclaimer**: Unlocking non-standard radio channels or changing regional country codes may be subject to regulatory restrictions in certain jurisdictions. Use responsibly.
- **License**: Open Source project released under the MIT License.




## SoftAP Router APK — Key Features

SoftAP Router is a powerful **local WebUI management solution for rooted Android devices**, designed to turn an Android phone into a feature-rich mobile router and hotspot management platform.

### 🌐 Local WebUI Management

Access and manage the Android router directly from a PC, laptop, or another device connected to the same local network — no cloud connection required.

### 📡 Hotspot & Wireless Configuration

Manage essential hotspot and wireless settings, including:

* Wi-Fi SSID
* Security configuration
* Wireless band and related settings
* Hotspot status and control

### 📶 Cellular Network Monitoring

Monitor real-time cellular information from the WebUI:

* Signal strength
* Network type
* Cellular interface
* Active modem status
* Network connectivity information

### ⚡ Quick Actions

Perform important router operations directly from the dashboard:

* **Restart Radio** — quickly restart the cellular modem/radio
* **Refresh Signal** — instantly refresh cellular signal and network metrics
* **Airplane Mode** — conveniently control Airplane Mode

### 🔄 Automated WebUI Recovery

A built-in recovery mechanism helps when the WebUI becomes unreachable, for example after a hotspot restart or IP address change.

The recovery page guides the user through:

* Checking the current network connection
* Connecting the PC to the mobile hotspot
* Connecting both devices to the same Wi-Fi network
* Using USB/USB tethering when supported
* Finding and opening the current WebUI IP address
* Basic network troubleshooting

### 📴 Offline-Ready Architecture

Designed specifically for **local-network operation**, allowing the router management interface to remain useful even when Internet connectivity is unavailable, unstable, or restricted.

### 🔐 Local-First Design

The management interface is intended for direct local access between the Android device and connected clients, minimizing dependence on external servers or cloud services.

---

**SoftAP Router**
*Turn your rooted Android device into a locally managed smart mobile router.*

