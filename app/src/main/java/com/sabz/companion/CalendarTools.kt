package com.sabz.companion
import android.icu.util.Calendar
import android.icu.util.ULocale
import android.icu.text.SimpleDateFormat
import java.time.*
import java.text.NumberFormat
import java.util.Locale

object CalendarTools {
 fun calendar(date:LocalDate,persian:Boolean):Calendar=Calendar.getInstance(ULocale(if(persian)"fa_IR@calendar=persian" else "en_US@calendar=gregorian")).apply{timeInMillis=date.atTime(12,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()}
 fun date(c:Calendar):LocalDate=Instant.ofEpochMilli(c.timeInMillis).atZone(ZoneId.systemDefault()).toLocalDate()
 fun label(date:LocalDate,persian:Boolean,fa:Boolean,pattern:String="d MMMM yyyy"):String{
  val c=calendar(date,persian);return SimpleDateFormat(pattern,ULocale((if(fa)"fa_IR" else "en_US")+if(persian)"@calendar=persian" else "@calendar=gregorian")).apply{calendar=c}.format(c.time)
 }
 fun start(date:LocalDate,persian:Boolean):LocalDate= date(calendar(date,persian).apply{set(Calendar.DAY_OF_MONTH,1)})
 fun shiftMonth(date:LocalDate,persian:Boolean,delta:Int):LocalDate=date(calendar(start(date,persian),persian).apply{add(Calendar.MONTH,delta)})
 fun fromParts(year:Int,month:Int,day:Int,persian:Boolean):LocalDate{
  val c=calendar(LocalDate.now(),persian);c.isLenient=false;c.set(Calendar.YEAR,year);c.set(Calendar.MONTH,month-1);c.set(Calendar.DAY_OF_MONTH,day);return date(c)
 }
 fun dayNumber(date:LocalDate,persian:Boolean)=calendar(date,persian).get(Calendar.DAY_OF_MONTH)
}
fun Store.num(value:Double,digits:Int=0):String=NumberFormat.getNumberInstance(Locale(if(fa)"fa" else "en")).apply{maximumFractionDigits=digits}.format(value)
fun Store.num(value:Int)=num(value.toDouble())
fun Store.duration(minutes:Double):String {val m=minutes.toInt();val h=m/60;return if(h>0)num(h)+" "+t("ساعت","h")+(if(m%60>0)" "+num(m%60)+" "+t("دقیقه","min") else "") else num(m)+" "+t("دقیقه","min")}
fun Store.dateLabel(date:LocalDate)=CalendarTools.label(date,settings.optString("calendar")=="persian",fa)
fun Store.foodName(f:org.json.JSONObject)=f.optString(if(fa)"fa" else "en")
fun intervalLabel(e:org.json.JSONObject):String=if(e.has("autoSlot"))"%02d:00 – %02d:00".format(e.getInt("autoStart"),e.getInt("autoSlot")+3) else e.optString("time")

fun String.inputNumber():String = map { c -> when(c){in '۰'..'۹'->'0'+(c-'۰');in '٠'..'٩'->'0'+(c-'٠');'٫'->'.';else->c} }.joinToString("")
