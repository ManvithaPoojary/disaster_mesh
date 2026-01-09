# WiFi Direct + Bluetooth LE Mesh Implementation Guide

## Overview

This implementation replaces Google's Nearby Connections API with a hybrid approach using:
- **Bluetooth LE** for low-power device discovery
- **WiFi Direct** for high-bandwidth data transfer

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Device A      │    │   Device B      │    │   Device C      │
│                 │    │                 │    │                 │
│ BLE Advertising │◄──►│ BLE Scanning    │◄──►│ BLE Advertising │
│ WiFi Direct     │    │ WiFi Direct     │    │ WiFi Direct     │
│ Group Owner     │◄──►│ Client          │◄──►│ Client          │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Key Components

### 1. BluetoothLeManager
- Handles BLE advertising and scanning
- Discovers nearby DisasterMesh devices
- Low power consumption for continuous operation
- Range: ~100 meters

### 2. WiFiDirectManager  
- Manages WiFi Direct connections
- Handles peer-to-peer group formation
- TCP socket communication for data transfer
- Range: ~200 meters, High bandwidth

### 3. MeshService (Updated)
- Orchestrates both BLE and WiFi Direct
- Implements callback interfaces for both managers
- Handles alert relay logic
- Maintains device discovery state

## Usage

### Testing the Implementation

```kotlin
// In your activity or service
val meshTest = MeshNetworkTest(this)
meshTest.runAllTests()
```

### Manual Testing Steps

1. **Enable Required Features**:
   - Turn on Bluetooth
   - Turn on WiFi
   - Grant location permissions

2. **Start Mesh Service**:
   ```kotlin
   val intent = Intent(this, MeshService::class.java)
   intent.action = MeshService.ACTION_START_BROADCASTING
   startForegroundService(intent)
   ```

3. **Send Test Alert**:
   ```kotlin
   val testAlert = DisasterAlert(
       alertId = "test-${System.currentTimeMillis()}",
       latitude = 37.7749,
       longitude = -122.4194,
       radiusMeters = 1000.0,
       levelOfRisk = RiskLevel.HIGH,
       message = "Test emergency alert",
       receivedAtMillis = System.currentTimeMillis(),
       senderAuthority = Authority.ADMIN,
       targetAuthority = Authority.ALL
   )
   
   val relayIntent = Intent(this, MeshService::class.java)
   relayIntent.action = MeshService.ACTION_RELAY_ALERT
   relayIntent.putExtra(MeshService.EXTRA_ALERT, testAlert)
   startService(relayIntent)
   ```

## Advantages Over Nearby API

### ✅ **No Google Dependencies**
- Works on all Android devices
- No Google Play Services required
- Open source implementation

### ✅ **Better Performance**
- BLE: Ultra-low power discovery
- WiFi Direct: Up to 250 Mbps data transfer
- Longer range than Nearby API

### ✅ **Enhanced Control**
- Custom connection logic
- Configurable discovery parameters
- Direct socket programming

### ✅ **Improved Reliability**
- Dual-technology fallback
- Automatic retry mechanisms
- Better error handling

## Permissions Required

```xml
<!-- Location (required for both BLE and WiFi Direct) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- Bluetooth LE -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- WiFi Direct -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
```

## Troubleshooting

### Common Issues

1. **BLE Not Working**:
   - Check if device supports BLE: `PackageManager.FEATURE_BLUETOOTH_LE`
   - Ensure Bluetooth is enabled
   - Verify location permissions granted

2. **WiFi Direct Not Connecting**:
   - Check if WiFi is enabled
   - Ensure location permissions granted
   - Some devices have WiFi Direct limitations

3. **No Devices Discovered**:
   - Ensure both devices are running the app
   - Check that BLE advertising is active
   - Verify devices are within range (~100m for BLE)

### Debug Logging

The implementation includes extensive logging via `AlertLog`. Check logs for:
- BLE advertising/scanning status
- WiFi Direct connection events
- Data transfer success/failure
- Device discovery events

### Performance Optimization

1. **Battery Usage**:
   - BLE scanning uses low power mode
   - WiFi Direct connections are established only when needed
   - Automatic cleanup when devices disconnect

2. **Range Extension**:
   - Each device acts as a relay
   - Mesh topology extends effective range
   - Automatic multi-hop message forwarding

## Migration from Nearby API

The new implementation maintains the same external interface:
- Same `MeshService` actions and extras
- Same alert processing logic
- Same notification system
- Same authority-based filtering

Only the underlying transport mechanism has changed from Nearby API to BLE + WiFi Direct.