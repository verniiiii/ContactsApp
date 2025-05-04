package com.example.contactsapp.domain.usecase

import android.content.Context
import com.example.contactsapp.domain.model.Contact
import com.example.contactsapp.domain.repository.ContactsRepository
import javax.inject.Inject

class GetContactsUseCase @Inject constructor(
    private val repository: ContactsRepository
) {
    fun execute(context: Context): List<Contact> {
        return repository.getContacts(context)
    }
}