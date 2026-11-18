package com.raulcedroj.football_predictions.avatar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class R2AvatarStorage implements AvatarStorage, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(R2AvatarStorage.class);

    private final S3Client s3;
    private final String bucket;
    private final String publicBaseUrl;

    R2AvatarStorage(S3Client s3, String bucket, String publicBaseUrl) {
        this.s3 = s3;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    @Override
    public String store(String key, byte[] jpeg) {
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("image/jpeg")
                        // Cada foto tiene un nombre único: el navegador y Cloudflare la pueden guardar para siempre
                        .cacheControl("public, max-age=31536000, immutable")
                        .build(),
                RequestBody.fromBytes(jpeg));
        return publicBaseUrl + "/" + key;
    }

    @Override
    public void delete(String publicUrl) {
        String prefix = publicBaseUrl + "/";
        if (publicUrl == null || !publicUrl.startsWith(prefix)) {
            log.warn("Not deleting avatar {}: it does not belong to {}", publicUrl, publicBaseUrl);
            return;
        }
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(publicUrl.substring(prefix.length()))
                .build());
    }

    @Override
    public void close() {
        s3.close();
    }
}
