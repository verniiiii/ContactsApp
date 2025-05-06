package com.example.contactsapp.data.service

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.example.contactsapp.service.IDuplicateContactService

class DuplicateContactServiceClient(
    private val context: Context
) {
    private var service: IDuplicateContactService? = null
    private var isBound = false

    private val connection = object  : ServiceConnection{
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            service = IDuplicateContactService.Stub.asInterface(p1)
            isBound = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            service = null
            isBound = false
        }
    }

    fun bindService() {
        val intent = Intent(context, DuplicateContactService::class.java).apply {
            action = "com.example.contactsapp.DUPLICATE_CONTACT_SERVICE"
        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        context.startService(intent)
    }

    fun unbindService(){
        if(isBound){
            context.unbindService(connection)
            isBound = false
        }
    }

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