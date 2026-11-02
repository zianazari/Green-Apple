package com.sabz.companion
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import android.icu.util.Calendar
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

@RunWith(AndroidJUnit4::class)
class NativeUiTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 @Before fun setup(){rule.runOnIdle{val blank=Domain.blank();blank.getJSONObject("settings").put("confirmed",true);rule.activity.store.restore(blank.toString())}}
 @Test fun immediateFoodAndPersistentTheme(){
  rule.onNodeWithText("ثبت",useUnmergedTree=true).performClick()
  rule.onNodeWithText("دسته‌بندی‌ها").performScrollTo().performClick()
  rule.onNodeWithText("سبزیجات").performScrollTo().performClick()
  rule.onNodeWithContentDescription("هویج خام +").performScrollTo().performClick()
  rule.runOnIdle{
   val store=rule.activity.store;val day=store.day(LocalDate.now())
   assertEquals(1,day.objects("entries").size)
   assertEquals(100.0,day.objects("entries")[0].objects("items")[0].number("amount"),0.0)
   assertEquals(60.0,Domain.totals(day).getValue("sitting"),0.0)
  }
  rule.onNodeWithContentDescription("هویج خام +").performScrollTo().performClick()
  rule.runOnIdle{assertEquals(120.0,rule.activity.store.day(LocalDate.now()).objects("entries")[0].objects("items")[0].number("amount"),0.0)}
  rule.onNodeWithContentDescription("تنظیمات").performScrollTo().performClick()
  rule.onNodeWithText("☾ ذغالی و آبی").performClick()
  rule.onNodeWithText("🇬🇧 English").performClick()
  rule.onNodeWithText("Gregorian").performClick()
  rule.runOnIdle{
   val fresh=Store(rule.activity);assertFalse(fresh.fa);assertTrue(fresh.dark);assertEquals("gregory",fresh.settings.getString("calendar"));assertEquals(120.0,fresh.day(LocalDate.now()).objects("entries")[0].objects("items")[0].number("amount"),0.0)
  }
 }
 @Test fun calendarsAndNativePdf(){
  val date=LocalDate.of(2024,3,20);val c=CalendarTools.calendar(date,true)
  assertEquals(1403,c.get(Calendar.YEAR));assertEquals(0,c.get(Calendar.MONTH));assertEquals(1,c.get(Calendar.DAY_OF_MONTH))
  assertEquals(date,CalendarTools.fromParts(1403,1,1,true))
  assertTrue(CalendarTools.label(date,true,true).contains("فروردین"))
  assertTrue(CalendarTools.label(LocalDate.of(2026,11,2),true,true).contains("آبان"))
  rule.runOnIdle{
   val store=rule.activity.store
   store.mutate(LocalDate.now(),null){it.put("sitting",0);it.getJSONObject("activities").put("walk",30);it.put("kegel",3)}
   val bytes=NativeReport.create(rule.activity,store.root,LocalDate.now(),LocalDate.now())
   assertTrue(bytes.size>1000);assertEquals("%PDF",String(bytes.take(4).toByteArray()))
   val file=File(rule.activity.cacheDir,"native-report-test.pdf");file.writeBytes(bytes)
   PdfRenderer(ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)).use{assertTrue(it.pageCount>=1)}
   val state=store.root.toString();val fresh=Store(rule.activity);assertEquals(state,fresh.root.toString())
  }
 }
 @Test fun restoreRejectsCorruptionAndRetainsExplicitZero(){
  rule.runOnIdle{
   val s=rule.activity.store;s.mutate(LocalDate.now(),null){it.put("sitting",0);it.getJSONObject("activities").put("walk",20)}
   s.mutate(LocalDate.now(),null){it.put("kegel",1)}
   assertEquals(0.0,Domain.totals(s.day(LocalDate.now())).getValue("sitting"),0.0)
   val before=s.root.toString();assertThrows(Exception::class.java){s.restore("""{"version":99}""")};assertEquals(before,s.root.toString())
   s.restore(before);assertEquals(before,s.root.toString())
  }
 }
 @Test fun cachedCustomFoodAndSittingInputs(){
  rule.onNodeWithText("ثبت",useUnmergedTree=true).performClick()
  rule.onNodeWithText("دسته‌بندی‌ها").performScrollTo().performClick()
  rule.onNodeWithText("＋ خوراکی یا غذای خانگی جدید").performScrollTo().performClick()
  rule.onNodeWithText("نام فارسی").performTextInput("غذای آزمایشی بومی")
  rule.onNodeWithText("بستن").performClick()
  rule.onNodeWithText("＋ خوراکی یا غذای خانگی جدید").performScrollTo().performClick()
  rule.onNodeWithText("غذای آزمایشی بومی").assertExists()
  rule.onNodeWithText("افزودن خوراکی").performClick()
  rule.runOnIdle{assertEquals("غذای آزمایشی بومی",rule.activity.store.root.objects("custom").last().getString("fa"))}
  rule.onNodeWithText("خانه",useUnmergedTree=true).performClick()
  rule.onNodeWithText("ثبت",useUnmergedTree=true).performClick()
  rule.onNodeWithContentDescription("نشستن +").performScrollTo().performClick()
  rule.runOnIdle{assertEquals(75.0,Domain.totals(rule.activity.store.day(LocalDate.now())).getValue("sitting"),0.0)}
  rule.onNodeWithContentDescription("نشستن −").performScrollTo().performClick()
  rule.runOnIdle{assertEquals(60.0,Domain.totals(rule.activity.store.day(LocalDate.now())).getValue("sitting"),0.0)}
 }
 @Test fun capturesNativeScreens(){
  rule.runOnIdle{
   val s=rule.activity.store;s.setSetting("theme","dark")
   val food=s.foods().first{it.optBoolean("water")}
   s.food(LocalDate.now(),null,food,exact=800.0)
  }
  rule.onRoot().captureToImage().asAndroidBitmap().let{bmp->File(rule.activity.cacheDir,"native-home-dark.png").outputStream().use{bmp.compress(Bitmap.CompressFormat.PNG,100,it)}}
  rule.onNodeWithContentDescription("تنظیمات").performScrollTo().performClick()
  rule.onRoot().captureToImage().asAndroidBitmap().let{bmp->File(rule.activity.cacheDir,"native-settings-dark.png").outputStream().use{bmp.compress(Bitmap.CompressFormat.PNG,100,it)}}
 }

 @Test fun finishDayWithNoEntriesOrMissingFieldsNeedsNoConfirmation(){
  rule.onNodeWithText("جزئیات و ویرایش روز").performScrollTo().performClick()
  rule.onNodeWithText("همهٔ اطلاعات روز را ثبت کرده‌ام").performScrollTo().performClick()
  rule.onNodeWithText("باز کردن روز برای ثبت").assertExists()
  rule.runOnIdle{
   val s=rule.activity.store;assertTrue(s.day(LocalDate.now()).getBoolean("complete"))
   assertEquals(0,s.day(LocalDate.now()).objects("entries").size)
   assertTrue(Store(rule.activity).day(LocalDate.now()).getBoolean("complete"))
   s.restore(s.root.toString())
  }
  rule.onNodeWithText("باز کردن روز برای ثبت").performClick()
  rule.runOnIdle{val s=rule.activity.store;s.mutate(LocalDate.now(),null){it.put("sitting",0)}}
  rule.onNodeWithText("همهٔ اطلاعات روز را ثبت کرده‌ام").performScrollTo().performClick()
  rule.runOnIdle{
   val s=rule.activity.store;assertTrue(s.day(LocalDate.now()).getBoolean("complete"))
   assertFalse(Domain.coverage(s.day(LocalDate.now())))
  }
 }

}
