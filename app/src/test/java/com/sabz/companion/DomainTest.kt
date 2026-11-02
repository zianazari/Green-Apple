package com.sabz.companion
import org.junit.Test
import org.junit.Assert.*
import org.json.*
import java.time.*

class DomainTest {
 private fun resource(name:String)=javaClass.classLoader!!.getResource(name)!!.readText()
 @Test fun legacySchemaAndParity(){
  val legacy=Domain.validate(JSONObject(resource("legacy-v1.json")))
  assertEquals(35,legacy.getJSONObject("days").getJSONObject("2026-10-22").objects("entries")[0].getInt("longest"))
  val cases=JSONArray(resource("domain-parity.json"))
  cases.objects().forEach{c->
   val state=Domain.validate(c.getJSONObject("state"));val day=Domain.day(state,"2026-10-22")
   val expected=c.getJSONObject("totals")
   Domain.totals(day).forEach{(k,v)->assertEquals(k,expected.getDouble(k),v,.000001)}
   assertEquals(if(c.isNull("score"))null else c.getInt("score"),Domain.score(day))
  }
 }
 @Test fun automaticIntervalsMergeOnlyEmptyPeriods(){
  val state=Domain.blank();val date=LocalDate.of(2026,11,1)
  val morning=Domain.interval(state,date,date.atTime(10,0))
  assertEquals(9,morning.getInt("autoSlot"));assertEquals(0,morning.getInt("autoStart"));assertFalse(Domain.hasData(morning))
  morning.put("sitting",0).put("longest",0)
  val day=Domain.day(state,date.toString()).put("entries",JSONArray().put(morning))
  state.getJSONObject("days").put(date.toString(),day)
  assertTrue(Domain.hasData(morning))
  assertEquals(morning.getString("id"),Domain.interval(state,date,date.atTime(11,59)).getString("id"))
  val evening=Domain.interval(state,date,date.atTime(17,0))
  assertEquals(15,evening.getInt("autoSlot"));assertEquals(12,evening.getInt("autoStart"))
  assertEquals(morning.toString(),Domain.interval(state,date,date.atTime(17,0),morning.getString("id")).toString())
  assertEquals(0,Domain.interval(state,date.plusDays(1),date.plusDays(1).atTime(1,0)).getInt("autoStart"))
 }
 @Test fun rejectsBadBackupWithoutRewritingLegacy(){
  val s=JSONObject(resource("legacy-v1.json"))
  s.getJSONObject("days").getJSONObject("2026-10-22").objects("entries")[0].put("sitting",1500)
  assertThrows(IllegalArgumentException::class.java){Domain.validate(s)}
 }
 @Test fun portionAndKegelAreIndependent(){
  val food=JSONObject("""{"base":100,"drink":false}""")
  assertEquals(100.0,Domain.nextAmount(food,0.0,1),0.0)
  assertEquals(120.0,Domain.nextAmount(food,100.0,1),0.0)
  val s=JSONObject(resource("legacy-v1.json"));val d=Domain.day(s,"2026-10-22")
  assertEquals(3.0,Domain.totals(d).getValue("kegel"),0.0);assertEquals(30.0,Domain.totals(d).getValue("activity"),0.0)
 }
 @Test fun scheduleUsesLocalTimeAndDst(){
  val zone=ZoneId.of("Europe/Helsinki")
  val now=ZonedDateTime.of(2026,10,25,8,30,0,0,zone)
  val next=ReminderSchedule.next(now.toInstant().toEpochMilli(),zone,8,22)!!
  assertEquals(9,Instant.ofEpochMilli(next.millis).atZone(zone).hour);assertTrue(next.check)
  val night=ReminderSchedule.next(ZonedDateTime.of(2026,11,1,21,59,0,0,zone).toInstant().toEpochMilli(),zone,8,22)!!
  assertTrue(night.review);assertEquals("2026-11-01",night.date)
 }
 @Test fun nightlyReviewRequiresCompleteData(){
  val d=Domain.day(Domain.blank(),"2026-11-01")
  assertTrue(NightReview.message(d,false).contains("incomplete"))
 }
 @Test fun completedEmptyOrPartialDaysAreValidBackupsAndGetAReview(){
  val state=Domain.blank();val date="2026-11-02"
  val empty=Domain.day(state,date).put("complete",true)
  state.getJSONObject("days").put(date,empty)
  Domain.validate(state)
  assertFalse(Domain.coverage(empty))
  assertNotNull(Domain.score(empty))
  assertFalse(NightReview.message(empty,false).contains("incomplete"))
  val partial=Domain.interval(state,LocalDate.parse(date),LocalDateTime.of(2026,11,2,9,0)).put("sitting",60).put("longest",60)
  empty.put("entries",JSONArray().put(partial))
  Domain.validate(state)
  assertFalse(Domain.coverage(empty))
  assertNotNull(Domain.score(empty))
  assertFalse(NightReview.message(empty,false).contains("incomplete"))
 }

 @Test fun adultEnergyEstimateUsesHeightWeightAndAgeTogether(){
  val base=Profile.estimate(175.0,70.0,30,"male","inactive")
  assertEquals(2552.67,base.calories,.000001)
  assertEquals(56.0,base.protein,.000001)
  assertEquals(2550.0,Profile.roundedCalories(base.calories),0.0)
  assertEquals(2195.30,Profile.estimate(175.0,70.0,30,"female","inactive").calories,.000001)
  assertEquals(2934.62,Profile.estimate(175.0,70.0,30,"male","active").calories,.000001)
  assertEquals(65.0,Profile.estimate(185.0,70.0,30,"male","inactive").calories-base.calories,.000001)
  assertEquals(141.0,Profile.estimate(175.0,80.0,30,"male","inactive").calories-base.calories,.000001)
  assertEquals(-216.6,Profile.estimate(175.0,70.0,50,"male","inactive").calories-base.calories,.000001)
  assertThrows(IllegalArgumentException::class.java){Profile.estimate(175.0,70.0,18,"male","inactive")}
  assertThrows(IllegalArgumentException::class.java){Profile.estimate(175.0,70.0,30,"unspecified","inactive")}
 }
 @Test fun englishDefaultAndLegacyProfileCompatibility(){
  assertEquals("en",Domain.defaults().getString("lang"))
  val old=JSONObject(resource("legacy-v1.json"));Domain.validate(old)
  assertEquals("fa",old.getJSONObject("settings").getString("lang"))
  assertFalse(old.getJSONObject("settings").has("height"))
  val historical=old.getJSONObject("days").toString()
  old.getJSONObject("settings").put("height",175).put("age",30).put("weight",70).put("calorieSex","male").put("activityLevel","inactive").put("kcal",2550)
  Domain.validate(old);assertEquals(historical,old.getJSONObject("days").toString())
  old.getJSONObject("settings").put("height",-1)
  assertThrows(IllegalArgumentException::class.java){Domain.validate(old)}
 }

}
