package net.kemitix.s3thorp

sealed trait S3Action
case class UploadS3Action() extends S3Action
case class CopyS3Action() extends S3Action
case class MoveS3Action() extends S3Action
case class DeleteS3Action() extends S3Action