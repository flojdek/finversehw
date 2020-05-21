package io.yisland.service

import anorm._
import io.yisland.model.{Column, DT, PatchData1, PostData1, Spec1}
import io.yisland.util.Util
import javax.inject._
import play.api.db.Database

class Spec1Service @Inject () (db: Database) {

  // PUBLIC

  def getData1(inputFilters: Map[String, String]) = {
    val (spec1s, columnNameToColumnNumber, columnNameToColumnType, columnNumberToColumnName, columnNumberToColumnType, validColumnNames) = getSpec1MetaData()

    val validFilterKeys = validColumnNames
    val actualFiltersStr = Util.mkSqlFilterStr(validFilterKeys, inputFilters.filterKeys(validFilterKeys.contains), columnNameToColumnNumber, columnNameToColumnType)

    // Explicitly select columns as we don't want `row` in the return result set.
    val selectColumnsStr = spec1s.map(sp => s"column_${sp.columnNum}").mkString(",")

    Util.collectResultSetIntoJson({
      db.withConnection { implicit conn =>
        SQL(s"SELECT $selectColumnsStr FROM $data1TableName $actualFiltersStr").as(Util.anormDynamicRowParser(data1TableName) *)
      }
    }, columnNumberToColumnType, columnNumberToColumnName)
  }

  def patchData1(patchData1: PatchData1, inputFilters: Map[String, String]): Either[Seq[String], Unit] = {

    val (_, columnNameToColumnNumber, columnNameToColumnType, _, _, validColumnNames) = getSpec1MetaData()

    val errors = Util.validate(Seq(Column(patchData1.columnName, patchData1.newValue)), validColumnNames, columnNameToColumnType, validateMissingColumns = false)
    if (errors.nonEmpty) {
      Left(errors)
    } else {
      val actualFiltersStr = Util.mkSqlFilterStr(validColumnNames, inputFilters.filterKeys(validColumnNames.contains), columnNameToColumnNumber, columnNameToColumnType)

      val updateColumnStr = s"column_${columnNameToColumnNumber(patchData1.columnName)}"
      val newValue = Util.columnValueToSqlLiteral(patchData1.columnName, patchData1.newValue, columnNameToColumnType)
      val result = db.withConnection { implicit conn =>
        SQL(s"UPDATE $data1TableName SET $updateColumnStr = $newValue $actualFiltersStr").executeUpdate()
      }

      Right(())
    }

  }

  def postData1(postData1: PostData1): Either[Seq[String], Unit] = {

    val (_, columnNameToColumnNumber, columnNameToColumnType, _, _, validColumnNames) = getSpec1MetaData()

    val errors = Util.validate(postData1.data, validColumnNames, columnNameToColumnType)
    if (errors.nonEmpty) {
      Left(errors)
    } else {
      val values = postData1.data.map { case Column(columnName, columnValue) =>
        columnNameToColumnType(columnName) match {
          case DT.TEXT => (columnNameToColumnNumber(columnName), s"'$columnValue'")
          case _ => (columnNameToColumnNumber(columnName), columnValue)
        }
      }.sortBy(_._1).map(_._2)

      db.withConnection { implicit conn =>
        SQL(s"INSERT INTO $data1TableName VALUES ((SELECT MAX(row) + 1 FROM $data1TableName), ${values.mkString(",")})").executeInsert()
      }

      Right(())
    }

  }

  // PRIVATE

  private val spec1TableName = "spec1"
  private val data1TableName = "data1"

  private def getSpec1s(): Seq[Spec1] = {
    db.withConnection { implicit conn =>
      SQL(s"SELECT * FROM $spec1TableName").as(Spec1.rowParser *)
    }
  }

  private def getSpec1MetaData() = {
    val spec1s = getSpec1s()
    val colNameToColNum = spec1s.map(e => e.columnName -> e.columnNum).toMap
    val colNameToColType = spec1s.map(e => e.columnName -> e.dataType).toMap
    val colNumToColName = spec1s.map(e => e.columnNum -> e.columnName).toMap
    val colNumToColType = spec1s.map(e => e.columnNum -> e.dataType).toMap
    val validColumnNames = spec1s.map(_.columnName)
    (spec1s, colNameToColNum, colNameToColType, colNumToColName, colNumToColType, validColumnNames)
  }

}
