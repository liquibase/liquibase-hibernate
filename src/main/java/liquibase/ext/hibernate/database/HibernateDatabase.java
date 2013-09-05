package liquibase.ext.hibernate.database;

import java.util.HashMap;

import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.DatabaseConnection;
import liquibase.exception.DatabaseException;

import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.event.PostInsertEventListener;

public class HibernateDatabase extends AbstractJdbcDatabase {

    private Configuration configuration;
    private Dialect dialect;

    public HibernateDatabase() {
    }

    private boolean isEjb3Configuration() {
        return getConnection().getURL().startsWith("persistence");
    }

    private String getConfigFile() {
        return getConnection().getURL().replaceFirst("hibernate:", "");
    }

    public boolean requiresPassword() {
        return false;
    }

    public boolean requiresUsername() {
        return false;
    }

    public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
        if (conn.getURL().startsWith("hibernate:")) {
            return true;
        } else if (conn.getURL().startsWith("persistence:")) {
            return true;
        }
        return false;
    }

    public String getDefaultDriver(String url) {
        return "liquibase.ext.hibernate.database.HibernateDriver";
    }

    public int getPriority() {
        return PRIORITY_DEFAULT;
    }

    private void configure() throws DatabaseException {
        if (isEjb3Configuration()) {
            Ejb3Configuration ejb3Configuration = new Ejb3Configuration();
            ejb3Configuration.configure(getConnection().getURL().substring("persistence:".length()), new HashMap());
            configuration = ejb3Configuration.getHibernateConfiguration();
            configuration.setProperty("hibernate.dialect", ejb3Configuration.getProperties().getProperty("hibernate.dialect"));
            for (PostInsertEventListener postInsertEventListener : configuration.getEventListeners().getPostInsertEventListeners()) {
                if (postInsertEventListener instanceof org.hibernate.envers.event.AuditEventListener) {
                    AuditConfiguration.getFor(configuration);
                }
            }

        } else {
            configuration = new AnnotationConfiguration();
            configuration.configure(getConfigFile());
        }
        configuration.buildMappings();
        String dialectString = configuration.getProperty("hibernate.dialect");
        if (dialectString != null)
            try {
                dialect = (Dialect) Class.forName(dialectString).newInstance();
            } catch (Exception e) {
                throw new DatabaseException(e);
            }
        else
            dialect = new HibernateGenericDialect(configuration);
    }

    @Override
    public String getShortName() {
        return "hibernate";
    }

    @Override
    public Integer getDefaultPort() {
        return 0;
    }

    @Override
    public boolean supportsInitiallyDeferrableColumns() {
        return false;
    }

    @Override
    public boolean supportsTablespaces() {
        return false;
    }

    @Override
    protected String getDefaultDatabaseProductName() {
        return getDatabaseProductName();
    }

    public Configuration getConfiguration() throws DatabaseException {
        if (configuration == null)
            configure();
        return configuration;
    }

    public Dialect getDialect() throws DatabaseException {
        if (dialect == null)
            configure();
        return dialect;
    }

    @Override
    protected String getConnectionCatalogName() throws DatabaseException {
        return null;
    }

    @Override
    protected String getConnectionSchemaName() {
        return null;
    }

}
