package com.example.disastermesh.mesh

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.disastermesh.logging.AlertLog
import android.Manifest

class MeshDebugger(private val context: Context) {
    
    fun diagnoseConnectivityIssues(): String {
        val report = StringBuilder()
        report.appendLine("=== MESH NETWORK DIAGNOSTIC REPORT ===")
        report.appendLine("Timestamp: ${System.currentTimeMillis()}")
        report.appendLine()
        
        // Check Bluetooth status
        report.appendLine("📱 BLUETOOTH STATUS:")
        val bluetoothStatus = checkBluetoothStatus()
        report.appendLine(bluetoothStatus)
        report.appendLine()
        
        // Check WiFi status
        report.appendLine("📶 WIFI STATUS:")
        val wifiStatus = checkWiFiStatus()
        report.appendLine(wifiStatus)
        report.appendLine()
        
        // Check permissions
        report.appendLine("🔐 PERMISSIONS STATUS:")
        val permissionStatus = checkPermissions()
        report.appendLine(permissionStatus)
        report.appendLine()
        
        // Check mesh service status
        report.appendLine("🔗 MESH SERVICE STATUS:")
        val meshStatus = checkMeshServiceStatus()
        report.appendLine(meshStatus)
        report.appendLine()
        
        // Recommendations
        report.appendLine("💡 RECOMMENDATIONS:")
        val recommendations = generateRecommendations()
        report.appendLine(recommendations)
        
        val fullReport = report.toString()
        AlertLog.d("Mesh Diagnostic Report:\n$fullReport")
        return fullReport
    }
    
    private fun checkBluetoothStatus(): String {
        val report = StringBuilder()
        
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            
            if (bluetoothAdapter == null) {
                report.appendLine("❌ Bluetooth not supported on this device")
                return report.toString()
            }
            
            report.appendLine("✅ Bluetooth adapter available")
            report.appendLine("   Enabled: ${bluetoothAdapter.isEnabled}")
            report.appendLine("   State: ${getBluetoothStateString(bluetoothAdapter.state)}")
            
            // Check BLE support
            val bleSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
            report.appendLine("   BLE Support: ${if (bleSupported) "✅ Yes" else "❌ No"}")
            
            // Check BLE advertiser
            val advertiser = bluetoothAdapter.bluetoothLeAdvertiser
            report.appendLine("   BLE Advertiser: ${if (advertiser != null) "✅ Available" else "❌ Not available"}")
            
            // Check BLE scanner
            val scanner = bluetoothAdapter.bluetoothLeScanner
            report.appendLine("   BLE Scanner: ${if (scanner != null) "✅ Available" else "❌ Not available"}")
            
        } catch (e: Exception) {
            report.appendLine("❌ Error checking Bluetooth: ${e.message}")
        }
        
        return report.toString()
    }
    
    private fun checkWiFiStatus(): String {
        val report = StringBuilder()
        
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            
            report.appendLine("✅ WiFi manager available")
            report.appendLine("   WiFi Enabled: ${wifiManager.isWifiEnabled}")
            report.appendLine("   WiFi State: ${getWiFiStateString(wifiManager.wifiState)}")
            
            // Check WiFi Direct support
            val wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
            report.appendLine("   WiFi Direct Support: ${if (wifiP2pManager != null) "✅ Available" else "❌ Not available"}")
            
            // Check connection info
            val connectionInfo = wifiManager.connectionInfo
            if (connectionInfo != null) {
                report.appendLine("   Current Network: ${connectionInfo.ssid}")
                report.appendLine("   Signal Strength: ${connectionInfo.rssi} dBm")
            }
            
        } catch (e: Exception) {
            report.appendLine("❌ Error checking WiFi: ${e.message}")
        }
        
        return report.toString()
    }
    
    private fun checkPermissions(): String {
        val report = StringBuilder()
        
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions.addAll(listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            ))
        } else {
            requiredPermissions.addAll(listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            ))
        }
        
        requiredPermissions.addAll(listOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE
        ))
        
        var allGranted = true
        for (permission in requiredPermissions) {
            val granted = ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            val status = if (granted) "✅" else "❌"
            report.appendLine("   $status $permission")
            if (!granted) allGranted = false
        }
        
        report.appendLine()
        report.appendLine("Overall Permission Status: ${if (allGranted) "✅ All granted" else "❌ Missing permissions"}")
        
        return report.toString()
    }
    
    private fun checkMeshServiceStatus(): String {
        val report = StringBuilder()
        
        try {
            // Check if service is running (this is a simplified check)
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
            
            val meshServiceRunning = runningServices.any { 
                it.service.className.contains("MeshService") || it.service.className.contains("SimpleMeshService")
            }
            
            val serviceType = if (runningServices.any { it.service.className.contains("SimpleMeshService") }) {
                "SimpleMeshService (Bluetooth Classic)"
            } else if (runningServices.any { it.service.className.contains("MeshService") }) {
                "MeshService (WiFi Direct + BLE)"
            } else {
                "None"
            }
            
            report.appendLine("   Service Running: ${if (meshServiceRunning) "✅ Yes" else "❌ No"}")
            report.appendLine("   Service Type: $serviceType")
            
            if (!meshServiceRunning) {
                report.appendLine("   ⚠️  No mesh service running - alerts won't be relayed")
            }
            
            // Check paired devices for SimpleMeshService
            if (serviceType.contains("SimpleMeshService")) {
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
                val bluetoothAdapter = bluetoothManager.adapter
                val pairedDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
                val disasterMeshDevices = pairedDevices.filter { it.name?.startsWith("DM_") == true }
                
                report.appendLine("   Paired DisasterMesh Devices: ${disasterMeshDevices.size}")
                disasterMeshDevices.forEach { device ->
                    report.appendLine("     - ${device.name} (${device.address})")
                }
            }
            
        } catch (e: Exception) {
            report.appendLine("❌ Error checking service status: ${e.message}")
        }
        
        return report.toString()
    }
    
    private fun generateRecommendations(): String {
        val report = StringBuilder()
        
        // Check Bluetooth
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
        
        if (bluetoothAdapter?.isEnabled != true) {
            report.appendLine("1. 🔵 Enable Bluetooth in device settings")
        }
        
        // Check WiFi
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) {
            report.appendLine("2. 📶 Enable WiFi in device settings")
        }
        
        // Check location services
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            report.appendLine("3. 📍 Enable Location Services (required for BLE and WiFi Direct)")
        }
        
        // Check permissions
        val fineLocationGranted = ActivityCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!fineLocationGranted) {
            report.appendLine("4. 🔐 Grant Location permission in app settings")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bluetoothScanGranted = ActivityCompat.checkSelfPermission(
                context, 
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!bluetoothScanGranted) {
                report.appendLine("5. 🔐 Grant Bluetooth permissions in app settings")
            }
        }
        
        report.appendLine("6. 📱 Ensure both devices are running DisasterMesh app")
        report.appendLine("7. 📏 Keep devices within 100m range for BLE discovery")
        report.appendLine("8. 🔄 Try restarting the app if issues persist")
        
        return report.toString()
    }
    
    private fun getBluetoothStateString(state: Int): String {
        return when (state) {
            BluetoothAdapter.STATE_OFF -> "OFF"
            BluetoothAdapter.STATE_TURNING_ON -> "TURNING_ON"
            BluetoothAdapter.STATE_ON -> "ON"
            BluetoothAdapter.STATE_TURNING_OFF -> "TURNING_OFF"
            else -> "UNKNOWN($state)"
        }
    }
    
    private fun getWiFiStateString(state: Int): String {
        return when (state) {
            WifiManager.WIFI_STATE_DISABLED -> "DISABLED"
            WifiManager.WIFI_STATE_DISABLING -> "DISABLING"
            WifiManager.WIFI_STATE_ENABLED -> "ENABLED"
            WifiManager.WIFI_STATE_ENABLING -> "ENABLING"
            WifiManager.WIFI_STATE_UNKNOWN -> "UNKNOWN"
            else -> "UNKNOWN($state)"
        }
    }
    
    fun testMeshConnectivity() {
        AlertLog.d("=== STARTING MESH CONNECTIVITY TEST ===")
        
        // Test BLE functionality
        testBluetoothLE()
        
        // Test WiFi Direct functionality  
        testWiFiDirect()
        
        // Test alert encoding/decoding
        testAlertCodec()
        
        AlertLog.d("=== MESH CONNECTIVITY TEST COMPLETE ===")
    }
    
    private fun testBluetoothLE() {
        AlertLog.d("Testing Bluetooth LE...")
        
        val bleManager = BluetoothLeManager(context, object : BluetoothLeManager.BluetoothLeCallback {
            override fun onDeviceDiscovered(device: android.bluetooth.BluetoothDevice, deviceName: String) {
                AlertLog.d("✅ BLE Test: Device discovered - $deviceName")
            }
            override fun onDeviceLost(device: android.bluetooth.BluetoothDevice) {
                AlertLog.d("ℹ️ BLE Test: Device lost - ${device.address}")
            }
            override fun onAdvertisingStarted() {
                AlertLog.d("✅ BLE Test: Advertising started successfully")
            }
            override fun onAdvertisingFailed(errorCode: Int) {
                AlertLog.e("❌ BLE Test: Advertising failed - $errorCode")
            }
            override fun onScanStarted() {
                AlertLog.d("✅ BLE Test: Scanning started successfully")
            }
            override fun onScanFailed(errorCode: Int) {
                AlertLog.e("❌ BLE Test: Scanning failed - $errorCode")
            }
        })
        
        if (bleManager.isBluetoothEnabled() && bleManager.isBluetoothLeSupported()) {
            bleManager.startAdvertising()
            bleManager.startScanning()
            
            // Stop after 10 seconds
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                bleManager.stopAdvertising()
                bleManager.stopScanning()
                AlertLog.d("BLE test completed")
            }, 10000)
        } else {
            AlertLog.e("❌ BLE Test: Bluetooth LE not available")
        }
    }
    
    private fun testWiFiDirect() {
        AlertLog.d("Testing WiFi Direct...")
        
        val wifiManager = WiFiDirectManager(context, object : WiFiDirectManager.WiFiDirectCallback {
            override fun onPeersChanged(peers: List<android.net.wifi.p2p.WifiP2pDevice>) {
                AlertLog.d("✅ WiFi Test: Peers found - ${peers.size} devices")
            }
            override fun onConnectionChanged(isConnected: Boolean, groupInfo: android.net.wifi.p2p.WifiP2pGroup?) {
                AlertLog.d("✅ WiFi Test: Connection changed - connected: $isConnected")
            }
            override fun onDeviceChanged(device: android.net.wifi.p2p.WifiP2pDevice?) {
                AlertLog.d("ℹ️ WiFi Test: Device changed")
            }
            override fun onDataReceived(data: ByteArray, fromAddress: String) {
                AlertLog.d("✅ WiFi Test: Data received from $fromAddress")
            }
            override fun onDataSent(success: Boolean, toAddress: String) {
                AlertLog.d("✅ WiFi Test: Data sent to $toAddress - success: $success")
            }
        })
        
        if (wifiManager.initialize()) {
            wifiManager.startDiscovery()
            AlertLog.d("✅ WiFi Direct test started")
            
            // Cleanup after 15 seconds
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                wifiManager.cleanup()
                AlertLog.d("WiFi Direct test completed")
            }, 15000)
        } else {
            AlertLog.e("❌ WiFi Test: WiFi Direct initialization failed")
        }
    }
    
    private fun testAlertCodec() {
        AlertLog.d("Testing Alert Codec...")
        
        try {
            val testAlert = com.example.disastermesh.model.DisasterAlert(
                alertId = "test-${System.currentTimeMillis()}",
                latitude = 37.7749,
                longitude = -122.4194,
                radiusMeters = 1000.0,
                levelOfRisk = com.example.disastermesh.model.RiskLevel.HIGH,
                message = "Test connectivity alert",
                receivedAtMillis = System.currentTimeMillis(),
                senderAuthority = com.example.disastermesh.model.Authority.ADMIN,
                targetAuthority = com.example.disastermesh.model.Authority.ALL
            )
            
            val encoded = com.example.disastermesh.util.AlertCodec.encode(testAlert)
            val decoded = com.example.disastermesh.util.AlertCodec.decode(encoded)
            
            if (decoded != null && decoded.alertId == testAlert.alertId) {
                AlertLog.d("✅ Alert Codec Test: PASSED")
            } else {
                AlertLog.e("❌ Alert Codec Test: FAILED - decoding mismatch")
            }
        } catch (e: Exception) {
            AlertLog.e("❌ Alert Codec Test: FAILED - ${e.message}")
        }
    }
}