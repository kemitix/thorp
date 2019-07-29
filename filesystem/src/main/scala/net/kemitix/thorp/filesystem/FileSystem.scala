package net.kemitix.thorp.filesystem

trait FileSystem {
  val filesystem: FileSystem.Service
}

object FileSystem {
  trait Service {}
  trait Live extends FileSystem {
    override val filesystem: Service = new Service {}
  }
  trait Test extends FileSystem {
    override val filesystem: Service = new Service {}
  }
}
