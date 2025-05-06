package com.example.contactsapp.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.contactsapp.data.service.DuplicateContactService
import com.example.contactsapp.data.service.DuplicateContactServiceClient
import com.example.contactsapp.presentation.viewmodel.ContactsViewModel
import com.example.contactsapp.util.makePhoneCall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val client = remember(context) { DuplicateContactServiceClient(context) }

    var pendingPhoneNumber by remember { mutableStateOf<String?>(null) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && pendingPhoneNumber != null) {
            makePhoneCall(context, pendingPhoneNumber!!)
            pendingPhoneNumber = null
        } else {
            Toast.makeText(context, "Разрешение на звонки не предоставлено", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        client.bindService()
        onDispose {
            client.unbindService()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadContacts(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Контакты") },
                actions = {
                    IconButton(onClick = {
                        showDeleteDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить дубликаты"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text("Загрузка контактов...")
            }
        } else {
            val groupedContacts = contacts.groupBy {
                it.name.firstOrNull()?.uppercaseChar() ?: '#'
            }.toSortedMap()

            LazyColumn(
                contentPadding = padding,
                modifier = Modifier.fillMaxSize()
            ) {
                groupedContacts.forEach { (initial, contactsForLetter) ->
                    // Заголовок с буквой
                    item {
                        Text(
                            text = initial.toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Контакты под этой буквой
                    items(contactsForLetter.size) { index ->  // Используем size и index
                        val contact = contactsForLetter[index]  // Получаем контакт по индексу
                        ContactListItem(contact = contact) {
                            val phone = it.phoneNumber
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.CALL_PHONE
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                makePhoneCall(context, phone)
                            } else {
                                pendingPhoneNumber = phone
                                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Удалить дубликаты?") },
                text = { Text("Вы уверены, что хотите удалить дублирующиеся контакты?") },
                confirmButton = {
                    Text(
                        text = "Удалить",
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                showDeleteDialog = false
                                client.removeDuplicateContacts { success ->
                                    if (success) {
                                        viewModel.loadContacts(context)
                                        Toast.makeText(context, "Дубликаты удалены", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Ошибка при удалении", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                    )
                },
                dismissButton = {
                    Text(
                        text = "Отмена",
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable { showDeleteDialog = false }
                    )
                }
            )
        }
    }
}

