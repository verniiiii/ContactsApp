package com.example.contactsapp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri

fun makePhoneCall(context: Context, phoneNumber: String) {
    try {
        // Intent для совершения звонка
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = "tel:$phoneNumber".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK // Запуск новой активности
        }

        context.startActivity(intent)

    } catch (e: SecurityException) {
        Toast.makeText(context, "Ошибка: нет разрешения на звонки", Toast.LENGTH_SHORT).show()

    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка при попытке звонка", Toast.LENGTH_SHORT).show()
    }
}
