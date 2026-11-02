package com.sabz.companion
import android.app.*
import android.content.*
import android.os.Build
import org.json.JSONObject
import java.time.*

class ReminderReceiver: BroadcastReceiver() {
 override fun onReceive(c:Context,i:Intent) {
  if(i.action=="com.sabz.companion.REMIND"&&c.getSharedPreferences("prefs",0).getBoolean("enabled",false)){
   if(i.getBooleanExtra("review",false))notifyReview(c,i.getStringExtra("date")?:LocalDate.now().toString(),false)
   else notifyUser(c,i.getBooleanExtra("check",false),i.getBooleanExtra("move",false))
  }
  schedule(c)
 }
 companion object {
  const val CHANNEL="sabz_reminders"
  fun createChannel(c:Context){c.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"یادآوری سیب سلامت / Green Apple",NotificationManager.IMPORTANCE_DEFAULT))}
  private fun alarm(c:Context,n:ReminderSchedule.Next?=null):PendingIntent {
   val i=Intent(c,ReminderReceiver::class.java).setAction("com.sabz.companion.REMIND").putExtra("check",n?.check?:false).putExtra("move",n?.move?:false).putExtra("review",n?.review?:false).putExtra("date",n?.date?:"")
   return PendingIntent.getBroadcast(c,1,i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  }
  fun schedule(c:Context){
   val am=c.getSystemService(AlarmManager::class.java);am.cancel(alarm(c))
   val p=c.getSharedPreferences("prefs",0);if(!p.getBoolean("enabled",false))return
   val n=ReminderSchedule.next(System.currentTimeMillis(),ZoneId.systemDefault(),p.getInt("wake",8),p.getInt("sleep",22))?:return
   val pi=alarm(c,n)
   try{if(Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,n.millis,pi) else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,n.millis,pi)}catch(_:SecurityException){am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,n.millis,pi)}
  }
  private fun post(c:Context,title:String,body:String,date:String?){
   createChannel(c);val i=Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
   if(date==null)i.putExtra("quick",true) else i.putExtra("reviewDate",date)
   val pi=PendingIntent.getActivity(c,if(date==null)2 else 3,i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
   val n=Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body).setStyle(Notification.BigTextStyle().bigText(body)).setContentIntent(pi).setAutoCancel(true).build()
   try{c.getSystemService(NotificationManager::class.java).notify(if(date==null)7 else 8,n)}catch(_:SecurityException){}
  }
  fun notifyUser(c:Context,check:Boolean,move:Boolean){
   val fa=c.getSharedPreferences("prefs",0).getBoolean("fa",true)
   post(c,if(fa)"یک قدم کوچک برای حال بهتر 🍏" else "A small step for a better day 🍏",
    (if(check)if(fa)"غذا، نوشیدنی، نشستن و فعالیتت را ثبت کن. " else "Log food, drinks, sitting and activity. " else "")+
    (if(move)if(fa)"بلند شو، کمی حرکت کن و یک لیوان آب بنوش." else "Stand up, move a little and have a glass of water." else ""),null)
  }
  fun notifyReview(c:Context,date:String,preview:Boolean){
   val p=c.getSharedPreferences("prefs",0);if(!preview&&date==p.getString("lastReviewDate",""))return
   val fa=p.getBoolean("fa",true);val state=runCatching{JSONObject(c.getSharedPreferences("data",0).getString("state","")!!)}.getOrElse{Domain.blank()}
   val d=runCatching{Domain.day(state,date)}.getOrElse{Domain.day(Domain.blank(),date)}
   post(c,if(fa)"جمع‌بندی روز 🍏" else "Your daily review 🍏",NightReview.message(d,fa),date)
   if(!preview)p.edit().putString("lastReviewDate",date).apply()
  }
 }
}
