package co.hypertest.mongodb.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() {
        ConnectionString connString = new ConnectionString(mongoUri);
        PojoCodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connString)
                .codecRegistry(fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(pojoCodecProvider)))
                .build();
        return MongoClients.create(settings);
    }

    @Bean
    public MongoDatabase mongoDatabase(MongoClient client) {
        // It's better to get the database name from the connection string
        return client.getDatabase(new ConnectionString(mongoUri).getDatabase());
    }

    @Bean
    public ClientSession clientSession(MongoClient mongoClient) {
        return mongoClient.startSession();
    }
}
