package com.example.disastermesh.mesh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.disastermesh.R
import com.example.disastermesh.data.AuthorityManager
import com.example.disastermesh.data.ProcessedAlerts
import com.example.disastermesh.logging.AlertLog
import com.example.disastermesh.model.Authority
import com.example.disastermesh.model.DisasterAlert

/**
 * @deprecated Use MeshService instead for robust BLE + WiFi Direct networking
 */
@Deprecated("Use MeshService instead")
class SimpleMeshService : Service(), SimpleMeshManager.SimpleMeshCallback {
    companion object {
        const val ACTION_RELAY_ALERT = "ACTION_RELAY_ALERT"
        const val ACTION_START_BROADCASTING = "ACTION_START_BROADCASTING"
        const val ACTION_STOP_BROADCASTING = "ACTION_STOP_BROADCASTING"
        const val EXTRA_ALERT = "EXTRA_ALERT"
        private const val CHANNEL_ID = "simple_mesh"
        private const val NOTIFICATION_ID = 2
    }

    private lateinit var meshManager: SimpleMeshManager
    private lateinit var authorityManager: AuthorityManager
    private var currentAlert: DisasterAlert? = null
    private var connectedDeviceCount = 0
    private var currentStatus = "Starting..."

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        
        authorityManager = AuthorityManager(this)
        meshManager = SimpleMeshManager(this, this)
        
        startForeground(NOTIFICATION_ID, buildNotification())
        
        // Start mesh networking
        if (meshManager.startMesh()) {
            AlertLog.d("SimpleMeshService started successfully")
        } else {
            AlertLog.e("Failed to start SimpleMeshService")
        }
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
                if (!meshManager.isRunning()) {
                    meshManager.startMesh()
                }
            }
            ACTION_STOP_BROADCASTING -> {
                meshManager.stopMesh()
                stopSelf()
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
                    "Simple Mesh Network",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows simple mesh network status"
                }
            )
        }

        val statusText = if (connectedDeviceCount > 0) {
            "Connected: $connectedDeviceCount devices - $currentStatus"
        } else {
            currentStatus
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("DisasterMesh - Simple Mode")
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun relayAlert(alert: DisasterAlert) {
        currentAlert = alert
        meshManager.sendAlert(alert)
        AlertLog.d("Relaying alert ${alert.alertId} via simple mesh")
        AlertLog.append(this, "Relayed alert ${alert.alertId} to $connectedDeviceCount devices")
    }

    // SimpleMeshCallback implementations
    override fun onDeviceConnected(device: BluetoothDevice) {
        connectedDeviceCount = meshManager.getConnectedDeviceCount()
        AlertLog.d("Device connected: ${device.address}")
        AlertLog.append(this, "Connected: ${device.address}")
        updateNotification()
        
        // Send current alert to newly connected device
        currentAlert?.let { meshManager.sendAlert(it) }
    }

    override fun onDeviceDisconnected(device: BluetoothDevice) {
        connectedDeviceCount = meshManager.getConnectedDeviceCount()
        AlertLog.d("Device disconnected: ${device.address}")
        AlertLog.append(this, "Disconnected: ${device.address}")
        updateNotification()
    }

    override fun onAlertReceived(alert: DisasterAlert, fromDevice: BluetoothDevice) {
        handleIncomingAlert(alert, fromDevice)
    }

    override fun onAlertSent(alert: DisasterAlert, toDevice: BluetoothDevice, success: Boolean) {
        if (success) {
            AlertLog.d("Alert sent to ${toDevice.address}")
            AlertLog.append(this, "Sent to ${toDevice.address}")
        } else {
            AlertLog.e("Failed to send alert to ${toDevice.address}")
        }
    }

    override fun onStatusChanged(status: String) {
        currentStatus = status
        AlertLog.d("Mesh status: $status")
        updateNotification()
    }

    private fun handleIncomingAlert(alert: DisasterAlert, fromDevice: BluetoothDevice) {
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
        AlertLog.append(this, "Received alert ${alert.alertId} from ${fromDevice.address}")

        // Show notification for alert
        com.example.disastermesh.NotificationHelper(this)
            .showAlertNotificationWithAlert(
                title = getString(R.string.notification_title),
                body = alert.message ?: "Risk Level: ${alert.levelOfRisk}",
                alert = alert
            )

        // Relay to other devices
        currentAlert = alert
        meshManager.sendAlert(alert)
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
        meshManager.stopMesh()
        AlertLog.d("SimpleMeshService destroyed")
    }
}