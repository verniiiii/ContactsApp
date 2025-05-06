package com.example.contactsapp.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.contactsapp.presentation.ui.ContactListScreen
import com.example.contactsapp.ui.theme.ContactsAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ContactsAppTheme {
                // Состояние, предоставлены ли все разреешния
                var contactsPermissionGranted by remember { mutableStateOf(false) }

                // Лаунчер для запроса разрешений
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { grants ->
                    // Если все разрешения предоставлены, обновляем состояние
                    contactsPermissionGranted = grants.all { it.value }
                }

                // Логика проверки разрешений
                LaunchedEffect(Unit) {
                    val requiredPermissions = arrayOf(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.CALL_PHONE
                    )

                    // Проверяем, предоставлены ли все необходимые разрешения
                    val granted = requiredPermissions.all {
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            it
                        ) == PackageManager.PERMISSION_GRANTED
                    }

                    // Если разрешения уже предоставлены, обновляем состояние
                    if (granted) {
                        contactsPermissionGranted = true
                    } else {
                        // Запускаем запрос разрешений, если они не предоставлены
                        launcher.launch(requiredPermissions)
                    }
                }

                // Отображение UI в зависимости от статуса разрешений
                if (contactsPermissionGranted) {
                    ContactListScreen()
                } else {
                    Text("Требуются разрешения для работы с контактами")
                }
            }
        }
    }
}
