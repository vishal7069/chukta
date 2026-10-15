package com.chukta

interface Platform {
    val apiUrl: String
    val development: Boolean
    fun read(key: String): String?
    fun write(key: String, value: String?)
    fun openUrl(url: String): Boolean
    fun share(text: String)
    fun copy(text: String)
    fun newId(): String
    fun today(): String
    fun formatDate(iso: String): String
    fun pickPhoto(onResult: (String?) -> Unit)
    fun pickContact(onResult: (String?) -> Unit)
    fun photoData(uri: String): String?
}
