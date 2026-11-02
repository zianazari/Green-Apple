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
}
