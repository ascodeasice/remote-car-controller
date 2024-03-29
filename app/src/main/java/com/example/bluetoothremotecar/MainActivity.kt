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
import kotlin.math.abs
import kotlin.math.atan2


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

    var lastMessage:String?=null;

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
            // Send data when the joystick position changes
            val message = getMessage(x,y) // encode degree for hardware
            if(message!=lastMessage) {
                lastMessage=message
                sendMessage(bluetoothSocket,message)
            }
        }
    }
}

fun calculateAngle(x: Float, y: Float): Double {
    // If both x and y are zero, return 0 degrees
    if (x == 0f && y == 0f) {
        return 0.0
    }

    // Calculate the angle in radians using arctangent
    var angle = Math.toDegrees(atan2(y.toDouble(), x.toDouble()))

    // Ensure the angle is positive
    if (angle < 0) {
        angle += 360.0
    }

    return angle
}

fun getMessage(x:Float, y:Float):String
{

    val threshold=10f
    // If both x and y are zero, return '0'
    if (abs(x) <=threshold && abs(y) <=threshold) {
        return "0"
    }

    // Calculate the angle in degrees
    val angle = calculateAngle(x, y)

    // Define character categories for each 60-degree increment
    val categories = arrayOf("f","a", "b", "c", "d","e") // Counter-clockwise mapping
    // a: 60 to 120
    // f: 0 to 60
    // e: -60 to 0

    // Categorize the angle based on 60-degree increments
    val categoryIndex = (angle% 360 / 60).toInt()
    return categories[categoryIndex]
}

   private fun sendMessage(socket: BluetoothSocket?, message: String) {
        if (socket != null && socket.isConnected) {
            try {
                val outputStream: OutputStream = socket.outputStream

                // Send the actual message
                outputStream.write(message.toByteArray())
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