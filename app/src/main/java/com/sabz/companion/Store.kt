package com.sabz.companion

import android.content.Context
import androidx.compose.runtime.*
import org.json.*
import java.time.*
import java.util.UUID

/** Keeps the v1 JSON contract and SharedPreferences location for in-place upgrades. */
class Store(val context: Context) {
 private val prefs=context.getSharedPreferences("data",Context.MODE_PRIVATE)
 val originalRaw=prefs.getString("state","").orEmpty()
 var loadError by mutableStateOf(false); private set
 var root by mutableStateOf(runCatching { if(originalRaw.isBlank())Domain.blank() else Domain.validate(JSONObject(originalRaw)) }.getOrElse { loadError=true;Domain.blank() }); private set
 private val catalog=JSONObject(context.assets.open("foods.json").bufferedReader().use{it.readText()})
 val categories=catalog.objects("categories")
 val settings get()=root.getJSONObject("settings")
 val fa get()=settings.optString("lang","fa")=="fa"
 val dark get()=settings.optString("theme")=="dark"
 fun t(faText:String,en:String)=if(fa)faText else en
 fun foods()=catalog.objects("foods")+root.objects("custom")
 fun day(date:LocalDate)=Domain.day(root,date.toString())
 fun entry(date:LocalDate,edit:String?=null,now:LocalDateTime=LocalDateTime.now())=Domain.interval(root,date,now,edit)
 fun update(change:(JSONObject)->Unit){
  check(!loadError) { t("اطلاعات قبلی خوانده نشد. ابتدا پشتیبان را بازیابی کن.","Existing data could not be read. Restore a backup first.") }
  val copy=root.copyJson();change(copy);persist(copy)
 }
 private fun persist(copy:JSONObject){
  Domain.validate(copy);val raw=copy.toString();require(raw.length<=5_000_000)
  val edit=prefs.edit()
  if(!prefs.contains("preNativeState")&&originalRaw.isNotBlank())edit.putString("preNativeState",originalRaw)
  check(edit.putString("state",raw).commit()){t("ذخیره انجام نشد؛ فضای گوشی را بررسی کن.","Save failed. Check device storage.")}
  root=copy;loadError=false
 }
 fun restore(raw:String){require(raw.length<=5_000_000);val copy=Domain.validate(JSONObject(raw));persist(copy);configure()}
 fun configure(){
  context.getSharedPreferences("prefs",Context.MODE_PRIVATE).edit().putBoolean("fa",fa).putBoolean("dark",dark).putBoolean("enabled",settings.optBoolean("reminders")).putInt("wake",settings.optInt("wake",8)).putInt("sleep",settings.optInt("sleep",22)).commit()
  ReminderReceiver.schedule(context)
 }
 fun setSetting(key:String,value:Any){update{it.getJSONObject("settings").put(key,value)};configure()}
 fun mutate(date:LocalDate,edit:String?,change:(JSONObject)->Unit){
  require(settings.optBoolean("confirmed")){t("ابتدا هدف‌های روزانه را در تنظیمات تأیید کن.","Confirm your daily goals in Settings first.")}
  require(!date.isAfter(LocalDate.now())){t("برای آینده نمی‌توان ثبت کرد.","Future dates cannot be logged.")}
  update { state ->
   val day=Domain.day(state,date.toString());val entries=day.objects("entries").toMutableList()
   val entry=Domain.interval(state,date,LocalDateTime.now(),edit)
   val index=entries.indexOfFirst{it.getString("id")==entry.getString("id")}
   if(index<0&&!entry.recorded("sitting"))entry.put("sitting",60)
   if(!entry.has("activities")){val a=JSONObject();if(entry.recorded("activity"))a.put(entry.optString("activityType","walk"),entry.getInt("activity"));entry.put("activities",a)}
   change(entry)
   entry.put("longest",if(entry.recorded("sitting"))entry.get("sitting") else JSONObject.NULL)
   val a=entry.getJSONObject("activities")
   if(a.length()>0)entry.put("activity",a.keys().asSequence().sumOf{a.getInt(it)})
   if(index<0)entries.add(entry) else entries[index]=entry
   day.put("entries",jsonArray(entries.filter{!it.has("autoSlot")||Domain.hasData(it)})).put("complete",false)
   val total=Domain.totals(day)
   require(total.getValue("sitting")<=1440&&total.getValue("activity")<=1440){t("مجموع زمان روز نمی‌تواند بیش از ۲۴ ساعت باشد.","Daily duration cannot exceed 24 hours.")}
   state.getJSONObject("days").put(date.toString(),day)
  }
 }
 fun food(date:LocalDate,edit:String?,f:JSONObject,dir:Int=1,exact:Double?=null) {
  mutate(date,edit){e->
   val items=e.objects("items");val amount=items.filter{it.getJSONObject("food").getString("id")==f.getString("id")}.sumOf{it.getDouble("amount")}
   val next=exact?:Domain.nextAmount(f,amount,dir);require(next.isFinite()&&next in 0.0..20000.0)
   val updated=items.filter{it.getJSONObject("food").getString("id")!=f.getString("id")}.toMutableList()
   if(next>0)updated.add(JSONObject().put("food",f.copyJson()).put("amount",next))
   e.put("items",jsonArray(updated))
   val drink=f.optBoolean("drink")
   e.put(if(drink)"noDrink" else "noFood",next==0.0&&updated.none{it.getJSONObject("food").optBoolean("drink")==drink})
  }
  if(exact==null){val e=entry(date,edit);val value=e.objects("items").filter{it.getJSONObject("food").getString("id")==f.getString("id")}.sumOf{it.number("amount")};cache("food:$date:"+e.getString("id")+":"+f.getString("id"),value.toString())}
 }
 fun delete(date:LocalDate,id:String){update{val d=Domain.day(it,date.toString());d.put("entries",jsonArray(d.objects("entries").filter{e->e.getString("id")!=id})).put("complete",false);it.getJSONObject("days").put(date.toString(),d)}}
 fun complete(date:LocalDate){update{val d=Domain.day(it,date.toString());d.put("complete",!d.optBoolean("complete"));it.getJSONObject("days").put(date.toString(),d)}}
 fun cache(key:String,value:String){update{val ui=it.optJSONObject("ui")?:JSONObject().put("sessions",JSONObject()).put("forms",JSONObject()).also{u->it.put("ui",u)};val forms=ui.optJSONObject("forms")?:JSONObject().also{f->ui.put("forms",f)};forms.put(key,value)}}
 fun cached(key:String,default:String="")=root.optJSONObject("ui")?.optJSONObject("forms")?.optString(key,default)?:default
 fun custom(f:JSONObject){Domain.validateFood(f);update{it.getJSONArray("custom").put(f.copyJson().put("id","custom-"+UUID.randomUUID()));val forms=it.optJSONObject("ui")?.optJSONObject("forms");forms?.keys()?.asSequence()?.filter{k->k.startsWith("native-custom:")}?.toList()?.forEach{forms.remove(it)}}}
}
