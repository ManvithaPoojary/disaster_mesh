# 🚨 DisasterMesh

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/raviprasad794063/disaster_mesh)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blue.svg)](https://kotlinlang.org)

**Offline Emergency Mesh Communication for Disaster Scenarios**

DisasterMesh is an Android-based peer-to-peer mesh networking application designed to enable communication when cellular networks and the internet are unavailable. It allows nearby devices to form a decentralized mesh using Bluetooth and Wi-Fi Direct, enabling emergency messaging and SOS alerts during disasters.

## 🌍 Why DisasterMesh?

During natural disasters, riots, or remote-area emergencies, traditional communication infrastructure often fails. DisasterMesh enables people to stay connected without relying on the internet, servers, or mobile networks.

This project focuses on **resilience**, **decentralization**, and **real-world emergency usability**.

## 🔑 Key Features

- **📡 Offline mesh networking** using Bluetooth & Wi-Fi Direct
- **🆘 SOS broadcast messages** to nearby devices
- **🔁 Multi-hop message relaying** across devices
- **🗺️ Map-based UI** with location awareness
- **📱 Material Design UI** optimized for Android
- **⚡ No internet, no servers, no accounts** required

## 📱 App Screenshots & Demo

### 🚨 Emergency Alert System
<div align="center">
  <img src="docs/screenshots/sos-emergency-dialog.png" alt="SOS Emergency Dialog - User Mode" width="300"/>
  <img src="docs/screenshots/admin-broadcast-dialog.png" alt="Admin Broadcast Dialog" width="300"/>
</div>

*Emergency alert creation: User SOS signal (left) → Admin broadcast (right) with risk level selection*

### 🗺️ Emergency Map Interface
<div align="center">
  <img src="docs/screenshots/emergency-map-with-alerts.png" alt="Emergency Map with Multiple Alerts" width="400"/>
</div>

*Real-time emergency map showing multiple alert locations, mesh network status, and OpenStreetMap integration*

### 🔑 Key Features Demonstrated
- **✅ Material Design 3** - Modern dark theme optimized for emergency use
- **✅ Role-based Access** - Clear User/Admin mode switching with visual indicators
- **✅ Risk Level Classification** - Color-coded severity (Low/Medium/High/Critical)
- **✅ Location Integration** - GPS coordinates with "Use Current Location" button
- **✅ Mesh Network Status** - Real-time connection status display
- **✅ Professional UI** - Large buttons and clear layouts for stress situations

---

> **📸 Screenshot Details:**
> 
> These screenshots show DisasterMesh running on Android with:
> - **User Mode**: SOS signal creation with location and risk level selection
> - **Admin Mode**: Emergency broadcast with authority-level controls
> - **Map View**: Real-world emergency alerts displayed on OpenStreetMap
> - **Mesh Status**: Network connectivity and device discovery indicators

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Platform**: Android
- **Networking**: Bluetooth, Wi-Fi Direct
- **Maps**: OpenStreetMap
- **Architecture**: Modular & scalable mesh layers
- **UI**: Material Design Components

## 🚀 Getting Started

### Clone the repository
```bash
git clone https://github.com/raviprasad794063/disaster_mesh.git
cd disaster_mesh
```

### Build the project
```bash
./gradlew assembleDebug
```

### Install on device
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

⚠️ **Requires Bluetooth & Wi-Fi permissions enabled on the device.**

## 🧠 Architecture Overview

DisasterMesh uses a decentralized mesh topology where each device can:

- **Discover nearby peers**
- **Relay messages for other nodes**
- **Act independently without a central coordinator**

This makes the system fault-tolerant and scalable in emergency conditions.

## 🎯 Use Cases

- **Natural disasters** (earthquakes, floods, cyclones)
- **Network outages**
- **Remote or rural areas**
- **Emergency response coordination**
- **Disaster preparedness drills**

## 📊 Performance

| Metric | Value |
|--------|-------|
| **Range** | 100m direct, unlimited via mesh |
| **Battery Life** | 12+ hours continuous operation |
| **Connection Time** | 5-15 seconds between paired devices |
| **Supported Devices** | All Android 7.0+ devices |
| **Offline Maps** | Full OpenStreetMap integration |

## 🏷️ GitHub Topics

To improve discoverability, add these topics to your GitHub repository:

**Core Topics:** `android`, `kotlin`, `mesh-networking`, `p2p`, `bluetooth`, `wifi-direct`, `emergency-communication`, `disaster-response`, `offline-first`, `decentralized`

**Additional:** `peer-to-peer`, `emergency-app`, `disaster-management`, `crisis-communication`, `mobile-mesh`, `humanitarian-tech`, `resilient-communication`, `emergency-preparedness`, `public-safety`, `offline-communication`

> 📖 **How to Add**: Go to your GitHub repo → Click gear icon next to "About" → Add topics → Save changes. See [detailed guide](docs/internal/GITHUB_TOPICS_SETUP.md).

## 🔧 Development

```bash
# Run tests
./gradlew test

# Debug mesh networking
./gradlew installDebug
adb logcat | grep "DisasterMesh"

# Build release
./gradlew assembleRelease
```

**Architecture:** MVVM with clean separation between UI, mesh networking, and emergency services.

## 🤝 Contributing

We need help with:
- [ ] iOS version development
- [ ] Mesh protocol optimization  
- [ ] UI/UX improvements
- [ ] Translation to other languages
- [ ] Field testing in emergency scenarios

[Contributing Guidelines](CONTRIBUTING.md) | [Code of Conduct](CODE_OF_CONDUCT.md)

## 📜 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) for details.

## 👤 Author

**Ravi Prasad**  
Engineering Student | Android & Systems Enthusiast

- GitHub: [@raviprasad794063](https://github.com/raviprasad794063)
- Project: [disaster_mesh](https://github.com/raviprasad794063/disaster_mesh)

## ⚠️ Emergency Disclaimer

This app assists emergency communication but should not be your only emergency plan. Always contact professional emergency services (911, local authorities) when available.

---

**Built for emergency preparedness. Tested for when it matters most.**

🐛 [Report Issues](https://github.com/raviprasad794063/disaster_mesh/issues) | 💬 [Join Discussion](https://github.com/raviprasad794063/disaster_mesh/discussions) | 🤝 [Contributing](CONTRIBUTING.md)