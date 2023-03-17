package com.amazon.ivs.multihostdemo.common.extensions

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

// Amazon IVS Date example - "2022-10-31T17:47:41.000Z"
@SuppressLint("SimpleDateFormat")
fun String.toDate(): Date? = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(this)
