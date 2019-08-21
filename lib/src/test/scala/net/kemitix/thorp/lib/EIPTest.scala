package net.kemitix.thorp.lib

import org.scalatest.FreeSpec
import zio.stream._
import zio.{DefaultRuntime, IO, Queue, ZIO}

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
    type Message = Int
    type Channel = zio.Queue[Message]
    def producer(channel: Channel): ZIO[Any, Throwable, Unit] = {
      for {
        _ <- ZIO.foreach(1 to 3)(i => {
          println(s"put  $i")
          channel.offer(i)
        })
      } yield ()
    }
    def consumer(channel: Channel): ZIO[Any, Throwable, Unit] = {
      for {
        message <- channel.take
        _       <- ZIO(println(s"took $message"))
      } yield ()
    }
    val program = for {
      channel <- zio.Queue.bounded[Int](1)
      _       <- ZIO.forkAll(List(producer(channel), consumer(channel)))
    } yield ()
    new DefaultRuntime {}.unsafeRunSync(program)
  }

}
