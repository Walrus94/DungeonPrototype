package org.dungeon.prototype.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.dungeon.prototype.repository.converters.AttributeReadingConverter;
import org.dungeon.prototype.repository.converters.AttributeWritingConverter;
import org.dungeon.prototype.repository.converters.DirectionReadingConverter;
import org.dungeon.prototype.repository.converters.DirectionWritingConverter;
import org.dungeon.prototype.repository.converters.PointReadingConverter;
import org.dungeon.prototype.repository.converters.PointWritingConverter;
import org.jetbrains.annotations.NotNull;
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

    @NotNull
    @Override
    protected String getDatabaseName() {
        return "dungeon_proto_db";
    }

    @Bean
    @NotNull
    @Override
    public MongoClient mongoClient() {
        return MongoClients.create("mongodb://localhost:27017");
    }

    @Bean
    @NotNull
    @Override
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new AttributeReadingConverter());
        converters.add(new AttributeWritingConverter());
        converters.add(new DirectionReadingConverter());
        converters.add(new DirectionWritingConverter());
        converters.add(new PointReadingConverter());
        converters.add(new PointWritingConverter());
        return new MongoCustomConversions(converters);
    }
    @Override
    protected boolean autoIndexCreation() {
        return true;
    }

}
