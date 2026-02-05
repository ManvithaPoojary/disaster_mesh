package com.example.disastermesh.mesh

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.p2p.*
import android.net.wifi.WpsInfo
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.disastermesh.logging.AlertLog
import java.net.*
import java.io.*
import kotlinx.coroutines.*

import java.util.concurrent.CopyOnWriteArraySet

class WiFiDirectManager(
    private val context: Context,
    private val callback: WiFiDirectCallback
) {
    companion object {
        private const val SERVER_PORT = 8888
        private const val SERVICE_TYPE = "_disastermesh._tcp"
        private const val SERVICE_INSTANCE = "DisasterMesh"
    }

    interface WiFiDirectCallback {
        fun onPeersChanged(peers: List<WifiP2pDevice>)
        fun onConnectionChanged(isConnected: Boolean, groupInfo: WifiP2pGroup?)
        fun onDeviceChanged(device: WifiP2pDevice?)
        fun onDataReceived(data: ByteArray, fromAddress: String)
        fun onDataSent(success: Boolean, toAddress: String)
    }

    private val wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null
    private val peers = mutableListOf<WifiP2pDevice>()
    private var isGroupOwner = false
    private var serverSocket: ServerSocket? = null
    private var clientSockets = CopyOnWriteArraySet<Socket>()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val refreshedPeers = peerList.deviceList.toList()
        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers)
            AlertLog.d("WiFi Direct peers updated: ${peers.size} devices")
            callback.onPeersChanged(peers)
        }
    }

    private val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info ->
        isGroupOwner = info.isGroupOwner
        AlertLog.d("WiFi Direct connection info - Group Owner: $isGroupOwner")
        
        if (isGroupOwner) {
            startServer()
        } else {
            // Connect to group owner
            info.groupOwnerAddress?.let { address ->
                connectToServer(address.hostAddress ?: "")
            }
        }
    }

    private val groupInfoListener = WifiP2pManager.GroupInfoListener { group ->
        AlertLog.d("WiFi Direct group info: ${group?.networkName}")
        callback.onConnectionChanged(group != null, group)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    AlertLog.d("WiFi P2P state changed: $state")
                }
                
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    requestPeers()
                }
                
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        requestConnectionInfo()
                        requestGroupInfo()
                    } else {
                        callback.onConnectionChanged(false, null)
                        stopServer()
                    }
                }
                
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val device = intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    callback.onDeviceChanged(device)
                }
            }
        }
    }

    fun initialize(): Boolean {
        if (!hasWiFiDirectPermissions()) {
            AlertLog.e("Missing WiFi Direct permissions")
            return false
        }

        channel = wifiP2pManager.initialize(context, context.mainLooper, null)
        if (channel == null) {
            AlertLog.e("Failed to initialize WiFi Direct")
            return false
        }

        registerReceiver()
        AlertLog.d("WiFi Direct manager initialized")
        return true
    }

    fun startDiscovery(): Boolean {
        if (channel == null || !hasWiFiDirectPermissions()) {
            AlertLog.e("Cannot start WiFi Direct discovery: missing permissions or not initialized")
            return false
        }

        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                AlertLog.d("WiFi Direct discovery started successfully")
            }

            override fun onFailure(reason: Int) {
                AlertLog.e("WiFi Direct discovery failed: $reason")
                // Retry after a delay
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    AlertLog.d("Retrying WiFi Direct discovery...")
                    startDiscovery()
                }, 5000)
            }
        })
        return true
    }

    fun connectToPeer(device: WifiP2pDevice) {
        if (channel == null || !hasWiFiDirectPermissions()) {
            return
        }

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                AlertLog.d("Connection initiated to ${device.deviceName}")
            }

            override fun onFailure(reason: Int) {
                AlertLog.e("Failed to connect to ${device.deviceName}: $reason")
            }
        })
    }

    fun sendData(data: ByteArray, targetAddress: String? = null) {
        scope.launch {
            try {
                if (isGroupOwner) {
                    // Send to all connected clients
                    clientSockets.forEach { socket ->
                        try {
                            socket.getOutputStream().write(data)
                            socket.getOutputStream().flush()
                            callback.onDataSent(true, socket.inetAddress.hostAddress ?: "")
                        } catch (e: Exception) {
                            AlertLog.e("Failed to send data to client: ${e.message}")
                            callback.onDataSent(false, socket.inetAddress.hostAddress ?: "")
                        }
                    }
                } else {
                    // Send to group owner
                    clientSockets.firstOrNull()?.let { socket ->
                        try {
                            socket.getOutputStream().write(data)
                            socket.getOutputStream().flush()
                            callback.onDataSent(true, socket.inetAddress.hostAddress ?: "")
                        } catch (e: Exception) {
                            AlertLog.e("Failed to send data to group owner: ${e.message}")
                            callback.onDataSent(false, socket.inetAddress.hostAddress ?: "")
                        }
                    }
                }
            } catch (e: Exception) {
                AlertLog.e("Error sending data: ${e.message}")
                callback.onDataSent(false, targetAddress ?: "")
            }
        }
    }

    private fun startServer() {
        scope.launch {
            try {
                serverSocket = ServerSocket(SERVER_PORT)
                AlertLog.d("WiFi Direct server started on port $SERVER_PORT")

                while (serverSocket?.isClosed == false) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        clientSocket?.let { socket ->
                            clientSockets.add(socket)
                            AlertLog.d("Client connected: ${socket.inetAddress.hostAddress}")
                            handleClientConnection(socket)
                        }
                    } catch (e: Exception) {
                        if (serverSocket?.isClosed == false) {
                            AlertLog.e("Error accepting client connection: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                AlertLog.e("Error starting server: ${e.message}")
            }
        }
    }

    private fun connectToServer(serverAddress: String) {
        scope.launch {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(serverAddress, SERVER_PORT), 5000)
                clientSockets.add(socket)
                AlertLog.d("Connected to server: $serverAddress")
                handleServerConnection(socket)
            } catch (e: Exception) {
                AlertLog.e("Failed to connect to server $serverAddress: ${e.message}")
            }
        }
    }

    private fun handleClientConnection(socket: Socket) {
        scope.launch {
            try {
                val inputStream = socket.getInputStream()
                val buffer = ByteArray(1024)
                
                while (socket.isConnected && !socket.isClosed) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val data = buffer.copyOf(bytesRead)
                        callback.onDataReceived(data, socket.inetAddress.hostAddress ?: "")
                    }
                }
            } catch (e: Exception) {
                AlertLog.e("Error handling client connection: ${e.message}")
            } finally {
                clientSockets.remove(socket)
                try {
                    socket.close()
                } catch (e: Exception) {
                    AlertLog.e("Error closing client socket: ${e.message}")
                }
            }
        }
    }

    private fun handleServerConnection(socket: Socket) {
        scope.launch {
            try {
                val inputStream = socket.getInputStream()
                val buffer = ByteArray(1024)
                
                while (socket.isConnected && !socket.isClosed) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val data = buffer.copyOf(bytesRead)
                        callback.onDataReceived(data, socket.inetAddress.hostAddress ?: "")
                    }
                }
            } catch (e: Exception) {
                AlertLog.e("Error handling server connection: ${e.message}")
            } finally {
                clientSockets.remove(socket)
                try {
                    socket.close()
                } catch (e: Exception) {
                    AlertLog.e("Error closing server socket: ${e.message}")
                }
            }
        }
    }

    private fun stopServer() {
        try {
            serverSocket?.close()
            serverSocket = null
            clientSockets.forEach { it.close() }
            clientSockets.clear()
            AlertLog.d("WiFi Direct server stopped")
        } catch (e: Exception) {
            AlertLog.e("Error stopping server: ${e.message}")
        }
    }

    private fun requestPeers() {
        if (channel != null && hasWiFiDirectPermissions()) {
            wifiP2pManager.requestPeers(channel, peerListListener)
        }
    }

    private fun requestConnectionInfo() {
        if (channel != null && hasWiFiDirectPermissions()) {
            wifiP2pManager.requestConnectionInfo(channel, connectionInfoListener)
        }
    }

    private fun requestGroupInfo() {
        if (channel != null && hasWiFiDirectPermissions()) {
            wifiP2pManager.requestGroupInfo(channel, groupInfoListener)
        }
    }

    private fun registerReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        context.registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun hasWiFiDirectPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun disconnect() {
        if (channel != null) {
            wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    AlertLog.d("WiFi Direct group removed")
                }

                override fun onFailure(reason: Int) {
                    AlertLog.e("Failed to remove WiFi Direct group: $reason")
                }
            })
        }
    }

    fun cleanup() {
        try {
            context.unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            AlertLog.e("Error unregistering receiver: ${e.message}")
        }
        
        stopServer()
        scope.cancel()
        AlertLog.d("WiFi Direct manager cleaned up")
    }

    fun getPeers(): List<WifiP2pDevice> = peers.toList()
}