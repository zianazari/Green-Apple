package com.sabz.companion
import java.time.*
object ReminderSchedule {
 data class Next(val millis:Long,val check:Boolean,val move:Boolean,val review:Boolean,val date:String)
 fun next(now:Long,zone:ZoneId,wake:Int,sleep:Int):Next? {
  val date=Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
  return (0..1).flatMap{day->(0..23).mapNotNull{h->
   val check=h in listOf(9,12,15,18);val review=h==22;val move=h>=wake&&h<sleep&&(h-wake)%2==0
   val candidate=date.plusDays(day.toLong());val millis=ZonedDateTime.of(candidate,LocalTime.of(h,0),zone).toInstant().toEpochMilli()
   if((check||review||move)&&millis>now)Next(millis,check,move,review,candidate.toString()) else null
  }}.minByOrNull{it.millis}
 }
}
object NightReview {
 fun message(day:org.json.JSONObject,fa:Boolean):String {
  fun t(a:String,b:String)=if(fa)a else b
  if(!day.optBoolean("complete"))return t("اطلاعات امروز کامل نشده است. ثبت‌ها را مرور کن و پس از تکمیل، روز را کامل اعلام کن.","Today's record is incomplete. Review your entries and mark the day complete after logging everything.")
  val score=Domain.score(day)?:return t("بخشی از ارزش غذایی نامشخص است؛ اطلاعات خوراکی سفارشی را کامل کن.","Some nutrition is unknown. Complete the custom food's nutrition information.")
  val v=Domain.totals(day);val g=day.getJSONObject("targets");val tips=mutableListOf<String>()
  if(v.getValue("water")<g.getDouble("water")*.9)tips+=t("فردا آب را در طول روز منظم‌تر بنوش تا به هدف انتخابی نزدیک شوی.","Spread water through the day toward your chosen target.")
  if(v.getValue("activity")<g.getDouble("activity")*.9)tips+=t("فردا زمانی برای پیاده‌روی یا فعالیت مناسب خودت بگذار.","Plan some walking or activity that suits you tomorrow.")
  if(v.getValue("longest")>g.getDouble("breakLimit"))tips+=t("بین نوبت‌های نشستن زودتر بلند شو و حرکت کن.","Break up long sitting periods with movement.")
  if(v.getValue("fiber")<g.getDouble("fiber")*.9)tips+=t("خوراکی‌های فیبردار انتخاب کن؛ افزایش فیبر تدریجی باشد.","Choose fiber-rich foods; increase fiber gradually.")
  if(kotlin.math.abs(v.getValue("kcal")-g.getDouble("kcal"))>g.getDouble("kcal")*.1)tips+=t("وعده‌ها را با هدف کالری انتخابی هماهنگ‌تر کن.","Align meals more closely with your chosen calorie target.")
  if(v.getValue("protein")<g.getDouble("protein")*.9)tips+=t("منابع پروتئین متناسب با هدفت انتخاب کن.","Include protein sources that fit your target.")
  if(tips.isEmpty())tips+=t("همین عادت‌های منظم را ادامه بده.","Keep up these consistent habits.")
  return (if(score>=75)t("امروز خوب پیش رفتی 🍏","Good progress today 🍏") else t("فردا یک قدم بهتر 🍏","One step better tomorrow 🍏"))+" · "+t("امتیاز عادت‌ها: ","Habit score: ")+"$score/100. "+tips.take(2).joinToString(" ")
 }
}
