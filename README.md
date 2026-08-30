# 📶 SoftAP 2.0 — Root Wi‑Fi 7 Hotspot Controller & Router WebUI

**SoftAP 2.0** is an advanced rooted Android SoftAP / hotspot controller with a built-in router-style WebUI for live wireless control, client monitoring and telemetry.

The current tested build supports **2.4 GHz, 5 GHz and 6 GHz**, including **Wi‑Fi 7 (802.11be)** operation and **320 MHz on 6 GHz** on supported hardware/ROMs.

![Android](https://img.shields.io/badge/Android-Rooted-green)
![WiFi 7](https://img.shields.io/badge/Wi--Fi%207-802.11be-blue)
![6 GHz](https://img.shields.io/badge/6%20GHz-Supported-success)
![320 MHz](https://img.shields.io/badge/320%20MHz-Tested-success)
![WebUI](https://img.shields.io/badge/WebUI-Built--in-purple)

> **Important:** This project controls hardware/firmware features already present on the device. Actual channel availability, bandwidth and Wi‑Fi standard depend on the chipset, Android/ROM, firmware, driver and local regulations. Use only frequencies and channels permitted for your location and equipment.

---

## 🔥 Current Tested Capabilities

| Band | Typical maximum profile used by the app | Wi‑Fi mode | Current status |
|---|---:|---|---|
| **2.4 GHz** | up to ~**688 Mbps** class | Wi‑Fi 6/7 where supported | ✅ Supported |
| **5 GHz** | up to ~**2882 Mbps** class | Wi‑Fi 7 / 802.11be where supported | ✅ Supported |
| **6 GHz** | up to ~**5764 Mbps** class | **Wi‑Fi 7 / 802.11be** | ✅ Tested |
| **6 GHz / 320 MHz** | ~**5.76 Gbps PHY class** | **Wi‑Fi 7 / EHT** | ✅ Negotiated 320 MHz confirmed |

PHY rates are dynamic and depend on channel width, MCS, NSS, signal quality and client capability.

---

## ✅ 6 GHz / 320 MHz / Wi‑Fi 7 — End-to-End Test Result

The current SoftAP 2.0 setup has been tested with a **6 GHz Wi‑Fi 7 hotspot on Channel 37 (6135 MHz)**.

### SoftAP side

Live app/WebUI telemetry showed:

```text
SSID                  : MobSoftAP_Router
Band                  : 6 GHz
Operating Channel     : 37
Frequency             : 6135 MHz
Configured Width      : 320 MHz
Negotiated Width      : 320 MHz
PHY Mode              : Wi‑Fi 7 (802.11be)
Security              : WPA3-Personal (SAE)
Theoretical Max PHY   : ~5.76 Gbps / 5764 Mbps
```

The **negotiated 320 MHz width** and live negotiated PHY telemetry are obtained from the running wireless interface / station information rather than being inferred only from a configuration dropdown.

### Windows client side

A Windows PC using **Intel(R) Wi‑Fi 7 BE200 320MHz** connected to the same SoftAP and `netsh wlan show interfaces` reported:

```text
SSID                   : MobSoftAP_Router
Band                   : 6 GHz
Channel                : 37
Radio type             : 802.11be
Authentication         : WPA3-Personal (H2E)
Receive rate           : 5764.8 Mbps
Transmit rate          : 5764.8 Mbps
Signal                 : 99%
RSSI                   : -39 dBm
```

This provides client-side confirmation of an active **6 GHz / Channel 37 / 802.11be** connection while the SoftAP side reports **320 MHz negotiated width**.

---

## 🌐 Router-Style WebUI

SoftAP 2.0 includes a built-in WebUI designed to behave more like a dedicated router management interface than a basic Android hotspot screen.

Current WebUI pages/features include:

- **Overview dashboard** — hotspot state, band/channel/width, connected clients, internet source, traffic, CPU/RAM/battery/storage and system health
- **Wireless Settings** — SSID, WPA3 security, band, channel, bandwidth and live wireless telemetry
- **Cellular** — mobile-data / SIM information
- **Firewall & QoS** — traffic-policy controls
- **Diagnostic Tools** — runtime diagnostics and wireless checks
- **System Info** — root / service / platform information
- **Live traffic graphs** — download/upload/packet/error telemetry
- **Client count and client monitoring**
- **Live PHY capability telemetry**

### Live wireless telemetry

The WebUI can display the running state separately from the configured state, including values such as:

```text
Band
Actual operating channel
Frequency
Configured width
Negotiated width
Wi‑Fi standard / PHY mode
Negotiated PHY rate
MCS information
Connected clients
```

This is particularly useful for distinguishing **“320 MHz configured”** from **“320 MHz actually negotiated.”**

---

## 📡 Band & Channel Control

SoftAP 2.0 supports:

- **2.4 GHz**
- **5 GHz**
- **6 GHz (Wi‑Fi 6E / Wi‑Fi 7)**
- 20 / 40 / 80 / 160 / **320 MHz** width where supported
- fixed-channel selection where supported
- **Auto / ACS** channel selection

### 320 MHz ACS observation

On the current tested device, **320 MHz operation is reliable when the hotspot is started using Auto / ACS**. After SoftAP start/restart, ACS selects the actual operating channel; the app/WebUI then reads and displays the live channel from the running wireless state.

Example observed flow:

```text
Configured channel mode : Auto (ACS)
          ↓
SoftAP starts/restarts
          ↓
ACS selects live channel
          ↓
Observed operating Ch   : 37 (6135 MHz)
Negotiated width        : 320 MHz
PHY mode                : Wi‑Fi 7 (802.11be)
```

So **Auto (ACS)** is the configuration mode, while **Channel 37** is the actual channel selected during that test.

---

## ⚡ Wi‑Fi 7 / EHT Features

Depending on chipset, ROM and firmware support, SoftAP 2.0 exposes or reports features including:

- IEEE **802.11be / EHT**
- **320 MHz** channel width
- MU‑MIMO
- OFDMA
- Target Wake Time
- WPA3-Personal / SAE
- MLO controls where the underlying platform supports them

MLO availability is platform-dependent and is not required for a single-link 6 GHz / 320 MHz Wi‑Fi 7 connection.

---

## 🛠️ Requirements

- Rooted Android device
- Magisk / KernelSU / APatch or equivalent root environment
- Wi‑Fi hardware that supports the requested band and standard
- 6 GHz capable chipset for 6 GHz operation
- Wi‑Fi 7 capable hardware/firmware for 802.11be / 320 MHz operation
- Compatible Android/ROM vendor Wi‑Fi stack

The app cannot create radio capabilities that are absent from the hardware or firmware.

---

## 🔍 Verification

For independent verification on Android, inspect the running SoftAP / wireless interface state using the platform's normal diagnostic tools. Useful evidence includes:

```text
actual band
actual frequency/channel
configured width
negotiated width
Wi‑Fi standard
station PHY rate
```

On a Windows Wi‑Fi client, a useful live check is:

```powershell
netsh wlan show interfaces
```

Look for the SSID, band, channel, radio type and current receive/transmit rate.

---

## 📸 Evidence Screenshots

The current evidence set for the latest SoftAP 2.0 build includes:

1. **Android SoftAP Controller** — Hotspot Active, 6 GHz, Ch 37, 320 MHz, Wi‑Fi 7
2. **SoftAP Router WebUI Overview** — SoftAP Online, 6 GHz, Ch 37, 320 MHz, Wi‑Fi 7, connected clients
3. **Wireless Settings / Live PHY Telemetry** — configured **320 MHz** and negotiated **320 MHz**, live PHY rate read from the running wireless interface/station data
4. **Windows Network Properties** — Intel BE200 connected to `MobSoftAP_Router`, 6 GHz Ch 37, 802.11be, 5764/5764 Mbps class link
5. **Windows `netsh wlan show interfaces`** — live 5764.8/5764.8 Mbps, Ch 37, 802.11be connection

> When publishing screenshots, redact hotspot passwords, private IP information or other credentials that are not required as technical evidence.

---

## 🧪 Tested Reference Result

```text
SoftAP SSID       : MobSoftAP_Router
Band              : 6 GHz
Channel           : 37 / 6135 MHz
Channel width     : 320 MHz negotiated
Wi‑Fi standard    : Wi‑Fi 7 / 802.11be
Security          : WPA3-Personal / SAE
PC client         : Intel Wi‑Fi 7 BE200 320MHz
PC radio type     : 802.11be
PC live rate      : 5764.8 / 5764.8 Mbps
```

This is a test result from the current setup, not a guarantee of identical performance on every Android device.

---

## ⚠️ Compatibility & Limitations

Results can vary with:

- chipset / RF hardware
- vendor WLAN firmware
- Android / ROM version
- kernel and Wi‑Fi driver
- regulatory configuration
- selected channel
- client capabilities
- signal / interference
- thermal and power state

Some controls may be visible in the UI but unavailable on platforms whose driver/firmware does not expose the corresponding capability.

---

## 📄 Safety / Regulatory Note

SoftAP 2.0 is intended for technical testing and legitimate hotspot/router use on supported hardware. Band/channel availability is jurisdiction-specific. A UI option or hardware capability does **not** grant permission to transmit on a frequency that is restricted at your location.

---

## ⭐ Project Goal

The goal of SoftAP 2.0 is to turn a capable rooted Android device into a **powerful multi-band SoftAP/router platform** with a clean app interface, detailed WebUI, live wireless telemetry and proper verification of the actual runtime PHY state.

If this project is useful, please consider starring the repository.
