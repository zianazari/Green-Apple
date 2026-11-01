package com.sabz.companion;
import java.time.*;
public class ReminderScheduleTest {
 static void check(boolean x){if(!x)throw new AssertionError();}
 static ReminderSchedule.Next next(String local,ZoneId zone,int wake,int sleep){return ReminderSchedule.next(LocalDateTime.parse(local).atZone(zone).toInstant().toEpochMilli(),zone,wake,sleep);}
 public static void main(String[] args){ZoneId helsinki=ZoneId.of("Europe/Helsinki");ReminderSchedule.Next n=next("2026-10-22T21:59:00",helsinki,8,22);check(n.review&&!n.check&&!n.move);check(Instant.ofEpochMilli(n.millis).atZone(helsinki).getHour()==22);check(n.date.equals("2026-10-22"));n=next("2026-10-22T22:00:01",helsinki,8,22);check(n.move&&!n.review&&n.date.equals("2026-10-23"));n=next("2026-10-22T11:59:00",helsinki,8,22);check(n.move&&n.check&&!n.review);n=next("2026-10-22T21:59:00",helsinki,8,23);check(n.review&&n.move);n=next("2026-10-25T03:59:00",helsinki,8,22);check(Instant.ofEpochMilli(n.millis).atZone(helsinki).getHour()==8);n=next("2026-10-22T21:59:00",ZoneId.of("Asia/Tehran"),8,22);check(Instant.ofEpochMilli(n.millis).atZone(ZoneId.of("Asia/Tehran")).getHour()==22);System.out.println("PASS: 22:00 summary, next day, coincident reminders, time zones, daylight saving.");}
}
