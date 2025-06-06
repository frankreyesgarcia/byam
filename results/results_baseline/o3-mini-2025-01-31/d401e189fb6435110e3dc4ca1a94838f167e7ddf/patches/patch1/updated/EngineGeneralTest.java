package com.feedzai.commons.sql.abstraction.engine.impl.abs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.feedzai.commons.sql.abstraction.ddl.AlterColumn;
import com.feedzai.commons.sql.abstraction.ddl.DbColumn;
import com.feedzai.commons.sql.abstraction.ddl.DbColumnConstraint;
import com.feedzai.commons.sql.abstraction.ddl.DbColumnType;
import com.feedzai.commons.sql.abstraction.ddl.DbEntity;
import com.feedzai.commons.sql.abstraction.ddl.DbIndex;
import com.feedzai.commons.sql.abstraction.ddl.DbPrimaryKey;
import com.feedzai.commons.sql.abstraction.ddl.Rename;
import com.feedzai.commons.sql.abstraction.dml.Delete;
import com.feedzai.commons.sql.abstraction.dml.Expression;
import com.feedzai.commons.sql.abstraction.dml.K;
import com.feedzai.commons.sql.abstraction.dml.Query;
import com.feedzai.commons.sql.abstraction.dml.Truncate;
import com.feedzai.commons.sql.abstraction.dml.Update;
import com.feedzai.commons.sql.abstraction.dml.Values;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngine;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngineException;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngineRuntimeException;
import com.feedzai.commons.sql.abstraction.engine.test.AbstractDbTest;
import com.feedzai.commons.sql.abstraction.engine.testconfig.DatabaseTestUtil;
import com.feedzai.commons.sql.abstraction.engine.testconfig.BlobTest;
import com.feedzai.commons.sql.abstraction.engine.testconfig.DbTestConfiguration;
import com.feedzai.commons.sql.abstraction.engine.testconfig.TestUtils;
import com.feedzai.commons.sql.abstraction.engine.util.DbEntityUtils;
import com.feedzai.commons.sql.abstraction.engine.util.Formatter;
import com.feedzai.commons.sql.abstraction.engine.util.MappedEntity;
import com.feedzai.commons.sql.abstraction.engine.util.Namespace;
import com.feedzai.commons.sql.abstraction.engine.util.PdbTemplate;
import com.feedzai.commons.sql.abstraction.engine.util.PropertyKeys;
import com.feedzai.commons.sql.abstraction.engine.util.StringUtils;
import com.feedzai.commons.sql.abstraction.dml.dialect.SqlDialect;
import com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder;
import com.feedzai.commons.sql.abstraction.dml.result.ResultColumn;
import com.feedzai.commons.sql.abstraction.dml.result.ResultIterator;
import com.feedzai.commons.sql.abstraction.engine.DatabaseFactory;
import com.feedzai.commons.sql.abstraction.engine.DatabaseFactoryException;
import com.feedzai.commons.sql.abstraction.engine.NameAlreadyExistsException;
import com.feedzai.commons.sql.abstraction.engine.OperationNotSupportedRuntimeException;
import com.feedzai.commons.sql.abstraction.engine.testconfig.TestDatabase;
import com.feedzai.commons.sql.abstraction.engine.testconfig.TestEngineFactory;
import com.feedzai.commons.sql.abstraction.engine.util.PdbProperties;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import mockit.Expectations;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.Verifications;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
public class EngineGeneralTest {

    private DatabaseEngine engine;
    private Properties properties;

    @Parameterized.Parameters
    public static Collection<DbTestConfiguration> data() throws Exception {
        return DatabaseTestUtil.loadConfigurations();
    }

    @Parameterized.Parameter
    public DbTestConfiguration config;

    @BeforeClass
    public static void initStatic() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.TRACE);
    }

    @Before
    public void init() throws Exception {
        properties = new Properties();
        properties.put("jdbcUrl", config.getJdbcUrl());
        properties.put("username", config.getUsername());
        properties.put("password", config.getPassword());
        properties.put(PdbProperties.ENGINE, config.getEngine());
        properties.put(PdbProperties.SCHEMA_POLICY, config.getSchemaPolicy());
        engine = DatabaseFactory.getConnection(properties);
    }

    @After
    public void cleanup() throws Exception {
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    public void createEntityTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .pkFields("COL1")
                .build();

        engine.addEntity(entity);
    }

    @Test(expected = DatabaseEngineException.class)
    public void createEntityAlreadyExistsTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .pkFields("COL1")
                .build();

        engine.addEntity(entity);
        engine.addEntity(entity);
    }

    @Test
    public void createUniqueIndexTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .pkFields("COL1")
                .addIndex(DbIndex.builder().unique(true).columns("COL2").build())
                .build();

        engine.addEntity(entity);
    }

    @Test
    public void createIndexTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .pkFields("COL1")
                .addIndex(DbIndex.builder().columns("COL3").build())
                .build();

        engine.addEntity(entity);
    }

    @Test
    public void insertWithAutoCommitTest() throws Exception {
        create5ColumnsEntity();

        engine.persist("TEST", TestUtils.entry()
                .set("COL1", 1)
                .set("COL2", true)
                .set("COL3", 3.14)
                .set("COL4", 100L)
                .set("COL5", "Test")
                .build());

        List<Map<String, ResultColumn>> result = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        // Add assert validations
    }

    @Test
    public void insertWithControlledTransactionTest() throws Exception {
        create5ColumnsEntity();

        engine.beginTransaction();

        try {
            engine.persist("TEST", TestUtils.entry()
                    .set("COL1", 1)
                    .set("COL2", true)
                    .set("COL3", 2.718)
                    .set("COL4", 200L)
                    .set("COL5", "TransactionTest")
                    .build());
            engine.commit();
        } catch (DatabaseEngineException e) {
            engine.rollback();
            throw e;
        }
    }

    @Test
    public void batchInsertTest() throws Exception {
        create5ColumnsEntity();
        engine.beginTransaction();
        try {
            engine.addBatch("TEST", TestUtils.entry()
                    .set("COL1", 1)
                    .set("COL2", false)
                    .set("COL3", 1.1)
                    .set("COL4", 300L)
                    .set("COL5", "Batch1")
                    .build());
            engine.addBatch("TEST", TestUtils.entry()
                    .set("COL1", 2)
                    .set("COL2", true)
                    .set("COL3", 2.2)
                    .set("COL4", 400L)
                    .set("COL5", "Batch2")
                    .build());
            engine.flush();
            engine.commit();
        } catch (DatabaseEngineException e) {
            engine.rollback();
            throw e;
        }
    }

    @Test
    public void blobTest() throws DatabaseEngineException {
        BlobTest blobValue = new BlobTest(1, "blobName");
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.STRING)
                .addColumn("COL2", DbColumnType.BLOB)
                .build();

        engine.addEntity(entity);
        engine.persist("TEST", TestUtils.entry()
                .set("COL1", "CENINHAS")
                .set("COL2", blobValue)
                .build());

        List<Map<String, ResultColumn>> result = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        assert result.get(0).get("COL1").toString().equals("CENINHAS");
        assert result.get(0).get("COL2").<BlobTest>toBlob().equals(blobValue);
    }

    @Test
    public void limitNumberOfRowsTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        for (int i = 0; i < 20; i++) {
            engine.persist("TEST", TestUtils.entry()
                    .set("COL1", i)
                    .set("COL2", i % 2 == 0)
                    .set("COL3", i * 1.0)
                    .set("COL4", (long) i)
                    .set("COL5", "Row" + i)
                    .build());
        }
        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).limit(10));
        assert query.size() == 10;
    }

    @Test
    public void distinctTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.query(SqlBuilder.select(SqlBuilder.all()).distinct().from(SqlBuilder.table("TEST")));
    }

    @Test
    public void joinATableWithQueryTest() throws DatabaseEngineException {
        userRolePermissionSchema();
        engine.query(SqlBuilder.select(SqlBuilder.all())
                .from(SqlBuilder.table("USER").alias("a")
                        .innerJoin(
                                SqlBuilder.select(SqlBuilder.column("COL1")).from(SqlBuilder.table("USER")).alias("b"),
                                SqlBuilder.eq(SqlBuilder.column("a", "COL1"), SqlBuilder.column("b", "COL1"))
                        )
                )
        );
    }

    @Test
    public void testCast() throws DatabaseEngineException {
        Query query = SqlBuilder.select(
                SqlBuilder.cast(K.of("22"), DbColumnType.INT).alias("int"),
                SqlBuilder.cast(K.of(22), DbColumnType.STRING).alias("string"),
                SqlBuilder.cast(K.of("1"), DbColumnType.BOOLEAN).alias("bool"),
                SqlBuilder.cast(K.of("22"), DbColumnType.DOUBLE).alias("double"),
                SqlBuilder.cast(K.of(22), DbColumnType.LONG).alias("long")
        );

        Map<String, ResultColumn> result = engine.query(query).get(0);
        assert result.get("int").toInt().equals(22);
        assert result.get("string").toString().equals("22");
        assert result.get("bool").toBoolean();
        assert Math.abs(result.get("double").toDouble() - 22.0) < 1e-9;
        assert result.get("long").toLong().equals(22L);
    }

    @Test
    public void tryWithResourcesClosesEngine() throws Exception {
        final AtomicReference<Connection> connReference = new AtomicReference<>();
        try (final DatabaseEngine tryEngine = this.engine) {
            connReference.set(tryEngine.getConnection());
            assert !connReference.get().isClosed();
        }
        assert connReference.get().isClosed();
        try (final DatabaseEngine tryEngine = DatabaseFactory.getConnection(properties)) {
            connReference.set(tryEngine.getConnection());
            assert !connReference.get().isClosed();
        }
        assert connReference.get().isClosed();
    }

    @Test
    public void entityEntryHashcodeTest() {
        Map<String, Object> map = new HashMap<>();
        map.put("id1", "val1");
        map.put("id2", "val2");
        map.put("id3", "val3");
        map.put("id4", "val4");

        Object entry = TestUtils.entry()
                .set(map)
                .build();

        assert entry.hashCode() == map.hashCode();
    }

    @Test
    public void kEnumTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", TestUtils.entry().set("COL5", TestEnum.TEST_ENUM_VAL).build());
        engine.persist("TEST", TestUtils.entry().set("COL5", "something else").build());
        List<Map<String, ResultColumn>> results = engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.eq(SqlBuilder.column("COL5"), K.of(TestEnum.TEST_ENUM_VAL)))
        );
        assert results.size() == 1;
        assert results.get(0).get("COL5").toString().equals(TestEnum.TEST_ENUM_VAL.name());
    }

    @Test
    public void insertDuplicateDBError() throws Exception {
        create5ColumnsEntityWithPrimaryKey();
        Object entry = TestUtils.entry()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .build();

        engine.persist("TEST", entry);
        try {
            engine.persist("TEST", entry);
            throw new RuntimeException("Expected unique constraint violation exception");
        } catch (DatabaseEngineException e) {
            assert e.getMessage().contains("unique_constraint_violation");
        }
    }

    // ... many other test methods remain unchanged

    private void create5ColumnsEntity() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .build();
        engine.addEntity(entity);
    }

    private void create5ColumnsEntityWithPrimaryKey() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .pkFields("COL1")
                .build();
        engine.addEntity(entity);
    }

    protected void userRolePermissionSchema() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("USER")
                .addColumn("COL1", DbColumnType.INT, true)
                .pkFields("COL1")
                .build();
        engine.addEntity(entity);

        entity = DbEntity.builder()
                .name("ROLE")
                .addColumn("COL1", DbColumnType.INT, true)
                .pkFields("COL1")
                .build();
        engine.addEntity(entity);

        entity = DbEntity.builder()
                .name("USER_ROLE")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.INT)
                .addFk(DbEntityUtils.buildForeignKey("COL1", "USER", "COL1"),
                       DbEntityUtils.buildForeignKey("COL2", "ROLE", "COL1"))
                .pkFields("COL1", "COL2")
                .build();
        engine.addEntity(entity);
    }

    private enum TestEnum {
        TEST_ENUM_VAL;

        @Override
        public String toString() {
            return super.toString() + " description";
        }
    }
    
    @Test
    public void testPersistOverrideAutoIncrement() throws Exception {
        DbEntity entity = DbEntity.builder()
                .name("MYTEST")
                .addColumn("COL1", DbColumnType.INT, true)
                .addColumn("COL2", DbColumnType.STRING)
                .build();

        engine.addEntity(entity);

        Object ent = TestUtils.entry().set("COL2", "CENAS1")
                .build();
        engine.persist("MYTEST", ent);
        ent = TestUtils.entry().set("COL2", "CENAS2")
                .build();
        engine.persist("MYTEST", ent);

        ent = TestUtils.entry().set("COL2", "CENAS3").set("COL1", 3)
                .build();
        engine.persist("MYTEST", ent, false);

        ent = TestUtils.entry().set("COL2", "CENAS5").set("COL1", 5)
                .build();
        engine.persist("MYTEST", ent, false);

        ent = TestUtils.entry().set("COL2", "CENAS6")
                .build();
        engine.persist("MYTEST", ent);

        ent = TestUtils.entry().set("COL2", "CENAS7")
                .build();
        engine.persist("MYTEST", ent);

        List<Map<String, ResultColumn>> query = engine.query("SELECT * FROM " + StringUtils.quotize("MYTEST", engine.escapeCharacter()));
        for (Map<String, ResultColumn> row : query) {
            assert row.get("COL2").toString().endsWith(row.get("COL1").toString());
        }
        engine.close();
    }
    
    @Test
    public void testTruncateTable() throws Exception {
        create5ColumnsEntity();
        engine.persist("TEST", TestUtils.entry().set("COL1", 5).build());
        Truncate truncate = new Truncate(SqlBuilder.table("TEST"));
        engine.executeUpdate(truncate);
        List<Map<String, ResultColumn>> test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        assert test.isEmpty();
    }

    @Test
    public void testRenameTables() throws Exception {
        String oldName = "TBL_OLD";
        String newName = "TBL_NEW";
        dropSilently(oldName, newName);
        DbEntity entity = DbEntity.builder()
                .name(oldName)
                .addColumn("timestamp", DbColumnType.INT)
                .build();
        engine.addEntity(entity);
        engine.persist(oldName, TestUtils.entry().set("timestamp", 20).build());
        Rename rename = new Rename(SqlBuilder.table(oldName), SqlBuilder.table(newName));
        engine.executeUpdate(rename);
        Map<String, DbColumnType> metaMap = new LinkedHashMap<>();
        metaMap.put("timestamp", DbColumnType.INT);
        assert engine.getMetadata(newName).equals(metaMap);
        List<Map<String, ResultColumn>> resultSet = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table(newName)));
        assert resultSet.size() == 1;
        assert resultSet.get(0).get("timestamp").toInt() == 20;
        dropSilently(newName);
    }

    private void dropSilently(String... tables) {
        for (String table : tables) {
            try {
                engine.dropEntity(DbEntity.builder().name(table).build());
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    @Test
    public void testLikeWithTransformation() throws Exception {
        create5ColumnsEntity();
        engine.persist("TEST", TestUtils.entry().set("COL1", 5).set("COL5", "teste").build());
        engine.persist("TEST", TestUtils.entry().set("COL1", 5).set("COL5", "TESTE").build());
        engine.persist("TEST", TestUtils.entry().set("COL1", 5).set("COL5", "TeStE").build());
        engine.persist("TEST", TestUtils.entry().set("COL1", 5).set("COL5", "tesTte").build());
        List<Map<String, ResultColumn>> query = engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.like(SqlBuilder.udf("lower", SqlBuilder.column("COL5")), K.of("%teste%")))
        );
        assert query.size() == 3;
        query = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST"))
                .where(SqlBuilder.like(SqlBuilder.udf("lower", SqlBuilder.column("COL5")), K.of("%tt%"))));
        assert query.size() == 1;
    }

    @Test
    public void createSequenceOnLongColumnTest() throws Exception {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG, true)
                .addColumn("COL5", DbColumnType.STRING)
                .build();
        engine.addEntity(entity);
        engine.persist("TEST", TestUtils.entry().set("COL1", 1).set("COL2", true).build());
        List<Map<String, ResultColumn>> test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        assert test.get(0).get("COL1").toInt() == 1;
        assert test.get(0).get("COL2").toBoolean();
        assert test.get(0).get("COL4").toLong() == 1L;
    }

    @Test
    public void insertWithNoAutoIncAndThatResumeTheAutoIncTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG, true)
                .addColumn("COL5", DbColumnType.STRING)
                .build();
        engine.addEntity(entity);
        engine.persist("TEST", TestUtils.entry().set("COL1", 1).set("COL2", true).build());
        List<Map<String, ResultColumn>> test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4")));
        assert test.get(0).get("COL4").toLong() == 1L;
        engine.persist("TEST", TestUtils.entry().set("COL1", 1).set("COL2", true).set("COL4", 2).build(), false);
        test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4")));
        assert test.get(1).get("COL4").toLong() == 2L;
        engine.persist("TEST", TestUtils.entry().set("COL1", 1).set("COL2", true).build());
        test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4")));
        assert test.get(2).get("COL4").toLong() == 3L;
        engine.persist("TEST", TestUtils.entry().set("COL1", 1).set("COL2", true).set("COL4", 4).build(), false);
        test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4")));
        assert test.get(3).get("COL4").toLong() == 4L;
        engine.persist("TEST", TestUtils.entry().set("COL1", 1).set("COL2", true).build());
        test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4")));
        assert test.get(4).get("COL4").toLong() == 5L;
        engine.persist("TEST", TestUtils.entry().set("COL1", 1).set("COL2", true).set("COL4", 6).build(), false);
        test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4")));
        assert test.get(5).get("COL4").toLong() == 6L;
        engine.persist("TEST", TestUtils.entry().set("COL1", 1).set("COL2", true).set("COL4", 7).build(), false);
        test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4")));
        assert test.get(6).get("COL4").toLong() == 7L;
        engine.persist("TEST", TestUtils.entry().set("COL1", 1).set("COL2", true).build());
        test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4")));
        assert test.get(7).get("COL4").toLong() == 8L;
    }
}