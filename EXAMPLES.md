# PostgreSQL JSON Client - Advanced Usage Examples

This document provides advanced usage examples for the PgJson library, covering production-ready patterns, performance optimization, and real-world scenarios.

## Table of Contents

1. [Retry and Circuit Breaker Patterns](#retry-and-circuit-breaker-patterns)
2. [Schema Management](#schema-management)
3. [Performance Optimization](#performance-optimization)
4. [Complex Query Patterns](#complex-query-patterns)
5. [Production Monitoring](#production-monitoring)
6. [Spring Boot Integration](#spring-boot-integration)
7. [Error Handling Strategies](#error-handling-strategies)

## Transaction Guarantees

Multi-step write operations are **automatically wrapped in database transactions**. No user action is needed — the library handles `setAutoCommit(false)`, `commit()`, and `rollback()` internally.

**Transactional operations** (automatic commit/rollback):
- `insertData` — resolves schema, validates, inserts (rolled back if validation or insert fails)
- `updateData` — reads existing entry, validates, updates (rolled back if any step fails)
- `updateDataWithMerge` — reads existing entry, merges JSON, validates, updates (rolled back if any step fails)
- `deleteArrayElement` — modifies array, updates (rolled back on failure)

**Auto-commit operations** (single SQL statement, no explicit transaction needed):
- `selectData`, `selectDataByIdUuid` — read-only queries
- `deleteData` — single DELETE statement
- `insertSchema` — single INSERT statement
- `isDatabaseRunning` — health check

Each public operation uses exactly **one connection** from the pool, regardless of how many internal steps it performs. This prevents connection pool exhaustion under concurrent load.

---

## Retry and Circuit Breaker Patterns

The PostgreSQL JSON Client includes comprehensive retry logic and circuit breaker patterns to handle transient failures and prevent cascading failures.

### Basic Usage with Retry/Circuit Breaker

```java
// Get the operation executor with retry and circuit breaker capabilities
DatabaseOperationExecutor executor = client.getOperationExecutor();

// Execute client operations with automatic retry and explicit result variants
OperationResult<List<DatabaseEntry>> op = executor.executeOperationWithRetry(client.getDbUtil(), connection ->
    client.selectData(tableName, searchJson)
);
List<DatabaseEntry> results = op.getOrThrow();
```

### Custom Retry Configuration

```java
// Create custom retry configuration
RetryConfig retryConfig = RetryConfig.builder()
    .maxAttempts(5)                    // Retry up to 5 times
    .baseDelayMs(1000)                 // Start with 1 second delay
    .maxDelayMs(30000)                 // Cap at 30 seconds
    .multiplier(2.0)                   // Double delay each retry
    .enableJitter(true)                // Add random variation
    .build();

// Create custom circuit breaker
CircuitBreaker circuitBreaker = CircuitBreaker.builder()
    .failureThreshold(3)               // Open after 3 consecutive failures
    .timeoutMs(60000)                  // Wait 1 minute before retry
    .halfOpenMaxCalls(2)               // Allow 2 calls in half-open state
    .build();

// Create executor with custom configuration
DatabaseOperationExecutor executor = new DatabaseOperationExecutor(retryConfig, circuitBreaker);
```

### Monitoring Circuit Breaker State

```java
DatabaseOperationExecutor executor = client.getOperationExecutor();
CircuitBreaker circuitBreaker = executor.getCircuitBreaker();

// Check circuit breaker state
CircuitBreaker.State state = circuitBreaker.getState();
System.out.println("Circuit breaker state: " + state);

// Get metrics
long totalCalls = circuitBreaker.getTotalCalls();
long totalFailures = circuitBreaker.getTotalFailures();
double failureRate = circuitBreaker.getFailureRate();

System.out.println("Total calls: " + totalCalls);
System.out.println("Total failures: " + totalFailures);
System.out.println("Failure rate: " + failureRate + "%");

// Manually reset circuit breaker if needed
if (state == CircuitBreaker.State.OPEN) {
    circuitBreaker.reset();
}
```

### Exception Handling

```java
try {
    OperationResult<List<DatabaseEntry>> results = executor.executeOperationWithRetry(client.getDbUtil(), connection ->
        client.selectData(tableName, searchJson)
    );
} catch (CircuitBreaker.CircuitBreakerOpenException e) {
    // Circuit breaker is open - database is likely down
    log.error("Database circuit breaker is open: {}", e.getMessage());
    // Implement fallback logic or return cached data
}
```

### Retryable vs Non-Retryable Exceptions

The system automatically distinguishes between retryable and non-retryable exceptions:

**Retryable Exceptions:**
- Connection timeouts
- Deadlocks
- Temporary resource issues
- Network connectivity problems

**Non-Retryable Exceptions:**
- Invalid parameters (`IllegalArgumentException`)
- Validation errors (`RequestException`)
- Authentication failures (`SecurityException`)

### Best Practices

1. **Use Default Configuration**: Start with `RetryConfig.defaultConfig()` and `CircuitBreaker.defaultBreaker()`
2. **Monitor Metrics**: Regularly check circuit breaker state and failure rates
3. **Implement Fallbacks**: Handle `CircuitBreakerOpenException` with appropriate fallback logic
4. **Log Appropriately**: Use appropriate log levels for retry attempts vs final failures
5. **Test Failure Scenarios**: Test your application's behavior when the circuit breaker opens

### Configuration Properties

You can also configure HikariCP for additional resilience:

```properties
# Connection pool configuration
dataSource.maximumPoolSize=20
dataSource.connectionTimeout=30000
dataSource.leakDetectionThreshold=60000
dataSource.connectionTestQuery=SELECT 1
dataSource.validationTimeout=5000
```

This combination of retry logic, circuit breaker patterns, and connection pool tuning provides robust protection against database failures and transient issues.

## Schema Management

### Advanced Schema Design

```java
// Complex schema with nested objects and arrays
String complexSchema = "{\n" +
    "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
    "  \"type\": \"object\",\n" +
    "  \"required\": [\"id\", \"name\", \"email\"],\n" +
    "  \"properties\": {\n" +
    "    \"id\": {\n" +
    "      \"type\": \"string\",\n" +
    "      \"x-pgjson-index\": \"exact\"\n" +
    "    },\n" +
    "    \"name\": {\n" +
    "      \"type\": \"string\",\n" +
    "      \"x-pgjson-index\": \"fts\",\n" +
    "      \"x-pgjson-uiLabel\": {\n" +
    "        \"en\": \"Full Name\",\n" +
    "        \"es\": \"Nombre Completo\"\n" +
    "      }\n" +
    "    },\n" +
    "    \"profile\": {\n" +
    "      \"type\": \"object\",\n" +
    "      \"x-pgjson-index\": \"object\",\n" +
    "      \"properties\": {\n" +
    "        \"bio\": {\"type\": \"string\", \"x-pgjson-index\": \"fts\"},\n" +
    "        \"location\": {\"type\": \"string\"},\n" +
    "        \"preferences\": {\n" +
    "          \"type\": \"object\",\n" +
    "          \"properties\": {\n" +
    "            \"theme\": {\"type\": \"string\", \"enum\": [\"light\", \"dark\"]},\n" +
    "            \"notifications\": {\"type\": \"boolean\"}\n" +
    "          }\n" +
    "        }\n" +
    "      }\n" +
    "    },\n" +
    "    \"tags\": {\n" +
    "      \"type\": \"array\",\n" +
    "      \"x-pgjson-index\": \"nestedobject\",\n" +
    "      \"items\": {\n" +
    "        \"type\": \"object\",\n" +
    "        \"properties\": {\n" +
    "          \"name\": {\"type\": \"string\", \"x-pgjson-index\": \"fts\"},\n" +
    "          \"category\": {\"type\": \"string\", \"x-pgjson-index\": \"exact\"}\n" +
    "        }\n" +
    "      }\n" +
    "    }\n" +
    "  }\n" +
    "}";

String schemaUuid = client.insertSchema("users", "user-profile-v2", complexSchema).getOrThrow();
```

### Schema Versioning

```java
// Insert multiple schema versions for the same table
String schemaV1 = "{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}}";
String schemaV2 = "{\"type\": \"object\", \"required\": [\"name\", \"email\"], \"properties\": {\"name\": {\"type\": \"string\"}, \"email\": {\"type\": \"string\"}}}";

String v1Uuid = client.insertSchema("users", "user-v1", schemaV1).getOrThrow();
String v2Uuid = client.insertSchema("users", "user-v2", schemaV2).getOrThrow();

// Documents can coexist with different schema versions
// Old documents use v1 schema, new documents use v2 schema
```

## Performance Optimization

### Index Strategy

```java
// Create indexes based on schema definitions
TableDef schema = client.getTableDefByIdUuid(schemaUuid).getOrThrow();
List<IndexInfo> indexes = IndexUtil.getIndexes(schema.getSchema());

// The library automatically creates indexes based on x-pgjson-index hints
// Manual index creation for specific use cases
client.getDbUtil().executeUpdate("CREATE INDEX CONCURRENTLY pgjson_custom_users_created ON users (data_created)");
```

### Query Optimization

```java
// Use specific search patterns for optimal performance
String optimizedSearch = "{\n" +
    "  \"limit\": 100,\n" +
    "  \"offset\": 0,\n" +
    "  \"orderType\": \"desc\",\n" +
    "  \"searchTerm\": {\n" +
    "    \"name\": \"John\",           // Uses FTS index\n" +
    "    \"email\": \"john@example.com\", // Uses exact index\n" +
    "    \"profile.location\": \"New York\" // Uses object index\n" +
    "  },\n" +
    "  \"logicalOperator\": \"AND\"\n" +
    "}";

List<DatabaseEntry> results = client.selectData("users", optimizedSearch).getOrThrow();
```

### Batch Operations

```java
// Efficient batch processing
List<String> userData = Arrays.asList(
    "{\"name\": \"User 1\", \"email\": \"user1@example.com\"}",
    "{\"name\": \"User 2\", \"email\": \"user2@example.com\"}",
    "{\"name\": \"User 3\", \"email\": \"user3@example.com\"}"
);

List<Result> results = new ArrayList<>();
for (String userJson : userData) {
    OperationResult<Result> op = client.insertData("users", userJson);
    if (op instanceof OperationResult.Success<Result> s) {
        results.add(s.value());
    } else if (op instanceof OperationResult.Error<Result> e) {
        log.error("Failed to insert user: {}", e.message());
    }
}
```

## Complex Query Patterns

### Advanced Search Queries

```java
// Multi-criteria search with different index types
String complexSearch = "{\n" +
    "  \"limit\": 50,\n" +
    "  \"offset\": 0,\n" +
    "  \"searchTerm\": {\n" +
    "    \"name\": \"John\",                    // FTS search\n" +
    "    \"email\": \"@example.com\",           // Pattern matching\n" +
    "    \"profile.bio\": \"developer\",        // Nested FTS\n" +
    "    \"tags.category\": \"tech\"            // Array element search\n" +
    "  },\n" +
    "  \"logicalOperator\": \"AND\"\n" +
    "}";

List<DatabaseEntry> results = client.selectData("users", complexSearch).getOrThrow();
```

### Pagination with Sorting

```java
// Efficient pagination
public List<DatabaseEntry> getUsersPage(int page, int size, String sortField, String sortOrder) {
    String searchJson = String.format("{\n" +
        "  \"limit\": %d,\n" +
        "  \"offset\": %d,\n" +
        "  \"orderType\": \"%s\",\n" +
        "  \"searchTerm\": {}\n" +
        "}", size, page * size, sortOrder);
    
    return client.selectData("users", searchJson).getOrThrow();
}
```

## Production Monitoring

### Health Checks

```java
@Service
public class DatabaseHealthService {
    
    private final PostgreSqlJsonClient client;
    private final DatabaseOperationExecutor executor;
    
    public HealthStatus checkDatabaseHealth() {
        try {
            // Check if database is running
            boolean isRunning = client.isDatabaseRunning();
            if (!isRunning) {
                return HealthStatus.DOWN;
            }
            
            // Check circuit breaker state
            CircuitBreaker circuitBreaker = executor.getCircuitBreaker();
            if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
                return HealthStatus.DEGRADED;
            }
            
            // Perform a simple query
            String testSearch = "{\"limit\": 1, \"offset\": 0, \"searchTerm\": {}}";
            OperationResult<List<DatabaseEntry>> results = client.selectData("users", testSearch);
            
            return HealthStatus.UP;
            
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return HealthStatus.DOWN;
        }
    }
}
```

### Metrics Collection

```java
@Component
public class DatabaseMetrics {
    
    private final MeterRegistry meterRegistry;
    private final DatabaseOperationExecutor executor;
    
    @EventListener
    public void onDatabaseOperation(DatabaseOperationEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Execute operation
            OperationResult<Object> result = executor.executeOperationWithRetry(connection ->
                performDatabaseOperation(connection)
            );
            
            // Record success metrics
            sample.stop(Timer.builder("database.operation.duration")
                .tag("operation", event.getOperationType())
                .tag("status", "success")
                .register(meterRegistry));
                
        } catch (Exception e) {
            // Record failure metrics
            sample.stop(Timer.builder("database.operation.duration")
                .tag("operation", event.getOperationType())
                .tag("status", "error")
                .register(meterRegistry));
        }
    }
}
```

## Spring Boot Integration

### Advanced Configuration

```java
@Configuration
@EnableConfigurationProperties(PgJsonProperties.class)
public class PgJsonAdvancedConfiguration {
    
    @Bean
    @Primary
    public PostgreSqlJsonClient postgreSqlJsonClient(PgJsonProperties properties) {
        Properties props = new Properties();
        // ... configuration setup
        
        return new PostgreSqlJsonClient(props);
    }
    
    @Bean
    public DatabaseOperationExecutor databaseOperationExecutor(PgJsonProperties properties) {
        RetryConfig retryConfig = RetryConfig.builder()
            .maxAttempts(properties.getRetry().getMaxAttempts())
            .baseDelayMs(properties.getRetry().getBaseDelayMs())
            .maxDelayMs(properties.getRetry().getMaxDelayMs())
            .multiplier(properties.getRetry().getMultiplier())
            .enableJitter(properties.getRetry().isEnableJitter())
            .build();
            
        CircuitBreaker circuitBreaker = CircuitBreaker.builder()
            .failureThreshold(properties.getCircuitBreaker().getFailureThreshold())
            .timeoutMs(properties.getCircuitBreaker().getTimeoutMs())
            .halfOpenMaxCalls(properties.getCircuitBreaker().getHalfOpenMaxCalls())
            .build();
            
        return new DatabaseOperationExecutor(retryConfig, circuitBreaker);
    }
}
```

### Service Layer with Transactions

```java
@Service
@Transactional
public class UserService {
    
    private final PostgreSqlJsonClient client;
    private final DatabaseOperationExecutor executor;
    
    public User createUser(UserDto userDto) {
        try {
            // Validate input
            validateUserDto(userDto);
            
            // Insert user with retry logic
            String userJson = objectMapper.writeValueAsString(userDto);
            OperationResult<Result> op = executor.executeOperationWithRetry(connection -> {
                return client.insertData("users", userJson);
            });
            Result result = op.getOrThrow();
            
            if (Boolean.TRUE.equals(result.getResultStatus())) {
                return findUserById(result.getIduuid());
            } else {
                throw new UserCreationException("Failed to create user");
            }
            
        } catch (CircuitBreaker.CircuitBreakerOpenException e) {
            log.warn("Database circuit breaker is open, user creation failed");
            throw new ServiceUnavailableException("Database temporarily unavailable");
        } catch (Exception e) {
            log.error("User creation failed", e);
            throw new UserCreationException("Failed to create user", e);
        }
    }
}
```

## Error Handling Strategies

### Comprehensive Exception Handling

```java
@Service
public class RobustDocumentService {
    
    private final PostgreSqlJsonClient client;
    private final DatabaseOperationExecutor executor;
    
    public List<DatabaseEntry> searchWithFallback(String tableName, String searchJson) {
        try {
            OperationResult<List<DatabaseEntry>> op = executor.executeOperationWithRetry(connection -> {
                try {
                    return client.selectData(tableName, searchJson);
                } catch (RequestException e) {
                    return OperationResult.error("Invalid search request", e);
                }
            });
            if (op instanceof OperationResult.Success<List<DatabaseEntry>> s) {
                return s.value();
            }
            return getCachedData(tableName, searchJson);
            
        } catch (CircuitBreaker.CircuitBreakerOpenException e) {
            log.warn("Circuit breaker is open, using cached data");
            return getCachedData(tableName, searchJson);
        }
    }
    
    private List<DatabaseEntry> getCachedData(String tableName, String searchJson) {
        // Implement cache fallback logic
        return Collections.emptyList();
    }
}
```

### Retry with Exponential Backoff

```java
@Component
public class ResilientDataService {
    
    @Retryable(
        value = {PostgreJsonException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public OperationResult<Result> insertDataWithRetry(String tableName, String jsonData) {
        return client.insertData(tableName, jsonData);
    }
    
    @Recover
    public OperationResult<Result> recoverFromInsertFailure(PostgreJsonException ex, String tableName, String jsonData) {
        log.error("Failed to insert data after retries", ex);
        return OperationResult.error("Insert failed after retries", ex);
    }
}
```

This comprehensive examples guide covers production-ready patterns, performance optimization, and real-world scenarios for the PgJson library.
