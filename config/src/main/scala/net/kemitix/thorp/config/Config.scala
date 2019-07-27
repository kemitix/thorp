package net.kemitix.thorp.config

trait Config {
  val config: Config.Service
}

object Config {

  trait Service {}

  class Live(args: List[String]) extends Config {

    CliArgs.parse(args)

    val config: Service = new Service {}
  }

}
