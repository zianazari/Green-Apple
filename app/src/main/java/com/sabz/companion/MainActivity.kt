package com.sabz.companion
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.time.LocalDate
import org.json.JSONObject

class MainActivity:ComponentActivity(){
 lateinit var store:Store
 var request by mutableStateOf<Pair<String,String>?>(null)
 var incoming by mutableStateOf<String?>(null)
 private val exportFile get()=java.io.File(cacheDir,"pending-export.bin")
 private val importFile get()=java.io.File(cacheDir,"pending-import.json")
 fun clearIncoming(){incoming=null;importFile.delete()}
 private val exportJson=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->writeExport(uri)}
 private val exportPdf=registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){uri->writeExport(uri)}
 private val importJson=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->
  if(uri!=null)lifecycleScope.launch{runCatching{withContext(Dispatchers.IO){contentResolver.openInputStream(uri)!!.use{val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var count=it.read(buffer);while(count>=0){if(count>0)out.write(buffer,0,count);require(out.size()<=5_000_000);count=it.read(buffer)};out.toString("UTF-8")}}}.onSuccess{raw->runCatching{Domain.validate(JSONObject(raw))}.onSuccess{importFile.writeText(raw);incoming=raw}.onFailure{toast(store.t("پشتیبان معتبر نیست؛ اطلاعات فعلی حفظ شد.","Invalid backup; existing data is unchanged."))}}.onFailure{toast(store.t("خواندن فایل انجام نشد.","Could not read the file."))}}
 }
 private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()){ReminderReceiver.schedule(this)}
 override fun onCreate(b:Bundle?){super.onCreate(b);store=Store(this);if(importFile.exists())incoming=runCatching{importFile.readText().also{Domain.validate(JSONObject(it))}}.getOrNull();consume(intent);ReminderReceiver.createChannel(this);setContent{GreenAppleApp(store,this)}}
 override fun onResume(){super.onResume();ReminderReceiver.schedule(this)}
 override fun onNewIntent(i:Intent){super.onNewIntent(i);setIntent(i);consume(i)}
 private fun consume(i:Intent){val date=i.getStringExtra("reviewDate");if(date!=null&&runCatching{LocalDate.parse(date)}.isSuccess)request="details" to date else if(i.getBooleanExtra("quick",false))request="quick" to LocalDate.now().toString()}
 fun toast(text:String){Toast.makeText(this,text,Toast.LENGTH_LONG).show()}
 fun backup(){runCatching{exportFile.writeText(if(store.loadError)store.originalRaw else store.root.toString());exportJson.launch("green-apple-backup-${LocalDate.now()}.json")}.onFailure{toast(store.t("فایل ذخیره نشد.","Could not save file."))}}
 fun importBackup(){importJson.launch(arrayOf("application/json","text/plain","application/octet-stream"))}
 fun report(start:LocalDate,end:LocalDate){
  if(end<start||java.time.temporal.ChronoUnit.DAYS.between(start,end)>30){toast(store.t("بازهٔ گزارش باید ۱ تا ۳۱ روز باشد.","Choose a range of 1–31 days."));return}
  val snapshot=store.root.copyJson()
  lifecycleScope.launch{runCatching{withContext(Dispatchers.Default){NativeReport.create(this@MainActivity,snapshot,start,end)}}.onSuccess{exportFile.writeBytes(it);exportPdf.launch("green-apple-$start-$end.pdf")}.onFailure{toast(store.t("گزارش ساخته نشد.","Could not create report."))}}
 }
 private fun writeExport(uri:Uri?){
  if(uri==null){exportFile.delete();return};if(!exportFile.exists())return
  lifecycleScope.launch{runCatching{withContext(Dispatchers.IO){contentResolver.openOutputStream(uri,"wt")!!.use{out->exportFile.inputStream().use{it.copyTo(out)}};exportFile.delete()}}.onSuccess{toast(store.t("فایل ذخیره شد.","File saved."))}.onFailure{toast(store.t("فایل ذخیره نشد.","Could not save file."))}}
 }
 fun requestNotifications(){
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED)permission.launch("android.permission.POST_NOTIFICATIONS")
  else if(Build.VERSION.SDK_INT>=31&&!getSystemService(AlarmManager::class.java).canScheduleExactAlarms())runCatching{startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:$packageName")))}
  else startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName))
 }
 fun configureReminders(){store.configure();if(store.settings.optBoolean("reminders")&&Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED)permission.launch("android.permission.POST_NOTIFICATIONS")}
}
