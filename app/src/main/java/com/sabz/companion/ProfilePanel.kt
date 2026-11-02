package com.sabz.companion
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*

@Composable fun ProfilePanel(s:Store,values:MutableMap<String,String>,sex:String,level:String,onSex:(String)->Unit,onLevel:(String)->Unit,onChange:(String,String)->Unit,onApply:(Profile.Estimate)->Unit){
 Panel{
  Heading(s.t("مشخصات اولیه و برآورد هدف","Starting profile & goal estimate"))
  Hint(s.t("قد، وزن و سن با هم در برآورد کالری روزانه استفاده می‌شوند.","Height, weight and age are used together to estimate daily calories."))
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   listOf("height" to s.t("قد (سانتی‌متر)","Height (cm)"),"weight" to s.t("وزن (کیلوگرم)","Weight (kg)"),"age" to s.t("سن (سال)","Age (years)")).forEach{(key,label)->
    OutlinedTextField(values[key].orEmpty(),{onChange(key,it)},label={Text(label,fontSize=11.sp)},keyboardOptions=KeyboardOptions(keyboardType=if(key=="age")KeyboardType.Number else KeyboardType.Decimal),singleLine=true,modifier=Modifier.weight(1f))
   }
  }
  ChoiceGroup(s.t("جنسیت برای فرمول کالری (اختیاری)","Sex for calorie equation (optional)"),sex,listOf("male" to s.t("مرد","Male"),"female" to s.t("زن","Female"),"unspecified" to s.t("انتخاب نمی‌کنم","Not specified")),onSex)
  ChoiceGroup(s.t("سطح فعالیت معمول","Usual activity level"),level,listOf("inactive" to s.t("کم‌تحرک","Inactive"),"low" to s.t("کمی فعال","Low active"),"active" to s.t("فعال","Active"),"very" to s.t("بسیار فعال","Very active")),onLevel)
  Hint(s.t("سطح فعالیت مربوط به کل روز است، نه فقط ورزش ثبت‌شده.","Activity level describes your whole day, not just logged exercise."))
  val h=values["height"]?.inputNumber()?.toDoubleOrNull();val w=values["weight"]?.inputNumber()?.toDoubleOrNull();val a=values["age"]?.inputNumber()?.toIntOrNull()
  val valid=h!=null&&w!=null&&a!=null&&h in 100.0..250.0&&w in 30.0..300.0&&a in 19..120
  val estimate=if(valid&&sex!="unspecified")runCatching{Profile.estimate(h!!,w!!,a!!,sex,level)}.getOrNull() else null
  if(estimate!=null){
   Text(s.t("برآورد حفظ وزن: ","Estimated maintenance: ")+s.num(estimate.calories)+" kcal / "+s.t("روز","day"))
   Text(s.t("مرجع پروتئین: ","Protein reference: ")+s.num(estimate.protein,1)+" "+s.t("گرم در روز","g/day"))
   Button(onClick={onApply(estimate)},modifier=Modifier.fillMaxWidth()){Text(s.t("استفاده از هدف‌های پیشنهادی","Use estimated targets"))}
  }else if(valid&&sex=="unspecified"){
   val lower=Profile.estimate(h!!,w!!,a!!,"female",level).calories
   val upper=Profile.estimate(h,w,a,"male",level).calories
   Hint(s.t("بازهٔ دو فرمول کالری: ","Range across the two calorie equations: ")+s.num(minOf(lower,upper))+"–"+s.num(maxOf(lower,upper))+" kcal / "+s.t("روز. برای یک عدد، فرمول را انتخاب کن یا هدف خودت را وارد کن.","day. Choose an equation for one estimate, or enter your own target."))
  }else Hint(s.t("برای برآورد: قد ۱۰۰–۲۵۰ سانتی‌متر، وزن ۳۰–۳۰۰ کیلوگرم و سن ۱۹ سال یا بیشتر را وارد کن.","To estimate: enter height 100–250 cm, weight 30–300 kg and age 19 years or above."))
  Hint(s.t("برآورد عمومی برای حفظ وزن بزرگسالان ۱۹ سال به بالا، خارج از بارداری و شیردهی؛ هدف کاهش وزن نیست. مرجع: DRI 2023 / Health Canada. پروتئین: ۰٫۸ گرم به ازای هر کیلوگرم وزن. هدف نهایی را بررسی و در پایین ذخیره کن.","General adult maintenance estimate for ages 19+, excluding pregnancy and breastfeeding; not a weight-loss target. Source: 2023 DRI / Health Canada. Protein: 0.8 g/kg body weight. Review and save your final goals below."))
 }
}
