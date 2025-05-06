// IDuplicateContactService.aidl
package com.example.contactsapp.service;

// Declare any non-default types here with import statements

interface IDuplicateContactService {
    /**
     * Удаляет повторяющиеся контакты и возвращает статус
     * 0 - удалено успешно
     * 1 - не найдено дубликатов
     * 2 - произошла ошибка
     */
    int removeDuplicateContacts();
}