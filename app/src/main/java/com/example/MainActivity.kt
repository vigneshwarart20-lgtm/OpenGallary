package com.example

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.components.JapaneseFanSplash
import com.example.ui.components.PermissionScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.PureGalleryTheme
import com.example.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themePreference by viewModel.themePreference.collectAsState()
            val hasPermission by viewModel.hasPermission.collectAsState()

            PureGalleryTheme(themePreference = themePreference) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GalleryRoot(
                        viewModel = viewModel,
                        hasPermission = hasPermission,
                        checkPermissions = { checkAndRequestPermissions() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsState()
    }

    private fun checkPermissionsState() {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasImages = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
            val hasVideos = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED

            val hasSelected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                ) == PackageManager.PERMISSION_GRANTED
            } else false

            (hasImages && hasVideos) || hasSelected
        } else {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        viewModel.setPermissionGranted(granted)
    }

    private fun checkAndRequestPermissions() {
        checkPermissionsState()
    }
}

@Composable
fun GalleryRoot(
    viewModel: GalleryViewModel,
    hasPermission: Boolean,
    checkPermissions: () -> Unit
) {
    var isAppOpening by rememberSaveable { mutableStateOf(true) }

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val anyGranted = results.values.any { it }
        viewModel.setPermissionGranted(anyGranted)
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    if (isAppOpening) {
        JapaneseFanSplash(
            onAnimationComplete = { isAppOpening = false }
        )
    } else if (hasPermission) {
        MainScreen(viewModel = viewModel)
    } else {
        PermissionScreen(
            onRequestPermission = {
                launcher.launch(permissionsToRequest)
            }
        )
    }
}
