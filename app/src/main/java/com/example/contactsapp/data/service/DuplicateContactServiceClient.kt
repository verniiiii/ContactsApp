package com.example.contactsapp.data.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.example.contactsapp.service.IDuplicateContactService

/**
 * Клиент для взаимодействия с AIDL-сервисом DuplicateContactService.
 * Отвечает за привязку к сервису, отвязку и вызов метода удаления дубликатов.
 */
class DuplicateContactServiceClient(
    private val context: Context
) {
    private var service: IDuplicateContactService? = null

    private var isBound = false

    // Объект ServiceConnection для обработки подключения и отключения от сервиса
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            // Получаем ссылку на удалённый интерфейс
            service = IDuplicateContactService.Stub.asInterface(binder)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
        }
    }

    /**
     * Привязка к сервису удаления дубликатов.
     */
    fun bindService() {
        val intent = Intent(context, DuplicateContactService::class.java).apply {
            action = "com.example.contactsapp.DUPLICATE_CONTACT_SERVICE"
        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        context.startService(intent)
    }

    /**
     * Отвязка от сервиса.
     */
    fun unbindService() {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
    }

    /**
     * Запуск удаления дубликатов контактов.
     * @param onComplete Колбэк с результатом выполнения [DuplicateContactResult].
     */
    fun removeDuplicateContacts(onComplete: (DuplicateContactResult) -> Unit) {
        try {
            val result = service?.removeDuplicateContacts()
            val status = when (result) {
                0 -> DuplicateContactResult.SUCCESS
                1 -> DuplicateContactResult.NO_DUPLICATES_FOUND
                else -> DuplicateContactResult.ERROR
            }
            onComplete(status)
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(DuplicateContactResult.ERROR)
        }
    }
}
