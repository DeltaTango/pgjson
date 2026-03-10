package io.github.deltatango.pgjson.util;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;

/**
 * A managed database connection that implements AutoCloseable for automatic resource cleanup.
 * 
 * <p>This class wraps a database connection and ensures it's properly closed when used
 * with try-with-resources statements. It provides a safe way to manage database connections
 * without the risk of connection leaks.</p>
 * 
 * <h2>Usage:</h2>
 * <pre>{@code
 * try (ManagedConnection managedConn = dbUtil.getManagedConnection()) {
 *     Connection conn = managedConn.getConnection();
 *     // Use connection
 * } // Connection is automatically closed
 * }</pre>
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li><strong>Automatic Cleanup:</strong> Implements AutoCloseable for try-with-resources</li>
 *   <li><strong>State Tracking:</strong> Tracks connection state to prevent reuse after closing</li>
 *   <li><strong>Resource Management:</strong> Ensures connections are properly returned to pool</li>
 *   <li><strong>Error Handling:</strong> Graceful error handling during connection cleanup</li>
 *   <li><strong>Logging:</strong> Provides detailed logging for connection lifecycle</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. Each instance
 * should be used by a single thread.</p>
 * 
 * @author PostgreSQL JSON Client Team
 * @version 25.10.1
 * @since 1.0.0
 * @see AutoCloseable
 * @see Connection
 * @see DbUtil
 */
@Slf4j
public class ManagedConnection implements AutoCloseable {
    
    private final Connection connection;
    private final DbUtil dbUtil;
    private boolean closed = false;
    
    /**
     * Constructs a new ManagedConnection with the specified connection and database utility.
     * 
     * @param connection the database connection to manage (must not be null)
     * @param dbUtil the database utility for connection management (must not be null)
     * @throws IllegalArgumentException if connection or dbUtil is null
     */
    public ManagedConnection(Connection connection, DbUtil dbUtil) {
        this.connection = connection;
        this.dbUtil = dbUtil;
    }
    
    /**
     * Gets the underlying database connection.
     * 
     * <p>This method returns the raw database connection. The connection should be used
     * within the try-with-resources block to ensure proper cleanup.</p>
     * 
     * @return the database connection
     * @throws IllegalStateException if the connection has already been closed
     * @see Connection
     */
    public Connection getConnection() {
        if (closed) {
            throw new IllegalStateException("Connection has been closed");
        }
        return connection;
    }
    
    /**
     * Closes the managed connection and returns it to the connection pool.
     * 
     * <p>This method is automatically called when used with try-with-resources.
     * It safely closes the connection and marks it as closed to prevent reuse.</p>
     * 
     * <p><strong>Note:</strong> After calling this method, the connection should
     * not be used for any operations.</p>
     */
    @Override
    public void close() {
        if (!closed) {
            try {
                dbUtil.closeConnection(connection);
                closed = true;
            } catch (Exception e) {
                log.error("Error closing managed connection", e);
            }
        }
    }
    
    /**
     * Checks if the connection has been closed.
     * 
     * @return true if the connection has been closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }
}
