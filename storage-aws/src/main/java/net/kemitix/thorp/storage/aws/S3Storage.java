package net.kemitix.thorp.storage.aws;

import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import net.kemitix.thorp.domain.*;
import net.kemitix.thorp.storage.Storage;
import net.kemitix.thorp.uishell.UploadEventListener;

import java.util.function.Function;

public class S3Storage implements Storage {

    private final AmazonS3Client client =
            AmazonS3Client.create(AmazonS3ClientBuilder.standard().build());
    private final S3TransferManager transferManager =
            S3TransferManager.create(
                    TransferManagerBuilder.defaultTransferManager());
    private final Function<PutObjectRequest, StorageEvent> uploader =
            S3Uploader.uploader(transferManager);
    private final Function<ListObjectsV2Request, RemoteObjects> lister =
            S3Lister.lister(client);
    private final Function<CopyObjectRequest, StorageEvent> copier =
            S3Copier.copier(client);
    private final Function<DeleteObjectRequest, StorageEvent> deleter =
            S3Deleter.deleter(client);

    @Override
    public RemoteObjects list(
            Bucket bucket,
            RemoteKey prefix
    ) {
        return lister.apply(S3Lister.request(bucket, prefix));
    }

    @Override
    public StorageEvent upload(
            LocalFile localFile,
            Bucket bucket,
            UploadEventListener.Settings listener
    ) {
        return uploader.apply(S3Uploader.request(localFile, bucket));
    }

    @Override
    public StorageEvent copy(
            Bucket bucket,
            RemoteKey sourceKey,
            MD5Hash hash,
            RemoteKey targetKey
    ) {
        return copier.apply(S3Copier.request(bucket, sourceKey, hash, targetKey));
    }

    @Override
    public StorageEvent delete(
            Bucket bucket,
            RemoteKey remoteKey
    ) {
        return deleter.apply(S3Deleter.request(bucket, remoteKey));
    }
}
