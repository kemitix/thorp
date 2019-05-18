package net.kemitix.s3thorp

import java.io.File

sealed trait Action
case class ToUpload(file: File) extends Action
