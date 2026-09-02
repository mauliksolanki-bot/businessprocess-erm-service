package com.org.erm.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@Configuration
public class FlywayStartupConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlywayStartupConfig.class);
    private static final String FLYWAY_TABLE = "ERM_FLYWAY_SCHEMA_HISTORY";

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            ensureSchemaHistoryWithBaseline(flyway);
            flyway.repair();
            flyway.migrate();
        };
    }

    private void ensureSchemaHistoryWithBaseline(Flyway flyway) {
        try (Connection connection = flyway.getConfiguration().getDataSource().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS ERM_FLYWAY_SCHEMA_HISTORY (
                        installed_rank INT NOT NULL,
                        version VARCHAR(50) NULL,
                        description VARCHAR(200) NOT NULL,
                        type VARCHAR(20) NOT NULL,
                        script VARCHAR(1000) NOT NULL,
                        checksum INT NULL,
                        installed_by VARCHAR(100) NOT NULL,
                        installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        execution_time INT NOT NULL,
                        success TINYINT(1) NOT NULL,
                        PRIMARY KEY (installed_rank)
                    )
                    """);

            if (!hasAnyHistoryRow(connection)) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("""
                        INSERT INTO ERM_FLYWAY_SCHEMA_HISTORY (
                            installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success
                        ) VALUES (
                            1, ?, 'Baseline existing schema', 'BASELINE', '<< Flyway Baseline >>', NULL, SUBSTRING_INDEX(USER(), '@', 1), CURRENT_TIMESTAMP, 0, 1
                        )
                        """)) {
                    preparedStatement.setString(1, "4");
                    preparedStatement.executeUpdate();
                }
                LOGGER.info("Initialized {} with baseline version 4.", FLYWAY_TABLE);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to prepare Flyway schema history table", exception);
        }
    }

    private boolean hasAnyHistoryRow(Connection connection) throws Exception {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT COUNT(*) FROM ERM_FLYWAY_SCHEMA_HISTORY");
             ResultSet resultSet = preparedStatement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1) > 0;
        }
    }
}
