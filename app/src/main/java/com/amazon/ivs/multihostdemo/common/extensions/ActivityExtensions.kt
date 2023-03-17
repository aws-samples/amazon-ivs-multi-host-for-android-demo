package com.amazon.ivs.multihostdemo.common.extensions

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import com.amazon.ivs.multihostdemo.R

fun FragmentActivity.getNavController() =
    (supportFragmentManager.findFragmentById(R.id.fragment_host_view) as NavHostFragment).navController

fun AppCompatActivity.getCurrentFragment() =
    supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.first()
