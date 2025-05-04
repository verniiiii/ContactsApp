package com.example.contactsapp.domain.repository

import android.content.Context
import com.example.contactsapp.domain.model.Contact

interface ContactsRepository {
    fun getContacts(context: Context): List<Contact>
}