package com.sabz.companion;
import java.time.*;
/** Local-wall-clock scheduling, independently testable without Android services. */
public final class ReminderSchedule {
 public static final class Next {public long millis;public boolean check,move,review;public String date;}
 public static Next next(long now,ZoneId zone,int wake,int sleep){LocalDate date=Instant.ofEpochMilli(now).atZone(zone).toLocalDate();Next best=null;for(int day=0;day<2;day++)for(int h=0;h<24;h++){boolean check=h==9||h==12||h==15||h==18,review=h==22,move=h>=wake&&h<sleep&&(h-wake)%2==0;if(!check&&!review&&!move)continue;LocalDate candidate=date.plusDays(day);long time=ZonedDateTime.of(candidate,LocalTime.of(h,0),zone).toInstant().toEpochMilli();if(time>now&&(best==null||time<best.millis)){best=new Next();best.millis=time;best.check=check;best.move=move;best.review=review;best.date=candidate.toString();}}return best;}
}
