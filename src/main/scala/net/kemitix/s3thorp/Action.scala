package net.kemitix.s3thorp

sealed trait Action
case class ToUpload(localFile: LocalFile) extends Action
