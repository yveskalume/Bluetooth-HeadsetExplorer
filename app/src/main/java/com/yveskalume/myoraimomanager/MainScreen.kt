package com.yveskalume.myoraimomanager

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val headsetManager = rememberBluetoothHeadsetManager()
    val headsetsState by headsetManager.headsetsState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        with(context) {
            headsetManager.fetchDevices()
        }
    }

    var shouldAskPermission by remember {
        mutableStateOf(false)
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            shouldAskPermission = false
            with(context) {
                headsetManager.fetchDevices()
            }
        } else {
            shouldAskPermission = true
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    var isRefreshing by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(1000)
            isRefreshing = false
        }
    }

    val refreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        state = refreshState,
        onRefresh = {},
        isRefreshing = isRefreshing,
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (shouldAskPermission) {
            AskForPermissionContent(
                onLaunchPermissionRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            when (val state = headsetsState) {
                BluetoothHeadsetsState.DisabledBluetooth -> {
                    DisabledBluetoothContent(
                        onEnableBluetooth = {
                            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            bluetoothLauncher.launch(intent)
                        }
                    )
                }

                BluetoothHeadsetsState.Loading -> {
                    CircularProgressIndicator()
                }

                is BluetoothHeadsetsState.Success -> {
                    DeviceListContent(
                        devices = state.devices,
                        onRefresh = {
                            isRefreshing = true
                            with(context) { headsetManager.fetchDevices() }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }


}

@Composable
fun AskForPermissionContent(
    onLaunchPermissionRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Bluetooth permission is required")
        Button(onClick = onLaunchPermissionRequest) {
            Text(text = "Grant permission")
        }
    }
}

@Composable
fun DeviceListContent(
    devices: List<HeadsetDevice>,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit
) {
    if (devices.isEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "No devices found")
            TextButton(onClick = onRefresh) {
                Text(text = "Refresh")
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                devices,
                key = { it.address }
            ) { device ->
                DeviceItem(device = device)
            }
        }
    }
}

@Composable
fun DisabledBluetoothContent(
    onEnableBluetooth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Bluetooth is disabled")
        Button(onClick = onEnableBluetooth) {
            Text(text = "Enable Bluetooth")
        }
    }
}

@Composable
fun DeviceItem(
    device: HeadsetDevice,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(text = "Name : ${device.name}")
            Text(text = "Address : ${device.address}")
            Text(text = "Battery Level : ${device.batteryLevel}")
        }
    }
}