package com.sabz.companion
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
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
import org.json.JSONObject
import java.time.LocalDate

@Composable fun ChoiceGroup(title:String,value:String,options:List<Pair<String,String>>,onChange:(String)->Unit){
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Heading(title);options.chunked(2).forEach{pair->Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){pair.forEach{(id,label)->
  FilterChip(selected=value==id,onClick={onChange(id)},label={Text(label)},modifier=Modifier.weight(1f).heightIn(min=56.dp))
 };if(pair.size==1)Spacer(Modifier.weight(1f))}}}
}
@Composable fun SettingsScreen(s:Store,host:MainActivity,act:(()->Unit)->Unit){
 Panel{
  Heading(s.t("ظاهر، زبان و تاریخ","Appearance, language & calendar"))
  ChoiceGroup(s.t("تم برنامه","App theme"),s.settings.optString("theme","light"),listOf("light" to s.t("☀ روشن","☀ Light"),"dark" to s.t("☾ ذغالی و آبی","☾ Charcoal & blue"))){act{s.setSetting("theme",it)}}
  ChoiceGroup(s.t("زبان","Language"),s.settings.getString("lang"),listOf("fa" to "🇮🇷 فارسی","en" to "🇬🇧 English")){act{s.setSetting("lang",it)}}
  ChoiceGroup(s.t("تقویم","Calendar"),s.settings.getString("calendar"),listOf("persian" to s.t("هجری شمسی","Solar Hijri"),"gregory" to s.t("میلادی","Gregorian"))){act{s.setSetting("calendar",it)}}
 }
 val labels=listOf(Triple("weight",s.t("وزن (کیلوگرم)","Weight (kg)"),30.0..300.0),Triple("kcal",s.t("کالری (kcal)","Calories (kcal)"),800.0..6000.0),Triple("protein",s.t("پروتئین (گرم)","Protein (g)"),20.0..300.0),Triple("fiber",s.t("فیبر (گرم)","Fiber (g)"),10.0..60.0),Triple("water",s.t("آب ساده (میلی‌لیتر)","Plain water (ml)"),200.0..5000.0),Triple("activity",s.t("فعالیت (دقیقه)","Activity (min)"),1.0..300.0),Triple("breakLimit",s.t("حد نشستن پیوسته (دقیقه)","Sitting interval goal (min)"),15.0..120.0))
 val values=remember{mutableStateMapOf<String,String>().apply{labels.forEach{(k,_,_)->put(k,s.cached("settings:s-$k",s.settings.number(k).toString()))};listOf("wake","sleep").forEach{put(it,s.cached("settings:s-$it",s.settings.optInt(it).toString()))}}}
 var confirmed by rememberSaveable{mutableStateOf(s.settings.optBoolean("confirmed"))}
 Panel{
  Heading(s.t("هدف‌های روزانه","Daily goals"));Hint(s.t("این اعداد مرجع عمومی‌اند؛ هدف مناسب خودت را انتخاب کن. تغییر هدف برای روزهای جدید است.","These are general starting references. Choose goals that suit you. Changes apply to newly recorded days."))
  labels.forEach{(k,label,_)->OutlinedTextField(values[k].orEmpty(),{v->values[k]=v;act{s.cache("settings:s-$k",v)}},label={Text(label)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true,modifier=Modifier.fillMaxWidth())}
  TextButton(onClick={val w=values["weight"]?.inputNumber()?.toDoubleOrNull();if(w!=null){values["protein"]=kotlin.math.round(w*.8).toInt().toString();act{s.cache("settings:s-protein",values["protein"]!!)}}}){Text(s.t("مرجع پروتئین: ۰٫۸ × وزن","Protein reference: 0.8 × weight"))}
  Hint(s.t("۲۰۰۰ کالری مرجع برچسب غذایی است. مرجع فیبر بزرگسالان ۲۲ تا ۳۴ گرم؛ افزایش تدریجی باشد. آب ۱۶۰۰ میلی‌لیتر و حد نشستن ۶۰ دقیقه پیش‌فرض برنامه‌اند، نه نیاز همگانی. هدف فعالیت ۲۲ دقیقه، تقریب ۱۵۰ دقیقه در هفته است.","2,000 kcal is a food-label reference. Adult fiber reference: 22–34 g; increase gradually. 1,600 ml water and 60-minute sitting intervals are app defaults, not universal requirements. 22 activity minutes approximates 150 minutes weekly."))
  Row(verticalAlignment=Alignment.CenterVertically){Checkbox(confirmed,{confirmed=it});Text(s.t("هدف‌ها را بررسی کرده‌ام و مناسب من هستند.","I have reviewed these goals and they suit me."))}
  Button(onClick={act{
   require(confirmed){s.t("هدف‌ها را تأیید کن.","Confirm the goals.")}
   val parsed=labels.associate{(k,_,range)->val v=values[k]?.inputNumber()?.toDoubleOrNull();require(v!=null&&v.isFinite()&&v in range){s.t("مقدار هدف معتبر نیست.","Invalid goal value.")};k to v}
   s.update{r->val settings=r.getJSONObject("settings");parsed.forEach{(k,v)->settings.put(k,v)};settings.put("confirmed",true);val forms=r.optJSONObject("ui")?.optJSONObject("forms");parsed.keys.forEach{forms?.remove("settings:s-$it")}}
   host.toast(s.t("هدف‌ها ذخیره شدند.","Goals saved."))
  }},modifier=Modifier.fillMaxWidth()){Text(s.t("ذخیره و تأیید هدف‌ها","Save & confirm goals"))}
 }
 Panel{
  Heading(s.t("🔔 یادآوری‌ها","🔔 Reminders"))
  Row(verticalAlignment=Alignment.CenterVertically){Switch(s.settings.optBoolean("reminders"),{b->act{s.setSetting("reminders",b);host.configureReminders()}});Text(s.t("فعال‌کردن اعلان‌ها","Enable reminders"),Modifier.padding(start=10.dp))}
  Hint(s.t("ثبت اطلاعات: ۹، ۱۲، ۱۵ و ۱۸. ساعت ۲۲: جمع‌بندی روز. حرکت و آب: هر دو ساعت در بازهٔ بیداری.","Check-ins: 09:00, 12:00, 15:00, 18:00. Daily review: 22:00. Movement and water: every 2 hours during waking hours."))
  Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){listOf("wake","sleep").forEach{k->OutlinedTextField(values[k].orEmpty(),{v->values[k]=v;act{s.cache("settings:s-$k",v)}},label={Text(if(k=="wake")s.t("شروع بیداری","Wake hour") else s.t("پایان بیداری","End hour"))},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.weight(1f))}}
  OutlinedButton(onClick={act{val w=values["wake"]?.inputNumber()?.toIntOrNull();val e=values["sleep"]?.inputNumber()?.toIntOrNull();require(w!=null&&e!=null&&w in 0..22&&e in 1..23&&w<e){s.t("ساعت‌ها معتبر نیستند.","Invalid hours.")};s.update{it.getJSONObject("settings").put("wake",w).put("sleep",e)};s.configure();host.toast(s.t("ساعت‌ها ذخیره شدند.","Hours saved."))}}){Text(s.t("ذخیرهٔ ساعت‌ها","Save hours"))}
  Hint(s.t("اندروید ممکن است اعلان را به‌علت تنظیمات باتری یا مجوز هشدار دقیق با تأخیر نشان دهد.","Android may delay reminders due to battery settings or exact-alarm permissions."))
  TextButton(onClick={host.requestNotifications()}){Text(s.t("مجوز اعلان و زمان‌بندی دقیق","Notification & exact-alarm access"))}
  TextButton(onClick={if(host.getSystemService(NotificationManager::class.java).areNotificationsEnabled())ReminderReceiver.notifyUser(host,true,true) else host.requestNotifications()}){Text(s.t("آزمایش اعلان","Test notification"))}
  TextButton(onClick={if(host.getSystemService(NotificationManager::class.java).areNotificationsEnabled())ReminderReceiver.notifyReview(host,LocalDate.now().toString(),true) else host.requestNotifications()}){Text(s.t("آزمایش جمع‌بندی شبانه","Test nightly review"))}
 }
 Panel{Heading(s.t("اطلاعات من","My data"));Hint(s.t("اطلاعات روی همین گوشی ذخیره می‌شود. پیش از حذف برنامه یا تعویض گوشی پشتیبان بگیر.","Data stays on this phone. Back up before uninstalling or changing phones."));Button(onClick={host.backup()}){Text(s.t("ذخیرهٔ پشتیبان","Export backup"))};OutlinedButton(onClick={host.importBackup()}){Text(s.t("بازیابی پشتیبان","Restore backup"))}}
 Panel{
  Heading(s.t("منابع و توضیحات","Sources & notes"));Hint(s.t("بررسی منابع: ۲۲ اکتبر ۲۰۲۶. بانک خوراکی‌ها: USDA SR Legacy ۲۰۱۸. سیب سلامت برای پیگیری عمومی تغذیه، آب و فعالیت است؛ مقادیر غذایی تقریبی‌اند.","Sources reviewed October 22, 2026. Food data: USDA SR Legacy 2018. Green Apple tracks general nutrition, water and activity; nutrient values are approximate."))
  listOf("NIDDK · Fiber & fluids" to "https://www.niddk.nih.gov/health-information/digestive-diseases/constipation/eating-diet-nutrition","FDA · Calorie reference" to "https://www.fda.gov/food/nutrition-facts-label/calories-nutrition-facts-label","National Academies · Protein DRI" to "https://www.ncbi.nlm.nih.gov/books/NBK235261/","WHO · Physical activity" to "https://www.who.int/news-room/fact-sheets/detail/physical-activity","USDA · FoodData Central" to "https://fdc.nal.usda.gov/download-datasets/").forEach{(title,url)->TextButton(onClick={act{host.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}}){Text(title+" ↗")}}
  Hint(s.t("سیب سلامت","Green Apple")+" 2.0.2 · Android 11+\nKotlin · Jetpack Compose · Vazirmatn / SIL OFL")
 }
}
@Composable fun CustomDialog(s:Store,onDismiss:()->Unit,onSave:(JSONObject)->Unit){
 val vals=remember{mutableStateMapOf<String,String>().apply{listOf("fa","en","base","kcal","protein","fiber").forEach{put(it,s.cached("native-custom:$it",if(it=="base")"100" else ""))}}}
 var category by rememberSaveable{mutableStateOf(s.cached("native-custom:cat","meal"))}
 var drink by rememberSaveable{mutableStateOf(s.cached("native-custom:drink")=="true")}
 var error by rememberSaveable{mutableStateOf("")}
 AlertDialog(onDismissRequest=onDismiss,title={Text(s.t("خوراکی یا غذای خانگی","Food or home recipe"))},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
  listOf("fa" to s.t("نام فارسی","Persian name"),"en" to s.t("نام انگلیسی","English name"),"base" to s.t("سهم پایه (گرم)","Base portion (g)"),"kcal" to s.t("کالری در ۱۰۰ گرم/میلی‌لیتر","Calories per 100 g/ml"),"protein" to s.t("پروتئین در ۱۰۰ گرم/میلی‌لیتر","Protein per 100 g/ml"),"fiber" to s.t("فیبر در ۱۰۰ گرم/میلی‌لیتر","Fiber per 100 g/ml")).forEach{(k,label)->OutlinedTextField(vals[k].orEmpty(),{v->vals[k]=v;runCatching{s.cache("native-custom:$k",v)}},label={Text(label)},keyboardOptions=KeyboardOptions(keyboardType=if(k in listOf("fa","en"))KeyboardType.Text else KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())}
  Hint(s.t("اگر ارزش غذایی مشخص نیست، آن را خالی بگذار.","Leave unknown nutrient values empty."))
  Row(verticalAlignment=Alignment.CenterVertically){Checkbox(drink,{drink=it;runCatching{s.cache("native-custom:drink",it.toString())}});Text(s.t("نوشیدنی","Drink"))}
  ChoiceGroup(s.t("دسته‌بندی","Category"),category,s.categories.map{it.getString("id") to it.getString("emoji")+" "+s.foodName(it)}){category=it;runCatching{s.cache("native-custom:cat",it)}}
  if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
 }},confirmButton={TextButton(onClick={
  runCatching{
   require(vals["fa"].orEmpty().isNotBlank()||vals["en"].orEmpty().isNotBlank())
   val f=JSONObject().put("id","new").put("fa",vals["fa"].orEmpty().ifBlank{vals["en"]!!}).put("en",vals["en"].orEmpty().ifBlank{vals["fa"]!!}).put("cat",category).put("emoji",s.categories.first{it.getString("id")==category}.getString("emoji")).put("base",if(drink)200.0 else vals["base"]!!.inputNumber().toDouble()).put("drink",drink).put("water",false).put("source","User-provided label / recipe").put("approx",false)
   listOf("kcal","protein","fiber").forEach{k->f.put(k,if(vals[k].isNullOrBlank())JSONObject.NULL else vals[k]!!.inputNumber().toDouble())}
   Domain.validateFood(f);onSave(f)
  }.onFailure{error=s.t("نام، سهم و مقادیر غذایی معتبر وارد کن.","Enter a name, valid portion and nutrient values.")}
 }){Text(s.t("افزودن خوراکی","Add food"))}},dismissButton={TextButton(onClick=onDismiss){Text(s.t("بستن","Close"))}})
}
