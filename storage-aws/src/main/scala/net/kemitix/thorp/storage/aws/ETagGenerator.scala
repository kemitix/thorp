package net.kemitix.thorp.storage.aws

import com.amazonaws.services.s3.transfer.{TransferManager, TransferManagerConfiguration}

trait ETagGenerator {

  import com.amazonaws.services.s3.model.PutObjectRequest

  private val origReq: PutObjectRequest = ???
  private val transferManager: TransferManager = ???
  private val configuration: TransferManagerConfiguration = transferManager.getConfiguration

  import com.amazonaws.services.s3.transfer.internal.TransferManagerUtils

  private val optimalPartSize = TransferManagerUtils.calculateOptimalPartSize(origReq, configuration)
  private val totalFileSizeBytes = ???
  private val remainingBytes = totalFileSizeBytes
  private val totalNumberOfParts = Math.ceil(remainingBytes.asInstanceOf[Double] / optimalPartSize).toInt

}
