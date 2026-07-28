//package com.shrawan.resumebuilder.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.mongodb.config.EnableMongoAuditing;
//
//@Configuration
//@EnableMongoAuditing
//public class MongoConfig {
//
//}
package com.shrawan.resumebuilder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

    @Bean
    public MongoDatabaseFactory mongoDbFactory() {
        // This forces connection strictly to your custom database
        return new SimpleMongoClientDatabaseFactory("mongodb://localhost:27017/resumebuilder");
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoDbFactory());
    }
}
