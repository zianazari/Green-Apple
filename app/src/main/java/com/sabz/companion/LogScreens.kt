package com.sabz.companion
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import org.json.*
import java.time.LocalDate
import kotlin.math.*

@Composable fun Stepper(label:String,value:String,minus:()->Unit,plus:()->Unit,enabledMinus:Boolean=true){
 Surface(shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surfaceVariant){
  Row(Modifier.fillMaxWidth().padding(4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
   FilledTonalButton(onClick=minus,enabled=enabledMinus,contentPadding=PaddingValues(0.dp),modifier=Modifier.size(48.dp).semantics{contentDescription="$label −"},shape=RoundedCornerShape(11.dp)){Text("−",fontSize=22.sp)}
   Text(value,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
   FilledTonalButton(onClick=plus,contentPadding=PaddingValues(0.dp),modifier=Modifier.size(48.dp).semantics{contentDescription="$label +"},shape=RoundedCornerShape(11.dp)){Text("+",fontSize=22.sp)}
  }
 }
}

@Composable fun FoodRow(s:Store,n:NavState,f:JSONObject,act:(()->Unit)->Unit){
 val date=LocalDate.parse(n.date);val e=s.entry(date,n.edit,n.clock);val amount=e.objects("items").filter{it.getJSONObject("food").getString("id")==f.getString("id")}.sumOf{it.number("amount")}
 Surface(shape=RoundedCornerShape(16.dp),color=if(s.dark)androidx.compose.ui.graphics.Color(0xFF303747) else androidx.compose.ui.graphics.Color(0xFFF0F4EF)){
 Column(Modifier.fillMaxWidth().padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
  Row(Modifier.fillMaxWidth().clickable{n.foodId=f.getString("id");n.dialog="food"}.padding(vertical=3.dp),verticalAlignment=Alignment.CenterVertically){
   FoodArt(f.optString("emoji"),32);Spacer(Modifier.width(10.dp));Text(s.foodName(f),modifier=Modifier.weight(1f));Text("ⓘ",color=MaterialTheme.colorScheme.primary)
  }
  Stepper(s.foodName(f),if(f.optBoolean("drink"))s.num(amount/200,1)+" "+s.t("لیوان","glasses") else s.num(amount,1)+" "+s.t("گرم","g"),{act{s.food(date,n.edit,f,-1)}},{act{s.food(date,n.edit,f)}},amount>0)
 }
}
}
@Composable fun Frequent(s:Store,n:NavState,act:(()->Unit)->Unit){
 val foods=Domain.frequent(s.root,s.foods())
 Panel{Heading(s.t("⭐ پرکاربردهای من","⭐ My frequent foods"));Hint(s.t("بر اساس دفعات ثبت شما؛ مقدارها مربوط به نوبت جاری‌اند.","Based on log frequency; amounts are for this interval."));if(foods.isEmpty())Hint(s.t("با ثبت خوراکی‌ها، موارد پرکاربرد اینجا نمایش داده می‌شوند.","Your frequent foods will appear here as you log them."));foods.forEach{f->FoodRow(s,n,f,act)}}
}
val Activities=listOf(Triple("walk","🚶","پیاده‌روی"),Triple("exercise","🏋","ورزش متوسط"),Triple("stretch","🧘","کشش و حرکت"),Triple("other","⚡","سایر فعالیت‌ها"))
fun activityName(s:Store,id:String)=when(id){"walk"->s.t("پیاده‌روی","Walking");"exercise"->s.t("ورزش متوسط","Moderate exercise");"stretch"->s.t("کشش و حرکت","Stretch & move");else->s.t("سایر فعالیت‌ها","Other activity")}
@Composable fun QuickScreen(s:Store,n:NavState,act:(()->Unit)->Unit){
 val date=LocalDate.parse(n.date);val e=s.entry(date,n.edit,n.clock);val total=Domain.totals(s.day(date))
 Hint("✓ "+s.t("هر تغییر همان لحظه ذخیره می‌شود","Every change is saved immediately")+" · "+intervalLabel(e))
 if(n.edit!=null)TextButton(onClick={n.edit=null}){Text(s.t("پایان ویرایش؛ ثبت خودکار","Finish editing; automatic logging"))}
 Frequent(s,n,act)
 Panel{
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Heading(s.t("خوراکی و نوشیدنی","Food & drinks"));TextButton(onClick={n.category=null;n.search="";n.page="foods"}){Text(s.t("دسته‌بندی‌ها","Categories"))}}
  if(e.objects("items").isEmpty())Hint(s.t("خوراکی‌ها را از دسته‌بندی انتخاب کن.","Choose foods from the categories."))
  e.objects("items").forEach{FoodRow(s,n,it.getJSONObject("food"),act)}
  listOf("noFood" to s.t("در این نوبت چیزی نخوردم","No food this interval"),"noDrink" to s.t("در این نوبت چیزی ننوشیدم","No drinks this interval")).forEach{(key,label)->
   val disabled=e.objects("items").any{it.getJSONObject("food").optBoolean("drink")==(key=="noDrink")}
   Row(verticalAlignment=Alignment.CenterVertically){Checkbox(checked=e.optBoolean(key),enabled=!disabled,onCheckedChange={b->act{s.mutate(date,n.edit){it.put(key,b)}}});Text(label)}
  }
 }
 Panel{
  Heading(s.t("🪑 نشستن این نوبت","🪑 Sitting this interval"))
  val sitting=if(e.recorded("sitting"))e.getInt("sitting") else 60
  fun setSitting(value:Int){act{val next=value.coerceIn(0,1440);s.cache("sit:"+e.getString("id")+":hours",(next/60).toString());s.cache("sit:"+e.getString("id")+":minutes",(next%60).toString());s.mutate(date,n.edit){it.put("sitting",next)}}}
  Stepper(s.t("نشستن","Sitting"),s.duration(sitting.toDouble()),{setSitting(sitting-15)},{setSitting(sitting+15)},sitting>0)
  if(!e.recorded("sitting"))Hint(s.t("پیش‌فرض؛ با اولین ثبت ذخیره می‌شود.","Default; saved with the first log."))
  key(e.getString("id"),sitting){
   var hours by rememberSaveable{mutableStateOf(s.cached("sit:"+e.getString("id")+":hours",(sitting/60).toString()))}
   var minutes by rememberSaveable{mutableStateOf(s.cached("sit:"+e.getString("id")+":minutes",(sitting%60).toString()))}
   fun change(){
    act{s.cache("sit:"+e.getString("id")+":hours",hours);s.cache("sit:"+e.getString("id")+":minutes",minutes)}
    val h=hours.inputNumber().toIntOrNull();val m=minutes.inputNumber().toIntOrNull()
    if(h!=null&&m!=null&&h in 0..24&&m in 0..59&&h*60+m<=1440)act{s.mutate(date,n.edit){it.put("sitting",h*60+m)}}
   }
   Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
    OutlinedTextField(hours,{hours=it;change()},label={Text(s.t("ساعت","Hours"))},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.weight(1f),singleLine=true)
    OutlinedTextField(minutes,{minutes=it;change()},label={Text(s.t("دقیقه","Minutes"))},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.weight(1f),singleLine=true)
   }
  }
  Hint(s.t("مجموع روز: ","Day total: ")+s.duration(total.getValue("sitting"))+"\n"+s.t("طولانی‌ترین نوبت: ","Longest interval: ")+s.duration(total.getValue("longest")))
  Hint(s.t("نوبت‌ها خودکار سه‌ساعته‌اند. پیش‌فرض نشستن را متناسب با زمان واقعی تغییر بده. بازه‌های خالی ادغام می‌شوند.","Intervals advance every 3 hours. Adjust the sitting default to your actual time. Empty intervals merge forward."))
  TextButton(onClick={setSitting(0)}){Text(s.t("این نوبت ننشستم","No sitting this interval"))}
  Heading(s.t("وقفه‌های بلندشدن","Standing breaks"))
  Stepper(s.t("وقفه","Breaks"),if(e.recorded("breaks"))s.num(e.number("breaks")) else "—",{act{s.mutate(date,n.edit){it.put("breaks",max(0,it.number("breaks").toInt()-1))}}},{act{s.mutate(date,n.edit){it.put("breaks",min(1440,it.number("breaks").toInt()+1))}}})
 }
 Panel{
  Heading(s.t("فعالیت‌های این نوبت","Activity this interval"));Hint(s.t("فعالیت زمانی: شروع با ۲۰ دقیقه؛ گام تغییر ۱۰ دقیقه.","Timed activities start at 20 minutes; adjust by 10 minutes."))
  val a=e.optJSONObject("activities")?:JSONObject().also{if(e.recorded("activity"))it.put(e.optString("activityType","walk"),e.number("activity"))}
  Activities.chunked(2).forEach{pair->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){pair.forEach{(id,emoji,_)->
   Surface(Modifier.weight(1f),color=MaterialTheme.colorScheme.surfaceVariant,shape=RoundedCornerShape(16.dp)){
    Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
     val selected=a.has(id)
     TextButton(onClick={if(!selected)act{s.mutate(date,n.edit){it.getJSONObject("activities").put(id,20)}}},modifier=Modifier.fillMaxWidth()){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(emoji,fontSize=32.sp);Text(activityName(s,id),textAlign=androidx.compose.ui.text.style.TextAlign.Center);Hint(if(selected)s.num(a.number(id))+" "+s.t("دقیقه","min") else s.t("شروع با ۲۰ دقیقه","Start with 20 min"))}}
     if(selected)Stepper(activityName(s,id),s.num(a.number(id)),{act{s.mutate(date,n.edit){it.getJSONObject("activities").put(id,max(0,a.getInt(id)-10))}}},{act{s.mutate(date,n.edit){it.getJSONObject("activities").put(id,a.getInt(id)+10)}}},a.number(id)>0)
    }
   }
  }}}
  Heading(s.t("کگل","Kegel"));Hint(s.t("هدف روزانه: حداقل ۳ نوبت","Daily goal: at least 3 sessions"))
  Stepper(s.t("کگل","Kegel"),s.num(e.number("kegel"))+" "+s.t("نوبت","sessions"),{act{s.mutate(date,n.edit){it.put("kegel",max(0,it.number("kegel").toInt()-1))}}},{act{s.mutate(date,n.edit){it.put("kegel",min(1000,it.number("kegel").toInt()+1))}}},e.number("kegel")>0)
  Hint(s.t("امروز: ","Today: ")+s.num(total.getValue("kegel"))+" / "+s.num(3)+(if(total.getValue("kegel")>=3)" ✓" else ""))
  Hint(s.t("مجموع فعالیت روز: ","Daily activity: ")+s.num(total.getValue("activity"))+" "+s.t("دقیقه","min"))
  TextButton(onClick={act{s.mutate(date,n.edit){e->e.put("activities",JSONObject().also{a->Domain.activityIds.forEach{a.put(it,0)}}).put("activity",0).put("kegel",0)}}}){Text(s.t("این نوبت فعالیت نداشتم","No activity this interval"))}
 }
}
@Composable fun FoodsScreen(s:Store,n:NavState,act:(()->Unit)->Unit){
 TextButton(onClick={n.back()}){Text("‹ "+if(n.category!=null||n.search.isNotBlank())s.t("دسته‌بندی‌ها","Categories") else s.t("ثبت سریع","Quick log"))}
 OutlinedTextField(n.search,{n.search=it},label={Text(s.t("جستجوی خوراکی","Search foods"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
 if(n.category==null&&n.search.isBlank()){
  s.categories.chunked(2).forEach{pair->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){pair.forEach{c->
   FilledTonalButton(onClick={n.category=c.getString("id")},shape=RoundedCornerShape(18.dp),modifier=Modifier.weight(1f).heightIn(min=108.dp)){Column(horizontalAlignment=Alignment.CenterHorizontally){FoodArt(c.getString("emoji"),48);Text(s.foodName(c),textAlign=androidx.compose.ui.text.style.TextAlign.Center);Hint(s.num(s.foods().count{it.getString("cat")==c.getString("id")})+" "+s.t("خوراکی","items"))}}
  };if(pair.size==1)Spacer(Modifier.weight(1f))}}
 }else{
  Panel{
   if(n.category!=null)Heading(s.foodName(s.categories.first{it.getString("id")==n.category}))
   val foods=s.foods().filter{(n.category==null||it.getString("cat")==n.category)&&(n.search.isBlank()||it.getString("fa").contains(n.search,true)||it.getString("en").contains(n.search,true))}
   if(foods.isEmpty())Hint(s.t("موردی پیدا نشد.","No matching food."))
   foods.forEach{FoodRow(s,n,it,act)}
  }
 }
 OutlinedButton(onClick={n.dialog="custom"},modifier=Modifier.fillMaxWidth()){Text(s.t("＋ خوراکی یا غذای خانگی جدید","＋ New food or home recipe"))}
 Hint(s.t("اولین + سهم پایه را ثبت می‌کند؛ بعدی‌ها ۲۰٪ سهم پایه. نوشیدنی: هر گام یک لیوان ۲۰۰ میلی‌لیتری.","First + logs the base portion, then steps are 20%. Drinks: one 200 ml glass per step."))
}
@Composable fun FoodDialog(s:Store,n:NavState,f:JSONObject,act:(()->Unit)->Unit){
 val e=s.entry(LocalDate.parse(n.date),n.edit,n.clock);val amount=e.objects("items").filter{it.getJSONObject("food").getString("id")==f.getString("id")}.sumOf{it.number("amount")}
 var exact by rememberSaveable(f.getString("id"),e.getString("id")){mutableStateOf(s.cached("food:"+n.date+":"+e.getString("id")+":"+f.getString("id"),amount.toString()))}
 LaunchedEffect(amount){exact=s.cached("food:"+n.date+":"+e.getString("id")+":"+f.getString("id"),amount.toString())}
 AlertDialog(onDismissRequest={n.dialog=null},title={Text(f.optString("emoji")+" "+s.foodName(f))},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
  FoodRow(s,n,f,act)
  OutlinedTextField(exact,{v->exact=v;act{s.cache("food:"+n.date+":"+e.getString("id")+":"+f.getString("id"),v)};v.inputNumber().toDoubleOrNull()?.takeIf{it in 0.0..20000.0}?.let{value->act{s.food(LocalDate.parse(n.date),n.edit,f,exact=value)}}},label={Text(s.t("مقدار دقیق","Exact amount")+" ("+if(f.optBoolean("drink"))"ml)" else s.t("گرم)","g)"))},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))
  Hint(s.t("ارزش غذایی در هر ۱۰۰ گرم یا میلی‌لیتر:","Nutrition per 100 g or ml:"))
  listOf("kcal" to s.t("کالری","Calories"),"protein" to s.t("پروتئین","Protein"),"fiber" to s.t("فیبر","Fiber")).forEach{(k,label)->Text(label+": "+if(f.isNull(k))s.t("نامشخص","Unknown") else s.num(f.number(k),1))}
  if(f.optBoolean("approx"))Hint(s.t("تبدیل حجم به وزن تقریبی است.","Volume-to-weight conversion is approximate."))
  Hint(s.t("منبع: ","Source: ")+f.optString("source",s.t("کاربر","User"))+if(f.has("fdcId"))" · FDC "+f.optString("fdcId") else "")
 }},confirmButton={TextButton(onClick={n.dialog=null}){Text(s.t("بستن","Close"))}})
}
