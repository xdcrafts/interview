package forex

import cats.Eval
import com.typesafe.scalalogging._
import forex.config._
import forex.main._
import org.zalando.grafter._

object Main extends App with LazyLogging {

  var app: Option[Application] = None
  var started: Boolean = false

  pureconfig.loadConfig[ApplicationConfig]("app") match {
    case Left(errors) ⇒
      logger.error(s"Errors loading the configuration:\n${errors.toList.mkString("- ", "\n- ", "")}")
    case Right(applicationConfig) ⇒
      val application = configure[Application](applicationConfig).configure()

      Rewriter
        .startAll(application)
        .flatMap {
          case results if results.exists(!_.success) ⇒
            logger.error(toStartErrorString(results))
            logger.info(toStopString(Rewriter.stopAll(application).value))
            Eval.now(())
          case results ⇒
            started = true
            logger.info(toStartSuccessString(results))
            Eval.now {
              app = Some(application)
            }
        }
        .value
  }

  if (!started)
    System.exit(0)

  sys.addShutdownHook {
    app.foreach { application ⇒
      logger.info(toStopString(Rewriter.stopAll(application).value))
    }
  }

}
