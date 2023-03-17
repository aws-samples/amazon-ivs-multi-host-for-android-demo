package com.amazon.ivs.multihostdemo.common

import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

object BindingAdapters {
    @BindingAdapter("loadImage")
    @JvmStatic
    fun setImage(view: ImageView, resource: String?) {
        resource?.let { res ->
            Glide.with(view.context)
                .load(res)
                .circleCrop()
                .into(view)
        }
    }

    @BindingAdapter("loadCircularImage")
    @JvmStatic
    fun setCircularImage(view: ImageView, resource: String?) {
        resource?.let { res ->
            Glide.with(view.context).load(res).apply(RequestOptions.circleCropTransform()).into(view)
        }
    }

    // Tint has a bug with not working in databinding without an app:tint Binding Adapter
    @JvmStatic
    @BindingAdapter("tint")
    fun ImageView.setImageTint(@ColorInt color: Int) {
        setColorFilter(color)
    }
}

fun ImageView.loadImage(url: String) {
    BindingAdapters.setImage(this, url)
}
