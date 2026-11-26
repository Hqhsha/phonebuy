package com.phonebuy.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

private data class Phone(val id:Int,val model:String,val base:Int,val defects:MutableSet<String>,var inspected:Boolean=false,var ask:Int=0,var paid:Int=0,var listed:Boolean=false)
private val models=listOf("iPhone 8" to 9000,"iPhone 11" to 20000,"iPhone 12" to 29000,"iPhone 13" to 41000,"iPhone 14 Pro" to 68000,"Galaxy S10" to 11000,"Galaxy S21" to 27000,"Pixel 6" to 22000,"Redmi Note 10" to 9500,"Xiaomi 12" to 24000,"Poco X5" to 13000)
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity:ComponentActivity(){ override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState); val prefs=getSharedPreferences("phonebuy",0)
 setContent { MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF8175FF),background=Color(0xFF101028),surface=Color(0xFF202044))) {
 var money by remember { mutableIntStateOf(prefs.getInt("money",50000)) }; var day by remember { mutableIntStateOf(prefs.getInt("day",0)) }; var debt by remember { mutableIntStateOf(prefs.getInt("debt",0)) }; var id by remember { mutableIntStateOf(20) }
 val inv= remember { mutableStateListOf<Phone>() }; val market=remember { mutableStateListOf<Phone>() }; val offers=remember { mutableStateListOf<Pair<Int,Int>>() }; var page by remember { mutableStateOf("Главная") }; var msg by remember { mutableStateOf("Цель — накопить 1 000 000 ₽") }
 fun newPhone():Phone { val m=models.random(); val d=mutableSetOf<String>(); listOf("Экран","Батарея","Корпус").forEach{if(Random.nextFloat()<.35f)d.add(it)};return Phone(id++,m.first,m.second,d,ask=(m.second*Random.nextDouble(.72,1.12)).toInt()) }
 fun worth(p:Phone)= (p.base*(1-p.defects.sumOf{when(it){"Экран"->.25;"Батарея"->.12;else->.08}})).toInt()
 fun save(){prefs.edit().putInt("money",money).putInt("day",day).putInt("debt",debt).apply()}
 LaunchedEffect(Unit){ if(market.isEmpty()) repeat(6){market.add(newPhone())} }
 Scaffold(topBar={TopAppBar(title={Text("PhoneBuy  ·  День ${day+1}")},colors=TopAppBarDefaults.topAppBarColors(containerColor=Color(0xFF171735)))},containerColor=Color(0xFF101028),bottomBar={NavigationBar(containerColor=Color(0xFF191936)){listOf("Главная","Рынок","Склад","Банк","Задания").forEach{NavigationBarItem(selected=page==it,onClick={page=it},label={Text(it)},icon={Text(when(it){"Главная"->"⌂";"Рынок"->"📱";"Склад"->"▣";"Банк"->"₽";else->"★"})})}}}) { pad->
 Column(Modifier.padding(pad).fillMaxSize().padding(12.dp)) { Text("💰 ${money} ₽     🏦 −${debt} ₽",style=MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp)); Text(msg,color=Color(0xFFB7B5FF)); Spacer(Modifier.height(8.dp))
 when(page){
 "Главная"->{Text("Добро пожаловать в PhoneBuy",style=MaterialTheme.typography.headlineSmall); Text("Покупай телефоны ниже рыночной цены, проверяй дефекты, ремонтируй и продавай с прибылью."); Button(onClick={page="Рынок"}){Text("Открыть рынок")}; Button(onClick={page="Склад"}){Text("Мой склад (${inv.size})")}; Button(onClick={day++;debt=(debt*1.02).toInt();market.clear();repeat(6){market.add(newPhone())};offers.clear();inv.filter{it.listed}.forEach{p->if(Random.nextFloat()<.45f)offers.add(p.id to (p.ask*Random.nextDouble(.82,1.0)).toInt())};msg="Наступил новый день. Появились новые объявления и покупатели.";save()}){Text("😴 Лечь спать · следующий день")}}
 "Рынок"->{LazyColumn{items(market,key={it.id}){p->Card(Modifier.fillMaxWidth().padding(vertical=5.dp)){Column(Modifier.padding(12.dp)){Text(p.model,style=MaterialTheme.typography.titleMedium);Text("Цена продавца: ${p.ask} ₽");if(p.inspected)Text("Состояние: ${if(p.defects.isEmpty())"без дефектов" else p.defects.joinToString()}; стоимость ≈ ${worth(p)} ₽")
 Row {Button(onClick={p.inspected=true;msg="Осмотр: ${p.model}"}){Text("Осмотреть")};Button(onClick={if(Random.nextBoolean()){p.ask=(p.ask*.9).toInt();msg="Торг успешен: −10%"}else{market.remove(p);msg="Продавец отказал и снял объявление"}}){Text("Торг")};Button(onClick={if(money>=p.ask){money-=p.ask;p.paid=p.ask;p.inspected=true;inv.add(p);market.remove(p);msg="Куплен ${p.model}";save()}else msg="Не хватает денег"}){Text("Купить")}}}}}}
 "Склад"->{LazyColumn{items(inv,key={it.id}){p->Card(Modifier.fillMaxWidth().padding(vertical=5.dp)){Column(Modifier.padding(12.dp)){Text(p.model,style=MaterialTheme.typography.titleMedium);Text("Закупка ${p.paid} ₽ · стоимость ${worth(p)} ₽");Text(if(p.defects.isEmpty())"Дефектов нет" else p.defects.joinToString());Row{Button(onClick={p.listed=true;p.ask=(worth(p)*1.1).toInt();msg="Выставлен за ${p.ask} ₽"}){Text("Выставить")};Button(onClick={val pay=(p.base*.7).toInt();money+=pay;inv.remove(p);msg="Сдан в скупку за $pay ₽";save()}){Text("Скупка 70%")}}
 p.defects.toList().forEach{d->Button(onClick={val cost=(p.base*when(d){"Экран"->.15;"Батарея"->.06;else->.04}).toInt();if(money>=cost){money-=cost;p.defects.remove(d);msg="$d исправлен за $cost ₽";save()}else msg="Не хватает денег"}){Text("Ремонт: $d")}}
 offers.filter{it.first==p.id}.forEach{o->Button(onClick={money+=o.second;inv.remove(p);offers.remove(o);msg="Продано за ${o.second} ₽";save()}){Text("Принять предложение ${o.second} ₽")}}
 }}}}
 "Банк"->{Text("Баланс: $money ₽\nДолг: $debt ₽ (2% в день)");Row{Button(onClick={if(debt+10000<=200000){debt+=10000;money+=10000;save()}else msg="Лимит долга 200 000 ₽"}){Text("Займ 10k")};Button(onClick={val n=minOf(debt,money);debt-=n;money-=n;save()}){Text("Погасить")}}}
 else->{Text("Задания",style=MaterialTheme.typography.headlineSmall);Text("• Купи первый телефон\n• Отремонтируй 3 телефона\n• Продай 5 телефонов\n• Заработай 20 000 ₽ прибыли\n• Накопи 100 000 ₽\n• Накопи 1 000 000 ₽ — победа!");Text("Капитал: ${money+inv.sumOf{worth(it)}-debt} ₽")}
 }
 };save()
 } } }
}
