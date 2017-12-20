package forex.services

import forex.domain.Rate

package object oneforge {
  type OneForgeCache = Map[Rate.Pair, Rate]
}
