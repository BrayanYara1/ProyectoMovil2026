package com.example.gestionturnosapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.example.gestionturnosapp.network.RetrofitClient
import kotlinx.coroutines.launch
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.gestionturnosapp.data.UserManager
import com.example.gestionturnosapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var navController: NavController? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Pantalla de Inicio (Splash Screen) - Debe llamarse antes de super.onCreate
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Cargar sesión
        UserManager.loadUser(this)

        // Inicializar ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        
        if (navHostFragment != null) {
            val controller = navHostFragment.navController
            navController = controller

            val appBarConfiguration = AppBarConfiguration(
                setOf(R.id.welcomeFragment, R.id.homeFragment, R.id.turnosListFragment, R.id.userProfileFragment, R.id.especialidadesFragment)
            )
            binding.toolbar.setupWithNavController(controller, appBarConfiguration)
            binding.bottomNavigation.setupWithNavController(controller)

            controller.addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.homeFragment, R.id.turnosListFragment, R.id.userProfileFragment, R.id.especialidadesFragment -> {
                        binding.bottomNavigation.visibility = View.VISIBLE
                        binding.toolbar.visibility = View.VISIBLE
                    }
                    R.id.welcomeFragment, R.id.loginFragment, R.id.registerFragment -> {
                        binding.bottomNavigation.visibility = View.GONE
                        binding.toolbar.visibility = View.GONE
                    }
                    else -> {
                        binding.bottomNavigation.visibility = View.GONE
                        binding.toolbar.visibility = View.VISIBLE
                    }
                }
            }
        }
        
        pedirPermisoNotificaciones()
        sincronizarFcmToken()
    }

    private fun sincronizarFcmToken() {
        if (!UserManager.isFcmSynced(this)) {
            val token = UserManager.getFcmToken(this)
            token?.let {
                lifecycleScope.launch {
                    try {
                        RetrofitClient.instance.updateFcmToken(mapOf("token" to it))
                        UserManager.markFcmAsSynced(this@MainActivity)
                    } catch (e: Exception) {
                        android.util.Log.e("FCM", "Sync error", e)
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
