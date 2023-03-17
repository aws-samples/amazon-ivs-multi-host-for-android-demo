package com.amazon.ivs.multihostdemo.repository.cache

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.amazon.ivs.multihostdemo.common.AVATARS
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

const val SECURED_PREFERENCES_NAME = "secured_preferences"

class SecuredPreferenceProvider(val context: Context) {
    var username by stringPreference()
    var userId by stringPreference()
    var avatarUrl by stringPreference(AVATARS[0].url)
    var stageIdToDeleteOnExit by stringPreference()

    private var spec = KeyGenParameterSpec.Builder(
        MasterKey.DEFAULT_MASTER_KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE)
        .build()

    private var masterKey = MasterKey.Builder(context)
        .setKeyGenParameterSpec(spec)
        .build()

    val preferences = EncryptedSharedPreferences.create(
        context,
        SECURED_PREFERENCES_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun stringPreference(defaultValue: String? = null) =
        object : ReadWriteProperty<Any?, String?> {
            override fun getValue(thisRef: Any?, property: KProperty<*>) = preferences.getString(
                property.name,
                defaultValue
            )

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
                preferences.edit().putString(property.name, value).apply()
            }
        }
}
