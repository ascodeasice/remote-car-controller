package com.example.bluetoothremotecar

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bluetoothremotecar.ui.theme.BluetoothRemoteCarTheme


enum class Screen() {
    Home,
    Joystick,
}

class MainActivity : ComponentActivity() {
    private val requestBluetoothPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                setContent {
                    BluetoothRemoteCarTheme {
                        // Restart the UI if permission is granted
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            val context=this;
                            AppNavigation(context)
                        }
                    }
                }
            } else {
                navigateToPermissionDeniedScreen()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            requestBluetoothPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
        composable(Screen.Joystick.name) {
            JoystickScreen(navController = navController)
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
                onClick={navController.navigate(Screen.Joystick.name)}
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

@Composable
fun JoystickScreen(navController: NavController){
    Column {
        Text(text = "Screen Joystick")
        Button(onClick = { navController.navigate(Screen.Home.name) }) {
            Text(text = "Go to Screen A")
        }
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