package io.github.deltatango.pgjson.client;

import io.github.deltatango.pgjson.model.DatabaseEntry;
import io.github.deltatango.pgjson.model.operations.OperationResult;
import io.github.deltatango.pgjson.model.operations.Result;
import io.github.deltatango.pgjson.util.DatabaseConfigurationUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrent CRUD integration tests for {@link io.github.deltatango.pgjson.PostgreSqlJsonClient}.
 *
 * <p>These tests verify thread safety and correct behavior under concurrent access
 * using real database connections via TestContainers.</p>
 */
@Slf4j
@DisplayName("PostgreSqlJsonClient Concurrency Tests")
public class PostgreSqlJsonClientConcurrencyTest extends DatabaseConfigurationUtil {

    private static final String TABLE_NAME = "table1";
    private static final int THREAD_COUNT = 10;

    @Test
    @DisplayName("Should handle concurrent inserts without data loss")
    void testConcurrentInserts() throws Exception {
        int insertCount = THREAD_COUNT;
        ExecutorService executor = Executors.newFixedThreadPool(insertCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(insertCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<String> insertedUuids = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < insertCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // All threads start simultaneously
                    String jsonData = String.format(
                            "{\"attribute1\": \"concurrent_%d\", \"attribute2\": \"test data %d\", " +
                            "\"attribute3\": {\"attribute7\": \"val\", \"attribute8\": \"A\"}, " +
                            "\"attribute4\": [{\"attribute9\": \"X\", \"attribute10\": \"val\"}], " +
                            "\"attribute5\": {\"attribute6\": [{\"attribute11\": \"val\"}], " +
                            "\"attribute12\": {\"attribute13\": \"val\"}}}",
                            index, index);

                    OperationResult<Result> result = postgresqlJsonClient.insertData(TABLE_NAME, jsonData);
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                        Result r = result.getOrThrow();
                        if (r.getIduuid() != null) {
                            insertedUuids.add(r.getIduuid());
                        }
                    } else {
                        errorCount.incrementAndGet();
                        log.warn("Insert {} failed: {}", index, result);
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    log.error("Insert {} threw exception", index, e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release all threads
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All inserts should complete within 30 seconds");
        executor.shutdown();

        log.info("Concurrent inserts: {} succeeded, {} failed", successCount.get(), errorCount.get());
        assertEquals(insertCount, successCount.get(), "All concurrent inserts should succeed");
        assertEquals(insertCount, insertedUuids.size(), "All inserts should produce unique UUIDs");

        // Verify each inserted entry is retrievable
        for (String uuid : insertedUuids) {
            OperationResult<DatabaseEntry> entry = postgresqlJsonClient.selectDataByIdUuid(uuid, TABLE_NAME);
            assertTrue(entry.isSuccess(), "Inserted entry should be retrievable: " + uuid);
        }
    }

    @Test
    @DisplayName("Should handle concurrent reads while writing")
    void testConcurrentReadsAndWrites() throws Exception {
        int writerCount = 5;
        int readerCount = 5;
        int totalThreads = writerCount + readerCount;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);
        AtomicInteger writeSuccessCount = new AtomicInteger(0);
        AtomicInteger readSuccessCount = new AtomicInteger(0);
        AtomicInteger readErrorCount = new AtomicInteger(0);

        // Writers
        for (int i = 0; i < writerCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String jsonData = String.format(
                            "{\"attribute1\": \"rw_test_%d\", \"attribute2\": \"read write test %d\", " +
                            "\"attribute3\": {\"attribute7\": \"val\", \"attribute8\": \"B\"}, " +
                            "\"attribute4\": [{\"attribute9\": \"Y\", \"attribute10\": \"val\"}], " +
                            "\"attribute5\": {\"attribute6\": [{\"attribute11\": \"val\"}], " +
                            "\"attribute12\": {\"attribute13\": \"val\"}}}",
                            index, index);

                    OperationResult<Result> result = postgresqlJsonClient.insertData(TABLE_NAME, jsonData);
                    if (result.isSuccess()) {
                        writeSuccessCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("Writer {} threw exception", index, e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Readers -- read pre-existing data
        for (int i = 0; i < readerCount; i++) {
            final String existingUuid = "f54be158-0ff9-49d1-a070-954e6b64caac"; // From init.sql
            executor.submit(() -> {
                try {
                    startLatch.await();
                    OperationResult<DatabaseEntry> result = postgresqlJsonClient.selectDataByIdUuid(existingUuid, TABLE_NAME);
                    if (result.isSuccess()) {
                        readSuccessCount.incrementAndGet();
                    } else {
                        readErrorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    readErrorCount.incrementAndGet();
                    log.error("Reader threw exception", e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All operations should complete within 30 seconds");
        executor.shutdown();

        log.info("Writers: {} succeeded. Readers: {} succeeded, {} failed",
                writeSuccessCount.get(), readSuccessCount.get(), readErrorCount.get());
        assertEquals(writerCount, writeSuccessCount.get(), "All writes should succeed");
        assertEquals(readerCount, readSuccessCount.get(), "All reads should succeed");
        assertEquals(0, readErrorCount.get(), "No reads should fail");
    }

    @Test
    @DisplayName("Should handle concurrent updates to different entries")
    void testConcurrentUpdates() throws Exception {
        // First, insert entries to update
        List<String> uuids = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            String jsonData = String.format(
                    "{\"attribute1\": \"update_target_%d\", \"attribute2\": \"original %d\", " +
                    "\"attribute3\": {\"attribute7\": \"val\", \"attribute8\": \"C\"}, " +
                    "\"attribute4\": [{\"attribute9\": \"Z\", \"attribute10\": \"val\"}], " +
                    "\"attribute5\": {\"attribute6\": [{\"attribute11\": \"val\"}], " +
                    "\"attribute12\": {\"attribute13\": \"val\"}}}",
                    i, i);
            OperationResult<Result> insertResult = postgresqlJsonClient.insertData(TABLE_NAME, jsonData);
            assertTrue(insertResult.isSuccess(), "Setup insert should succeed");
            uuids.add(insertResult.getOrThrow().getIduuid());
        }

        // Now update all entries concurrently
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger updateSuccessCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int index = i;
            final String uuid = uuids.get(i);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String updatedData = String.format(
                            "{\"attribute1\": \"updated_%d\", \"attribute2\": \"updated data %d\", " +
                            "\"attribute3\": {\"attribute7\": \"updated\", \"attribute8\": \"A\"}, " +
                            "\"attribute4\": [{\"attribute9\": \"X\", \"attribute10\": \"updated\"}], " +
                            "\"attribute5\": {\"attribute6\": [{\"attribute11\": \"updated\"}], " +
                            "\"attribute12\": {\"attribute13\": \"updated\"}}}",
                            index, index);

                    OperationResult<Result> result = postgresqlJsonClient.updateData(TABLE_NAME, updatedData, uuid);
                    if (result.isSuccess()) {
                        updateSuccessCount.incrementAndGet();
                    } else {
                        log.warn("Update {} failed: {}", index, result);
                    }
                } catch (Exception e) {
                    log.error("Update {} threw exception", index, e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All updates should complete within 30 seconds");
        executor.shutdown();

        assertEquals(THREAD_COUNT, updateSuccessCount.get(), "All concurrent updates should succeed");

        // Verify all entries were updated
        for (int i = 0; i < THREAD_COUNT; i++) {
            OperationResult<DatabaseEntry> entry = postgresqlJsonClient.selectDataByIdUuid(uuids.get(i), TABLE_NAME);
            assertTrue(entry.isSuccess(), "Updated entry should be retrievable");
            assertTrue(entry.getOrThrow().getJsonData().contains("updated_" + i),
                    "Entry should contain updated data");
        }
    }

    @Test
    @DisplayName("Should handle concurrent deletes without errors")
    void testConcurrentDeletes() throws Exception {
        // Insert entries to delete
        List<String> uuids = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            String jsonData = String.format(
                    "{\"attribute1\": \"delete_target_%d\", \"attribute2\": \"to be deleted %d\", " +
                    "\"attribute3\": {\"attribute7\": \"val\", \"attribute8\": \"A\"}, " +
                    "\"attribute4\": [{\"attribute9\": \"X\", \"attribute10\": \"val\"}], " +
                    "\"attribute5\": {\"attribute6\": [{\"attribute11\": \"val\"}], " +
                    "\"attribute12\": {\"attribute13\": \"val\"}}}",
                    i, i);
            OperationResult<Result> insertResult = postgresqlJsonClient.insertData(TABLE_NAME, jsonData);
            assertTrue(insertResult.isSuccess(), "Setup insert should succeed");
            uuids.add(insertResult.getOrThrow().getIduuid());
        }

        // Delete all entries concurrently
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger deleteSuccessCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final String uuid = uuids.get(i);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    OperationResult<Boolean> result = postgresqlJsonClient.deleteData(TABLE_NAME, uuid);
                    if (result.isSuccess()) {
                        deleteSuccessCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("Delete threw exception", e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All deletes should complete within 30 seconds");
        executor.shutdown();

        assertEquals(THREAD_COUNT, deleteSuccessCount.get(), "All concurrent deletes should succeed");

        // Verify entries are gone
        for (String uuid : uuids) {
            OperationResult<DatabaseEntry> entry = postgresqlJsonClient.selectDataByIdUuid(uuid, TABLE_NAME);
            assertTrue(entry.isNotFound(), "Deleted entry should not be found: " + uuid);
        }
    }

    @Test
    @DisplayName("Should handle more threads than connection pool size")
    void testConnectionPoolExhaustion() throws Exception {
        // Use more threads than the default HikariCP pool size (typically 10)
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String jsonData = String.format(
                            "{\"attribute1\": \"pool_test_%d\", \"attribute2\": \"pool exhaustion test %d\", " +
                            "\"attribute3\": {\"attribute7\": \"val\", \"attribute8\": \"B\"}, " +
                            "\"attribute4\": [{\"attribute9\": \"Y\", \"attribute10\": \"val\"}], " +
                            "\"attribute5\": {\"attribute6\": [{\"attribute11\": \"val\"}], " +
                            "\"attribute12\": {\"attribute13\": \"val\"}}}",
                            index, index);

                    OperationResult<Result> result = postgresqlJsonClient.insertData(TABLE_NAME, jsonData);
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("Pool test {} threw exception", index, e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(60, TimeUnit.SECONDS),
                "All operations should complete within 60 seconds even with pool contention");
        executor.shutdown();

        log.info("Pool exhaustion test: {} of {} succeeded", successCount.get(), threadCount);
        assertEquals(threadCount, successCount.get(),
                "All operations should succeed despite pool contention (connections should queue)");
    }
}
