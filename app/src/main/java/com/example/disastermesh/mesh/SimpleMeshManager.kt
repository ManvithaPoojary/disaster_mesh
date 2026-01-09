package com.example.disastermesh.mesh

import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.disastermesh.logging.AlertLog
import com.example.disastermesh.model.DisasterAlert
import com.example.disastermesh.util.AlertCodec
import kotlinx.coroutines.*
import java.io.*
import java.util.*
import android.Manifest

/**
 * Simplified mesh networking using Bluetooth Classic
 * More reliable than WiFi Direct + BLE approach
 */
/**
 * Simplified mesh networking using Bluetooth Classic
 * More reliable than WiFi Direct + BLE approach
 * 
 * @deprecated Use WiFiDirectManager and BluetoothLeManager instead
 */
@Deprecated("Use WiFiDirectManager and BluetoothLeManager instead")
class SimpleMeshManager(
    private val context: Context,
    private val callback: SimpleMeshCallback
) {
    companion object {
        private val SERVICE_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        private const val SERVICE_NAME = "DisasterMesh"
        private const val DEVICE_NAME_PREFIX = "DM_"
    }

    interface SimpleMeshCallback {
        fun onDeviceConnected(device: BluetoothDevice)
        fun onDeviceDisconnected(device: BluetoothDevice)
        fun onAlertReceived(alert: DisasterAlert, fromDevice: BluetoothDevice)
        fun onAlertSent(alert: DisasterAlert, toDevice: BluetoothDevice, success: Boolean)
        fun onStatusChanged(status: String)
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    
    private var serverSocket: BluetoothServerSocket? = null
    private val connectedDevices = mutableMapOf<String, BluetoothSocket>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var isRunning = false

    fun startMesh(): Boolean {
        if (!hasBluetoothPermissions()) {
            AlertLog.e("Missing Bluetooth permissions")
            callback.onStatusChanged("❌ Missing Bluetooth permissions")
            return false
        }

        if (bluetoothAdapter?.isEnabled != true) {
            AlertLog.e("Bluetooth not enabled")
            callback.onStatusChanged("❌ Bluetooth not enabled")
            return false
        }

        if (isRunning) {
            AlertLog.d("Mesh already running")
            return true
        }

        try {
            // Set device discoverable
            makeDeviceDiscoverable()
            
            // Start server to accept connections
            startServer()
            
            // Start discovering and connecting to other devices
            startDiscovery()
            
            isRunning = true
            callback.onStatusChanged("✅ Mesh network active")
            AlertLog.d("Simple mesh started successfully")
            return true
            
        } catch (e: Exception) {
            AlertLog.e("Failed to start mesh: ${e.message}")
            callback.onStatusChanged("❌ Failed to start mesh: ${e.message}")
            return false
        }
    }

    private fun makeDeviceDiscoverable() {
        try {
            if (hasBluetoothPermissions()) {
                // Set device name to be discoverable
                val deviceName = "$DEVICE_NAME_PREFIX${Build.MODEL.replace(" ", "_")}"
                bluetoothAdapter?.name = deviceName
                AlertLog.d("Device name set to: $deviceName")
            }
        } catch (e: Exception) {
            AlertLog.e("Failed to set device name: ${e.message}")
        }
    }

    private fun startServer() {
        scope.launch {
            try {
                if (!hasBluetoothPermissions()) return@launch
                
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                    SERVICE_NAME, SERVICE_UUID
                )
                
                AlertLog.d("Bluetooth server started, waiting for connections...")
                callback.onStatusChanged("📡 Listening for connections...")
                
                while (isRunning && serverSocket != null) {
                    try {
                        val socket = serverSocket?.accept()
                        socket?.let { clientSocket ->
                            val device = clientSocket.remoteDevice
                            AlertLog.d("Client connected: ${device.address}")
                            
                            connectedDevices[device.address] = clientSocket
                            callback.onDeviceConnected(device)
                            callback.onStatusChanged("🔗 Connected: ${connectedDevices.size} devices")
                            
                            // Handle this connection
                            handleConnection(clientSocket)
                        }
                    } catch (e: IOException) {
                        if (isRunning) {
                            AlertLog.e("Error accepting connection: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                AlertLog.e("Server error: ${e.message}")
                callback.onStatusChanged("❌ Server error: ${e.message}")
            }
        }
    }

    private fun startDiscovery() {
        scope.launch {
            while (isRunning) {
                try {
                    if (!hasBluetoothPermissions()) {
                        delay(5000)
                        continue
                    }
                    
                    AlertLog.d("Starting device discovery...")
                    callback.onStatusChanged("🔍 Discovering devices...")
                    
                    val pairedDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
                    
                    // Try to connect to paired DisasterMesh devices
                    for (device in pairedDevices) {
                        if (device.name?.startsWith(DEVICE_NAME_PREFIX) == true && 
                            !connectedDevices.containsKey(device.address)) {
                            connectToDevice(device)
                        }
                    }
                    
                    // Start discovery for new devices
                    if (bluetoothAdapter?.isDiscovering == true) {
                        bluetoothAdapter.cancelDiscovery()
                    }
                    bluetoothAdapter?.startDiscovery()
                    
                    // Wait before next discovery cycle
                    delay(30000) // 30 seconds between discovery cycles
                    
                } catch (e: Exception) {
                    AlertLog.e("Discovery error: ${e.message}")
                    delay(10000) // Wait 10 seconds before retry
                }
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        scope.launch {
            try {
                if (!hasBluetoothPermissions()) return@launch
                
                AlertLog.d("Attempting to connect to: ${device.address}")
                
                val socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                socket.connect()
                
                connectedDevices[device.address] = socket
                callback.onDeviceConnected(device)
                callback.onStatusChanged("🔗 Connected: ${connectedDevices.size} devices")
                
                AlertLog.d("Successfully connected to: ${device.address}")
                
                // Handle this connection
                handleConnection(socket)
                
            } catch (e: Exception) {
                AlertLog.e("Failed to connect to ${device.address}: ${e.message}")
            }
        }
    }

    private fun handleConnection(socket: BluetoothSocket) {
        scope.launch {
            try {
                val inputStream = socket.inputStream
                val buffer = ByteArray(1024)
                
                while (socket.isConnected && isRunning) {
                    try {
                        val bytesRead = inputStream.read(buffer)
                        if (bytesRead > 0) {
                            val data = buffer.copyOf(bytesRead)
                            handleReceivedData(data, socket.remoteDevice)
                        }
                    } catch (e: IOException) {
                        // Connection lost
                        break
                    }
                }
                
            } catch (e: Exception) {
                AlertLog.e("Error handling connection: ${e.message}")
            } finally {
                // Clean up connection
                val device = socket.remoteDevice
                connectedDevices.remove(device.address)
                callback.onDeviceDisconnected(device)
                callback.onStatusChanged("🔗 Connected: ${connectedDevices.size} devices")
                
                try {
                    socket.close()
                } catch (e: Exception) {
                    AlertLog.e("Error closing socket: ${e.message}")
                }
            }
        }
    }

    private fun handleReceivedData(data: ByteArray, fromDevice: BluetoothDevice) {
        try {
            val alert = AlertCodec.decode(data)
            if (alert != null) {
                AlertLog.d("Received alert ${alert.alertId} from ${fromDevice.address}")
                callback.onAlertReceived(alert, fromDevice)
            } else {
                AlertLog.e("Failed to decode received data")
            }
        } catch (e: Exception) {
            AlertLog.e("Error processing received data: ${e.message}")
        }
    }

    fun sendAlert(alert: DisasterAlert) {
        scope.launch {
            val alertData = AlertCodec.encode(alert)
            var successCount = 0
            
            for ((address, socket) in connectedDevices) {
                try {
                    socket.outputStream.write(alertData)
                    socket.outputStream.flush()
                    
                    successCount++
                    callback.onAlertSent(alert, socket.remoteDevice, true)
                    AlertLog.d("Alert sent to $address")
                    
                } catch (e: Exception) {
                    AlertLog.e("Failed to send alert to $address: ${e.message}")
                    callback.onAlertSent(alert, socket.remoteDevice, false)
                    
                    // Remove failed connection
                    connectedDevices.remove(address)
                    try {
                        socket.close()
                    } catch (closeException: Exception) {
                        AlertLog.e("Error closing failed socket: ${closeException.message}")
                    }
                }
            }
            
            AlertLog.d("Alert ${alert.alertId} sent to $successCount/${connectedDevices.size} devices")
            callback.onStatusChanged("📤 Alert sent to $successCount devices")
        }
    }

    fun stopMesh() {
        isRunning = false
        
        try {
            // Stop discovery
            if (hasBluetoothPermissions() && bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            
            // Close server socket
            serverSocket?.close()
            serverSocket = null
            
            // Close all client connections
            for ((address, socket) in connectedDevices) {
                try {
                    socket.close()
                    AlertLog.d("Closed connection to $address")
                } catch (e: Exception) {
                    AlertLog.e("Error closing connection to $address: ${e.message}")
                }
            }
            connectedDevices.clear()
            
            // Cancel coroutines
            scope.cancel()
            
            callback.onStatusChanged("⏹️ Mesh network stopped")
            AlertLog.d("Simple mesh stopped")
            
        } catch (e: Exception) {
            AlertLog.e("Error stopping mesh: ${e.message}")
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getConnectedDeviceCount(): Int = connectedDevices.size
    
    fun isRunning(): Boolean = isRunning
}