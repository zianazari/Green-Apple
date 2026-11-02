package com.sabz.companion

import org.json.JSONArray
import org.json.JSONObject
import java.time.*
import kotlin.math.*

fun JSONObject.copyJson() = JSONObject(toString())
fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
fun JSONObject.objects(key: String) = optJSONArray(key)?.objects().orEmpty()
fun JSONObject.number(key: String): Double = if (isNull(key)) 0.0 else optDouble(key, 0.0).takeIf { it.isFinite() } ?: 0.0
fun JSONObject.recorded(key: String) = has(key) && !isNull(key)
fun jsonArray(values: List<JSONObject>) = JSONArray().also { a -> values.forEach { a.put(it) } }

object Domain {
 val goalKeys = listOf("kcal","protein","fiber","water","activity","breakLimit")
 val activityIds = listOf("walk","exercise","stretch","other")
 fun defaults() = JSONObject("""{"lang":"fa","theme":"light","calendar":"persian","kcal":2000,"protein":56,"fiber":28,"water":1600,"activity":22,"breakLimit":60,"weight":70,"confirmed":false,"reminders":false,"wake":8,"sleep":22,"triggers":[]}""")
 fun blank() = JSONObject().put("version",1).put("settings",defaults()).put("days",JSONObject()).put("custom",JSONArray()).put("ui",JSONObject().put("sessions",JSONObject()).put("forms",JSONObject()))
 fun targets(s: JSONObject) = JSONObject().also { g -> goalKeys.forEach { g.put(it,s.getDouble(it)) } }
 fun day(s: JSONObject, date: String): JSONObject = s.getJSONObject("days").optJSONObject(date) ?: JSONObject().put("entries",JSONArray()).put("targets",targets(s.getJSONObject("settings"))).put("complete",false)
 fun totals(day: JSONObject): Map<String,Double> {
  val t = listOf("kcal","protein","fiber","water","fluids","activity","kegel","sitting","longest","breaks","unknown").associateWith { 0.0 }.toMutableMap()
  day.objects("entries").forEach { e ->
   e.objects("items").forEach { i ->
    val f=i.getJSONObject("food"); val amount=i.getDouble("amount")
    listOf("kcal","protein","fiber").forEach { k -> if(f.isNull(k)) t["unknown"]=t.getValue("unknown")+1 else t[k]=t.getValue(k)+f.getDouble(k)*amount/100 }
    if(f.optBoolean("drink")) t["fluids"]=t.getValue("fluids")+amount
    if(f.optBoolean("water")) t["water"]=t.getValue("water")+amount
   }
   listOf("activity","sitting","breaks","kegel").forEach { t[it]=t.getValue(it)+e.number(it) }
   t["longest"]=max(t.getValue("longest"),e.number("sitting"))
  }
  return t
 }
 fun coverage(d: JSONObject): Boolean {
  val es=d.objects("entries")
  return es.any { it.optBoolean("noFood")||it.objects("items").any { i->!i.getJSONObject("food").optBoolean("drink") } } &&
   es.any { it.optBoolean("noDrink")||it.objects("items").any { i->i.getJSONObject("food").optBoolean("drink") } } &&
   es.any { it.recorded("sitting") } && es.any { it.recorded("activity") }
 }
 fun score(d: JSONObject): Int? {
  if(!d.optBoolean("complete")) return null
  val t=totals(d); if(t.getValue("unknown")>0) return null
  val g=d.getJSONObject("targets")
  fun cap(k:String)=min(1.0,t.getValue(k)/g.getDouble(k))
  return (15*max(0.0,1-abs(t.getValue("kcal")-g.getDouble("kcal"))/g.getDouble("kcal"))+15*cap("protein")+25*cap("fiber")+20*cap("water")+15*cap("activity")+10*(if(t.getValue("sitting")==0.0)1.0 else min(1.0,g.getDouble("breakLimit")/max(g.getDouble("breakLimit"),t.getValue("longest"))))).roundToInt()
 }
 fun week(s:JSONObject,end:LocalDate):List<Pair<LocalDate,Int?>> = (6 downTo 0).map { val d=end.minusDays(it.toLong()); d to score(day(s,d.toString())) }
 fun hasData(e:JSONObject)=e.objects("items").isNotEmpty()||e.optBoolean("noFood")||e.optBoolean("noDrink")||listOf("sitting","activity","breaks","kegel").any{e.recorded(it)}
 fun interval(s:JSONObject,date:LocalDate,now:LocalDateTime,editId:String?=null):JSONObject {
  val es=day(s,date.toString()).objects("entries")
  es.find{it.optString("id")==editId}?.let{return it.copyJson()}
  val slot=if(date==now.toLocalDate()) now.hour/3*3 else 21
  es.find{it.optInt("autoSlot",-1)==slot&&hasData(it)}?.let{return it.copyJson()}
  val earlier=es.filter{hasData(it)}.mapNotNull{ if(it.has("autoSlot"))it.getInt("autoSlot") else it.optString("time").take(2).toIntOrNull()?.div(3)?.times(3)}.filter{it<slot}
  return JSONObject().put("id","auto-$date-$slot").put("time","%02d:00".format(slot)).put("autoSlot",slot).put("autoStart",earlier.maxOrNull()?.plus(3)?:0).put("items",JSONArray()).put("noFood",false).put("noDrink",false).put("activities",JSONObject()).put("activityType","walk").also { e->listOf("sitting","activity","longest","breaks").forEach{e.put(it,JSONObject.NULL)} }
 }
 fun nextAmount(f:JSONObject,amount:Double,dir:Int):Double {
  val step=if(f.optBoolean("drink"))200.0 else f.getDouble("base")*.2
  return max(0.0,round((if(dir>0&&amount==0.0)if(f.optBoolean("drink"))200.0 else f.getDouble("base") else amount+dir*step)*10)/10)
 }
 fun frequent(s:JSONObject,foods:List<JSONObject>):List<JSONObject> {
  val count=mutableMapOf<String,Int>();val last=mutableMapOf<String,String>();val days=s.getJSONObject("days")
  days.keys().forEach { date->days.getJSONObject(date).objects("entries").forEach { e->e.objects("items").filter{it.number("amount")>0}.map{it.getJSONObject("food").getString("id")}.distinct().forEach{id->count[id]=(count[id]?:0)+1;last[id]=maxOf(last[id]?:"",date)} } }
  return foods.filter{count.contains(it.getString("id"))}.sortedWith(compareByDescending<JSONObject>{count[it.getString("id")]}.thenByDescending{last[it.getString("id")]}.thenBy{it.getString("id")}).take(8)
 }
 fun validate(s:JSONObject):JSONObject {
  require(s.getInt("version")==1)
  val settings=s.getJSONObject("settings")
  require(settings.getString("lang") in listOf("fa","en")&&settings.getString("calendar") in listOf("persian","gregory"))
  require(settings.optString("theme","light") in listOf("light","dark"))
  (goalKeys+"weight").forEach { range(settings,it,0.001,10000.0) }
  require(settings.getInt("wake") in 0..22 && settings.getInt("sleep") in 1..23 && settings.getInt("wake")<settings.getInt("sleep"))
  require(s.getJSONArray("custom").length()<=1000);s.objects("custom").forEach{validateFood(it)}
  val days=s.getJSONObject("days");require(days.length()<=36600)
  days.keys().forEach { key->
   require(LocalDate.parse(key).toString()==key)
   val d=days.getJSONObject(key);d.getBoolean("complete");goalKeys.forEach{range(d.getJSONObject("targets"),it,.001,10000.0)}
   val es=d.getJSONArray("entries");require(es.length()<=1000)
   es.objects().forEach { e->
    e.getString("id");e.getString("time");require(e.getJSONArray("items").length()<=200)
    e.objects("items").forEach { i->validateFood(i.getJSONObject("food"));range(i,"amount",.001,20000.0) }
    listOf("sitting","activity","longest","breaks").forEach { if(e.recorded(it))range(e,it,0.0,1440.0) }
    if(e.has("kegel")){range(e,"kegel",0.0,1000.0);require(e.getDouble("kegel")%1==0.0)}
    require(e.number("longest")<=e.number("sitting"))
    if(e.number("sitting")>0)require(e.number("longest")>0)
    e.optJSONObject("activities")?.let { a->a.keys().forEach{require(it in activityIds);range(a,it,0.0,1440.0);require(a.getDouble(it)%1==0.0)};if(a.length()>0)require(a.keys().asSequence().sumOf{a.number(it)}==e.number("activity")) }
    if(e.has("autoSlot")){require(e.getInt("autoSlot") in 0..21&&e.getInt("autoSlot")%3==0);require(e.getInt("autoStart") in 0..e.getInt("autoSlot")&&e.getInt("autoStart")%3==0)}
   }
   val t=totals(d);require(t.getValue("sitting")<=1440&&t.getValue("activity")<=1440)
  }
  return s
 }
 fun range(o:JSONObject,k:String,min:Double,max:Double){val n=o.getDouble(k);require(n.isFinite()&&n in min..max)}
 fun validateFood(f:JSONObject){
  listOf("id","fa","en","emoji","cat").forEach{f.getString(it)};require(f.getString("fa").length<=160&&f.getString("en").length<=160)
  f.getBoolean("drink");f.getBoolean("water");range(f,"base",.001,10000.0)
  listOf("kcal","protein","fiber").forEach{require(f.has(it));if(!f.isNull(it))range(f,it,0.0,1000.0)}
 }
}
