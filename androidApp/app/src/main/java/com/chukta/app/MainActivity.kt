package com.chukta.app

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.chukta.ChuktaApp
import com.chukta.Platform
import java.security.KeyStore
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.io.ByteArrayOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class MainActivity: ComponentActivity() {
    private var photoCallback: ((String?) -> Unit)? = null
    private var contactCallback: ((String?) -> Unit)? = null
    private val contactPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val name=result.data?.data?.let { uri -> runCatching {
            contentResolver.query(uri,arrayOf(ContactsContract.Contacts.DISPLAY_NAME),null,null,null)?.use { cursor -> if(cursor.moveToFirst())cursor.getString(0) else null }
        }.getOrNull() }
        contactCallback?.invoke(name);contactCallback=null
    }
    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { runCatching { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        photoCallback?.invoke(uri?.toString()); photoCallback = null
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = getSharedPreferences("chukta", Context.MODE_PRIVATE)
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "chukta-storage"
        val key = if (keyStore.containsAlias(alias)) keyStore.getKey(alias, null) as SecretKey else KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
        val platform = object: Platform {
            override val apiUrl: String get() = read("api-url") ?: BuildConfig.API_URL
            override val development = BuildConfig.DEBUG
            override fun read(keyName: String): String? = runCatching {
                val stored = preferences.getString(keyName, null) ?: return@runCatching null
                val parts = stored.split(":")
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
                String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
            }.getOrNull()
            override fun write(keyName: String, value: String?) {
                if (value == null) { preferences.edit().remove(keyName).apply(); return }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
                val encoded = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(cipher.doFinal(value.toByteArray()), Base64.NO_WRAP)
                preferences.edit().putString(keyName, encoded).apply()
            }
            override fun newId() = UUID.randomUUID().toString()
            override fun today() = LocalDate.now().toString()
            override fun formatDate(iso: String) = runCatching{OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM, h:mm a"))}.getOrDefault(iso)
            override fun openUrl(url: String): Boolean = try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))); true } catch (_: ActivityNotFoundException) { false }
            override fun share(text: String) { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share with friends")) }
            override fun copy(text: String) { (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Chukta", text)) }
            override fun pickPhoto(onResult: (String?) -> Unit) { photoCallback = onResult; photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            override fun pickContact(onResult: (String?) -> Unit) { contactCallback=onResult; contactPicker.launch(Intent(Intent.ACTION_PICK,ContactsContract.Contacts.CONTENT_URI)) }
            override fun photoData(uri: String): String? = runCatching {
                val source=contentResolver.openInputStream(Uri.parse(uri))?.use{BitmapFactory.decodeStream(it)} ?: return@runCatching null
                val ratio=minOf(1f,1024f/maxOf(source.width,source.height))
                val scaled=Bitmap.createScaledBitmap(source,(source.width*ratio).toInt(),(source.height*ratio).toInt(),true)
                val out=ByteArrayOutputStream();scaled.compress(Bitmap.CompressFormat.JPEG,75,out)
                "data:image/jpeg;base64,"+Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP)
            }.getOrNull()
        }
        intent.data?.takeIf{it.scheme=="chukta"&&it.host=="invite"}?.lastPathSegment?.takeIf{it.matches(Regex("[a-f0-9]{32}"))}?.let{platform.write("pendingInvite",it)}
        setContent { ChuktaApp(platform) { enabled, back -> BackHandler(enabled=enabled,onBack = back) } }
    }
}
