package com.voyageai.voyageaibackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Configuration class for MongoDB.
 * 
 * <p>This class provides MongoDB configuration for the application.
 * MongoDB is used as a platform-agnostic NoSQL alternative to DynamoDB.
 * 
 * <p>Configuration properties in application.properties:
 * <pre>
 * spring.data.mongodb.uri=mongodb://localhost:27017/voyageai
 * spring.data.mongodb.database=voyageai
 * </pre>
 * 
 * <p>Benefits of MongoDB over DynamoDB:
 * <ul>
 *   <li>Platform-agnostic (runs anywhere, no AWS lock-in)</li>
 *   <li>Flexible querying with rich query language</li>
 *   <li>Local development without cloud emulators</li>
 *   <li>Widely used in China (Aliyun, Tencent Cloud support)</li>
 * </ul>
 */
@Configuration
@EnableMongoAuditing
public class MongoDbConfig {

  @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/voyageai}")
  private String mongoUri;

  @Value("${spring.data.mongodb.database:voyageai}")
  private String database;

  /**
   * Get the MongoDB URI.
   *
   * @return the MongoDB connection URI
   */
  public String getMongoUri() {
    return mongoUri;
  }

  /**
   * Get the database name.
   *
   * @return the database name
   */
  public String getDatabase() {
    return database;
  }
}

