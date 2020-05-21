package io.yisland

import io.yisland.model.{Column, PatchData1, PostData1}
import io.yisland.service.Spec1Service
import org.scalatest.FlatSpec
import play.api.db.Databases
import play.api.db.evolutions._
import play.api.libs.json.{JsBoolean, JsNumber, JsObject, JsString, JsValue, Json}

class Spec1ServiceTest extends FlatSpec {

  // FIXME: Obviously I wouldn't keep credentials ever in code. This is just for time reasons.
  val db = Databases(
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql://localhost:5435/fvdb?currentSchema=public",
    name = "fvdb",
    config = Map(
      "username" -> "xxx",
      "password" -> "xxx"
    )
  )

  behavior of "getData1"

  it should "find data entries by filtering" in Evolutions.withEvolutions(db) {
    val spec1Service = new Spec1Service(db)
    val result1 = spec1Service.getData1(Map("name" -> "Apple"))
    assert(result1.value.size == 1)
    assert(result1.value(0).fieldValue("name").as[JsString].value.equals("Apple"))
    assert(result1.value(0).fieldValue("valid").as[JsBoolean].value.equals(true))
    assert(result1.value(0).fieldValue("count").as[JsNumber].value.equals(1))
    val result2 = spec1Service.getData1(Map("valid" -> "false"))
    assert(result2.value.size == 1)
    assert(result2.value(0).fieldValue("name").as[JsString].value.equals("Banana"))
    assert(result2.value(0).fieldValue("valid").as[JsBoolean].value.equals(false))
    assert(result2.value(0).fieldValue("count").as[JsNumber].value.equals(-12))
  }

  behavior of "patchData1"

  // FIXME: All the other tests for pretty much all the patch validation fail scenarios need to be written here. Just one for demonstration.
  it should "not patch existing data if there is an integer parse error" in Evolutions.withEvolutions(db) {
    val spec1Service = new Spec1Service(db)
    val result1 = spec1Service.patchData1(PatchData1(columnName = "count", newValue = "0.5"), Map("name" -> "Apple"))
    assert(result1.equals(Left(List("error.validation.invalid-integer-expression: columnName='count', columnValue='0.5'"))))
  }

  it should "patch existing data successfully" in Evolutions.withEvolutions(db) {
    val spec1Service = new Spec1Service(db)
    spec1Service.patchData1(PatchData1(columnName = "name", newValue = "Orange"), Map("name" -> "Apple"))
    val result1 = spec1Service.getData1(Map("name" -> "Apple"))
    assert(result1.value.isEmpty)
    val result2 = spec1Service.getData1(Map("name" -> "Orange"))
    assert(result2.value.size == 1)
    assert(result2.value(0).fieldValue("valid").as[JsBoolean].value.equals(true))
    assert(result2.value(0).fieldValue("count").as[JsNumber].value.equals(1))
  }

  behavior of "postData1"

  // FIXME: All the other tests for pretty much all the insert validation fail scenarios need to be written here. Just one for demonstration.
  it should "not insert new data if there are missing columns" in Evolutions.withEvolutions(db) {
    val spec1Service = new Spec1Service(db)
    val result1 = spec1Service.postData1(PostData1(data = Seq(Column(name = "name", value = "Peach"))))
    assert(result1.equals(Left(List("error.validation.missing-columns: missingColumns='valid,count'"))))
  }

  it should "insert new data successfully" in Evolutions.withEvolutions(db) {
    val spec1Service = new Spec1Service(db)
    spec1Service.postData1(PostData1(data = Seq(
      Column(name = "name", value = "Peach"),
      Column(name = "valid", value = "true"),
      Column(name = "count", value = "10"),
    )))
    val result1 = spec1Service.getData1(Map("name" -> "Peach"))
    assert(result1.value.size == 1)
    assert(result1.value(0).fieldValue("valid").as[JsBoolean].value.equals(true))
    assert(result1.value(0).fieldValue("count").as[JsNumber].value.equals(10))
  }

  implicit class JsValueOps(value: JsValue) {
    def fieldValue(name: String) = value.as[JsObject].fields.find(_._1 == name).get._2
  }

}
