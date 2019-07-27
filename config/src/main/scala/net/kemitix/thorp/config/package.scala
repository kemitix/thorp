package net.kemitix.thorp

import zio.ZIO

package object config {

  final val configService: ZIO[Config, Nothing, Config.Service] =
    ZIO.access(_.config)

}
