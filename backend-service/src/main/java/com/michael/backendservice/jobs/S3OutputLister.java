package com.michael.backendservice.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class S3OutputLister {

    private static final Logger log = LoggerFactory.getLogger(S3OutputLister.class);

    private final S3Client s3Client;
    private final S3Presigner presigner;

    public S3OutputLister(S3Client s3Client, S3Presigner presigner) {
        this.s3Client = s3Client;
        this.presigner = presigner;
    }

    public List<OutputFile> listOutputFiles(String bucket, String outputPath) {
        List<OutputFile> files = new ArrayList<>();

        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(outputPath)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            if (response.contents() == null || response.contents().isEmpty()) {
                log.warn("No objects found at s3://{}/{}", bucket, outputPath);
                return files;
            }

            for (S3Object obj : response.contents()) {
                String key = obj.key();

                // Skip directory markers and internal metadata
                if (key.endsWith("/") || key.endsWith("_SUCCESS") || key.contains("_metadata")) {
                    continue;
                }

                // Skip partition directories (processed_year=*, etc.)
                if (key.endsWith("=") || !key.contains(".")) {
                    continue;
                }

                String fileName = key.substring(key.lastIndexOf("/") + 1);
                String downloadUrl = generatePresignedUrl(bucket, key);

                files.add(new OutputFile(
                        fileName,
                        key,
                        obj.size(),
                        downloadUrl,
                        300
                ));
            }

            log.info("Listed {} output files from s3://{}/{}", files.size(), bucket, outputPath);
        } catch (Exception e) {
            log.error("Failed to list output files from s3://{}/{}: {}", bucket, outputPath, e.getMessage());
        }

        return files;
    }

    private String generatePresignedUrl(String bucket, String key) {
        try {
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(300))
                    .getObjectRequest(objectRequest)
                    .build();

            PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for s3://{}/{}: {}", bucket, key, e.getMessage());
            return null;
        }
    }
}