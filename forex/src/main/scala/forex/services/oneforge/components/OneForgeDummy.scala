package forex.services.oneforge.components

import cats.Eval
import forex.config.ApplicationConfig
import forex.main.{ AppEffect, AppStack }
import forex.services.OneForge
import forex.services.oneforge.algebra.Interpreters
import org.zalando.grafter.macros.readerOf
import org.zalando.grafter.{ Start, StartResult }

@readerOf[ApplicationConfig]
class OneForgeDummy() extends OneForgeComponent with Start {
  override final lazy val oneForge: OneForge[AppEffect] = Interpreters.dummy[AppStack]

  override def start: Eval[StartResult] = StartResult.eval("OneForgeDummy") {}
}
