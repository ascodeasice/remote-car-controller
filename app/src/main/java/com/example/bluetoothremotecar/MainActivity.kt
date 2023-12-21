package com.example.bluetoothremotecar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
  Joystick
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
