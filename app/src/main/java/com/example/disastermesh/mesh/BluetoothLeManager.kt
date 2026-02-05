package com.example.disastermesh.mesh

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import com.example.disastermesh.logging.AlertLog
import java.util.*

class BluetoothLeManager(
    private val context: Context,
    private val callback: BluetoothLeCallback
) {
    companion object {
        // Custom service UUID for DisasterMesh
        private val SERVICE_UUID = UUID.fromString("12345678-1234-5678-9012-123456789abc")
        private val CHARACTERISTIC_UUID = UUID.fromString("87654321-4321-8765-2109-cba987654321")
        
        private const val DEVICE_NAME_PREFIX = "DM_"
    }

    interface BluetoothLeCallback {
        fun onDeviceDiscovered(device: BluetoothDevice, deviceName: String)
        fun onDeviceLost(device: BluetoothDevice)
        fun onAdvertisingStarted()
        fun onAdvertisingFailed(errorCode: Int)
        fun onScanStarted()
        fun onScanFailed(errorCode: Int)
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    private var isAdvertising = false
    private var isScanning = false
    private val discoveredDevices = mutableSetOf<BluetoothDevice>()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
            AlertLog.d("BLE advertising started successfully")
            callback.onAdvertisingStarted()
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
            AlertLog.e("BLE advertising failed: $errorCode")
            callback.onAdvertisingFailed(errorCode)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { scanResult ->
                val device = scanResult.device
                val deviceName = getDeviceName(scanResult)
                
                // Log all discovered devices for debugging
                AlertLog.d("BLE scan result: $deviceName (${device.address})")
                
                if (deviceName.startsWith(DEVICE_NAME_PREFIX) && !discoveredDevices.contains(device)) {
                    discoveredDevices.add(device)
                    AlertLog.d("Discovered DisasterMesh device: $deviceName")
                    callback.onDeviceDiscovered(device, deviceName)
                } else if (deviceName != "Unknown") {
                    // Log non-DisasterMesh devices for debugging
                    AlertLog.d("Non-DisasterMesh device found: $deviceName")
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            AlertLog.e("BLE scan failed: $errorCode")
            callback.onScanFailed(errorCode)
        }
    }

    fun startAdvertising(): Boolean {
        if (!hasBluetoothPermissions() || bluetoothLeAdvertiser == null) {
            AlertLog.e("Cannot start advertising: missing permissions or BLE not supported")
            return false
        }

        if (isAdvertising) {
            AlertLog.d("Already advertising")
            return true
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(false)
            .setTimeout(0) // Advertise indefinitely
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        try {
            bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback)
            return true
        } catch (e: Exception) {
            AlertLog.e("Failed to start advertising: ${e.message}")
            return false
        }
    }

    fun startScanning(): Boolean {
        if (!hasBluetoothPermissions() || bluetoothLeScanner == null) {
            AlertLog.e("Cannot start scanning: missing permissions or BLE not supported")
            return false
        }

        if (isScanning) {
            AlertLog.d("Already scanning")
            return true
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        // Scan for all devices, not just our service UUID
        // This is more reliable for discovery
        val filters = emptyList<ScanFilter>()

        try {
            bluetoothLeScanner.startScan(filters, settings, scanCallback)
            isScanning = true
            AlertLog.d("BLE scanning started (all devices)")
            callback.onScanStarted()
            return true
        } catch (e: Exception) {
            AlertLog.e("Failed to start scanning: ${e.message}")
            return false
        }
    }

    fun stopAdvertising() {
        if (isAdvertising && bluetoothLeAdvertiser != null) {
            try {
                bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
                isAdvertising = false
                AlertLog.d("BLE advertising stopped")
            } catch (e: Exception) {
                AlertLog.e("Failed to stop advertising: ${e.message}")
            }
        }
    }

    fun stopScanning() {
        if (isScanning && bluetoothLeScanner != null) {
            try {
                bluetoothLeScanner.stopScan(scanCallback)
                isScanning = false
                discoveredDevices.clear()
                AlertLog.d("BLE scanning stopped")
            } catch (e: Exception) {
                AlertLog.e("Failed to stop scanning: ${e.message}")
            }
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun isBluetoothLeSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getDeviceName(scanResult: ScanResult): String {
        // Try to get device name from scan record first
        val scanRecord = scanResult.scanRecord
        val localName = scanRecord?.deviceName
        
        if (!localName.isNullOrEmpty()) {
            return localName
        }
        
        // Fallback to device name if permissions allow
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                scanResult.device.name ?: "Unknown"
            } else {
                "Unknown"
            }
        } else {
            scanResult.device.name ?: "Unknown"
        }
    }

    fun getDiscoveredDevices(): Set<BluetoothDevice> = discoveredDevices.toSet()
}