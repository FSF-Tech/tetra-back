package org.fsf.tetra

import org.fsf.tetra.model.config.config.{ loadConfig, AppConfig }
import org.fsf.tetra.module.db.userRepository._
import org.fsf.tetra.module.logger.logger.{ Logger => AppLogger }
import org.fsf.tetra.route.UserRoute
import org.http4s.HttpApp
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.server.middleware.Logger

import cats.effect.{ ConcurrentEffect, ExitCode }
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import types._

import zio.interop.catz._
import zio.{ Runtime, ZEnv, ZIO }

object Main extends App {
  val rt = Runtime.default

  private val userRoute = new UserRoute[AppEnvironment]
  private val yaml      = userRoute.getEndPoints.toOpenAPI("User", "1.0").toYaml
  private val httpApp =
    Router("/" -> userRoute.getRoutes, "/docs" -> new SwaggerHttp4s(yaml).routes[AppTask]).orNotFound
  private val finalHttpApp = Logger.httpApp[AppTask](true, true)(httpApp)

  val env = ZEnv.live ++ UserRepository.live ++ AppLogger.live

  def runHttp(app: HttpApp[AppTask], cfg: AppConfig) = { implicit rts: ConcurrentEffect[AppTask] =>
    BlazeServerBuilder[AppTask]
      .bindHttp(cfg.server.port, cfg.server.host)
      .withHttpApp(CORS(app))
      .serve
      .compile[AppTask, AppTask, ExitCode]
      .drain
  }

  val prog = ZIO.fromEither(loadConfig).map(_ => runHttp(finalHttpApp, _)) //.provideCustomLayer(env)

  rt.unsafeRun(prog)
}
