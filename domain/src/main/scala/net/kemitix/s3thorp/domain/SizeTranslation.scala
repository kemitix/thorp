package net.kemitix.s3thorp.domain

object SizeTranslation {

  def sizeInEnglish(length: Long): String =
    length match {
      case bytes if bytes > 1024 * 1024 * 1024 => s"${bytes / 1024 / 1024 /1024}Gb"
      case bytes if bytes > 1024 * 1024 => s"${bytes / 1024 / 1024}Mb"
      case bytes if bytes > 1024 => s"${bytes / 1024}Kb"
      case bytes => s"${length}b"
    }


}
