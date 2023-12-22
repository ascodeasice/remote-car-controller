package com.example.bluetoothremotecar

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bluetoothremotecar.ui.theme.BluetoothRemoteCarTheme
import com.manalkaff.jetstick.JoyStick
import java.io.IOException
import java.io.OutputStream
import java.util.UUID


enum class Screen() {
    Home,
    Joystick,
}

class MainActivity : ComponentActivity() {
    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val grantedPermissions = permissions.filterValues { it }
            val deniedPermissions = permissions.filterValues { !it }

            if (grantedPermissions.isNotEmpty()) {
                // Handle the granted permissions
                if (grantedPermissions.containsKey(Manifest.permission.ACCESS_FINE_LOCATION)
                    && grantedPermissions.containsKey(Manifest.permission.BLUETOOTH_CONNECT)
                    && grantedPermissions.containsKey(Manifest.permission.BLUETOOTH_SCAN)
                ) {
                    // Both permissions granted, proceed with your operations
                    setContent {
                        BluetoothRemoteCarTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                AppNavigation(this)
                            }
                        }
                    }
                } else {
                    // Handle scenario when only one of the permissions is granted
                    navigateToPermissionDeniedScreen()
                }
            }

            if (deniedPermissions.isNotEmpty()) {
                // Handle the denied permissions
                navigateToPermissionDeniedScreen()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }


        val permissionsNeeded = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            requestMultiplePermissions.launch(permissionsNeeded.toTypedArray())
            return
        }

        setContent {
            BluetoothRemoteCarTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context=this
                    AppNavigation(context)
                }
            }
        }
    }

    private fun navigateToPermissionDeniedScreen() {
        setContent {
            BluetoothRemoteCarTheme {
                    PermissionDeniedScreen()
            }
        }
    }
}

@Composable
fun AppNavigation(context: Context) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Home.name) {
        composable(Screen.Home.name) {
            HomeScreen(navController = navController, context=context)
        }
        composable(
            "${Screen.Joystick.name}/{address}",
            arguments=listOf(navArgument("address"){defaultValue="none"})
        ) {
            backStackEntry ->
            val dataReceived = backStackEntry.arguments?.getString("address")?:"No data"
            JoystickScreen(navController = navController, address=dataReceived)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(navController: NavController, context:Context){
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter
    if (bluetoothAdapter?.isEnabled == false) {
            Toast.makeText(context, "Please enable bluetooth and restart the app", Toast.LENGTH_SHORT).show()
    }
    val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .fillMaxWidth() ,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ){
        Text(text="已配對裝置", style= TextStyle(
            fontSize=24.sp,
        ))
        pairedDevices?.forEach { device ->
            Card(
                modifier = Modifier
                    .size(width = 240.dp, height = 80.dp),
                onClick={navController.navigate("${Screen.Joystick.name}/${device.address}")}
            )
            {
                Text(
                    text=device.name,
                    style=TextStyle(
                        fontSize=20.sp
                    ),
                    modifier = Modifier
                        .padding(8.dp),
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun JoystickScreen(navController: NavController, address:String){
    var bluetoothSocket: BluetoothSocket? = null

    DisposableEffect(Unit) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothDevice: BluetoothDevice? = bluetoothAdapter.getRemoteDevice(address)

        val connectThread = object : Thread() {
            override fun run() {
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") // SPP UUID
                try {
                    bluetoothSocket = bluetoothDevice?.createRfcommSocketToServiceRecord(uuid)
                    bluetoothAdapter.cancelDiscovery()
                    bluetoothSocket?.connect()
                    Log.d("Bluetooth", "Connected successfully")
                } catch (e: IOException) {
                    Log.e("Bluetooth", "Connection failed: ${e.message}")
                    try {
                        bluetoothSocket?.close()
                    } catch (closeException: IOException) {
                        Log.e("Bluetooth", "Could not close the client socket", closeException)
                    }
                }
            }
        }
        connectThread.start()

        onDispose {
            // Clean up and close the socket when the composable is removed from the composition
            try {
                bluetoothSocket?.close()
            } catch (e: IOException) {
                Log.e("Bluetooth", "Could not close the client socket", e)
            }
        }
    }

    Column (
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center, // Vertically center the items
        horizontalAlignment = Alignment.CenterHorizontally // Horizontally center the items
    ){
        JoyStick(
            Modifier.padding(30.dp),
            size = 200.dp,
            dotSize = 50.dp
        ) { x: Float, y: Float ->
            Log.d("JoyStick", "$x, $y")
            // Send data when the joystick position changes
            val message = "$x $y"
            sendMessage(bluetoothSocket, message) // Change baud rate as needed
        }
    }
}

   private fun sendMessage(socket: BluetoothSocket?, message: String) {
        if (socket != null && socket.isConnected) {
            try {
                val outputStream: OutputStream = socket.outputStream

                // Send the actual message
                outputStream.write(message.toByteArray())
                outputStream.write("\r\n".toByteArray())
                outputStream.flush()

                Log.d("Bluetooth", "Message sent: $message")
            } catch (e: IOException) {
                Log.e("Bluetooth", "Error sending message: ${e.message}")
            }
        } else {
            Log.e("Bluetooth", "Socket is not connected")
        }
    }

@Composable
fun PermissionDeniedScreen() {
    BluetoothRemoteCarTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                Text(text = "Bluetooth Permission Denied")
                Text(text = "Please grant Bluetooth permission in Settings")
                // You can add a button to open app settings for the user to grant the permission
            }
        }
    }
}