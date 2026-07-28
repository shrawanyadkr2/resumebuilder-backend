//package com.shrawan.resumebuilder;
//
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//
//@SpringBootApplication
//public class ResumebuilderApplication {
//
//	public static void main(String[] args) {
//		SpringApplication.run(ResumebuilderApplication.class, args);
//	}
//
//}
package com.shrawan.resumebuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.MongoTemplate;

@SpringBootApplication
public class ResumebuilderApplication implements CommandLineRunner {

    @Autowired
    private MongoTemplate mongoTemplate;

    public static void main(String[] args) {
        SpringApplication.run(ResumebuilderApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String dbName = mongoTemplate.getDb().getName();
        System.out.println("=========================================");
        System.out.println("CONNECTED TO MONGODB DATABASE: " + dbName);
        System.out.println("=========================================");
    }
}
