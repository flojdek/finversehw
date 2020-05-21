package io.yisland.model

import anorm._
import anorm.SqlParser.get
import play.api.libs.json.Json

case class Spec1(
  columnNum: Int,
  columnName: String,
  dataType: String
)

object Spec1 {

  implicit val writes = Json.writes[Spec1]

  val rowParser: RowParser[Spec1] = {
    get[Int]("column_num") ~ get[String]("column_name") ~ get[String]("data_type") map {
      case a ~ b ~ c => Spec1(a, b, c)
    }
  }

}
