# 🚨 DisasterMesh

**Emergency Communication Network for Disaster Response**

DisasterMesh is an Android application that creates a decentralized mesh network for emergency communication during disasters when traditional communication infrastructure may be compromised.

## 📱 Features

### 🌐 **Mesh Networking**
- **Peer-to-peer communication** using Bluetooth LE + WiFi Direct
- **Offline functionality** - works without internet connectivity
- **Auto-discovery** of nearby devices using Bluetooth LE
- **Message relay** - extends communication range through device hopping

### 🗺️ **Interactive Map**
- **Real-time location detection** - automatically centers on your current location
- **Emergency alert visualization** with risk-level color coding
- **Offline maps** using OpenStreetMap (OSM)
- **Location-based alerts** with customizable radius

### 🚨 **Emergency Alerts**
- **SOS signals** for immediate help requests
- **Admin broadcasts** for official emergency information
- **Risk level classification** (Low, Medium, High, Critical)
- **Automatic alert relay** through the mesh network

### 🎨 **Modern UI/UX**
- **Material Design 3** implementation
- **Dark/Light theme** support
- **Intuitive interface** optimized for emergency situations
- **Accessibility features** for users with disabilities

## 🏗️ **Architecture**

### **Core Components**
- **MeshService** - Background service managing peer connections
- **MapsFragment** - Interactive map with location services
- **AlertMessagingService** - Firebase Cloud Messaging integration
- **AuthorityManager** - Role-based access control (Admin/User)

### **Technology Stack**
- **Language**: Kotlin
- **UI Framework**: Material Design 3
- **Maps**: OpenStreetMap (osmdroid)
- **Networking**: Bluetooth LE + WiFi Direct
- **Location**: Google Play Services Location API
- **Push Notifications**: Firebase Cloud Messaging
- **Architecture**: MVVM with Android Architecture Components

## 🚀 **Getting Started**

### **Prerequisites**
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Google Play Services
- Location services enabled on device

### **Installation**

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/DisasterMesh.git
   cd DisasterMesh
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Configure Firebase (Optional)**
   - Create a new Firebase project
   - Add your `google-services.json` file to `app/` directory
   - Enable Cloud Messaging in Firebase Console

4. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```

### **Configuration**

#### **Signing Configuration**
Update `app/build.gradle` with your signing configuration:
```gradle
signingConfigs {
    release {
        storeFile file("path/to/your/keystore.jks")
        storePassword "YOUR_STORE_PASSWORD"
        keyAlias "YOUR_KEY_ALIAS"
        keyPassword "YOUR_KEY_PASSWORD"
    }
}
```

#### **Admin Access**
Default admin password: `DisasterResponse2024!Secure` (MUST be changed in production)
⚠️ **SECURITY WARNING**: Change admin authentication before public deployment
Location: `MapsFragment.kt` - `verifyAdminPassword()` method

## 📋 **Permissions Required**

### **Essential Permissions**
- `ACCESS_FINE_LOCATION` - For location services and map centering
- `NEARBY_WIFI_DEVICES` - For WiFi Direct discovery (Android 13+)
- `BLUETOOTH_ADVERTISE` - For mesh network advertising (Android 12+)
- `BLUETOOTH_SCAN` - For discovering nearby devices (Android 12+)
- `BLUETOOTH_CONNECT` - For establishing connections (Android 12+)
- `CHANGE_WIFI_STATE` - For WiFi Direct connections
- `CHANGE_NETWORK_STATE` - For WiFi Direct group management

### **Optional Permissions**
- `POST_NOTIFICATIONS` - For emergency alert notifications (Android 13+)

## 🎯 **Usage**

### **For Regular Users**
1. **Grant permissions** when prompted
2. **View your location** on the map automatically
3. **Send SOS signals** using the red emergency button
4. **Receive alerts** from emergency authorities
5. **Relay messages** automatically to extend network reach

### **For Emergency Authorities**
1. **Switch to Admin mode** using the role switch button
2. **Enter admin password** (default: `DisasterMesh2024!Emergency`)
3. **Broadcast emergency alerts** with risk levels
4. **Monitor network activity** through activity logs
5. **Coordinate response efforts** using location data

## 🔧 **Development**

### **Project Structure**
```
app/src/main/
├── java/com/example/disastermesh/
│   ├── data/           # Data management classes
│   ├── logging/        # Logging utilities
│   ├── mesh/          # Mesh networking components
│   ├── model/         # Data models
│   ├── util/          # Utility classes
│   ├── MainActivity.kt
│   └── MapsFragment.kt
├── res/
│   ├── layout/        # UI layouts
│   ├── values/        # Colors, strings, styles
│   └── drawable/      # Icons and graphics
└── AndroidManifest.xml
```

### **Key Classes**
- `MeshService` - Manages peer-to-peer connections
- `DisasterAlert` - Alert data model with location and risk info
- `AuthorityManager` - Handles user roles and permissions
- `AlertCodec` - Serializes/deserializes alert messages
- `RiskColor` - Maps risk levels to visual colors

### **Building for Release**
```bash
./gradlew assembleRelease
```

## 🤝 **Contributing**

We welcome contributions! Please follow these steps:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit your changes** (`git commit -m 'Add amazing feature'`)
4. **Push to the branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

### **Code Style**
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add comments for complex logic
- Write unit tests for new features

## 📄 **License**

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 **Emergency Use Disclaimer**

**IMPORTANT**: This application is designed to assist in emergency communication but should not be relied upon as the sole means of emergency communication. Always follow official emergency procedures and contact professional emergency services when available.

## 🙏 **Acknowledgments**

- **OpenStreetMap** for providing free map data
- **Google** for Play Services Location API
- **Firebase** for cloud messaging services
- **Material Design** for UI/UX guidelines
- **OSMDroid** for Android map implementation

## 📞 **Support**

- **Issues**: [GitHub Issues](https://github.com/yourusername/DisasterMesh/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/DisasterMesh/discussions)
- **Email**: your.email@example.com

## 🔄 **Changelog**

### **v1.0.0** (Current)
- ✅ Initial release with mesh networking
- ✅ Material Design 3 UI implementation
- ✅ Real-time location detection
- ✅ Emergency alert system
- ✅ Role-based access control
- ✅ Dark/Light theme support

---

**Built with ❤️ for emergency preparedness and community safety**