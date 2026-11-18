package com.raulcedroj.football_predictions.avatar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import java.net.URI;
import java.nio.file.Path;

@Configuration
class AvatarConfig {

    private static final Logger log = LoggerFactory.getLogger(AvatarConfig.class);

    @Bean
    AvatarStorage avatarStorage(AvatarProperties properties) {
        if (!properties.usesR2()) {
            Path directory = Path.of(System.getProperty("java.io.tmpdir"), "golify-avatars");
            log.warn("Cloudflare R2 is not configured: avatars are stored in {}", directory);
            return new LocalAvatarStorage(directory);
        }

        AvatarProperties.R2 r2 = properties.r2();
        // Configuración recomendada por Cloudflare para el SDK de AWS en Java
        S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(r2.endpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2.accessKeyId(), r2.secretAccessKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
        return new R2AvatarStorage(s3, r2.bucket(), properties.publicBaseUrl());
    }

    /** Solo en local: sirve la carpeta de fotos en /api/avatars/** (en producción las sirve Cloudflare). */
    @Bean
    WebMvcConfigurer localAvatarResources(AvatarStorage storage) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                if (storage instanceof LocalAvatarStorage local) {
                    registry.addResourceHandler(LocalAvatarStorage.URL_PREFIX + "**")
                            .addResourceLocations(local.directory().toUri().toString());
                }
            }
        };
    }
}
