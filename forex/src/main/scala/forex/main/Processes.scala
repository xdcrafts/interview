package forex.main

import forex.config._
import forex.services.OneForge
import forex.services.oneforge.components.OneForgeComponent
import forex.{ processes ⇒ p }
import org.zalando.grafter.macros._

@readerOf[ApplicationConfig]
case class Processes(
    oneForgeComponent: OneForgeComponent
) {

  implicit val _oneForge: OneForge[AppEffect] = oneForgeComponent.oneForge

  final val Rates = p.Rates[AppEffect]

}
