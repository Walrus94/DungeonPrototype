package org.dungeon.prototype.config;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import jakarta.validation.constraints.NotNull;
import org.dungeon.prototype.repository.converters.DirectionReadingConverter;
import org.dungeon.prototype.repository.converters.DirectionWritingConverter;
import org.dungeon.prototype.repository.converters.EffectAttributeReadingConverter;
import org.dungeon.prototype.repository.converters.EffectAttributeWritingConverter;
import org.dungeon.prototype.repository.converters.PlayerAttributeReadingConverter;
import org.dungeon.prototype.repository.converters.PlayerAttributeWritingConverter;
import org.dungeon.prototype.repository.converters.PointReadingConverter;
import org.dungeon.prototype.repository.converters.PointWritingConverter;
import org.dungeon.prototype.repository.converters.WearableTypeReadingConverter;
import org.dungeon.prototype.repository.converters.WearableTypeWritingConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableMongoRepositories(basePackages = "org.dungeon.prototype.repository")
public class MongoConfig extends AbstractMongoClientConfiguration {
    private final String uri;
    private final String database;
    private final String username;
    private final String password;

    public MongoConfig(@Value("${spring.data.mongodb.uri}") String uri,
                       @Value("${spring.data.mongodb.database}") String database,
                       @Value("${spring.data.mongodb.username}") String username,
                       @Value("${spring.data.mongodb.password}") String password) {
        this.uri = uri;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    @NotNull
    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Bean
    @Override
    public MongoClient mongoClient() {
        return MongoClients.create(uri);
    }

    @Override
    protected void configureClientSettings(MongoClientSettings.Builder builder) {
        builder.applyToClusterSettings(settings ->
                        settings.hosts(List.of(new ServerAddress("mongo", 27017))))
                .credential(MongoCredential.createCredential(username, database, password.toCharArray()));
    }

    @Bean
    @NotNull
    @Override
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new WearableTypeReadingConverter());
        converters.add(new WearableTypeWritingConverter());
        converters.add(new EffectAttributeReadingConverter());
        converters.add(new EffectAttributeWritingConverter());
        converters.add(new PlayerAttributeReadingConverter());
        converters.add(new PlayerAttributeWritingConverter());
        converters.add(new DirectionReadingConverter());
        converters.add(new DirectionWritingConverter());
        converters.add(new PointReadingConverter());
        converters.add(new PointWritingConverter());
        return new MongoCustomConversions(converters);
    }
}
