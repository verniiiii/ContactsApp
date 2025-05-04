package com.example.contactsapp.di

import com.example.contactsapp.data.repository.ContactsRepositoryImpl
import com.example.contactsapp.domain.repository.ContactsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AppModule {
    @Binds
    fun bindContactsRepository(
        impl: ContactsRepositoryImpl
    ): ContactsRepository
}