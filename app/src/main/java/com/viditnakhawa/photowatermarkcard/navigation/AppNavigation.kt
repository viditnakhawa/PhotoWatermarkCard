package com.viditnakhawa.photowatermarkcard.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.viditnakhawa.photowatermarkcard.AutomationScreen
import com.viditnakhawa.photowatermarkcard.gallery.GalleryScreen
import com.viditnakhawa.photowatermarkcard.templates.FrameTemplatesScreen
import androidx.navigation.navDeepLink

object AppRoutes {
    const val AUTOMATION_SCREEN = "automation"
    const val GALLERY_SCREEN = "gallery"
    const val TEMPLATES_SCREEN = "templates"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.GALLERY_SCREEN
    ) {
        composable(
            route = AppRoutes.AUTOMATION_SCREEN,
            deepLinks = listOf(navDeepLink { uriPattern = "app://photowatermarkcard/automation" })
        ) {
            AutomationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(AppRoutes.TEMPLATES_SCREEN) {
            FrameTemplatesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(AppRoutes.GALLERY_SCREEN) {
            GalleryScreen(
                onNavigateToAutomation = {
                    navController.navigate(AppRoutes.AUTOMATION_SCREEN)
                },
                onNavigateToTemplates = {
                    navController.navigate((AppRoutes.TEMPLATES_SCREEN))
                }
            )
        }
    }
}