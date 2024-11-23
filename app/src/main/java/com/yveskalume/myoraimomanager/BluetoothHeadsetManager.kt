package com.yveskalume.myoraimomanager

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun rememberBluetoothHeadsetManager() = remember {
    BluetoothHeadsetManager()
}

sealed interface BluetoothHeadsetsState {
    data object Loading : BluetoothHeadsetsState
    data object DisabledBluetooth : BluetoothHeadsetsState
    data class Success(val devices: List<HeadsetDevice>) : BluetoothHeadsetsState
}

class BluetoothHeadsetManager {

    private val _headsetsState =
        MutableStateFlow<BluetoothHeadsetsState>(BluetoothHeadsetsState.Loading)
    val headsetsState: StateFlow<BluetoothHeadsetsState> = _headsetsState.asStateFlow()

    private val profileListener = object : BluetoothProfile.ServiceListener {

        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HEADSET) {
                val headset = proxy as BluetoothHeadset
                val devices = headset.connectedDevices.map { device ->
                    val batteryLevel: String = getBatteryLevel(device)
                    HeadsetDevice(
                        name = device.name,
                        address = device.address,
                        batteryLevel = batteryLevel
                    )
                }
                _headsetsState.value = BluetoothHeadsetsState.Success(devices)
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HEADSET) {
                Log.d("MainActivity", "Headset disconnected")
            }
        }
    }


    context(Context)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun fetchDevices() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.getProfileProxy(
                this@Context,
                profileListener,
                BluetoothProfile.HEADSET
            )
        } else {
            _headsetsState.value = BluetoothHeadsetsState.DisabledBluetooth
        }
    }

    private fun getBatteryLevel(device: BluetoothDevice): String {
        val percentage = device.javaClass
            .getMethod("getBatteryLevel")
            .invoke(device)?.toString()
        if (percentage != null) {
            return "$percentage %"
        }
        return "N/A"
    }

}