package net.kemitix.thorp.config

trait Config {
  val config: Config.Service
}

object Config {

  trait Service {}

  trait Live extends Config {
    val config: Service = new Service {}
  }

  object Live extends Live

}
