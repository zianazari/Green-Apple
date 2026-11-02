package com.sabz.companion

import kotlin.math.roundToInt

/** Adult 2023 DRI EER equations, as published by Health Canada. */
object Profile {
 const val SOURCE="https://www.canada.ca/en/health-canada/services/food-nutrition/healthy-eating/dietary-reference-intakes/tables/equations-estimate-energy-requirement.html"
 val activityLevels=listOf("inactive","low","active","very")
 data class Estimate(val calories:Double,val protein:Double)
 fun estimate(height:Double,weight:Double,age:Int,sex:String,level:String):Estimate {
  require(height.isFinite()&&height in 100.0..250.0)
  require(weight.isFinite()&&weight in 30.0..300.0)
  require(age in 19..120)
  require(sex in listOf("male","female"))
  val i=activityLevels.indexOf(level);require(i>=0)
  val coefficients=if(sex=="male")listOf(
   listOf(753.07,-10.83,6.50,14.10),listOf(581.47,-10.83,8.30,14.94),
   listOf(1004.82,-10.83,6.52,15.91),listOf(-517.88,-10.83,15.61,19.11)
  )else listOf(
   listOf(584.90,-7.01,5.72,11.71),listOf(575.77,-7.01,6.60,12.14),
   listOf(710.25,-7.01,6.54,12.34),listOf(511.83,-7.01,9.07,12.56)
  )
  val c=coefficients[i]
  return Estimate(c[0]+c[1]*age+c[2]*height+c[3]*weight,weight*.8)
 }
 fun roundedCalories(value:Double)=((value/50).roundToInt()*50).toDouble()
}
