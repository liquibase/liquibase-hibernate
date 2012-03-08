package liquibase.ext.hibernate.database;

import liquibase.change.Change;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.RanChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.structure.DatabaseObject;
import liquibase.exception.*;
import liquibase.sql.visitor.SqlVisitor;
import liquibase.statement.DatabaseFunction;
import liquibase.statement.SqlStatement;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.NamingStrategy;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.net.URLDecoder;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class HibernateDatabase implements Database {

    private String defaultSchema;
    private DatabaseConnection conn;

    public HibernateDatabase() {
    }

    public String getConfigFile(String url) {
        String trimProtocol = url.replaceFirst("hibernate:", "");
        String trimParameters = trimProtocol;
        if(isParameterized(url)) {
           trimParameters =  trimProtocol.substring(0, trimProtocol.indexOf("?"));
        }
        return trimParameters;
    }

    boolean isParameterized(String url) {
        return (url.indexOf("?") != -1);
    }

    public boolean requiresPassword() {
        return false;
    }

    public boolean requiresUsername() {
        return false;
    }
    public DatabaseObject[] getContainingObjects() {
        return null;
    }

    public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
        return conn.getURL().startsWith("hibernate:");
    }

    public String getDefaultDriver(String url) {
        return "liquibase.ext.hibernate.database.HibernateDriver";
    }

    public DatabaseConnection getConnection() {
        return null;
    }

    public void setConnection(DatabaseConnection conn) {
        this.conn = conn;
    }


    public void checkDatabaseChangeLogTable(boolean b, DatabaseChangeLog databaseChangeLog) throws DatabaseException {

    }

    public List<DatabaseFunction> getDatabaseFunctions() {
        return new ArrayList<DatabaseFunction>();
    }

    public void reset() {
    }

    public boolean supportsForeignKeyDisable() {
        return false;
    }

    public boolean disableForeignKeyChecks() throws DatabaseException {
        return false;
    }

    public void enableForeignKeyChecks() throws DatabaseException {
    }

    public int getPriority() {
        return PRIORITY_DEFAULT;
    }

    public boolean getAutoCommitMode() {
        return false;
    }

    public boolean isAutoCommit() throws DatabaseException {
        return false;
    }

    public void setAutoCommit(boolean b) throws DatabaseException {
        
    }

    public boolean supportsDDLInTransaction() {
        return false;
    }

    public String getDatabaseProductName() {
        return "Hibernate Mapping";
    }

    public String getDatabaseProductVersion() throws DatabaseException {
        return "N/A";
    }

    public int getDatabaseMajorVersion() throws DatabaseException {
        return -1;
    }

    public int getDatabaseMinorVersion() throws DatabaseException {
        return -1;
    }

    public String getTypeName() {
        return "hibernate";
    }

    public String getDefaultCatalogName() throws DatabaseException {
        return null;
    }

    public String getDefaultSchemaName() {
        return defaultSchema;
    }

    public void setDefaultSchemaName(String schemaName) throws DatabaseException {
        this.defaultSchema = schemaName;
    }

    public boolean supportsInitiallyDeferrableColumns() {
        return false;
    }

    public boolean supportsSequences() {
        return false;
    }

    public boolean supportsAutoIncrement() {
        return false;
    }

    public String getDateLiteral(String isoDate) {
        return null;
    }

    public String getCurrentDateTimeFunction() {
        return null;
    }

    public void setCurrentDateTimeFunction(String function) {

    }

    public String getLineComment() {
        return null;
    }

    public String getAutoIncrementClause() {
        return null;
    }

    public String getDatabaseChangeLogTableName() {
        return null;
    }

    public String getDatabaseChangeLogLockTableName() {
        return null;
    }
    
    /**
     * Does nothing because this is a hibernate database
     * @see liquibase.database.Database#setDatabaseChangeLogLockTableName(java.lang.String)
     */
    public void setDatabaseChangeLogLockTableName(String tableName) {
    }

	/**
	 * Does nothing because this is a hibernate database
     * @see liquibase.database.Database#setDatabaseChangeLogTableName(java.lang.String)
     */
    public void setDatabaseChangeLogTableName(String tableName) {
    }

	public String getConcatSql(String... values) {
        return null;
    }

    public void setCanCacheLiquibaseTableInfo(boolean canCacheLiquibaseTableInfo) {
        //
    }

    public boolean hasDatabaseChangeLogTable() {
        return false;
    }

    public boolean hasDatabaseChangeLogLockTable() {
        return false;
    }

    public void checkDatabaseChangeLogTable() throws DatabaseException {

    }

    public void checkDatabaseChangeLogLockTable() throws DatabaseException {

    }

    public void dropDatabaseObjects(String schema) throws DatabaseException {

    }

    public void tag(String tagString) throws DatabaseException {

    }

    public boolean doesTagExist(String tag) throws DatabaseException {
        return false;
    }

    public boolean isSystemTable(String catalogName, String schemaName, String tableName) {
        return false;
    }

    public boolean isLiquibaseTable(String tableName) {
        return false;
    }

    public boolean shouldQuoteValue(String value) {
        return false;
    }

    public boolean supportsTablespaces() {
        return false;
    }

    public String getViewDefinition(String schemaName, String name) throws DatabaseException {
        return null;
    }

    public String getDatabaseProductName(DatabaseConnection conn) throws DatabaseException {
        return null;
    }

    public boolean isSystemView(String catalogName, String schemaName, String name) {
        return false;
    }

    public String getDateLiteral(Date date) {
        return null;
    }

    public String getTimeLiteral(Time time) {
        return null;
    }

    public String getDateTimeLiteral(Timestamp timeStamp) {
        return null;
    }

    public String getDateLiteral(java.util.Date defaultDateValue) {
        return null;
    }

    public String escapeTableName(String schemaName, String tableName) {
        return null;
    }

    public String escapeIndexName(String schemaName, String indexName) {
        return null;
    }

    public String escapeDatabaseObject(String objectName) {
        return null;
    }

    public String escapeColumnName(String schemaName, String tableName, String columnName) {
        return null;
    }

    public String escapeColumnNameList(String columnNames) {
        return null;
    }

    public String convertRequestedSchemaToSchema(String requestedSchema) throws DatabaseException {
        return null;
    }

    public String convertRequestedSchemaToCatalog(String requestedSchema) throws DatabaseException {
        return null;
    }

    public boolean supportsSchemas() {
        return false;
    }

    public String generatePrimaryKeyName(String tableName) {
        return null;
    }

    public String escapeSequenceName(String schemaName, String sequenceName) {
        return null;
    }

    public String escapeViewName(String schemaName, String viewName) {
        return null;
    }

    public ChangeSet.RunStatus getRunStatus(ChangeSet changeSet) throws DatabaseException, DatabaseHistoryException {
        return null;
    }

    public RanChangeSet getRanChangeSet(ChangeSet changeSet) throws DatabaseException, DatabaseHistoryException {
        return null;
    }

    public void markChangeSetExecStatus(ChangeSet changeSet, ChangeSet.ExecType execType) throws DatabaseException {

    }

    public List<RanChangeSet> getRanChangeSetList() throws DatabaseException {
        return null;
    }

    public java.util.Date getRanDate(ChangeSet changeSet) throws DatabaseException, DatabaseHistoryException {
        return null;
    }

    public void removeRanStatus(ChangeSet changeSet) throws DatabaseException {

    }

    public void commit() throws DatabaseException {

    }

    public void rollback() throws DatabaseException {

    }

    public String escapeStringForDatabase(String string) {
        return null;
    }

    public void close() throws DatabaseException {

    }

    public Configuration createConfiguration() {
        String url = conn.getURL();
        Configuration config = new AnnotationConfiguration();
        config.configure(getConfigFile(url));
        Properties properties = getProperties(url);
        processProperties(config, properties);
        return config;
    }

    Properties getProperties(String url) {
        Properties properties = getPropertiesInstance();
        if(isParameterized(url)) {
            String parameters = url.substring(url.indexOf("?") + 1);
            parameters = parameters.replaceAll("&", System.getProperty("line.separator"));
            parameters = URLDecoder.decode(parameters);
            try {
                properties.load(new StringReader(parameters));
            } catch (IOException ioe) {
                throw new IllegalStateException("Failed to read properties from url", ioe);
            }
        }
        return properties;
    }

    protected Properties getPropertiesInstance() {
        return new Properties();
    }

    void processProperties(Configuration config, Properties properties) {
        config.addProperties(properties);
        String namingStrategy = properties.getProperty("hibernate.namingStrategy");
        if(namingStrategy != null) {
            try {
                config.setNamingStrategy((NamingStrategy)Class.forName(namingStrategy).newInstance());
            } catch (InstantiationException e) {
                throw new IllegalStateException("Failed to instantiate naming strategy", e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Couldn't access naming strategy", e);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Failed to find naming strategy", e);
            }
        }
    }

    public boolean supportsRestrictForeignKeys() {
        return false;
    }

    public String escapeConstraintName(String constraintName) {
        return constraintName;
    }

    /**
     * As the connectionURL for Hibernate refers to a config file, just return false
     */
    public boolean isLocalDatabase() throws DatabaseException {
    	return false;
    }

    public void executeStatements(Change change, DatabaseChangeLog changeLog, List<SqlVisitor> sqlVisitors) throws LiquibaseException, UnsupportedChangeException {
        ;
    }

    public void execute(SqlStatement[] statements, List<SqlVisitor> sqlVisitors) throws LiquibaseException {
        ;
    }

    public void saveStatements(Change change, List<SqlVisitor> sqlVisitors, Writer writer) throws IOException, UnsupportedChangeException, StatementNotSupportedOnDatabaseException, LiquibaseException {
        ;
    }

    public void executeRollbackStatements(Change change, List<SqlVisitor> sqlVisitors) throws LiquibaseException, UnsupportedChangeException, RollbackImpossibleException {
        ;
    }

    public void saveRollbackStatement(Change change, List<SqlVisitor> sqlVisitors, Writer writer) throws IOException, UnsupportedChangeException, RollbackImpossibleException, StatementNotSupportedOnDatabaseException, LiquibaseException {
        ;
    }

	public String getLiquibaseSchemaName(){
		return null;
	}

    public int getNextChangeSetSequenceValue() throws LiquibaseException {
        return 1;
    }

    public java.util.Date parseDate(String dateAsString) throws DateParseException {
        return new java.util.Date();
    }

    public boolean isReservedWord(String string) {
        return false;
    }

    public void checkDatabaseChangeLogTable(boolean updateExistingNullChecksums, DatabaseChangeLog databaseChangeLog, String[] contexts) throws DatabaseException {

    }

    public boolean supportsDropTableCascadeConstraints() {
        return false;
    }
}
