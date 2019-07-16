package net.kemitix.thorp.domain

object MD5HashData {

  object Root {
    val hash   = MD5Hash("a3a6ac11a0eb577b81b3bb5c95cc8a6e")
    val base64 = "o6asEaDrV3uBs7tclcyKbg=="
  }
  object Leaf {
    val hash   = MD5Hash("208386a650bdec61cfcd7bd8dcb6b542")
    val base64 = "IIOGplC97GHPzXvY3La1Qg=="
  }
  object BigFile {
    val hash = MD5Hash("b1ab1f7680138e6db7309200584e35d8")
    object Part1 {
      val offset = 0
      val size   = 1048576
      val hash   = MD5Hash("39d4a9c78b9cfddf6d241a201a4ab726")
    }
    object Part2 {
      val offset = 1048576
      val size   = 1048576
      val hash   = MD5Hash("af5876f3a3bc6e66f4ae96bb93d8dae0")
    }
  }

}
