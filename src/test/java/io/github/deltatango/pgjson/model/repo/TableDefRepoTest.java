package io.github.deltatango.pgjson.model.repo;

import io.github.deltatango.pgjson.model.TableDef;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import io.github.deltatango.pgjson.util.DatabaseConfigurationUtil;
import io.github.deltatango.pgjson.util.ManagedConnection;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class TableDefRepoTest extends DatabaseConfigurationUtil {

    TableDefRepo repo = new TableDefRepo();

    @Test
    public void selectAllEffectiveTableDefTest() throws SQLException {
        try (ManagedConnection managedConnection = postgresqlJsonClient.getDbUtil().getManagedConnection()) {
            OperationResult<List<TableDef>> result = repo.selectAllEffectiveTableDef(managedConnection.getConnection());
            assertInstanceOf(OperationResult.Success.class, result);
            List<TableDef> tableDefs = ((OperationResult.Success<List<TableDef>>) result).value();
            log.info("Table defs: {}", tableDefs);
            assertEquals(4, tableDefs.size());
            assertEquals("6dc4bf18-49e7-4c10-8a23-a4752e48f6d0", tableDefs.get(0).getIdUuid());
            assertEquals("6a4ab7da-eb74-4280-a0a2-990feff6f836", tableDefs.get(1).getIdUuid());
            assertEquals("e51e38d9-4a1e-427b-88fc-4026dfd576ec", tableDefs.get(2).getIdUuid());
        }
    }

    @Test
    public void selectTableDefByTableName_only_one_schema_per_Table_Test() throws SQLException {
        try (ManagedConnection managedConnection = postgresqlJsonClient.getDbUtil().getManagedConnection()) {
            OperationResult<TableDef> result = repo.selectTableDefByTableName(managedConnection.getConnection(), "table1");
            assertInstanceOf(OperationResult.Success.class, result);
            assertNotNull(((OperationResult.Success<TableDef>) result).value());
        }
    }

    @Test
    public void selectTableDefByTableName_multiple_schema_per_table_Test() throws SQLException {
        try (ManagedConnection managedConnection = postgresqlJsonClient.getDbUtil().getManagedConnection()) {
            OperationResult<TableDef> result = repo.selectTableDefByTableName(managedConnection.getConnection(), "table2");
            assertInstanceOf(OperationResult.Success.class, result);
            assertNotNull(((OperationResult.Success<TableDef>) result).value());
        }
    }
}
