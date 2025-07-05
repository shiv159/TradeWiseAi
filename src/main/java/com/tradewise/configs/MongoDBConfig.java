package com.tradewise.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;

import com.tradewise.model.StockData;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Configuration
@Slf4j
public class MongoDBConfig {

    private final MongoTemplate mongoTemplate;

    public MongoDBConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initIndexes() {
        log.info("Initializing MongoDB indexes");
        
        // Create compound index for stockSymbol and dataType
        IndexOperations indexOps = mongoTemplate.indexOps(StockData.class);
        
        // Check if compound index already exists
        List<IndexInfo> indexes = indexOps.getIndexInfo();
        boolean compoundIndexExists = indexes.stream()
                .anyMatch(index -> index.getName().equals("stockSymbol_dataType_idx"));
        
        if (!compoundIndexExists) {
            // Create compound index on stockSymbol and dataType
            Index compoundIndex = new Index()
                    .on("stockSymbol", org.springframework.data.domain.Sort.Direction.ASC)
                    .on("dataType", org.springframework.data.domain.Sort.Direction.ASC)
                    .named("stockSymbol_dataType_idx")
                    .unique();
            
            indexOps.createIndex(compoundIndex);
            log.info("Created compound index on stockSymbol and dataType");
        }
        
        // Create index on lastUpdated for TTL queries
        boolean lastUpdatedIndexExists = indexes.stream()
                .anyMatch(index -> index.getName().equals("lastUpdated_idx"));
        
        if (!lastUpdatedIndexExists) {
            Index lastUpdatedIndex = new Index()
                    .on("lastUpdated", org.springframework.data.domain.Sort.Direction.DESC)
                    .named("lastUpdated_idx");
            indexOps.createIndex(lastUpdatedIndex);
            log.info("Created index on lastUpdated field");
        }
        
        log.info("MongoDB index initialization completed");
    }
}
