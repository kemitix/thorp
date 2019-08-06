package net.kemitix.thorp.config

import java.util.concurrent.atomic.AtomicReference

import net.kemitix.thorp.domain.{Bucket, Filter, RemoteKey, Sources}
import zio.{UIO, ZIO}

trait Config {
  val config: Config.Service
}

object Config {

  trait Service {
    def setConfiguration(config: Configuration): ZIO[Config, Nothing, Unit]
    def isBatchMode: ZIO[Config, Nothing, Boolean]
    def bucket: ZIO[Config, Nothing, Bucket]
    def prefix: ZIO[Config, Nothing, RemoteKey]
    def sources: ZIO[Config, Nothing, Sources]
    def filters: ZIO[Config, Nothing, List[Filter]]
  }

  trait Live extends Config {

    val config: Service = new Service {
      private val configRef = new AtomicReference(Configuration.empty)
      override def setConfiguration(
          config: Configuration): ZIO[Config, Nothing, Unit] =
        UIO(configRef.set(config))

      override def bucket: ZIO[Config, Nothing, Bucket] =
        UIO(configRef.get).map(_.bucket)

      override def sources: ZIO[Config, Nothing, Sources] =
        UIO(configRef.get).map(_.sources)

      override def prefix: ZIO[Config, Nothing, RemoteKey] =
        UIO(configRef.get).map(_.prefix)

      override def isBatchMode: ZIO[Config, Nothing, Boolean] =
        UIO(configRef.get).map(_.batchMode)

      override def filters: ZIO[Config, Nothing, List[Filter]] =
        UIO(configRef.get).map(_.filters)
    }
  }

  object Live extends Live

  final def set(config: Configuration): ZIO[Config, Nothing, Unit] =
    ZIO.accessM(_.config setConfiguration config)

  final def batchMode: ZIO[Config, Nothing, Boolean] =
    ZIO.accessM(_.config isBatchMode)

  final def bucket: ZIO[Config, Nothing, Bucket] =
    ZIO.accessM(_.config bucket)

  final def prefix: ZIO[Config, Nothing, RemoteKey] =
    ZIO.accessM(_.config prefix)

  final def sources: ZIO[Config, Nothing, Sources] =
    ZIO.accessM(_.config sources)

  final def filters: ZIO[Config, Nothing, List[Filter]] =
    ZIO.accessM(_.config filters)
}
