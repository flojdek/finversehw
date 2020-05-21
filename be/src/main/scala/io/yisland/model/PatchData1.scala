package io.yisland.model

import play.api.libs.json.Json

case class PatchData1(columnName: String, newValue: String)

object PatchData1 {
  implicit val reads = Json.reads[PatchData1]
}
