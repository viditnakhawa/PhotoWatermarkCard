package com.viditnakhawa.photowatermarkcard.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.viditnakhawa.photowatermarkcard.AutomationScreen
import com.viditnakhawa.photowatermarkcard.gallery.GalleryScreen

object AppRoutes {
    const val AUTOMATION_SCREEN = "automation"
    const val GALLERY_SCREEN = "gallery"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.AUTOMATION_SCREEN
    ) {
        composable(AppRoutes.AUTOMATION_SCREEN) {
            AutomationScreen(
                onNavigateToGallery = {
                    navController.navigate(AppRoutes.GALLERY_SCREEN)
                }
            )
        }

        composable(AppRoutes.GALLERY_SCREEN) {
            GalleryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
