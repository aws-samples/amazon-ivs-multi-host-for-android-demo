package com.amazon.ivs.multihostdemo.repository.cache

import android.content.Context
import androidx.core.content.edit
import com.amazon.ivs.multihostdemo.common.AVATARS
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

const val PREFERENCES_NAME = "secured_preferences"

@Singleton
class PreferenceProvider @Inject constructor(
    @ApplicationContext val context: Context
) {
    var username by stringPreference()
    var userId by stringPreference()
    var avatarUrl by stringPreference(AVATARS[0].url)
    var stageIdToDeleteOnExit by stringPreference()

    private val sharedPreferences by lazy { context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) }

    private fun stringPreference(defaultValue: String? = null) =
        object : ReadWriteProperty<Any?, String?> {
            override fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getString(
                property.name,
                defaultValue
            )

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
                sharedPreferences.edit { putString(property.name, value) }
            }
        }
}
