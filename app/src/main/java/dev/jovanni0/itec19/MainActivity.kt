package dev.jovanni0.itec19


import androidx.compose.runtime.remember
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dev.jovanni0.itec19.screen.ArScreen
import dev.jovanni0.itec19.screen.FloorplanScreen
import dev.jovanni0.itec19.screen.MapScreen
import dev.jovanni0.itec19.screen.TeamPeakScreen
import dev.jovanni0.itec19.stores.AppStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {
    private var canShowAR by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            lifecycleScope.launch(Dispatchers.IO) {
                // small delay to let the permission system settle
                delay(100)
                withContext(Dispatchers.Main) {
                    canShowAR = true
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        AppStore.init(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            canShowAR = true
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            var selectedTab by remember { mutableIntStateOf(0) }

            LaunchedEffect(Unit) {
                while (true) {
                    AppStore.serverReachable = withContext(Dispatchers.IO) {
                        try {
                            val socket = java.net.Socket()
                            socket.connect(java.net.InetSocketAddress(AppStore.SERVER_IP, 8080), 2000)
                            socket.close()
                            true
                        } catch (e: Exception) {
                            false
                        }
                    }
                    delay(5000) // recheck every 5 seconds
                }
            }

            Scaffold(
                bottomBar = {
                    if (AppStore.team != null)
                    {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.LocationOn, contentDescription = "Map") },
                                label = { Text("Map") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Team") },
                                label = { Text("Team") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.List, contentDescription = "Floor Plan") },
                                label = { Text("Floor Plan") }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                ) {
                    if (canShowAR)
                    {
                        ArScreen(
                            assets = assets,
                            modifier = Modifier.fillMaxSize().then(
                                if (selectedTab == 0) Modifier else Modifier.size(0.dp)
                            ),
                            isActive = selectedTab == 0
                        )
                    }
                    else if (selectedTab == 0)
                    {
                        Box(Modifier.fillMaxSize().background(Color.White)) {
                            Text("Waiting for Camera...")
                        }
                    }

                    if (selectedTab == 1) {
                        MapScreen(modifier = Modifier.fillMaxSize().background(Color.White))
                    }

                    if (selectedTab == 2 || AppStore.team == null)
                    {
                        TeamPeakScreen(onTeamSelected = {
                            AppStore.team = it
                            selectedTab = 0
                        })
                    }

                    if (selectedTab == 3)
                    {
                        FloorplanScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                        )
                    }

                    when (AppStore.serverReachable) {
                        false -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Red.copy(alpha = 0.8f))
                                .padding(8.dp)
                                .align(Alignment.TopCenter),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Server unreachable",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        null -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Gray.copy(alpha = 0.8f))
                                .padding(8.dp)
                                .align(Alignment.TopCenter),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Connecting to server...", color = Color.White)
                        }

                        true -> {}
                    }
                }
            }
        }

    }

}