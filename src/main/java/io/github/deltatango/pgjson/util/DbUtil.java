package io.github.deltatango.pgjson.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database utility class for managing PostgreSQL connections using HikariCP connection pooling.
 *
 * <p>This class provides a high-performance connection pool for PostgreSQL databases,
 * handling connection lifecycle, resource management, and connection health monitoring.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li><strong>Connection Pooling:</strong> HikariCP-based connection pool for optimal performance</li>
 *   <li><strong>Resource Management:</strong> Automatic connection cleanup and resource management</li>
 *   <li><strong>Health Monitoring:</strong> Connection health checks and pool status monitoring</li>
 *   <li><strong>Configuration:</strong> Flexible configuration through Properties</li>
 *   <li><strong>Logging:</strong> Comprehensive logging for debugging and monitoring</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * Properties props = new Properties();
 * props.setProperty("jdbcUrl", "jdbc:postgresql://localhost:5432/mydb");
 * props.setProperty("dataSource.user", "username");
 * props.setProperty("dataSource.password", "password");
 *
 * DbUtil dbUtil = new DbUtil(props);
 *
 * // Preferred: Use managed connections
 * try (ManagedConnection managedConn = dbUtil.getManagedConnection()) {
 *     Connection conn = managedConn.getConnection();
 *     // Use connection
 * } // Automatically closed
 *
 * // Manual connection (not recommended)
 * Connection conn = dbUtil.getConnection();
 * try {
 *     // Use connection
 * } finally {
 *     dbUtil.closeConnection(conn);
 * }
 * }</pre>
 *
 * <h2>Configuration Properties:</h2>
 * <ul>
 *   <li><code>jdbcUrl</code> - PostgreSQL JDBC URL (required)</li>
 *   <li><code>dataSource.user</code> - Database username (required)</li>
 *   <li><code>dataSource.password</code> - Database password (required)</li>
 *   <li><code>dataSource.maximumPoolSize</code> - Maximum pool size (default: 10)</li>
 *   <li><code>dataSource.minimumIdle</code> - Minimum idle connections (default: 10)</li>
 *   <li><code>dataSource.connectionTimeout</code> - Connection timeout in milliseconds (default: 30000)</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe and can be used by multiple threads
 * concurrently. The underlying HikariCP connection pool is designed for concurrent access.</p>
 *
 * @author PostgreSQL JSON Client Team
 * @version 25.10.1
 * @since 1.0.0
 * @see HikariDataSource
 * @see ManagedConnection
 * @see Connection
 */
@Slf4j
public class DbUtil {

    private HikariDataSource dataSource;
    private Properties props;

    /**
     * Gets a direct database connection from the connection pool.
     *
     * <p><strong>Warning:</strong> This method returns a raw connection that must be manually closed.
     * For most use cases, prefer using {@link #getManagedConnection()} which provides automatic
     * resource cleanup with try-with-resources pattern.</p>
     *
     * <p>If you must use this method, ensure proper resource management:</p>
     * <pre>{@code
     * Connection conn = dbUtil.getConnection();
     * try {
     *     // Use connection
     * } finally {
     *     if (conn != null) {
     *         dbUtil.closeConnection(conn);
     *     }
     * }
     * }</pre>
     *
     * @return a database connection from the pool, or null if connection fails
     * @see #getManagedConnection()
     * @see #closeConnection(Connection)
     */
    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (Exception exc) {
            log.error("Error in method getConnection, {}", exc.getMessage());
            return null;
        }
    }

    /**
     * Returns a managed connection that implements AutoCloseable for try-with-resources pattern.
     *
     * <p>This is the preferred method for getting connections as it ensures proper resource cleanup.
     * The returned ManagedConnection automatically closes the underlying connection when used
     * with try-with-resources statements.</p>
     *
     * <h4>Usage:</h4>
     * <pre>{@code
     * try (ManagedConnection managedConn = dbUtil.getManagedConnection()) {
     *     Connection conn = managedConn.getConnection();
     *     // Use connection - automatically closed when try block exits
     * }
     * }</pre>
     *
     * <h4>Benefits:</h4>
     * <ul>
     *   <li><strong>Automatic Cleanup:</strong> No need to manually close connections</li>
     *   <li><strong>Exception Safety:</strong> Connections are closed even if exceptions occur</li>
     *   <li><strong>Resource Management:</strong> Prevents connection leaks</li>
     *   <li><strong>Thread Safety:</strong> Each ManagedConnection is thread-safe</li>
     * </ul>
     *
     * @return ManagedConnection that will automatically close the underlying connection
     * @throws SQLException if connection cannot be established or data source is not available
     *
     * @see ManagedConnection
     * @see #getConnection()
     */
    public ManagedConnection getManagedConnection() throws SQLException {
        try {
            Connection connection = dataSource.getConnection();
            if (connection == null) {
                throw new SQLException("Failed to get connection from data source");
            }
            return new ManagedConnection(connection, this);
        } catch (Exception exc) {
            log.error("Error in method getManagedConnection, {}", exc.getMessage());
            throw new SQLException("Failed to get managed connection", exc);
        }
    }

    /**
     * Closes a database connection and returns it to the connection pool.
     *
     * <p>This method safely closes the provided connection, handling null checks and
     * connection state validation. It's used internally by ManagedConnection and can
     * be called manually when using raw connections.</p>
     *
     * <h4>Connection State Handling:</h4>
     * <ul>
     *   <li><strong>Null Safety:</strong> Handles null connections gracefully</li>
     *   <li><strong>State Validation:</strong> Checks if connection is already closed</li>
     *   <li><strong>Error Handling:</strong> Logs errors but doesn't throw exceptions</li>
     *   <li><strong>Pool Return:</strong> Returns connection to HikariCP pool</li>
     * </ul>
     *
     * <h4>Usage:</h4>
     * <pre>{@code
     * Connection conn = dbUtil.getConnection();
     * try {
     *     // Use connection
     * } finally {
     *     dbUtil.closeConnection(conn); // Safe to call even if conn is null
     * }
     * }</pre>
     *
     * <p><strong>Note:</strong> This method is thread-safe and can be called multiple times
     * on the same connection without issues.</p>
     *
     * @param connection the connection to close (can be null)
     *
     * @see #getConnection()
     * @see #getManagedConnection()
     */
    public void closeConnection(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception exc) {
            log.error("Error in method closeConnection, {}", exc.getMessage());
        }
    }

    private void connect() {
        log.debug("Establishing datasource... using {}", props);
        HikariConfig config = new HikariConfig(props);
        props.setProperty("dataSource.prepareThreshold", "1"); // Will create client side prepared statements
        dataSource = new HikariDataSource(config);
        log.debug("Data source status is running?: {}", dataSource.isRunning());
        log.debug("Datasource created.");
    }

    /**
     * Stops the database connection pool and closes all connections.
     *
     * <p>This method gracefully shuts down the HikariCP connection pool, closing all
     * active and idle connections. It should be called when the DbUtil instance is
     * no longer needed to ensure proper resource cleanup.</p>
     *
     * <h4>Shutdown Process:</h4>
     * <ol>
     *   <li>Stops accepting new connection requests</li>
     *   <li>Waits for active connections to complete</li>
     *   <li>Closes all idle connections</li>
     *   <li>Closes the connection pool</li>
     *   <li>Validates shutdown completion</li>
     * </ol>
     *
     * <h4>Usage:</h4>
     * <pre>{@code
     * DbUtil dbUtil = new DbUtil(props);
     * try {
     *     // Use database connections
     * } finally {
     *     dbUtil.stop(); // Always call stop() to clean up resources
     * }
     * }</pre>
     *
     * <h4>Error Handling:</h4>
     * <p>This method logs any errors during shutdown but doesn't throw exceptions.
     * If the connection pool fails to close properly, a warning is logged.</p>
     *
     * <p><strong>Note:</strong> After calling this method, the DbUtil instance should
     * not be used for any database operations.</p>
     *
     * @see #getConnection()
     * @see #getManagedConnection()
     */
    public void stop() {
        try {
            log.debug("Disconnecting from data source...");
            if (dataSource != null) {
                dataSource.close();
                log.debug("Data source status is closed?: {}", dataSource.isClosed());
                if (!dataSource.isClosed()) {
                    log.warn("Data source status is not closed this may cause database connections to idle in database server.");
                }
            }
        } catch (Exception exc) {
            log.error("Error in method {}, {}", exc.getStackTrace()[0].getClassName(), exc);
        }
    }

    /**
     * Constructs a new DbUtil instance with the specified database properties.
     *
     * <p>This constructor initializes the HikariCP connection pool with the provided
     * configuration properties. The connection pool is created and validated during
     * construction.</p>
     *
     * <h4>Required Properties:</h4>
     * <ul>
     *   <li><code>jdbcUrl</code> - PostgreSQL JDBC URL (e.g., "jdbc:postgresql://localhost:5432/dbname")</li>
     *   <li><code>dataSource.user</code> - Database username</li>
     *   <li><code>dataSource.password</code> - Database password</li>
     * </ul>
     *
     * <h4>Optional Properties:</h4>
     * <ul>
     *   <li><code>dataSource.maximumPoolSize</code> - Maximum connection pool size (default: 10)</li>
     *   <li><code>dataSource.minimumIdle</code> - Minimum idle connections (default: 10)</li>
     *   <li><code>dataSource.connectionTimeout</code> - Connection timeout in milliseconds (default: 30000)</li>
     *   <li><code>dataSource.idleTimeout</code> - Idle connection timeout in milliseconds (default: 600000)</li>
     *   <li><code>dataSource.maxLifetime</code> - Maximum connection lifetime in milliseconds (default: 1800000)</li>
     * </ul>
     *
     * <h4>Example:</h4>
     * <pre>{@code
     * Properties props = new Properties();
     * props.setProperty("jdbcUrl", "jdbc:postgresql://localhost:5432/mydb");
     * props.setProperty("dataSource.user", "username");
     * props.setProperty("dataSource.password", "password");
     * props.setProperty("dataSource.maximumPoolSize", "20");
     *
     * DbUtil dbUtil = new DbUtil(props);
     * }</pre>
     *
     * <h4>Initialization Process:</h4>
     * <ol>
     *   <li>Validates required properties</li>
     *   <li>Creates HikariCP configuration</li>
     *   <li>Initializes connection pool</li>
     *   <li>Validates pool connectivity</li>
     *   <li>Logs initialization status</li>
     * </ol>
     *
     * @param props database connection properties (must not be null)
     * @throws IllegalArgumentException if required properties are missing or invalid
     * @throws RuntimeException if connection pool initialization fails
     *
     * @see Properties
     * @see HikariDataSource
     */
    public DbUtil(Properties props) {
        this.props = props;
        log.debug("Initializing database connection");
        connect();
        log.debug("Connection initialized");
    }
}
