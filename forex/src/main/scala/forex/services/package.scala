package forex

import forex.services.oneforge.algebra.{ Algebra, Error }

package object services {

  type OneForge[F[_]] = Algebra[F]
  type OneForgeError = Error
  final val OneForgeError = oneforge.algebra.Error

}
