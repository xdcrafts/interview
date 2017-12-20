package forex.main

import forex.config._
import forex.services.oneforge.components._
import org.zalando.grafter.macros._
import org.zalando.grafter.syntax.rewriter._

@readerOf[ApplicationConfig]
case class Application(
    api: Api,
    oneForge: OneForgeImpl
) {
  def configure(): Application =
    this
      .replace[OneForgeComponent](oneForge)
      .singletons
}
