package com.dhwani.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dhwani.app.data.OnboardingStore
import com.dhwani.app.ui.CallScreen
import com.dhwani.app.ui.CallViewModel
import com.dhwani.app.ui.PermissionGate
import com.dhwani.app.ui.call.MakeCallScreen
import com.dhwani.app.ui.home.HomeScreen
import com.dhwani.app.ui.navigation.DhwaniBottomBar
import com.dhwani.app.ui.navigation.DhwaniTab
import com.dhwani.app.ui.onboarding.OnboardingScreen
import com.dhwani.app.ui.profile.ProfileScreen
import com.dhwani.app.ui.sign.SignInterpreterScreen
import com.dhwani.app.ui.theme.DhwaniTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DhwaniTheme {
                val context = LocalContext.current
                val onboardingStore = remember { OnboardingStore(context) }
                var showOnboarding by remember { mutableStateOf(!onboardingStore.isCompleted) }

                if (showOnboarding) {
                    OnboardingScreen(
                        onGetStarted = {
                            onboardingStore.isCompleted = true
                            showOnboarding = false
                        }
                    )
                } else {
                    val permissions = buildList {
                        add(Manifest.permission.RECORD_AUDIO)
                        add(Manifest.permission.CAMERA)
                    }
                    val permissionState = rememberMultiplePermissionsState(permissions)
                    PermissionGate(
                        permissions = permissionState,
                        content = { DhwaniMainApp() },
                    )
                }
            }
        }
    }
}

@Composable
fun DhwaniMainApp(vm: CallViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(DhwaniTab.HOME) }
    val state by vm.state.collectAsState()

    Scaffold(
        bottomBar = {
            DhwaniBottomBar(
                currentTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        when (selectedTab) {
            DhwaniTab.HOME -> HomeScreen(
                onNavigateTab = { selectedTab = it },
                modifier = Modifier.padding(paddingValues)
            )
            DhwaniTab.LIVE -> CallScreen(
                vm = vm,
                modifier = Modifier.padding(paddingValues)
            )
            DhwaniTab.SIGN -> SignInterpreterScreen(
                state = state,
                vm = vm,
                modifier = Modifier.padding(paddingValues)
            )
            DhwaniTab.CALL -> MakeCallScreen(
                state = state,
                vm = vm,
                modifier = Modifier.padding(paddingValues)
            )
            DhwaniTab.YOU -> ProfileScreen(
                state = state,
                vm = vm,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
