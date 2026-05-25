package com.example.gestionturnosapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.gestionturnosapp.data.local.PreferenceManager
import com.example.gestionturnosapp.data.UserManager
import com.example.gestionturnosapp.data.remote.ApiService
import com.example.gestionturnosapp.databinding.ActivityMainBinding
import com.example.gestionturnosapp.data.remote.RetrofitClient
import com.example.gestionturnosapp.notifications.NotificationHelper
import com.example.gestionturnosapp.util.NetworkMonitor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var navController: NavController? = null

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var apiService: ApiService

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            installSplashScreen()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "SplashScreen error", e)
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Inflate Error", t)
            // Fallback manual if ViewBinding fails
            setContentView(R.layout.activity_main)
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        
        if (navHostFragment != null && ::binding.isInitialized) {
            val controller = navHostFragment.navController
            navController = controller

            val appBarConfiguration = AppBarConfiguration(
                setOf(R.id.welcomeFragment, R.id.homeFragment, R.id.turnosListFragment, R.id.userProfileFragment, R.id.especialidadesFragment)
            )
            binding.toolbar.setupWithNavController(controller, appBarConfiguration)
            binding.bottomNavigation.setupWithNavController(controller)

            controller.addOnDestinationChangedListener { _, destination, _ ->
                if (!::binding.isInitialized) return@addOnDestinationChangedListener
                
                // Configuración de visibilidad centralizada
                when (destination.id) {
                    R.id.homeFragment -> {
                        binding.bottomNavigation.visibility = View.VISIBLE
                        binding.toolbar.visibility = View.VISIBLE
                    }
                    R.id.turnosListFragment, R.id.userProfileFragment -> {
                        binding.bottomNavigation.visibility = View.VISIBLE
                        binding.toolbar.visibility = View.GONE // Usan su propia UI o Toolbar
                    }
                    R.id.especialidadesFragment, R.id.solicitarTurnoFragment, R.id.settingsFragment, R.id.turnoDetailFragment -> {
                        binding.bottomNavigation.visibility = View.GONE
                        binding.toolbar.visibility = View.GONE // Usan su propio Toolbar interno
                    }
                    R.id.welcomeFragment, R.id.loginFragment, R.id.registerFragment -> {
                        binding.bottomNavigation.visibility = View.GONE
                        binding.toolbar.visibility = View.GONE
                    }
                    else -> {
                        binding.bottomNavigation.visibility = View.VISIBLE
                        binding.toolbar.visibility = View.VISIBLE
                    }
                }
            }
        }
        
        // Sincronizar FCM y pedir permisos
        sincronizarFcmToken()
        pedirPermisoNotificaciones()
        observeNetworkStatus()
        handleIntent(intent)
    }

    private fun observeNetworkStatus() {
        networkMonitor.isOnline
            .onEach { isOnline ->
                if (::binding.isInitialized) {
                    binding.tvOfflineBanner.visibility = if (isOnline) View.GONE else View.VISIBLE
                }
            }
            .launchIn(lifecycleScope)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val type = intent.getStringExtra("type") ?: return
        
        // Solo navegar si el usuario ya está autenticado
        if (userManager.getUser() == null) return

        when (type) {
            "chat" -> navController?.navigate(R.id.chatFragment)
            "turno" -> navController?.navigate(R.id.turnosListFragment)
        }
    }

    private fun sincronizarFcmToken() {
        // Solo sincronizar si el usuario está logueado
        if (userManager.getUser() == null) return

        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                userManager.saveFcmToken(token)
                
                if (!userManager.isFcmSynced()) {
                    lifecycleScope.launch {
                        try {
                            apiService.updateFcmToken(mapOf("token" to token))
                            userManager.markFcmAsSynced()
                            Log.d("FCM", "Token synced successfully")
                        } catch (e: Exception) {
                            Log.e("FCM", "Sync error", e)
                        }
                    }
                }
            }
        }
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController?.navigateUp() ?: super.onSupportNavigateUp()
    }
}
