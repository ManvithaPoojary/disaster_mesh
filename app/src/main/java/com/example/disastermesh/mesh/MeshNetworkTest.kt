package com.example.disastermesh.mesh

import android.content.Context
import com.example.disastermesh.logging.AlertLog
import com.example.disastermesh.model.DisasterAlert
import com.example.disastermesh.model.Authority
import com.example.disastermesh.model.RiskLevel
import com.example.disastermesh.util.AlertCodec

/**
 * Simple test class to verify mesh networking functionality
 * This can be used for debugging and testing the hybrid BLE + WiFi Direct implementation
 */
class MeshNetworkTest(private val context: Context) {
    
    fun testAlertEncoding() {
        val testAlert = DisasterAlert(
            alertId = "test-123",
            latitude = 37.7749,
            longitude = -122.4194,
            radiusMeters = 1000.0,
            levelOfRisk = RiskLevel.HIGH,
            message = "Test emergency alert",
            receivedAtMillis = System.currentTimeMillis(),
            senderAuthority = Authority.ADMIN,
            targetAuthority = Authority.ALL
        )
        
        try {
            // Test encoding
            val encoded = AlertCodec.encode(testAlert)
            AlertLog.d("Alert encoded successfully: ${encoded.size} bytes")
            
            // Test decoding
            val decoded = AlertCodec.decode(encoded)
            if (decoded != null && decoded.alertId == testAlert.alertId) {
                AlertLog.d("Alert codec test PASSED")
            } else {
                AlertLog.e("Alert codec test FAILED - decoding mismatch")
            }
        } catch (e: Exception) {
            AlertLog.e("Alert codec test FAILED: ${e.message}")
        }
    }
    
    fun testBluetoothLeAvailability(): Boolean {
        val bleManager = BluetoothLeManager(context, object : BluetoothLeManager.BluetoothLeCallback {
            override fun onDeviceDiscovered(device: android.bluetooth.BluetoothDevice, deviceName: String) {
                AlertLog.d("Test: Device discovered - $deviceName")
            }
            override fun onDeviceLost(device: android.bluetooth.BluetoothDevice) {}
            override fun onAdvertisingStarted() {
                AlertLog.d("Test: BLE advertising started")
            }
            override fun onAdvertisingFailed(errorCode: Int) {
                AlertLog.e("Test: BLE advertising failed - $errorCode")
            }
            override fun onScanStarted() {
                AlertLog.d("Test: BLE scanning started")
            }
            override fun onScanFailed(errorCode: Int) {
                AlertLog.e("Test: BLE scanning failed - $errorCode")
            }
        })
        
        val isBluetoothEnabled = bleManager.isBluetoothEnabled()
        val isLeSupported = bleManager.isBluetoothLeSupported()
        
        AlertLog.d("Bluetooth enabled: $isBluetoothEnabled")
        AlertLog.d("BLE supported: $isLeSupported")
        
        return isBluetoothEnabled && isLeSupported
    }
    
    fun testWiFiDirectAvailability(): Boolean {
        val wifiManager = WiFiDirectManager(context, object : WiFiDirectManager.WiFiDirectCallback {
            override fun onPeersChanged(peers: List<android.net.wifi.p2p.WifiP2pDevice>) {
                AlertLog.d("Test: WiFi peers changed - ${peers.size} devices")
            }
            override fun onConnectionChanged(isConnected: Boolean, groupInfo: android.net.wifi.p2p.WifiP2pGroup?) {
                AlertLog.d("Test: WiFi connection changed - connected: $isConnected")
            }
            override fun onDeviceChanged(device: android.net.wifi.p2p.WifiP2pDevice?) {
                AlertLog.d("Test: WiFi device changed")
            }
            override fun onDataReceived(data: ByteArray, fromAddress: String) {
                AlertLog.d("Test: Data received from $fromAddress")
            }
            override fun onDataSent(success: Boolean, toAddress: String) {
                AlertLog.d("Test: Data sent to $toAddress - success: $success")
            }
        })
        
        val initialized = wifiManager.initialize()
        AlertLog.d("WiFi Direct initialized: $initialized")
        
        if (initialized) {
            wifiManager.cleanup()
        }
        
        return initialized
    }
    
    fun runAllTests() {
        AlertLog.d("=== Starting Mesh Network Tests ===")
        
        testAlertEncoding()
        
        val bleAvailable = testBluetoothLeAvailability()
        val wifiAvailable = testWiFiDirectAvailability()
        
        AlertLog.d("=== Test Results ===")
        AlertLog.d("Bluetooth LE: ${if (bleAvailable) "AVAILABLE" else "NOT AVAILABLE"}")
        AlertLog.d("WiFi Direct: ${if (wifiAvailable) "AVAILABLE" else "NOT AVAILABLE"}")
        
        if (bleAvailable && wifiAvailable) {
            AlertLog.d("Mesh networking should work properly")
        } else {
            AlertLog.e("Some mesh networking features may not work")
        }
    }
}