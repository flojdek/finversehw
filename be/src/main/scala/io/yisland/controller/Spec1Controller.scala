package io.yisland.controller

import io.yisland.model.{PatchData1, PostData1}
import io.yisland.service.Spec1Service
import javax.inject._
import play.api.db.Database
import play.api.libs.json.{JsArray, JsString}
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class Spec1Controller @Inject() (db: Database, cc: ControllerComponents, spec1Service: Spec1Service)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def handleGetData1() = Action { implicit request =>
    val result = spec1Service.getData1(stripQueryString(request))
    Ok(result)
  }

  def handlePatchData1() = Action(parse.json) { implicit request =>
    val result = spec1Service.patchData1(request.body.validate[PatchData1].get, stripQueryString(request))
    if (result.isLeft) BadRequest(JsArray(result.left.get.map(JsString))) else Ok("")
  }

  def handlePostData1() = Action(parse.json) { implicit request =>
    val result = spec1Service.postData1(request.body.validate[PostData1].get)
    if (result.isLeft) BadRequest(JsArray(result.left.get.map(JsString))) else Ok("")
  }

  // PRIVATE

  private def stripQueryString[A](request: Request[A]) = {
    request.queryString.map { case (key, values) => (key, values.head) }
  }

}
