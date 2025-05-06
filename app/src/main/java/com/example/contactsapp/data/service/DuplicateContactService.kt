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


class DuplicateContactService : Service() {

    private val binder = object : IDuplicateContactService.Stub(){
        override fun removeDuplicateContacts(): Int {
            // Проверка разрешений
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
                return 2 // Код ошибки
            }


            try {
                val resolver = contentResolver
                val numberToContacts = mutableMapOf<String, MutableList<Pair<Long, String>>>()

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

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val rawNumber = cursor.getString(0) ?: continue
                        val number = rawNumber.replace("[^\\d]".toRegex(), "") // оставить только цифры
                        val contactId = cursor.getString(1)?.toLongOrNull() ?: continue
                        val name = cursor.getString(2) ?: continue

                        Log.d("DebugCheck", "Найден номер: $rawNumber → $number, ID: $contactId, Имя: $name")

                        numberToContacts.getOrPut(number) { mutableListOf() }
                            .add(contactId to name)
                    }
                    cursor.close()
                }

                val contactsToDelete = mutableListOf<Long>()

                for ((number, list) in numberToContacts) {
                    if (list.size > 1) {
                        Log.d("DebugCheck", "Дубликаты номера $number: ${list.map { it.first to it.second }}")

                        val sorted = list.sortedByDescending { it.first } // по убыванию ID
                        contactsToDelete.addAll(sorted.drop(1).map { it.first }) // оставляем только один (новейший)
                    }
                }

                Log.d("DuplicateRemoval", "Контактов к удалению: ${contactsToDelete.size}")

                if (contactsToDelete.isEmpty()) return 1

                for (contactId in contactsToDelete) {
                    val rawContactCursor = resolver.query(
                        ContactsContract.RawContacts.CONTENT_URI,
                        arrayOf(ContactsContract.RawContacts._ID),
                        "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                        arrayOf(contactId.toString()),
                        null
                    )

                    if (rawContactCursor != null) {
                        while (rawContactCursor.moveToNext()) {
                            val rawContactId = rawContactCursor.getLong(0)
                            val rows = resolver.delete(
                                ContactsContract.RawContacts.CONTENT_URI,
                                "${ContactsContract.RawContacts._ID} = ?",
                                arrayOf(rawContactId.toString())
                            )
                            Log.d("DuplicateRemoval", "Удалено $rows записей RawContact с ID $rawContactId")
                        }
                        rawContactCursor.close()
                    } else {
                        Log.d("DuplicateRemoval", "RawContactCursor для ID $contactId пуст")
                    }
                }

                return 0
            } catch (e: Exception) {
                Log.e("DuplicateContactService", "Ошибка удаления: ${e.message}", e)
                return 2
            }
        }

    }

    override fun onBind(p0: Intent?): IBinder {
        Log.d("DuplicateContactService", "onBind вызван")

        return binder
    }
}