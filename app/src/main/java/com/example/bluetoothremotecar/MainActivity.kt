package com.example.bluetoothremotecar

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                            AppNavigation()
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
                    AppNavigation()
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
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Home.name) {
        composable(Screen.Home.name) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Joystick.name) {
            JoystickScreen(navController = navController)
        }
    }
}

@Composable
fun HomeScreen(navController: NavController){
        Column {
        Text(text = "Home Screen")
        Button(onClick = { navController.navigate(Screen.Joystick.name) }) {
            Text(text = "Go to Screen B")
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