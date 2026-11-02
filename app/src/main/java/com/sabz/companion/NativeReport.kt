package com.sabz.companion
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.text.*
import org.json.JSONObject
import java.time.*
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale

/** PDF generated using Android's text shaping and Canvas; no web renderer. */
object NativeReport {
 fun create(context:Context,state:JSONObject,start:LocalDate,end:LocalDate):ByteArray{
  val settings=state.getJSONObject("settings");val fa=settings.optString("lang")=="fa";val persian=settings.optString("calendar")=="persian"
  fun t(a:String,b:String)=if(fa)a else b
  fun n(v:Double)=NumberFormat.getNumberInstance(Locale(if(fa)"fa" else "en")).apply{maximumFractionDigits=1}.format(v)
  val paint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(27,45,50);typeface=context.resources.getFont(R.font.vazirmatn);textSize=12f}
  val pdf=PdfDocument();var page:PdfDocument.Page?=null;var y=40f;var pageNo=0
  fun newPage(){page?.let{pdf.finishPage(it)};page=pdf.startPage(PdfDocument.PageInfo.Builder(595,842,++pageNo).create());y=40f}
  fun line(text:String,size:Float=12f){
   paint.textSize=size
   val layout=StaticLayout.Builder.obtain(text,0,text.length,paint,515).setTextDirection(if(fa)TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR).setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(true).build()
   if(page==null||y+layout.height>802)newPage()
   val canvas=page!!.canvas;canvas.save();canvas.translate(40f,y);layout.draw(canvas);canvas.restore();y+=layout.height+8
  }
  var date=start
  try{
   while(date<=end){
    newPage();line(t("سیب سلامت — گزارش روزانه","Green Apple — daily report"),21f)
    line(CalendarTools.label(date,persian,fa),16f);line(date.toString())
    val day=Domain.day(state,date.toString());val totals=Domain.totals(day);val goals=day.getJSONObject("targets")
    line(if(day.optBoolean("complete"))t("روز کامل","Complete day") else t("اطلاعات ناقص / در حال ثبت","Incomplete / in progress"))
    val metrics=listOf(Triple("kcal",t("کالری","Calories"),"kcal"),Triple("protein",t("پروتئین","Protein"),t("گرم","g")),Triple("fiber",t("فیبر","Fiber"),t("گرم","g")),Triple("water",t("آب ساده","Plain water"),"ml"),Triple("fluids",t("همهٔ نوشیدنی‌ها","All drinks"),"ml"),Triple("sitting",t("نشستن","Sitting"),t("دقیقه","min")),Triple("longest",t("طولانی‌ترین نشستن","Longest sitting"),t("دقیقه","min")),Triple("activity",t("فعالیت","Activity"),t("دقیقه","min")),Triple("breaks",t("وقفه‌ها","Breaks"),""),Triple("kegel",t("کگل","Kegel"),t("نوبت","sessions")))
    for((key,label,unit)in metrics)line("$label: ${n(totals.getValue(key))} $unit"+if(goals.has(key))" · "+t("هدف: ","Target: ")+n(goals.number(key)) else if(key=="kegel")" · "+t("هدف: ۳","Target: 3") else "")
    line(t("امتیاز عادت‌ها: ","Habit score: ")+(Domain.score(day)?.let{n(it.toDouble())+"/100"}?:"—"))
    if(totals.getValue("unknown")>0)line(t("برخی ارزش‌های غذایی نامشخص‌اند؛ جمع‌ها ناقص هستند.","Some nutrition is unknown; totals are incomplete."))
    if(day.objects("entries").isEmpty())line(t("چیزی ثبت نشده است.","Nothing recorded."))
    for(e in day.objects("entries")){
     line("\u2066"+intervalLabel(e)+"\u2069",16f)
     e.objects("items").forEach{i->val f=i.getJSONObject("food");line(f.getString(if(fa)"fa" else "en")+" · "+n(i.number("amount"))+" "+if(f.optBoolean("drink"))"ml" else t("گرم","g"))}
     if(e.optBoolean("noFood"))line(t("چیزی نخوردم","No food"))
     if(e.optBoolean("noDrink"))line(t("چیزی ننوشیدم","No drinks"))
     listOf("sitting","activity","breaks","kegel").forEach{k->if(e.recorded(k)){val m=metrics.first{it.first==k};line(m.second+": "+n(e.number(k))+" "+m.third)}}
     e.optJSONObject("activities")?.let{a->a.keys().forEach{k->val label=when(k){"walk"->t("پیاده‌روی","Walking");"exercise"->t("ورزش متوسط","Moderate exercise");"stretch"->t("کشش و حرکت","Stretch & move");else->t("سایر فعالیت‌ها","Other activity")};line(label+": "+n(a.number(k))+" "+t("دقیقه","min"))}}
    }
    line(t("مقادیر غذایی تقریبی‌اند؛ این گزارش تشخیص پزشکی نیست.","Nutrition is approximate; this is not a medical assessment."),10f)
    date=date.plusDays(1)
   }
   page?.let{pdf.finishPage(it)};page=null
   return ByteArrayOutputStream().use{pdf.writeTo(it);it.toByteArray()}
  } finally {pdf.close()}
 }
}
