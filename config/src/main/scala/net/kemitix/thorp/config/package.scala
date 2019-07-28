package net.kemitix.thorp

import net.kemitix.thorp.domain.{Bucket, Filter, RemoteKey, Sources}
import zio.ZIO

package object config {

  final val configService: ZIO[Config, Nothing, Config.Service] =
    ZIO.access(_.config)

  final def setConfig(config: LegacyConfig): ZIO[Config, Nothing, Unit] =
    ZIO.accessM(_.config setConfig config)

  final def isBatchMode: ZIO[Config, Nothing, Boolean] =
    ZIO.accessM(_.config isBatchMode)

  final def getBucket: ZIO[Config, Nothing, Bucket] =
    ZIO.accessM(_.config bucket)

  final def getPrefix: ZIO[Config, Nothing, RemoteKey] =
    ZIO.accessM(_.config prefix)

  final def getSources: ZIO[Config, Nothing, Sources] =
    ZIO.accessM(_.config sources)

  final def getFilters: ZIO[Config, Nothing, List[Filter]] =
    ZIO.accessM(_.config filters)
}
