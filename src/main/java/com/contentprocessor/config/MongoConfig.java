package com.contentprocessor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableMongoAuditing
public class MongoConfig {
    // This class enables automatic setting of @CreatedDate and @LastModifiedDate

    //Configure MongoDB Auditing
    //To make the @CreatedDate and @LastModifiedDate annotations work automatically, we need to enable MongoDB auditing in a configuration file.
}