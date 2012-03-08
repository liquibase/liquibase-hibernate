package liquibase.ext.hibernate.database;

import liquibase.database.jvm.JdbcConnection;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * User: adrian.george
 * Date: 8/26/11
 * Time: 12:57 PM
 */
public class HibernateDatabaseTest {
    private HibernateDatabase database;

    @Before
    public void setUp() throws Exception {
        database = new HibernateDatabase();
    }

    @Test
    public void isParameterized_returnsTrue() throws Exception {
        String url = "hibernate:testConfig.cfg.xml?some.property=zazz";
        assertThat(database.isParameterized(url), equalTo(true));
    }

    @Test
    public void isParameterized_returnsFalse() throws Exception {
        String url = "hibernate:testConfig.cfg.xml";
        assertThat(database.isParameterized(url), equalTo(false));
    }

    @Test
    public void getConfigFile_stripsProtocol() {
        String url = "hibernate:testConfig.cfg.xml";
        assertThat(database.getConfigFile(url), equalTo("testConfig.cfg.xml"));
    }

    @Test
    public void getConfigFile_stripsParameters() throws Exception {
        String url = "hibernate:testConfig.cfg.xml?some.property=zazz";
        assertThat(database.getConfigFile(url), equalTo("testConfig.cfg.xml"));
    }

    @Test
    public void getProperties_getsTheParamterSection() throws Exception {
        String url = "shouldnt=getMe?some.property=zazz";
        Properties props = database.getProperties(url);
        assertThat(props, allOf(not(hasKey((Object) "shouldnt")), hasKey("some.property")));
    }

    @Test
    public void getProperties_decodesParameters() throws Exception {
        String url = "hibernate:testConfig.cfg.xml?some.property=banana+phone";
        Properties props = database.getProperties(url);
        assertThat(props, hasValue((Object) "banana phone"));
    }

    @Test
    public void getProperties_breaksupMultipleProperties() throws Exception {
        String url = "hibernate:testConfig.cfg.xml?some.property=zazz&foo=bar";
        Properties props = database.getProperties(url);
        assertThat(props, allOf(hasKey((Object) "some.property"), hasKey("foo")));
    }

    @Test(expected = IllegalStateException.class)
    public void getProperties_transformsIOException() throws Exception {
        final Properties properties = mock(Properties.class);
        database = new HibernateDatabase(){
            @Override
            protected Properties getPropertiesInstance() {
                return properties;
            }
        };
        doThrow(new IOException("Test Exception")).when(properties).load(any(StringReader.class));
        String url = "hibernate:testConfig.cfg.xml?some.property=banana+phone";
        database.getProperties(url);
    }

    @Test
    public void getProperties_completesWhenNotParameterized() throws Exception {
        final Properties properties = mock(Properties.class);
        database = new HibernateDatabase(){
            @Override
            protected Properties getPropertiesInstance() {
                return properties;
            }
        };
        String url = "hibernate:testConfig.cfg.xml";
        database.getProperties(url);
        verify(properties, never()).load(any(StringReader.class));
    }

    @Test
    public void processProperties_addsProperties() throws Exception {
        Configuration config = mock(Configuration.class);
        database.processProperties(config, new Properties());
        verify(config).addProperties(any(Properties.class));
    }

    @Test
    public void processProperties_addsNamingStrategy() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("hibernate.namingStrategy", "org.hibernate.cfg.ImprovedNamingStrategy");
        Configuration config = mock(Configuration.class);
        database.processProperties(config, properties);
        verify(config).setNamingStrategy(any(ImprovedNamingStrategy.class));
    }

    @Test(expected = IllegalStateException.class)
    public void processProperties_transformsClassNotFoundException() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("hibernate.namingStrategy", "foo.bar.NonExistentNamingStrategy");
        Configuration config = mock(Configuration.class);
        database.processProperties(config, properties);
    }

    @Test(expected = IllegalStateException.class)
    public void processProperties_transformsInstantiationException() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("hibernate.namingStrategy", "liquibase.ext.hibernate.database.HibernateDatabaseTest$FooNamingStrategy");
        Configuration config = mock(Configuration.class);
        database.processProperties(config, properties);
    }

    @Test(expected = IllegalStateException.class)
    public void processProperties_transformsIllegalAccessException() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("hibernate.namingStrategy", "liquibase.ext.hibernate.database.HibernateDatabaseTest$BarNamingStrategy");
        Configuration config = mock(Configuration.class);
        database.processProperties(config, properties);
    }

    @Test
    public void createConnection_loadsConfig() throws Exception {
        database.setConnection(new JdbcConnection(new HibernateConnection("hibernate:hibernate/Hibernate.cfg.xml")));
        Configuration config = database.createConfiguration();
        assertThat(config.getProperty("connection.pool_size"), equalTo("5"));
    }

    @Test
    public void createConnection_overridesProperties() throws Exception {
        database.setConnection(new JdbcConnection(new HibernateConnection("hibernate:hibernate/Hibernate.cfg.xml?connection.pool_size=6")));
        Configuration config = database.createConfiguration();
        assertThat(config.getProperty("connection.pool_size"), equalTo("6"));
    }

    public static class FooNamingStrategy implements NamingStrategy {
        public FooNamingStrategy(String whatever){}

        public String classToTableName(String className) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.FooNamingStrategy.classToTableName");
        }

        public String propertyToColumnName(String propertyName) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.FooNamingStrategy.propertyToColumnName");
        }

        public String tableName(String tableName) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.FooNamingStrategy.tableName");
        }

        public String columnName(String columnName) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.FooNamingStrategy.columnName");
        }

        public String collectionTableName(String ownerEntity, String ownerEntityTable, String associatedEntity, String associatedEntityTable, String propertyName) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.FooNamingStrategy.collectionTableName");
        }

        public String joinKeyColumnName(String joinedColumn, String joinedTable) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.FooNamingStrategy.joinKeyColumnName");
        }

        public String foreignKeyColumnName(String propertyName, String propertyEntityName, String propertyTableName, String referencedColumnName) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.FooNamingStrategy.foreignKeyColumnName");
        }

        public String logicalColumnName(String columnName, String propertyName) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.FooNamingStrategy.logicalColumnName");
        }

        public String logicalCollectionTableName(String tableName, String ownerEntityTable, String associatedEntityTable, String propertyName) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.FooNamingStrategy.logicalCollectionTableName");
        }

        public String logicalCollectionColumnName(String columnName, String propertyName, String referencedColumn) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.FooNamingStrategy.logicalCollectionColumnName");
        }
    }

    public static class BarNamingStrategy implements NamingStrategy {
        private BarNamingStrategy() {}

        public String classToTableName(String className) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.BarNamingStrategy.classToTableName");
        }

        public String propertyToColumnName(String propertyName) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.BarNamingStrategy.propertyToColumnName");
        }

        public String tableName(String tableName) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.BarNamingStrategy.tableName");
        }

        public String columnName(String columnName) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.BarNamingStrategy.columnName");
        }

        public String collectionTableName(String ownerEntity, String ownerEntityTable, String associatedEntity, String associatedEntityTable, String propertyName) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.BarNamingStrategy.collectionTableName");
        }

        public String joinKeyColumnName(String joinedColumn, String joinedTable) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.BarNamingStrategy.joinKeyColumnName");
        }

        public String foreignKeyColumnName(String propertyName, String propertyEntityName, String propertyTableName, String referencedColumnName) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.BarNamingStrategy.foreignKeyColumnName");
        }

        public String logicalColumnName(String columnName, String propertyName) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.BarNamingStrategy.logicalColumnName");
        }

        public String logicalCollectionTableName(String tableName, String ownerEntityTable, String associatedEntityTable, String propertyName) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.BarNamingStrategy.logicalCollectionTableName");
        }

        public String logicalCollectionColumnName(String columnName, String propertyName, String referencedColumn) {
            throw new UnsupportedOperationException("Not written liquibase.ext.hibernate.database.HibernateDatabaseTest.BarNamingStrategy.logicalCollectionColumnName");
        }
    }
}
