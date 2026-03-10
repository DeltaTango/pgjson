package io.github.deltatango.pgjson.util;

import io.github.deltatango.pgjson.PostgreSqlJsonClient;
import io.github.deltatango.pgjson.exceptions.PostgreJsonException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Properties;

@Slf4j
@Testcontainers
public abstract class DatabaseConfigurationUtil {

    public static PostgreSqlJsonClient postgresqlJsonClient;

    FileUtil fileUtil = new FileUtil();

    @Container
    public static PostgreSQLContainer postgres =
            new PostgreSQLContainer(TestContainerConfiguration.POSTGRESQL_VERSION)
                    .withDatabaseName(TestContainerConfiguration.POSTGRESQL_DATABASE_NAME)
                    .withUsername(TestContainerConfiguration.POSTGRESQL_USER_NAME)
                    .withPassword(TestContainerConfiguration.POSTGRESQL_PASSWORD)
                    .withInitScript("sql/init.sql")
                    .withCommand("postgres -c max_connections=250");

    @BeforeEach
    public void connectClient() throws PostgreJsonException {
        Properties props = new Properties();
        props.setProperty("dataSource.user", postgres.getUsername());
        props.setProperty("dataSource.password", postgres.getPassword());
        props.setProperty("jdbcUrl", postgres.getJdbcUrl());
        props.setProperty("dataSource.leakDetectionThreshold", "5000");
        log.info("Connecting to PostgreSqlJsonClient with parameters: {}", props);
        postgresqlJsonClient = new PostgreSqlJsonClient(props);
        fileUtil = new FileUtil();
    }

    @AfterEach
    public void disconnectClient() {
        if (postgresqlJsonClient != null) {
            try {
                postgresqlJsonClient.close();
            } catch (Exception e) {
                log.warn("Error closing PostgreSqlJsonClient after test", e);
            }
            postgresqlJsonClient = null;
        }
    }

    @AfterAll
    public static void shutdownClient() {
        if (postgresqlJsonClient != null) {
            try {
                postgresqlJsonClient.close();
            } catch (Exception e) {
                // Swallow — container is shutting down
            }
            postgresqlJsonClient = null;
        }
    }
}
