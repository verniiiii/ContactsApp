package com.example.contactsapp.data.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.contactsapp.service.IDuplicateContactService

/**
 * Сервис, реализующий AIDL-интерфейс для удаления дубликатов контактов.
 * Дубликаты определяются по одинаковому номеру телефона и имени.
 */
class DuplicateContactService : Service() {

    // Реализация AIDL-интерфейса
    private val binder = object : IDuplicateContactService.Stub() {

        /**
         * Удаляет дубликаты контактов.
         * Возвращает:
         * 0 — дубликаты успешно удалены,
         * 1 — дубликаты не найдены,
         * 2 — ошибка или недостаточно разрешений.
         */
        override fun removeDuplicateContacts(): Int {
            // Проверка разрешений на чтение и запись контактов
            if (ContextCompat.checkSelfPermission(
                    this@DuplicateContactService,
                    Manifest.permission.READ_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this@DuplicateContactService,
                    Manifest.permission.WRITE_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("DuplicateContactService", "Не хватает разрешений")
                return 2
            }

            return try {
                val resolver = contentResolver

                val map = mutableMapOf<Pair<String, String>, MutableList<Long>>()

                val cursor = resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    ),
                    null,
                    null,
                    null
                )

                cursor?.use {
                    while (it.moveToNext()) {
                        val rawNumber = it.getString(0) ?: continue
                        val number = rawNumber.replace("[^\\d]".toRegex(), "") // Удаляем все символы кроме цифр
                        val contactId = it.getString(1)?.toLongOrNull() ?: continue
                        val name = it.getString(2)?.trim() ?: continue

                        val key = number to name
                        map.getOrPut(key) { mutableListOf() }.add(contactId)
                    }
                }

                // Оставляем один contactId из каждой группы, остальные — в список на удаление
                val contactsToDelete = map.values
                    .filter { it.size > 1 }
                    .flatMap { it.sortedDescending().drop(1) } // Оставляем самый "свежий" (с наибольшим ID)

                if (contactsToDelete.isEmpty()) {
                    Log.d("DuplicateRemoval", "Дубликаты не найдены")
                    return 1
                }

                // Удаляем RawContacts по contactId
                for (contactId in contactsToDelete) {
                    val rawContactCursor = resolver.query(
                        ContactsContract.RawContacts.CONTENT_URI,
                        arrayOf(ContactsContract.RawContacts._ID),
                        "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                        arrayOf(contactId.toString()),
                        null
                    )

                    rawContactCursor?.use { rc ->
                        while (rc.moveToNext()) {
                            val rawContactId = rc.getLong(0)

                            // Удаление RawContact по его _ID
                            val rows = resolver.delete(
                                ContactsContract.RawContacts.CONTENT_URI,
                                "${ContactsContract.RawContacts._ID} = ?",
                                arrayOf(rawContactId.toString())
                            )
                            Log.d("DuplicateRemoval", "Удалено $rows записей RawContact с ID $rawContactId")
                        }
                    }
                }

                Log.d("DuplicateRemoval", "Удалено ${contactsToDelete.size} дублей")
                0
            } catch (e: Exception) {
                Log.e("DuplicateContactService", "Ошибка удаления: ${e.message}", e)
                2
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder {
        Log.d("DuplicateContactService", "onBind вызван")
        return binder
    }
}
