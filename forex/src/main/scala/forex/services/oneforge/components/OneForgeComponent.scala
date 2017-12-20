package forex.services.oneforge.components

import forex.main.AppEffect
import forex.services.OneForge
import org.zalando.grafter.macros.defaultReader

@defaultReader[OneForgeLive]
trait OneForgeComponent {
  val oneForge: OneForge[AppEffect]
}
