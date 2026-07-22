package com.sacompanion.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sacompanion.service.background.SAAssistantService
import com.sacompanion.ui.screens.*
import com.sacompanion.ui.viewmodel.MainViewModel

object Routes {
    const val BOOT = "boot"
    const val HOME = "home"
    const val CHAT = "chat"
    const val MEMORY = "memory"
    const val FAMILY = "family"
    const val SETTINGS = "settings"
}

@Composable
fun SAApp(
    onStartService: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current

    // Listen to service broadcasts
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val state = intent.getStringExtra(SAAssistantService.EXTRA_STATE) ?: return
                val response = intent.getStringExtra(SAAssistantService.EXTRA_RESPONSE) ?: ""
                viewModel.onServiceUpdate(state, response)
            }
        }
        val filter = IntentFilter(SAAssistantService.BROADCAST_STATE)
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.BOOT,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { fadeIn(animationSpec = tween(600)) },
        exitTransition = { fadeOut(animationSpec = tween(400)) }
    ) {
        composable(Routes.BOOT) {
            BootAnimationScreen(
                onBootComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.BOOT) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToChat = { navController.navigate(Routes.CHAT) },
                onNavigateToMemory = { navController.navigate(Routes.MEMORY) },
                onNavigateToFamily = { navController.navigate(Routes.FAMILY) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onStartService = onStartService,
                onRequestOverlay = onRequestOverlay
            )
        }
        composable(Routes.CHAT) {
            ChatScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.MEMORY) {
            MemoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.FAMILY) {
            FamilyScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
