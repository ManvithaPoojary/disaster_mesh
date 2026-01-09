# 🔧 Mesh Network Troubleshooting Guide

## 🚨 **Issue: SOS/Broadcast alerts not showing between devices**

### **Quick Diagnosis Steps**

1. **Tap the WiFi Debug FAB** (new blue WiFi icon) in the app
2. **Review the diagnostic report** - it will show exactly what's wrong
3. **Tap "Test Connectivity"** to run automated tests
4. **Check logs** via the logs FAB button

### **Common Issues & Solutions**

#### 🔵 **Bluetooth Issues**
```
❌ Bluetooth not enabled
✅ Solution: Enable Bluetooth in device settings
```

```
❌ BLE not supported
✅ Solution: Use a different device (BLE required for discovery)
```

```
❌ Bluetooth permissions denied
✅ Solution: Grant Bluetooth permissions in app settings
```

#### 📶 **WiFi Direct Issues**
```
❌ WiFi not enabled
✅ Solution: Enable WiFi in device settings
```

```
❌ Location permission denied
✅ Solution: Grant "Precise Location" permission (required for WiFi Direct)
```

```
❌ WiFi Direct not supported
✅ Solution: Check if device supports WiFi Direct (most modern Android devices do)
```

#### 📍 **Location Services Issues**
```
❌ Location services disabled
✅ Solution: Enable Location Services in device settings
```

```
❌ App location permission denied
✅ Solution: Go to Settings > Apps > DisasterMesh > Permissions > Location > Allow
```

#### 🔗 **Mesh Service Issues**
```
❌ MeshService not running
✅ Solution: Restart the app or use "Restart Mesh" button in debug dialog
```

### **Step-by-Step Troubleshooting**

#### **Step 1: Check Basic Requirements**
Both devices must have:
- ✅ Bluetooth enabled
- ✅ WiFi enabled  
- ✅ Location services enabled
- ✅ DisasterMesh app running
- ✅ All permissions granted

#### **Step 2: Verify Permissions**
Required permissions:
- 📍 **Location** (Precise/Fine) - CRITICAL
- 🔵 **Bluetooth** (Scan, Advertise, Connect) - CRITICAL  
- 📶 **WiFi State** (Change WiFi State) - CRITICAL

#### **Step 3: Test Device Discovery**
1. **Open debug dialog** (tap WiFi debug FAB)
2. **Look for "BLE device found"** messages in logs
3. **Check "WiFi peers"** count in status

#### **Step 4: Test Alert Relay**
1. **Long press the WiFi debug FAB** to send test alert
2. **Check if test alert appears** on the same device
3. **Check logs** for "Alert sent" or "Alert received" messages

#### **Step 5: Force Restart Mesh**
1. **Open debug dialog**
2. **Tap "Restart Mesh"** button
3. **Wait 10 seconds** for mesh to reinitialize
4. **Try sending alert again**

### **Advanced Debugging**

#### **Check Mesh Service Status**
```kotlin
// In logs, look for these messages:
✅ "MeshService created - Broadcasting as: DM_..."
✅ "BLE advertising started"  
✅ "BLE scanning started"
✅ "WiFi Direct discovery started"
❌ "Bluetooth LE not available"
❌ "WiFi Direct not available"
```

#### **Monitor Device Discovery**
```kotlin
// Successful discovery shows:
✅ "Discovered Bluetooth device: DM_..."
✅ "WiFi Direct peers updated: X devices"
✅ "WiFi connected: ..."
```

#### **Track Alert Flow**
```kotlin
// Alert sending:
✅ "Relaying alert via WiFi Direct"
✅ "Alert sent successfully via WiFi Direct"

// Alert receiving:
✅ "Received alert via WiFi Direct"
✅ "Processed alert (HIGH) from ADMIN"
```

### **Device-Specific Issues**

#### **Samsung Devices**
- May have aggressive power management
- **Solution**: Add DisasterMesh to "Never sleeping apps"
- **Path**: Settings > Device Care > Battery > App Power Management

#### **Xiaomi/MIUI Devices**
- MIUI may block background services
- **Solution**: Enable "Autostart" for DisasterMesh
- **Path**: Settings > Apps > Manage Apps > DisasterMesh > Autostart

#### **Huawei Devices**
- May have strict background app limits
- **Solution**: Add to "Protected Apps" list
- **Path**: Settings > Apps > Protected Apps

### **Network Environment Issues**

#### **Corporate/School WiFi**
- May block WiFi Direct
- **Solution**: Use mobile hotspot or different network

#### **Crowded Areas**
- Too many WiFi networks can interfere
- **Solution**: Move to less crowded area or use 5GHz WiFi

### **Testing Scenarios**

#### **Single Device Test**
1. Send test alert (long press WiFi debug FAB)
2. Alert should appear on same device
3. Check logs for encoding/decoding success

#### **Two Device Test**
1. Both devices run DisasterMesh
2. Wait 30 seconds for discovery
3. Send alert from one device
4. Should appear on other device within 10 seconds

#### **Range Test**
1. Start with devices close together (1-2 meters)
2. Gradually increase distance
3. BLE discovery works up to ~100m
4. WiFi Direct works up to ~200m

### **Performance Optimization**

#### **Battery Optimization**
- BLE uses minimal power for discovery
- WiFi Direct only activates for data transfer
- Service runs in foreground to prevent killing

#### **Connection Speed**
- Initial discovery: 10-30 seconds
- Alert relay: 1-5 seconds once connected
- Automatic reconnection on disconnect

### **Emergency Fallback**

If mesh networking fails completely:
1. **Use mobile data** for Firebase alerts (if available)
2. **Manual sharing** via SMS/calls
3. **Check device compatibility** - some very old devices may not support required features

### **Getting Help**

If issues persist:
1. **Export logs** via logs FAB button
2. **Note device models** and Android versions
3. **Document exact error messages** from debug dialog
4. **Test with different device combinations**

The new implementation is much more robust than the old Nearby API, but requires proper permissions and device capabilities to function correctly.