package com.sabz.companion
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import android.icu.util.Calendar
import java.time.*
import org.json.JSONObject

@Composable fun CalendarGrid(s:Store,selected:LocalDate,onSelect:(LocalDate)->Unit){
 val persian=s.settings.getString("calendar")=="persian"
 var anchor by rememberSaveable(persian){mutableStateOf(CalendarTools.start(selected,persian).toString())}
 var jump by rememberSaveable{mutableStateOf(false)}
 val first=CalendarTools.start(LocalDate.parse(anchor),persian)
 val length=CalendarTools.calendar(first,persian).getActualMaximum(Calendar.DAY_OF_MONTH)
 // Saturday first in Persian; Monday first in Gregorian.
 val offset=if(persian)(first.dayOfWeek.value+1)%7 else first.dayOfWeek.value-1
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   TextButton(onClick={anchor=CalendarTools.shiftMonth(first,persian,-1).toString()}){Text("‹")}
   TextButton(onClick={jump=true}){Text(CalendarTools.label(first,persian,s.fa,"MMMM yyyy"))}
   TextButton(onClick={anchor=CalendarTools.shiftMonth(first,persian,1).toString()}){Text("›")}
  }
  val weekdays=if(persian)if(s.fa)listOf("ش","ی","د","س","چ","پ","ج") else listOf("Sa","Su","Mo","Tu","We","Th","Fr") else if(s.fa)listOf("د","س","چ","پ","ج","ش","ی") else listOf("Mo","Tu","We","Th","Fr","Sa","Su")
  Row(Modifier.fillMaxWidth()){weekdays.forEach{Text(it,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center,style=MaterialTheme.typography.labelSmall)}}
  (0 until ((offset+length+6)/7)).forEach{row->
   Row(Modifier.fillMaxWidth()){
    (0..6).forEach{col->val day=row*7+col-offset+1
     if(day !in 1..length)Spacer(Modifier.weight(1f).height(48.dp)) else{
      val date=first.plusDays((day-1).toLong());val d=s.day(date);val recorded=d.objects("entries").isNotEmpty()
      Surface(Modifier.weight(1f).heightIn(min=48.dp).padding(1.dp).clickable{onSelect(date)},shape=RoundedCornerShape(10.dp),color=if(date==selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=if(recorded).8f else .2f),contentColor=if(date==selected)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface){
       Column(Modifier.padding(vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(s.num(day));Text(if(d.optBoolean("complete"))"✓" else if(recorded)"•" else " ",fontSize=9.sp)}
      }
     }
    }
   }
  }
  TextButton(onClick={anchor=CalendarTools.start(LocalDate.now(),persian).toString();onSelect(LocalDate.now())}){Text(s.t("امروز","Today"))}
 }
 if(jump){
  val c=CalendarTools.calendar(first,persian)
  var year by rememberSaveable{mutableStateOf(c.get(Calendar.YEAR).toString())};var month by rememberSaveable{mutableStateOf((c.get(Calendar.MONTH)+1).toString())};var invalid by remember{mutableStateOf(false)}
  AlertDialog(onDismissRequest={jump=false},title={Text(s.t("رفتن به ماه","Go to month"))},text={Column{OutlinedTextField(year,{year=it},label={Text(s.t("سال","Year"))},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number));OutlinedTextField(month,{month=it},label={Text(s.t("ماه (۱ تا ۱۲)","Month (1–12)"))},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number));if(invalid)Text(s.t("سال و ماه معتبر وارد کن.","Enter a valid year and month."))}},confirmButton={TextButton(onClick={runCatching{val y=year.inputNumber().toInt();require(y in 1..9999);anchor=CalendarTools.fromParts(y,month.inputNumber().toInt(),1,persian).toString();jump=false}.onFailure{invalid=true}}){Text(s.t("برو","Go"))}},dismissButton={TextButton(onClick={jump=false}){Text(s.t("انصراف","Cancel"))}})
 }
}
@Composable fun DateDialog(s:Store,selected:LocalDate,onDismiss:()->Unit,onSelect:(LocalDate)->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text(s.t("انتخاب تاریخ","Choose date"))},text={CalendarGrid(s,selected,onSelect)},confirmButton={TextButton(onClick=onDismiss){Text(s.t("بستن","Close"))}})}
@Composable fun HistoryScreen(s:Store,n:NavState){
 Panel{CalendarGrid(s,LocalDate.parse(n.date)){n.date=it.toString();n.go("details")};Hint(s.t("• دارای ثبت   ✓ روز کامل","• Recorded   ✓ Complete day"))}
 Heading(s.t("ثبت‌های اخیر","Recent records"))
 s.root.getJSONObject("days").keys().asSequence().toList().sortedDescending().take(60).forEach{key->val d=Domain.day(s.root,key);if(d.objects("entries").isNotEmpty())OutlinedButton(onClick={n.date=key;n.go("details")},modifier=Modifier.fillMaxWidth()){Text(s.dateLabel(LocalDate.parse(key))+" · "+s.num(d.objects("entries").size)+" "+s.t("نوبت","intervals"))}}
}
@Composable fun DetailsScreen(s:Store,n:NavState,act:(()->Unit)->Unit){
 val date=LocalDate.parse(n.date);val d=s.day(date);val v=Domain.totals(d)
 Hint(if(d.optBoolean("complete"))s.t("✓ روز کامل","✓ Complete day") else s.t("در حال ثبت","In progress"))
 Nutrition(s,d)
 Panel{Heading(s.t("جمع روزانه","Daily totals"));Text(s.t("آب ساده: ","Plain water: ")+s.num(v.getValue("water"))+" ml");Text(s.t("همه نوشیدنی‌ها: ","All drinks: ")+s.num(v.getValue("fluids"))+" ml");Text(s.t("نشستن: ","Sitting: ")+s.duration(v.getValue("sitting")));Text(s.t("طولانی‌ترین نوبت: ","Longest interval: ")+s.duration(v.getValue("longest")));Text(s.t("فعالیت: ","Activity: ")+s.num(v.getValue("activity"))+" "+s.t("دقیقه","min"));Text(s.t("وقفه‌ها: ","Breaks: ")+s.num(v.getValue("breaks")));Text(s.t("کگل: ","Kegel: ")+s.num(v.getValue("kegel"))+" / "+s.num(3));Hint(s.t("هدف‌ها از زمان اولین ثبت این روز حفظ شده‌اند.","Goals are preserved from this day's first entry."))}
 if(d.objects("entries").isEmpty())Panel{Text(s.t("🍏 هنوز چیزی ثبت نشده","🍏 Nothing recorded yet"))}
 var deleteId by rememberSaveable{mutableStateOf<String?>(null)}
 d.objects("entries").forEach{e->Panel{
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Heading(intervalLabel(e));TextButton(onClick={n.edit=e.getString("id");n.page="quick"}){Text(s.t("ویرایش","Edit"))}}
  e.objects("items").forEach{i->val f=i.getJSONObject("food");Text(f.optString("emoji")+" "+s.foodName(f)+" · "+s.num(i.number("amount"),1)+" "+if(f.optBoolean("drink"))"ml" else s.t("گرم","g"))}
  if(e.optBoolean("noFood"))Hint(s.t("چیزی نخوردم","No food"))
  if(e.optBoolean("noDrink"))Hint(s.t("چیزی ننوشیدم","No drinks"))
  Text("🪑 "+if(e.recorded("sitting"))s.duration(e.number("sitting")) else "—")
  Text("👟 "+if(e.recorded("activity"))s.num(e.number("activity"))+" "+s.t("دقیقه","min") else "—")
  e.optJSONObject("activities")?.let{a->a.keys().forEach{Hint(activityName(s,it)+": "+s.num(a.number(it))+" "+s.t("دقیقه","min"))}}
  if(e.has("kegel"))Hint(s.t("کگل: ","Kegel: ")+s.num(e.number("kegel"))+" "+s.t("نوبت","sessions"))
  TextButton(onClick={deleteId=e.getString("id")}){Text(s.t("حذف نوبت","Delete interval"),color=MaterialTheme.colorScheme.error)}
 }}
 Button(onClick={n.edit=null;n.go("quick")},modifier=Modifier.fillMaxWidth()){Text(s.t("ثبت اطلاعات","Log data"))}
 OutlinedButton(onClick={if(d.optBoolean("complete"))act{s.complete(date)} else n.dialog="complete"},modifier=Modifier.fillMaxWidth()){Text(if(d.optBoolean("complete"))s.t("باز کردن روز برای ثبت","Reopen day") else s.t("همهٔ اطلاعات روز را ثبت کرده‌ام","I have recorded the whole day"))}
 Domain.score(d)?.let{Heading(s.t("امتیاز روز: ","Day score: ")+s.num(it)+"٪")}
 TextButton(onClick={n.dialog="report"}){Text(s.t("گزارش PDF","PDF report"))}
 if(deleteId!=null)AlertDialog(onDismissRequest={deleteId=null},title={Text(s.t("این نوبت حذف شود؟","Delete this interval?"))},confirmButton={TextButton(onClick={act{s.delete(date,deleteId!!);deleteId=null}}){Text(s.t("حذف","Delete"))}},dismissButton={TextButton(onClick={deleteId=null}){Text(s.t("انصراف","Cancel"))}})
}
@Composable fun TrendScreen(s:Store,n:NavState){
 val end=LocalDate.parse(n.date);val days=Domain.week(s.root,end);val scores=days.mapNotNull{it.second};val average=if(scores.isEmpty())null else scores.average().toInt()
 Panel{Heading(s.t("همراهی هفت‌روزه","Seven-day habits"));Row(verticalAlignment=Alignment.CenterVertically){Apple(s,average);Column{Text(average?.let{s.num(it)+"٪"}?:"—",fontSize=38.sp);Hint(s.num(scores.size)+" "+s.t("روز با اطلاعات کامل","days with complete data"))}};Hint(s.t("روزهای ناقص یا با ارزش غذایی نامشخص در میانگین نیستند.","Incomplete days and days with unknown nutrition are excluded."))}
 days.forEach{(date,score)->Panel{
  TextButton(onClick={n.date=date.toString();n.go("details")}){Text(s.dateLabel(date))}
  if(score!=null){LinearProgressIndicator(progress={score/100f},modifier=Modifier.fillMaxWidth());Text(s.num(score)+"٪")}else Hint(s.t("اطلاعات کافی نیست","Not enough data"))
  val v=Domain.totals(s.day(date));Hint(s.t("آب: ","Water: ")+s.num(v.getValue("water"))+" ml · "+s.t("فعالیت: ","Activity: ")+s.num(v.getValue("activity"))+" "+s.t("دقیقه","min"))
 }}
 TextButton(onClick={n.dialog="report"}){Text(s.t("گزارش PDF","PDF report"))}
}
@Composable fun ReportDialog(s:Store,host:MainActivity,date:LocalDate,onDismiss:()->Unit){
 var start by rememberSaveable{mutableStateOf(date.minusDays(6).toString())};var end by rememberSaveable{mutableStateOf(date.toString())};var choose by rememberSaveable{mutableStateOf<String?>(null)}
 AlertDialog(onDismissRequest=onDismiss,title={Text(s.t("گزارش PDF","PDF report"))},text={Column{TextButton(onClick={choose="start"}){Text(s.t("از: ","From: ")+s.dateLabel(LocalDate.parse(start)))};TextButton(onClick={choose="end"}){Text(s.t("تا: ","To: ")+s.dateLabel(LocalDate.parse(end)))};Hint(s.t("بازهٔ ۱ تا ۳۱ روز؛ اطلاعات هر روز در صفحهٔ جدا.","Choose 1–31 days; each day begins on its own page."))}},confirmButton={TextButton(onClick={val a=LocalDate.parse(start);val b=LocalDate.parse(end);if(b>=a&&java.time.temporal.ChronoUnit.DAYS.between(a,b)<=30){host.report(a,b);onDismiss()}else host.toast(s.t("بازه باید ۱ تا ۳۱ روز باشد.","Choose 1–31 days."))}){Text(s.t("ذخیره PDF","Save PDF"))}},dismissButton={TextButton(onClick=onDismiss){Text(s.t("انصراف","Cancel"))}})
 choose?.let{key->DateDialog(s,LocalDate.parse(if(key=="start")start else end),{choose=null}){if(key=="start")start=it.toString() else end=it.toString();choose=null}}
}
