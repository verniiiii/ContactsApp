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
                val map = mutableMapOf<Pair<String, String>, MutableList<Long>>() // (number, name) -> list of contactIds

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
                        val number = rawNumber.replace("[^\\d]".toRegex(), "") // оставить только цифры
                        val contactId = it.getString(1)?.toLongOrNull() ?: continue
                        val name = it.getString(2)?.trim() ?: continue

                        val key = number to name
                        map.getOrPut(key) { mutableListOf() }.add(contactId)
                    }
                }

                val contactsToDelete = map.values
                    .filter { it.size > 1 }
                    .flatMap { it.sortedDescending().drop(1) } // оставляем один, остальные удалим

                if (contactsToDelete.isEmpty()) {
                    Log.d("DuplicateRemoval", "Дубликаты не найдены")
                    return 1
                }

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