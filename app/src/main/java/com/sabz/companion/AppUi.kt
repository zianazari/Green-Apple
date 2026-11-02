@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.sabz.companion
import android.view.WindowInsetsController
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import org.json.*
import java.time.*

class NavState {
 var page by mutableStateOf("home")
 var date by mutableStateOf(LocalDate.now().toString())
 var edit by mutableStateOf<String?>(null)
 var category by mutableStateOf<String?>(null)
 var search by mutableStateOf("")
 var dialog by mutableStateOf<String?>(null)
 var foodId by mutableStateOf<String?>(null)
 var clock by mutableStateOf(LocalDateTime.now())
 fun go(p:String){if(p !in listOf("quick","foods"))edit=null;page=p;dialog=null}
 fun back(){if(dialog!=null){dialog=null;return};when(page){"foods"->if(category!=null||search.isNotBlank()){category=null;search=""}else page="quick";"quick"->go(if(edit!=null)"details" else "home");else->go("home")}}
 companion object {val Saver=listSaver<NavState,String>(save={listOf(it.page,it.date,it.edit?:"",it.category?:"",it.search,it.dialog?:"",it.foodId?:"")},restore={v->NavState().apply{page=v[0];date=v[1];edit=v[2].ifEmpty{null};category=v[3].ifEmpty{null};search=v[4];dialog=v[5].ifEmpty{null};foodId=v[6].ifEmpty{null}}})}
}
private val Dark=darkColorScheme(primary=Color(0xFF82AAFF),onPrimary=Color(0xFF181A20),background=Color(0xFF181A20),onBackground=Color(0xFFECEFF4),surface=Color(0xFF252832),onSurface=Color(0xFFECEFF4),surfaceVariant=Color(0xFF292F42),secondaryContainer=Color(0xFF353E57),onSecondaryContainer=Color(0xFF82AAFF),onSurfaceVariant=Color(0xFFACB1C1),outline=Color(0xFF3A3E4C))
private val Light=lightColorScheme(primary=Color(0xFF087F78),onPrimary=Color.White,background=Color(0xFFF8F8F2),onBackground=Color(0xFF163E3B),surface=Color.White,onSurface=Color(0xFF163E3B),surfaceVariant=Color(0xFFEAF3EE),secondaryContainer=Color(0xFFEAF3EE),onSecondaryContainer=Color(0xFF087F78),onSurfaceVariant=Color(0xFF657C76),outline=Color(0xFFDCE6DF))
val AppFont=FontFamily(Font(R.font.vazirmatn))
val AppTypography=Typography().let{b->b.copy(
 displayLarge=b.displayLarge.copy(fontFamily=AppFont),displayMedium=b.displayMedium.copy(fontFamily=AppFont),displaySmall=b.displaySmall.copy(fontFamily=AppFont),
 headlineLarge=b.headlineLarge.copy(fontFamily=AppFont),headlineMedium=b.headlineMedium.copy(fontFamily=AppFont),headlineSmall=b.headlineSmall.copy(fontFamily=AppFont,fontSize=24.sp,lineHeight=30.sp),
 titleLarge=b.titleLarge.copy(fontFamily=AppFont),titleMedium=b.titleMedium.copy(fontFamily=AppFont,fontSize=16.sp,lineHeight=22.sp),titleSmall=b.titleSmall.copy(fontFamily=AppFont),
 bodyLarge=b.bodyLarge.copy(fontFamily=AppFont,fontSize=15.sp,lineHeight=22.sp),bodyMedium=b.bodyMedium.copy(fontFamily=AppFont,fontSize=14.sp,lineHeight=20.sp),bodySmall=b.bodySmall.copy(fontFamily=AppFont,fontSize=12.sp,lineHeight=17.sp),
 labelLarge=b.labelLarge.copy(fontFamily=AppFont),labelMedium=b.labelMedium.copy(fontFamily=AppFont),labelSmall=b.labelSmall.copy(fontFamily=AppFont))}
@Composable fun GreenAppleApp(s:Store,host:MainActivity) {
 val nav=rememberSaveable(saver=NavState.Saver){NavState()}
 val colors=if(s.dark)Dark else Light
 fun act(action:()->Unit){runCatching(action).onFailure{host.toast(it.message?:s.t("انجام نشد.","Action failed."))}}
 LaunchedEffect(Unit){var last=LocalDate.now();while(true){nav.clock=LocalDateTime.now();val current=nav.clock.toLocalDate();if(nav.edit==null&&nav.date==last.toString()&&current!=last)nav.date=current.toString();last=current;delay(15000)}}
 LaunchedEffect(host.request){host.request?.let{nav.date=it.second;nav.go(it.first);host.request=null}}
 SideEffect {host.window.statusBarColor=colors.background.toArgb();host.window.navigationBarColor=colors.surface.toArgb();val light=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;host.window.insetsController?.setSystemBarsAppearance(if(s.dark)0 else light,light)}
 CompositionLocalProvider(LocalLayoutDirection provides if(s.fa)LayoutDirection.Rtl else LayoutDirection.Ltr){
  MaterialTheme(colorScheme=colors,typography=AppTypography){
   BackHandler(nav.page!="home"||nav.dialog!=null){nav.back()}
   Scaffold(containerColor=colors.background,bottomBar={
    NavigationBar(containerColor=colors.surface){
     listOf(Triple("home","⌂",s.t("خانه","Home")),Triple("quick","＋",s.t("ثبت","Log")),Triple("history","▦",s.t("تقویم","Calendar")),Triple("trend","▥",s.t("روند","Trends"))).forEach{(id,symbol,title)->
      NavigationBarItem(selected=nav.page==id,onClick={nav.go(id)},icon={Text(symbol,fontSize=25.sp)},label={Text(title,fontSize=12.sp)})
     }
    }
   }){padding->
    key(nav.page,nav.category){
     Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
      Spacer(Modifier.height(2.dp))
      Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
       Column(Modifier.weight(1f)){Text(s.t("سیب سلامت · همراه روزهای سالم","Green Apple · A little healthier daily"),style=MaterialTheme.typography.labelSmall,color=colors.primary);Text(when(nav.page){"quick"->s.t("ثبت سریع","Quick log");"foods"->s.t("خوراکی‌ها","Foods");"history"->s.t("تقویم","Calendar");"trend"->s.t("روند من","My trends");"settings"->s.t("تنظیمات","Settings");"details"->s.t("جزئیات روز","Day details");else->s.t("امروزِ من","My day")},style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
       IconButton(onClick={act{s.setSetting("theme",if(s.dark)"light" else "dark")}},modifier=Modifier.semantics{contentDescription=s.t("تغییر تم","Switch theme")}){Text(if(s.dark)"☀" else "☾",fontSize=24.sp)}
       IconButton(onClick={nav.go("settings")},modifier=Modifier.semantics{contentDescription=s.t("تنظیمات","Settings")}){Text("⚙",fontSize=24.sp)}
      }
      if(s.loadError)Panel{Text(s.t("اطلاعات قبلی خوانده نشد؛ برای محافظت از آن ثبت غیرفعال است.","Existing data could not be read. Logging is disabled to protect it."));Button(onClick={host.backup()}){Text(s.t("ذخیرهٔ فایل فعلی","Export original data"))};Button(onClick={host.importBackup()}){Text(s.t("بازیابی پشتیبان","Restore backup"))}}
      if(nav.page !in listOf("settings","history"))Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){AssistChip(onClick={nav.dialog="date"},label={Text("▦ "+s.dateLabel(LocalDate.parse(nav.date)))});if(nav.date!=LocalDate.now().toString())TextButton(onClick={nav.date=LocalDate.now().toString();nav.edit=null}){Text(s.t("امروز","Today"))}}
      when(nav.page){
       "home"->HomeScreen(s,nav,host,::act)
       "quick"->QuickScreen(s,nav,::act)
       "foods"->FoodsScreen(s,nav,::act)
       "history"->HistoryScreen(s,nav)
       "trend"->TrendScreen(s,nav)
       "settings"->SettingsScreen(s,host,::act)
       "details"->DetailsScreen(s,nav,::act)
      }
      Spacer(Modifier.height(12.dp))
     }
    }
   }
   when(nav.dialog){
    "date"->DateDialog(s,LocalDate.parse(nav.date),onDismiss={nav.dialog=null}){nav.date=it.toString();nav.edit=null;nav.dialog=null}
    "food"->s.foods().find{it.getString("id")==nav.foodId}?.let{FoodDialog(s,nav,it,::act)}
    "custom"->CustomDialog(s,onDismiss={nav.dialog=null},onSave={act{s.custom(it);nav.dialog=null}})
    "report"->ReportDialog(s,host,LocalDate.parse(nav.date)){nav.dialog=null}
   }
   host.incoming?.let{raw->AlertDialog(onDismissRequest={host.clearIncoming()},title={Text(s.t("بازیابی پشتیبان","Restore backup"))},text={Text(s.t("اطلاعات فعلی با این پشتیبان جایگزین می‌شود.","This backup will replace the current data."))},confirmButton={TextButton(onClick={act{s.restore(raw);host.clearIncoming();nav.go("home")}}){Text(s.t("بازیابی","Restore"))}},dismissButton={TextButton(onClick={host.clearIncoming()}){Text(s.t("انصراف","Cancel"))}})}
  }
 }
}
@Composable fun Panel(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){Surface(modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.55f))){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)}}
@Composable fun Hint(text:String){Text(text,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
@Composable fun Heading(text:String){Text(text,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)}
@Composable fun Metric(s:Store,label:String,value:Double,goal:Double,color:Color,unit:String){
 Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,fontWeight=FontWeight.Medium);Text(s.num(value,1)+" / "+s.num(goal)+" "+unit,style=MaterialTheme.typography.bodySmall)}
  LinearProgressIndicator(progress={(value/goal).toFloat().coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(7.dp),color=color,trackColor=MaterialTheme.colorScheme.outline)
  Hint(if(value<goal*.9)s.num(goal-value,1)+" "+unit+" "+s.t("تا هدف","to target") else if(value<=goal*1.1)s.t("نزدیک هدف","Near target") else s.num(value-goal,1)+" "+unit+" "+s.t("بیشتر از هدف","above target"))
 }
}
@Composable fun Nutrition(s:Store,d:JSONObject){val v=Domain.totals(d);val g=d.getJSONObject("targets");Panel{Heading(s.t("تغذیهٔ روز","Daily nutrition"));Metric(s,s.t("کالری","Calories"),v.getValue("kcal"),g.getDouble("kcal"),Color(0xFFED9461),"kcal");Metric(s,s.t("پروتئین","Protein"),v.getValue("protein"),g.getDouble("protein"),Color(0xFF158B81),s.t("گرم","g"));Metric(s,s.t("فیبر","Fiber"),v.getValue("fiber"),g.getDouble("fiber"),Color(0xFF83B45F),s.t("گرم","g"));if(v.getValue("unknown")>0)Hint(s.t("برخی ارزش‌های غذایی نامشخص‌اند؛ جمع‌ها ناقص هستند.","Some nutrition is unknown; totals are incomplete."))}}
@Composable fun Apple(s:Store,score:Int?,modifier:Modifier=Modifier){
 val swing by rememberInfiniteTransition(label="apple").animateFloat(-2f,2f,infiniteRepeatable(tween(2200),RepeatMode.Reverse),label="sway")
 Image(painterResource(if(score==null||score>=65)R.drawable.ic_launcher else R.drawable.apple_tired),contentDescription=if(score==null||score>=65)s.t("سیب سبز خندان","Smiling green apple") else s.t("سیب سبز خسته","Tired green apple"),modifier=modifier.size(108.dp).rotate(swing))
}
@Composable fun HomeScreen(s:Store,n:NavState,host:MainActivity,act:(()->Unit)->Unit){
 val date=LocalDate.parse(n.date);val d=s.day(date);val v=Domain.totals(d);val week=Domain.week(s.root,date).mapNotNull{it.second};val score=if(week.isEmpty())null else week.average().toInt()
 if(!s.settings.optBoolean("confirmed"))Panel{Text(s.t("برای شروع، قد، وزن و سن را برای تعیین هدف‌های اولیه وارد کن.","Enter your height, weight and age to set your starting goals."));Button(onClick={n.go("settings")}){Text(s.t("تنظیم هدف‌ها","Set goals"))}}
 Surface(shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surfaceVariant){Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){Hint(s.t("همراهی در ۷ روز گذشته","Your last 7 days"));Text(score?.let{s.num(it)+"٪"}?:"—",fontSize=36.sp,lineHeight=42.sp,fontWeight=FontWeight.Bold);Text(if(score==null)s.t("قدم اول، یک ثبت ساده","Start with one small entry") else if(score>=65)s.t("عادت‌های کوچک، حال بهتر","Small habits, better days") else s.t("قدم بعدی را با هم برداریم","One small step together"));Hint(s.num(week.size)+" "+s.t("روز کامل از ۷ روز","complete days out of 7"))};Apple(s,score)}}
 Nutrition(s,d)
 Panel{
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Heading(s.t("آب سادهٔ امروز","Plain water today"));Text(s.num(v.getValue("water")/200,1)+" "+s.t("لیوان از","of")+" "+s.num(d.getJSONObject("targets").number("water")/200),fontSize=23.sp);Hint(s.num(v.getValue("water"))+" / "+s.num(d.getJSONObject("targets").number("water"))+" ml")}
   val progress=(v.getValue("water")/d.getJSONObject("targets").number("water")).toFloat()
   Box(Modifier.size(86.dp),contentAlignment=Alignment.Center){CircularProgressIndicator(progress={progress.coerceIn(0f,1f)},modifier=Modifier.fillMaxSize(),strokeWidth=8.dp,trackColor=MaterialTheme.colorScheme.outline);Text(s.num((progress*100).toDouble())+"٪")}
  }
  FilledTonalButton(onClick={act{s.food(date,null,s.foods().first{it.optBoolean("water")})}},modifier=Modifier.fillMaxWidth()){Text(s.t("＋ یک لیوان آب · ثبت فوری","＋ One glass · log now"))}
 }
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
  Panel(Modifier.weight(1f)){Heading(s.t("🪑 نشستن","🪑 Sitting"));Text(s.duration(v.getValue("sitting")));Hint(s.t("طولانی‌ترین نوبت: ","Longest: ")+s.duration(v.getValue("longest")));Hint(s.t("وقفه‌ها: ","Breaks: ")+s.num(v.getValue("breaks")))}
  Panel(Modifier.weight(1f)){Heading(s.t("👟 فعالیت","👟 Activity"));Text(s.num(v.getValue("activity"))+" "+s.t("دقیقه","min"));Hint(s.t("کگل: ","Kegel: ")+s.num(v.getValue("kegel"))+" / "+s.num(3))}
 }

 Button(onClick={n.go("quick")},modifier=Modifier.fillMaxWidth()){Text(s.t("＋ ثبت سریع","＋ Quick log"))}
 OutlinedButton(onClick={n.go("details")},modifier=Modifier.fillMaxWidth()){Text(s.t("جزئیات و ویرایش روز","Day details & edits"))}
 TextButton(onClick={n.dialog="report"}){Text(s.t("گزارش PDF","PDF report"))}
 Hint(s.t("امتیاز، میزان پیگیری عادت‌هاست؛ تشخیص پزشکی نیست.","The score tracks habits; it is not a medical assessment."))
}
