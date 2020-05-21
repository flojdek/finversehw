package io.yisland.model

import play.api.libs.json.Json

case class Column(name: String, value: String)

object Column {
  implicit val reads = Json.reads[Column]
}

case class PostData1(data: Seq[Column])

object PostData1 {
  implicit val reads = Json.reads[PostData1]
}
