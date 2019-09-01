package net.kemitix.thorp.lib

import org.scalatest.FreeSpec
import zio.{DefaultRuntime, Ref, ZIO}

class RefTest extends FreeSpec {

  "Ref can be updated in for-comprehension" in {
    val program: ZIO[Any, Nothing, Int] = for {
      ref <- Ref.make(0)
      _   <- ref.update(_ + 1)
      r   <- ref.get
    } yield r
    val result: Either[Throwable, Int] =
      new DefaultRuntime {}.unsafeRunSync(program).toEither
    assertResult(Right(1))(result)
  }

  "Ref can be updated in nested for-comprehension" in {
    def nested(nestedRef: Ref[Int]): ZIO[Any, Nothing, Unit] =
      for {
        x <- nestedRef.get
        _ <- nestedRef.update(_ + 1)
      } yield ()
    val program: ZIO[Any, Nothing, Int] = for {
      ref <- Ref.make(0)
      _   <- ZIO.foreach(1 to 2)(_ => nested(ref))
      r   <- ref.get
    } yield r
    val result: Either[Throwable, Int] =
      new DefaultRuntime {}.unsafeRunSync(program).toEither
    assertResult(Right(2))(result)
  }

}
