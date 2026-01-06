package com.voyageai.voyageaibackend.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Configuration class for AWS DynamoDB.
 * 
 * <p>This class provides configuration for connecting to AWS DynamoDB,
 * including credential management and client initialization.
 * 
 * <p>It supports multiple credential strategies:
 * <ul>
 *   <li>Explicit credentials from application.properties</li>
 *   <li>Default AWS credentials chain (environment variables, AWS CLI, IAM roles)</li>
 *   <li>Local DynamoDB endpoint for testing</li>
 * </ul>
 */
@Configuration
public class DynamoDbConfig {

  @Value("${aws.region:us-east-1}")
  private String awsRegion;

  @Value("${aws.access-key-id:}")
  private String accessKeyId;

  @Value("${aws.secret-access-key:}")
  private String secretAccessKey;

  @Value("${aws.dynamodb.endpoint:}")
  private String dynamoDbEndpoint;

  /**
   * Creates and configures a DynamoDB client.
   * 
   * <p>The client is configured with:
   * <ul>
   *   <li>AWS region from application.properties</li>
   *   <li>Credentials from application-secrets.properties or default provider chain</li>
   *   <li>Optional custom endpoint for local DynamoDB testing</li>
   * </ul>
   *
   * @return configured DynamoDB client
   */
  @Bean
  public DynamoDbClient dynamoDbClient() {
    software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder builder = 
        DynamoDbClient.builder().region(Region.of(awsRegion));

    // Use explicit credentials if provided, otherwise fall back to default provider chain
    if (!accessKeyId.isEmpty() && !secretAccessKey.isEmpty()) {
      AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
      builder.credentialsProvider(StaticCredentialsProvider.create(awsCreds));
    } else {
      // Falls back to environment variables, AWS CLI config, IAM roles, etc.
      builder.credentialsProvider(DefaultCredentialsProvider.create());
    }

    // Optional: Use local DynamoDB endpoint for testing
    if (!dynamoDbEndpoint.isEmpty()) {
      builder.endpointOverride(URI.create(dynamoDbEndpoint));
    }

    return builder.build();
  }

  /**
   * Creates a DynamoDB Enhanced Client for high-level ORM-like operations.
   * 
   * <p>The enhanced client provides an ORM-like experience with:
   * <ul>
   *   <li>Automatic marshalling/unmarshalling of Java objects to DynamoDB items</li>
   *   <li>Type-safe table definitions with annotations</li>
   *   <li>Simplified CRUD operations</li>
   * </ul>
   *
   * @param dynamoDbClient the low-level DynamoDB client
   * @return configured DynamoDB enhanced client
   */
  @Bean
  public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
    return DynamoDbEnhancedClient.builder()
        .dynamoDbClient(dynamoDbClient)
        .build();
  }
}

