package net.kemitix.thorp.lib

import org.scalatest.FreeSpec
import zio.stream._
import zio.{DefaultRuntime, IO, UIO, ZIO}

// Experiment on how to trigger events asynchronously
// i.e. Have one thread add to a queue and another take from the queue, neither waiting for the other thread to finish
class EIPTest extends FreeSpec {

  "queue" in {
    type Callback[A] = IO[Option[Throwable], A] => Unit
    def offerInt(cb: Callback[Int], i: Int) = ZIO(cb(IO.succeed(i)))
    def closeStream(cb: Callback[Int])      = ZIO(cb(IO.fail(None)))
    def publish: Callback[Int] => IO[Throwable, _] =
      cb =>
        ZIO.foreach(1 to 3) { i =>
          ZIO {
            println(s"put $i")
            Thread.sleep(100)
          } *> offerInt(cb, i)
        } *> closeStream(cb)
    val program = Stream
      .effectAsyncM(publish)
      .mapM(i => ZIO(println(s"get $i")))

    new DefaultRuntime {}.unsafeRunSync(program.runDrain)
  }

  "EIP: Message Channel" in {
    type Message  = Int
    type Callback = IO[Option[Throwable], Message] => Unit
    def producer: Callback => UIO[Unit] =
      cb =>
        ZIO.foreach(1 to 3)(message =>
          UIO {
            println(s"put $message")
            cb(ZIO.succeed(message))
            Thread.sleep(100)
        }) *> UIO(cb(ZIO.fail(None)))
    def consumer: Message => ZIO[Any, Throwable, Unit] =
      message => ZIO(println(s"got $message"))
    val program = zio.stream.Stream
      .effectAsyncM(producer)
      .buffer(1)
      .mapM(consumer)
    new DefaultRuntime {}.unsafeRunSync(program.runDrain)
  }

}
