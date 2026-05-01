package com.maestro.po.ms.inference.provider;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.textract.TextractClient;

/**
 * Spring configuration that creates and exposes AWS SDK client beans:
 * {@link BedrockRuntimeAsyncClient}, {@link S3Client}, and {@link TextractClient}.
 * Supports both local profile-based credentials and IAM role assumption via STS.
 *
 * @author Ganesh Punde
 */
@Configuration
public class AwsClientConfiguration
{
    @Value("${maestro.system.profile:default}")
    private String profile;
    
    @Value("${maestro.system.region:us-east-1}")
    private String region;
    
    @Value("${maestro.system.local:false}")
    private boolean local;
    
    @Value("${maestro.system.roleArn:default-role}")
    private String roleArn;
    
    @Value("${maestro.system.sessionName:session-name}")
    private String sessionName;
    
    @Value("${maestro.system.connectionTimeout:30}")
    private long connectionTimeout;
    
    @Value("${maestro.system.readTimeout:30}")
    private long readTimeout;
    
    @Value("${maestro.system.connectionAquisitionTimeout:30}")
    private long connectionAquisitionTimeout;
    
    @Value("${maestro.system.maxIdleTime:30}")
    private long maxIdleTime;
    
    @Value("${maestro.system.maxConcurrency:50}")
    private int maxConcurrency;

    @Bean
    public BedrockRuntimeAsyncClient asyncBedrockRuntimeClient()
    {
        SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder()
                .connectionTimeout(Duration.ofMinutes(connectionTimeout)).connectionAcquisitionTimeout(Duration.ofMinutes(connectionAquisitionTimeout))
                .connectionMaxIdleTime(Duration.ofMinutes(maxIdleTime)).readTimeout(Duration.ofMinutes(readTimeout))
                .maxConcurrency(maxConcurrency).build();
        
        return BedrockRuntimeAsyncClient.builder()
        .region(Region.of(region))
        .httpClient(httpClient)
        .credentialsProvider(buildCredentialsProvider())
        .build();
    }
    
    @Bean
    public S3Client s3Client()
    {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(buildCredentialsProvider())
                .build();
    }
    
    @Bean
    public TextractClient textractClient()
    {
        return TextractClient.builder()
                .region(Region.of(region))
                .credentialsProvider(buildCredentialsProvider())
                .build();
    }
    
    private AwsCredentialsProvider buildCredentialsProvider() {
        
        if (local)
        {            
            return ProfileCredentialsProvider           
                    .create(profile);
        }
        
        AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder().roleArn(roleArn)
                .roleSessionName(sessionName).build();

        try (StsClient stsClient = StsClient.builder().region(Region.of(region)).build())
        {
            return StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(stsClient)
                    .refreshRequest(assumeRoleRequest)
                    .asyncCredentialUpdateEnabled(true)
                    .build();
        }
    }
}