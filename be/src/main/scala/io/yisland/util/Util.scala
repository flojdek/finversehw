package io.yisland.util

import anorm.{RowParser, SqlParser}
import io.yisland.model.{Column, DT}
import io.yisland.model.DT._
import play.api.libs.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString}

import scala.util.{Failure, Try}

// FIXME: Perfect for unit testing as all are pure functions.
object Util {

  def columnValueToSqlLiteral(
    columnName: String,
    columnValue: String,
    columnNameToColumnType: Map[String, String]
  ) = {
    columnNameToColumnType(columnName) match {
      case "TEXT" => s"'$columnValue'"
      case _ => columnValue
    }
  }

  def mkSqlFilterStr(
    validFilterKeys: Seq[String],
    inputFilters: Map[String, String],
    columnNameToColumnNumber: Map[String, Int],
    columnNameToColumnType: Map[String, String]
  ) = {
    val actualFilters = inputFilters.filterKeys(validFilterKeys.contains)
    val tmp = actualFilters.map { case (colName, colValue) =>
      val base = s"column_${columnNameToColumnNumber(colName)} = "
      columnNameToColumnType(colName) match {
        case TEXT => base + Util.columnValueToSqlLiteral(colName, colValue, columnNameToColumnType)
        case _ => base + Util.columnValueToSqlLiteral(colName, colValue, columnNameToColumnType)
      }
    }.mkString(" AND ")
    if (tmp.trim.isEmpty) "" else " WHERE " + tmp
  }

  def anormDynamicRowParser(
    tableNamePrefixToStrip: String
  ): RowParser[Map[String, Any]] = SqlParser.folder(Map.empty[String, Any]) { (map, value, meta) =>
    Right(map + (meta.column.qualified.substring(tableNamePrefixToStrip.length + 1) -> value))
  }

  def collectResultSetIntoJson(
    columnNameToValueCollection: List[Map[String, Any]],
    columnNumberToColumnType: Map[Int, String],
    columnNumberToColumnName: Map[Int, String],
  ) = {
    columnNameToValueCollection.foldLeft(JsArray.empty) { case (acc, row) =>
      val rowJson = JsObject(row.map { case (colNum, colValue) =>
        val number = colNum.substring("column_".length).toInt
        columnNumberToColumnType(number) match {
          case DT.TEXT => columnNumberToColumnName(number) -> JsString(colValue.asInstanceOf[String])
          case DT.BOOL => columnNumberToColumnName(number) -> JsBoolean(colValue.asInstanceOf[Boolean])
          case DT.INT => columnNumberToColumnName(number) -> JsNumber(colValue.asInstanceOf[Int])
        }
      })
      acc.append(rowJson)
    }
  }

  def validate(
    columns: Seq[Column],
    validColumnNames: Seq[String],
    columnNameToColumnType: Map[String, String],
    validateMissingColumns: Boolean = true
  ) = {

    var errors = columns.flatMap { case Column(columnName, columnValue) =>
      if (!validColumnNames.contains(columnName)) {
        Some(s"error.validation.column-does-not-exist: columnName='$columnName'")
      } else {
        columnNameToColumnType(columnName) match {
          case "TEXT" =>
            None
          case "BOOLEAN" =>
            val normalized = columnValue.trim.toLowerCase
            if (normalized == "true" || normalized == "false") {
              None
            } else {
              Some(s"error.validation.invalid-boolean-expression: columnName='$columnName', columnValue='$columnValue'")
            }
          case "INTEGER" =>
            Try(Integer.parseInt(columnValue.trim)) match {
              case Failure(e) =>
                Some(s"error.validation.invalid-integer-expression: columnName='$columnName', columnValue='$columnValue'")
              case _ =>
                None
            }
        }
      }
    }

    if (validateMissingColumns) {
      val diff = validColumnNames.toSet diff columns.map(_.name).toSet
      if (diff.nonEmpty) {
        errors ++= Seq(s"error.validation.missing-columns: missingColumns='${diff.mkString(",")}'")
      }
    }

    errors
  }

}
