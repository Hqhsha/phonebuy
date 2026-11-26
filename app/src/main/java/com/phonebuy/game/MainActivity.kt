package com.phonebuy.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private data class Phone(val id: Int, val model: String, val price: Int, var inspected: Boolean = false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("phonebuy", MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF8175FF), background = Color(0xFF101028), surface = Color(0xFF202044))) {
                var money by remember { mutableIntStateOf(prefs.getInt("money", 50000)) }
                var day by remember { mutableIntStateOf(prefs.getInt("day", 0)) }
                var debt by remember { mutableIntStateOf(prefs.getInt("debt", 0)) }
                var nextId by remember { mutableIntStateOf(1) }
                var page by remember { mutableStateOf("Главная") }
                var message by remember { mutableStateOf("Цель — накопить 1 000 000 ₽") }
                val inventory = remember { mutableStateListOf<Phone>() }
                val market = remember { mutableStateListOf<Phone>() }
                fun makePhone(): Phone {
                    val options = listOf("iPhone 8" to 9000, "iPhone 11" to 20000, "iPhone 12" to 29000, "iPhone 13" to 41000, "Galaxy S21" to 27000, "Pixel 6" to 22000, "Redmi Note 10" to 9500, "Xiaomi 12" to 24000)
                    val item = options.random()
                    return Phone(nextId++, item.first, (item.second * Random.nextDouble(0.7, 1.1)).toInt())
                }
                fun saveProgress() {
                    prefs.edit().putInt("money", money).putInt("day", day).putInt("debt", debt).apply()
                }
                LaunchedEffect(Unit) { if (market.isEmpty()) repeat(6) { market.add(makePhone()) } }
                Scaffold(
                    topBar = { TopAppBar(title = { Text("PhoneBuy · День ${day + 1}") }) },
                    bottomBar = {
                        NavigationBar {
                            listOf("Главная", "Рынок", "Склад", "Банк", "Задания").forEach { tab ->
                                NavigationBarItem(selected = page == tab, onClick = { page = tab }, label = { Text(tab) }, icon = { Text("•") })
                            }
                        }
                    }
                ) { padding ->
                    Column(Modifier.padding(padding).fillMaxSize().padding(12.dp)) {
                        Text("Баланс: $money ₽    Долг: $debt ₽", style = MaterialTheme.typography.titleMedium)
                        Text(message, color = Color(0xFFB7B5FF), modifier = Modifier.padding(vertical = 8.dp))
                        when (page) {
                            "Главная" -> {
                                Text("Добро пожаловать в PhoneBuy", style = MaterialTheme.typography.headlineSmall)
                                Text("Покупай телефоны, проверяй объявления и продавай с прибылью.")
                                Button(onClick = { page = "Рынок" }) { Text("Открыть рынок") }
                                Button(onClick = {
                                    day += 1
                                    debt = (debt * 1.02).toInt()
                                    market.clear()
                                    repeat(6) { market.add(makePhone()) }
                                    message = "Новый день: рынок обновлён, проценты по долгу начислены."
                                    saveProgress()
                                }) { Text("Лечь спать · следующий день") }
                            }
                            "Рынок" -> LazyColumn {
                                items(market, key = { it.id }) { phone ->
                                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(phone.model, style = MaterialTheme.typography.titleMedium)
                                            Text("Цена: ${phone.price} ₽")
                                            if (phone.inspected) Text("Осмотр завершён · оценка: ${(phone.price * 1.18).toInt()} ₽")
                                            Row {
                                                Button(onClick = { phone.inspected = true; message = "Осмотрено: ${phone.model}" }) { Text("Осмотреть") }
                                                Button(onClick = {
                                                    if (money >= phone.price) {
                                                        money -= phone.price
                                                        inventory.add(phone)
                                                        market.remove(phone)
                                                        message = "Куплен ${phone.model}"
                                                        saveProgress()
                                                    } else message = "Не хватает денег"
                                                }) { Text("Купить") }
                                            }
                                        }
                                    }
                                }
                            }
                            "Склад" -> LazyColumn {
                                items(inventory, key = { it.id }) { phone ->
                                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(phone.model, style = MaterialTheme.typography.titleMedium)
                                            Button(onClick = {
                                                val sale = (phone.price * 1.2).toInt()
                                                money += sale
                                                inventory.remove(phone)
                                                message = "Продано за $sale ₽"
                                                saveProgress()
                                            }) { Text("Продать скупщику") }
                                        }
                                    }
                                }
                            }
                            "Банк" -> {
                                Text("Кредитный лимит: 200 000 ₽ · ставка 2% в день")
                                Row {
                                    Button(onClick = {
                                        if (debt + 10000 <= 200000) { debt += 10000; money += 10000; saveProgress() }
                                        else message = "Лимит кредита достигнут"
                                    }) { Text("Взять 10 000 ₽") }
                                    Button(onClick = {
                                        val amount = minOf(debt, money)
                                        debt -= amount
                                        money -= amount
                                        saveProgress()
                                    }) { Text("Погасить долг") }
                                }
                            }
                            else -> {
                                Text("Задания", style = MaterialTheme.typography.headlineSmall)
                                Text("Купи телефон • Продай телефон • Накопи 100 000 ₽ • Цель: 1 000 000 ₽")
                                Text("Текущий капитал: ${money - debt + inventory.sumOf { it.price }} ₽")
                            }
                        }
                        saveProgress()
                    }
                }
            }
        }
    }
}
