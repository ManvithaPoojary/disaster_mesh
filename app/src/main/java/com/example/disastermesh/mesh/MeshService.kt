//package com.example.disastermesh.mesh
//
//import android.app.Notification
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.Service
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.os.IBinder
//import androidx.core.app.NotificationCompat
//import com.example.disastermesh.R
//import com.example.disastermesh.data.ProcessedAlerts
//import com.example.disastermesh.logging.AlertLog
//import com.example.disastermesh.model.DisasterAlert
//import com.example.disastermesh.util.AlertCodec
//import com.google.android.gms.nearby.Nearby
//import com.google.android.gms.nearby.connection.AdvertisingOptions
//import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
//import com.google.android.gms.nearby.connection.ConnectionResolution
//import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
//import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
//import com.google.android.gms.nearby.connection.DiscoveryOptions
//import com.google.android.gms.nearby.connection.Payload
//import com.google.android.gms.nearby.connection.PayloadCallback
//import com.google.android.gms.nearby.connection.PayloadTransferUpdate
//import com.google.android.gms.nearby.connection.Strategy
//
//class MeshService : Service() {
//    companion object {
//        const val ACTION_RELAY_ALERT = "ACTION_RELAY_ALERT"
//        const val EXTRA_ALERT = "EXTRA_ALERT"
//        private const val SERVICE_ID = "com.example.disastermesh.mesh"
//        private const val CHANNEL_ID = "mesh"
//    }
//
//    private val connections by lazy { Nearby.getConnectionsClient(this) }
//    private var currentAlert: DisasterAlert? = null
//
//    override fun onBind(intent: Intent?): IBinder? = null
//
//    override fun onCreate() {
//        super.onCreate()
//        startForeground(1, buildNotification())
//        startAdvertisingAndDiscovery()
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        if (intent?.action == ACTION_RELAY_ALERT) {
//            val alert = intent.getParcelableExtra(EXTRA_ALERT, DisasterAlert::class.java)
//            if (alert != null) {
//                currentAlert = alert
//                relayAlert(alert)
//            }
//        }
//        return START_STICKY
//    }
//
//    private fun buildNotification(): Notification {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            nm.createNotificationChannel(
//                NotificationChannel(CHANNEL_ID, "Mesh Relay", NotificationManager.IMPORTANCE_LOW)
//            )
//        }
//        return NotificationCompat.Builder(this, CHANNEL_ID)
//            .setSmallIcon(android.R.drawable.stat_sys_upload)
//            .setContentTitle(getString(R.string.app_name))
//            .setContentText("Mesh relay active")
//            .build()
//    }
//
//    private fun startAdvertisingAndDiscovery() {
//        val strategy = Strategy.P2P_CLUSTER
//
//        connections.startAdvertising(
//            "DisasterMesh",
//            SERVICE_ID,
//            connectionLifecycle,
//            AdvertisingOptions.Builder().setStrategy(strategy).build()
//        )
//
//        connections.startDiscovery(
//            SERVICE_ID,
//            discoveryCallback,
//            DiscoveryOptions.Builder().setStrategy(strategy).build()
//        )
//    }
//
//    private val payloadCallback = object : PayloadCallback() {
//        override fun onPayloadReceived(endpointId: String, payload: Payload) {
//            val bytes = payload.asBytes() ?: return
//            val alert = AlertCodec.decode(bytes) ?: return
//            handleIncomingAlert(alert)
//        }
//
//        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
//    }
//
//    private val connectionLifecycle = object : ConnectionLifecycleCallback() {
//        override fun onConnectionInitiated(endpointId: String, connectionInfo: com.google.android.gms.nearby.connection.ConnectionInfo) {
//            connections.acceptConnection(endpointId, payloadCallback)
//        }
//
//        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
//            if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
//                currentAlert?.let { sendAlert(endpointId, it) }
//            }
//        }
//
//        override fun onDisconnected(endpointId: String) {}
//    }
//
//    private val discoveryCallback = object : com.google.android.gms.nearby.connection.EndpointDiscoveryCallback() {
//        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
//            connections.requestConnection("DisasterMesh", endpointId, connectionLifecycle)
//        }
//
//        override fun onEndpointLost(endpointId: String) {}
//    }
//
//    private fun relayAlert(alert: DisasterAlert) {
//        currentAlert = alert
//        // endpoints found later will receive it in onConnectionResult
//    }
//
//    private fun sendAlert(endpointId: String, alert: DisasterAlert) {
//        val bytes = AlertCodec.encode(alert)
//        connections.sendPayload(endpointId, Payload.fromBytes(bytes))
//        AlertLog.d("Relayed alert ${alert.alertId} to $endpointId")
//    }
//
//    private fun handleIncomingAlert(alert: DisasterAlert) {
//        val processed = ProcessedAlerts(this)
//        if (processed.hasProcessed(alert.alertId)) return
//        processed.markProcessed(alert.alertId)
//
//        // Show notification
//        com.example.disastermesh.NotificationHelper(this)
//            .showAlertNotificationWithAlert(
//                title = getString(R.string.notification_title),
//                body = alert.message ?: "Risk ${alert.levelOfRisk}",
//                alert = alert
//            )
//
//        // Relay further
//        currentAlert = alert
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        connections.stopAdvertising()
//        connections.stopDiscovery()
//        connections.stopAllEndpoints()
//    }
//}
//
//
package com.example.disastermesh.mesh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.disastermesh.R
import com.example.disastermesh.data.AuthorityManager
import com.example.disastermesh.data.ProcessedAlerts
import com.example.disastermesh.logging.AlertLog
import com.example.disastermesh.model.Authority
import com.example.disastermesh.model.DisasterAlert
import com.example.disastermesh.util.AlertCodec

class MeshService : Service(), BluetoothLeManager.BluetoothLeCallback, WiFiDirectManager.WiFiDirectCallback {
    companion object {
        const val ACTION_RELAY_ALERT = "ACTION_RELAY_ALERT"
        const val ACTION_START_BROADCASTING = "ACTION_START_BROADCASTING"
        const val EXTRA_ALERT = "EXTRA_ALERT"
        private const val CHANNEL_ID = "mesh"
    }

    private lateinit var bluetoothLeManager: BluetoothLeManager
    private lateinit var wifiDirectManager: WiFiDirectManager
    private val discoveredBluetoothDevices = mutableSetOf<BluetoothDevice>()
    private val discoveredWiFiDevices = mutableSetOf<WifiP2pDevice>()
    private var currentAlert: DisasterAlert? = null
    private var isBluetoothActive = false
    private var isWiFiDirectActive = false
    private lateinit var authorityManager: AuthorityManager

    // Unique device identifier for the mesh network
    private val deviceName = "DM_${Build.MODEL.replace(" ", "_")}_${Build.ID.takeLast(4)}"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        authorityManager = AuthorityManager(this)
        
        // Set Bluetooth device name for discovery
        setBluetoothDeviceName()
        
        // Initialize managers
        bluetoothLeManager = BluetoothLeManager(this, this)
        wifiDirectManager = WiFiDirectManager(this, this)
        
        startForeground(1, buildNotification())

        // Auto-start mesh network on service creation
        startMeshNetworking()

        AlertLog.d("MeshService created - Broadcasting as: $deviceName")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RELAY_ALERT -> {
                val alert = intent.getParcelableExtra(EXTRA_ALERT, DisasterAlert::class.java)
                if (alert != null) {
                    currentAlert = alert
                    relayAlert(alert)
                }
            }
            ACTION_START_BROADCASTING -> {
                // Ensure mesh network is active
                if (!isBluetoothActive || !isWiFiDirectActive) {
                    startMeshNetworking()
                }
            }
            else -> {
                // Default action: ensure mesh is running
                if (!isBluetoothActive || !isWiFiDirectActive) {
                    startMeshNetworking()
                }
            }
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Mesh Network",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows mesh network status"
                }
            )
        }

        val totalDevices = discoveredBluetoothDevices.size + discoveredWiFiDevices.size
        val statusText = when {
            totalDevices > 0 ->
                "Nearby: $totalDevices device(s) (BT: ${discoveredBluetoothDevices.size}, WiFi: ${discoveredWiFiDevices.size})"
            isBluetoothActive || isWiFiDirectActive ->
                "Broadcasting - Visible to nearby devices"
            else ->
                "Starting mesh network..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, buildNotification())
    }

    private fun startMeshNetworking() {
        // Start Bluetooth LE for device discovery
        if (bluetoothLeManager.isBluetoothEnabled() && bluetoothLeManager.isBluetoothLeSupported()) {
            if (bluetoothLeManager.startAdvertising()) {
                AlertLog.d("BLE advertising started")
                AlertLog.append(this, "BLE advertising started")
            } else {
                AlertLog.e("Failed to start BLE advertising")
                AlertLog.append(this, "BLE advertising failed")
            }
            
            if (bluetoothLeManager.startScanning()) {
                AlertLog.d("BLE scanning started")
                AlertLog.append(this, "BLE scanning started")
            } else {
                AlertLog.e("Failed to start BLE scanning")
                AlertLog.append(this, "BLE scanning failed")
            }
        } else {
            AlertLog.e("Bluetooth LE not available")
            AlertLog.append(this, "BLE not available")
        }

        // Initialize WiFi Direct for data transfer
        if (wifiDirectManager.initialize()) {
            if (wifiDirectManager.startDiscovery()) {
                AlertLog.d("WiFi Direct discovery started")
                AlertLog.append(this, "WiFi Direct discovery started")
            } else {
                AlertLog.e("Failed to start WiFi Direct discovery")
                AlertLog.append(this, "WiFi Direct discovery failed")
            }
        } else {
            AlertLog.e("WiFi Direct not available")
            AlertLog.append(this, "WiFi Direct not available")
        }
    }

    private fun setBluetoothDeviceName() {
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                // Check permissions before setting name
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (androidx.core.app.ActivityCompat.checkSelfPermission(
                            this, 
                            android.Manifest.permission.BLUETOOTH_CONNECT
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        bluetoothAdapter.name = deviceName
                        AlertLog.d("Bluetooth device name set to: $deviceName")
                    }
                } else {
                    bluetoothAdapter.name = deviceName
                    AlertLog.d("Bluetooth device name set to: $deviceName")
                }
            }
        } catch (e: Exception) {
            AlertLog.e("Failed to set Bluetooth device name: ${e.message}")
        }
    }

    // Bluetooth LE Callbacks
    override fun onDeviceDiscovered(device: BluetoothDevice, deviceName: String) {
        discoveredBluetoothDevices.add(device)
        AlertLog.d("Discovered Bluetooth device: $deviceName")
        AlertLog.append(this, "BLE device found: $deviceName")
        updateNotification()
        
        // Try to establish WiFi Direct connection for data transfer
        tryWiFiDirectConnection(device)
    }

    override fun onDeviceLost(device: BluetoothDevice) {
        discoveredBluetoothDevices.remove(device)
        AlertLog.d("Lost Bluetooth device: ${device.address}")
        updateNotification()
    }

    override fun onAdvertisingStarted() {
        isBluetoothActive = true
        updateNotification()
    }

    override fun onAdvertisingFailed(errorCode: Int) {
        AlertLog.e("Bluetooth advertising failed: $errorCode")
        // Retry after delay
        android.os.Handler(mainLooper).postDelayed({
            bluetoothLeManager.startAdvertising()
        }, 5000)
    }

    override fun onScanStarted() {
        // Scanning started successfully
    }

    override fun onScanFailed(errorCode: Int) {
        AlertLog.e("Bluetooth scan failed: $errorCode")
        // Retry after delay
        android.os.Handler(mainLooper).postDelayed({
            bluetoothLeManager.startScanning()
        }, 5000)
    }

    // WiFi Direct Callbacks
    override fun onPeersChanged(peers: List<WifiP2pDevice>) {
        discoveredWiFiDevices.clear()
        discoveredWiFiDevices.addAll(peers)
        AlertLog.d("WiFi Direct peers updated: ${peers.size} devices")
        AlertLog.append(this, "WiFi peers: ${peers.size}")
        updateNotification()
        
        // Auto-connect to first available peer for mesh networking
        peers.firstOrNull()?.let { peer ->
            wifiDirectManager.connectToPeer(peer)
        }
    }

    override fun onConnectionChanged(isConnected: Boolean, groupInfo: WifiP2pGroup?) {
        isWiFiDirectActive = isConnected
        if (isConnected) {
            AlertLog.d("WiFi Direct connected - Group: ${groupInfo?.networkName}")
            AlertLog.append(this, "WiFi connected: ${groupInfo?.networkName}")
            
            // Send current alert if available
            currentAlert?.let { relayAlert(it) }
        } else {
            AlertLog.d("WiFi Direct disconnected")
            AlertLog.append(this, "WiFi disconnected")
        }
        updateNotification()
    }

    override fun onDeviceChanged(device: WifiP2pDevice?) {
        device?.let {
            AlertLog.d("WiFi Direct device changed: ${it.deviceName}")
        }
    }

    override fun onDataReceived(data: ByteArray, fromAddress: String) {
        try {
            val alert = AlertCodec.decode(data)
            if (alert != null) {
                AlertLog.d("Received alert via WiFi Direct from $fromAddress")
                handleIncomingAlert(alert)
            }
        } catch (e: Exception) {
            AlertLog.e("Failed to decode received data: ${e.message}")
        }
    }

    override fun onDataSent(success: Boolean, toAddress: String) {
        if (success) {
            AlertLog.d("Alert sent successfully via WiFi Direct to $toAddress")
            AlertLog.append(this, "Sent to $toAddress")
        } else {
            AlertLog.e("Failed to send alert via WiFi Direct to $toAddress")
        }
    }

    private fun relayAlert(alert: DisasterAlert) {
        currentAlert = alert
        
        // Send via WiFi Direct if connected
        if (isWiFiDirectActive) {
            val alertData = AlertCodec.encode(alert)
            wifiDirectManager.sendData(alertData)
            AlertLog.d("Relaying alert ${alert.alertId} via WiFi Direct")
            AlertLog.append(this, "Relayed alert ${alert.alertId} via WiFi Direct")
        } else {
            AlertLog.d("No WiFi Direct connection available for alert relay")
        }
    }

    private fun tryWiFiDirectConnection(bluetoothDevice: BluetoothDevice) {
        // When we discover a device via Bluetooth LE, we can try to establish
        // a WiFi Direct connection for actual data transfer
        // This is a simplified approach - in practice you might want to exchange
        // WiFi Direct device info via Bluetooth LE first
        
        AlertLog.d("Attempting WiFi Direct connection for BLE device: ${bluetoothDevice.address}")
        
        val wifiPeers = wifiDirectManager.getPeers()
        if (wifiPeers.isNotEmpty()) {
            // Try to connect to the first available WiFi Direct peer
            wifiDirectManager.connectToPeer(wifiPeers.first())
        } else {
            // Start WiFi Direct discovery to find peers
            wifiDirectManager.startDiscovery()
        }
    }

    private fun handleIncomingAlert(alert: DisasterAlert) {
        val processed = ProcessedAlerts(this)
        if (processed.hasProcessed(alert.alertId)) {
            AlertLog.d("Alert ${alert.alertId} already processed, skipping")
            return
        }
        
        // Authority-based filtering
        if (!shouldProcessAlert(alert)) {
            AlertLog.d("Alert ${alert.alertId} filtered by authority rules")
            return
        }
        
        processed.markProcessed(alert.alertId)
        AlertLog.append(this, "Processed alert ${alert.alertId} (${alert.levelOfRisk}) from ${alert.senderAuthority}")

        // Show notification for alert
        com.example.disastermesh.NotificationHelper(this)
            .showAlertNotificationWithAlert(
                title = getString(R.string.notification_title),
                body = alert.message ?: "Risk Level: ${alert.levelOfRisk}",
                alert = alert
            )

        // Update current alert and relay further
        currentAlert = alert
        relayAlert(alert)
    }
    
    private fun shouldProcessAlert(alert: DisasterAlert): Boolean {
        val currentAuthority = authorityManager.getAuthority()
        
        return when (alert.targetAuthority) {
            Authority.ALL -> true // Everyone should see this
            Authority.ADMIN -> currentAuthority == Authority.ADMIN // Only admins
            Authority.USER -> currentAuthority == Authority.USER // Only users (for SOS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Cleanup Bluetooth LE
        bluetoothLeManager.stopAdvertising()
        bluetoothLeManager.stopScanning()
        
        // Cleanup WiFi Direct
        wifiDirectManager.disconnect()
        wifiDirectManager.cleanup()
        
        isBluetoothActive = false
        isWiFiDirectActive = false
        
        AlertLog.d("MeshService destroyed")
    }
}