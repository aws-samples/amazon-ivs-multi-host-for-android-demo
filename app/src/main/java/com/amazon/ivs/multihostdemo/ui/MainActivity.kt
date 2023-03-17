package com.amazon.ivs.multihostdemo.ui

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.amazon.ivs.multihostdemo.common.PermissionHandler
import com.amazon.ivs.multihostdemo.common.extensions.getCurrentFragment
import com.amazon.ivs.multihostdemo.common.extensions.getNavController
import com.amazon.ivs.multihostdemo.databinding.ActivityMainBinding
import com.amazon.ivs.multihostdemo.ui.introduction.IntroductionFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val permissionHandler by lazy { PermissionHandler(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionHandler.checkPermissions()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onSupportNavigateUp()
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHandler.onPermissionResult(requestCode, grantResults)
    }

    override fun onSupportNavigateUp(): Boolean {
        val currentFragment = getCurrentFragment()
        when {
            currentFragment is IntroductionFragment -> finish()
            currentFragment !is BackHandler || currentFragment.canGoBack() -> getNavController().navigateUp()
        }
        return false
    }
}
