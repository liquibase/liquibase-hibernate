package liquibase.ext.hibernate;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.core.HsqlDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.DiffResult;
import liquibase.diff.Difference;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.diff.output.report.DiffToReport;
import liquibase.ext.hibernate.database.HibernateConnection;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.logging.LogFactory;
import liquibase.logging.Logger;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Column;
import liquibase.structure.core.ForeignKey;
import liquibase.structure.core.Index;
import liquibase.structure.core.PrimaryKey;
import liquibase.structure.core.Table;

import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

public class HibernateIntegrationTest {
	private final static Logger log = LogFactory.getLogger();
	private static final String HIBERNATE_CONFIG_FILE = "Hibernate.cfg.xml";
	private Database database;
	private Connection connection;
	private CompareControl compareControl;

	@Before
	public void setUp() throws Exception {
		Class.forName("org.hsqldb.jdbc.JDBCDriver");
		connection = DriverManager.getConnection("jdbc:hsqldb:mem:TESTDB",
				"SA", "");
		database = new HsqlDatabase();
		database.setConnection(new JdbcConnection(connection));

		Set<Class<? extends DatabaseObject>> typesToInclude = new HashSet<Class<? extends DatabaseObject>>();
		typesToInclude.add(Table.class);
		typesToInclude.add(Column.class);
		typesToInclude.add(PrimaryKey.class);
		typesToInclude.add(ForeignKey.class);
		typesToInclude.add(Index.class);
		// FIXME
		// typesToInclude.add(UniqueConstraint.class);
		compareControl = new CompareControl(typesToInclude);
	}

	@After
	public void tearDown() throws Exception {
		database.close();
		connection = null;
		database = null;
		compareControl = null;
	}

	@Test
	public void runGeneratedChangeLog() throws Exception {

		Liquibase liquibase = new Liquibase(null,
				new ClassLoaderResourceAccessor(), database);

		Database hibernateDatabase = new HibernateDatabase();
		hibernateDatabase.setDefaultSchemaName("PUBLIC");
		hibernateDatabase.setDefaultCatalogName("TESTDB");
		hibernateDatabase.setConnection(new JdbcConnection(
				new HibernateConnection("hibernate:" + HIBERNATE_CONFIG_FILE)));

		DiffResult diffResult = liquibase.diff(hibernateDatabase, database,
				compareControl);

		assertTrue(diffResult.getMissingObjects().size() > 0);

		File outFile = File.createTempFile("lb-test", ".xml");
		OutputStream outChangeLog = new FileOutputStream(outFile);
		String changeLogString = toChangeLog(diffResult);
		outChangeLog.write(changeLogString.getBytes("UTF-8"));
		outChangeLog.close();

		log.info("Changelog:\n" + changeLogString);

		liquibase = new Liquibase(outFile.toString(),
				new FileSystemResourceAccessor(), database);
		liquibase.update(null);

		diffResult = liquibase
				.diff(hibernateDatabase, database, compareControl);

		ignoreDatabaseChangeLogTable(diffResult);
		// FIXME
		ignoreUnexpectedIndexes(diffResult);
		// FIXME
		ignoreCaseDifferences(diffResult);
		// FIXME
		ignoreSomeTypeDifferences(diffResult);

		String differences = toString(diffResult);

		assertEquals(differences, 0, diffResult.getMissingObjects().size());
		assertEquals(differences, 0, diffResult.getUnexpectedObjects().size());
		// FIXME
		// assertEquals(differences, 0, diffResult.getChangedObjects().size());

	}

	@Test
	public void hibernateSchemaUpdate() throws Exception {

		SimpleNamingContextBuilder builder = SimpleNamingContextBuilder
				.emptyActivatedContextBuilder();
		SingleConnectionDataSource ds = new SingleConnectionDataSource(
				connection, true);
		builder.bind("java:/data", ds);
		builder.activate();

		Configuration cfg = new Configuration();
		cfg.configure(HIBERNATE_CONFIG_FILE);

		SchemaExport export = new SchemaExport(cfg);
		export.execute(true, true, false, false);

		Database hibernateDatabase = new HibernateDatabase();
		hibernateDatabase.setDefaultSchemaName("PUBLIC");
		hibernateDatabase.setDefaultCatalogName("TESTDB");
		hibernateDatabase.setConnection(new JdbcConnection(
				new HibernateConnection("hibernate:" + HIBERNATE_CONFIG_FILE)));

		Liquibase liquibase = new Liquibase(null,
				new ClassLoaderResourceAccessor(), database);
		DiffResult diffResult = liquibase.diff(hibernateDatabase, database,
				compareControl);

		ignoreDatabaseChangeLogTable(diffResult);
		// FIXME
		ignoreUnexpectedIndexes(diffResult);
		// FIXME
		ignoreMissingIndexes(diffResult);
		// FIXME
		ignoreCaseDifferences(diffResult);
		// FIXME
		ignoreSomeTypeDifferences(diffResult);

		String differences = toString(diffResult);

		assertEquals(differences, 0, diffResult.getMissingObjects().size());
		assertEquals(differences, 0, diffResult.getUnexpectedObjects().size());
		// FIXME
		// assertEquals(differences, 0, diffResult.getChangedObjects().size());

	}

	private String toString(DiffResult diffResult) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out, true, "UTF-8");
		DiffToReport diffToReport = new DiffToReport(diffResult, printStream);
		diffToReport.print();
		printStream.close();
		return out.toString("UTF-8");
	}

	private String toChangeLog(DiffResult diffResult) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out, true, "UTF-8");
		DiffToChangeLog diffToChangeLog = new DiffToChangeLog(diffResult,
				new DiffOutputControl());
		diffToChangeLog.print(printStream);
		printStream.close();
		return out.toString("UTF-8");
	}

	private void ignoreDatabaseChangeLogTable(DiffResult diffResult)
			throws Exception {
		Set<Table> unexpectedTables = diffResult
				.getUnexpectedObjects(Table.class);
		for (Iterator<Table> iterator = unexpectedTables.iterator(); iterator
				.hasNext();) {
			Table table = iterator.next();
			if ("DATABASECHANGELOGLOCK".equalsIgnoreCase(table.getName())
					|| "DATABASECHANGELOG".equalsIgnoreCase(table.getName()))
				diffResult.getUnexpectedObjects().remove(table);
		}
		Set<Column> unexpectedColumns = diffResult
				.getUnexpectedObjects(Column.class);
		for (Iterator<Column> iterator = unexpectedColumns.iterator(); iterator
				.hasNext();) {
			Column column = iterator.next();
			if ("DATABASECHANGELOGLOCK".equalsIgnoreCase(column.getRelation()
					.getName())
					|| "DATABASECHANGELOG".equalsIgnoreCase(column
							.getRelation().getName()))
				diffResult.getUnexpectedObjects().remove(column);
		}
		Set<Index> unexpectedIndexes = diffResult
				.getUnexpectedObjects(Index.class);
		for (Iterator<Index> iterator = unexpectedIndexes.iterator(); iterator
				.hasNext();) {
			Index index = iterator.next();
			if ("DATABASECHANGELOGLOCK".equalsIgnoreCase(index.getTable()
					.getName())
					|| "DATABASECHANGELOG".equalsIgnoreCase(index.getTable()
							.getName()))
				diffResult.getUnexpectedObjects().remove(index);
		}
		Set<PrimaryKey> unexpectedPrimaryKeys = diffResult
				.getUnexpectedObjects(PrimaryKey.class);
		for (Iterator<PrimaryKey> iterator = unexpectedPrimaryKeys.iterator(); iterator
				.hasNext();) {
			PrimaryKey primaryKey = iterator.next();
			if ("DATABASECHANGELOGLOCK".equalsIgnoreCase(primaryKey.getTable()
					.getName())
					|| "DATABASECHANGELOG".equalsIgnoreCase(primaryKey
							.getTable().getName()))
				diffResult.getUnexpectedObjects().remove(primaryKey);
		}
	}

	private void ignoreUnexpectedIndexes(DiffResult diffResult)
			throws Exception {
		Set<Index> unexpectedIndexes = diffResult
				.getUnexpectedObjects(Index.class);
		diffResult.getUnexpectedObjects().removeAll(unexpectedIndexes);
	}

	private void ignoreMissingIndexes(DiffResult diffResult) throws Exception {
		Set<Index> missingIndexes = diffResult.getMissingObjects(Index.class);
		diffResult.getMissingObjects().removeAll(missingIndexes);
	}

	private void ignoreCaseDifferences(DiffResult diffResult) throws Exception {
		Map<DatabaseObject, ObjectDifferences> differences = diffResult
				.getChangedObjects();
		List<DatabaseObject> objectsToRemove = new ArrayList<DatabaseObject>();
		for (Iterator<Entry<DatabaseObject, ObjectDifferences>> iterator = differences
				.entrySet().iterator(); iterator.hasNext();) {
			Entry<DatabaseObject, ObjectDifferences> changedObject = iterator
					.next();
			List<String> differencesToRemove = new ArrayList<String>();
			for (Iterator<Difference> iterator2 = changedObject.getValue()
					.getDifferences().iterator(); iterator2.hasNext();) {
				Difference difference = iterator2.next();
				if (difference
						.getReferenceValue()
						.toString()
						.equalsIgnoreCase(
								difference.getComparedValue().toString())) {
					log.info("Ignoring difference "
							+ changedObject.getKey().toString() + " "
							+ difference.toString());
					differencesToRemove.add(difference.getField());
				}
			}
			for (Iterator<String> iterator2 = differencesToRemove.iterator(); iterator2
					.hasNext();) {
				String differenceToRemove = iterator2.next();
				changedObject.getValue().removeDifference(differenceToRemove);
			}
			if (!changedObject.getValue().hasDifferences())
				objectsToRemove.add(changedObject.getKey());
		}
		for (Iterator<DatabaseObject> iterator = objectsToRemove.iterator(); iterator
				.hasNext();) {
			DatabaseObject objectToRemove = iterator.next();
			differences.remove(objectToRemove);
		}
	}

	private void ignoreSomeTypeDifferences(DiffResult diffResult)
			throws Exception {
		Map<DatabaseObject, ObjectDifferences> differences = diffResult
				.getChangedObjects();
		List<DatabaseObject> objectsToRemove = new ArrayList<DatabaseObject>();
		for (Iterator<Entry<DatabaseObject, ObjectDifferences>> iterator = differences
				.entrySet().iterator(); iterator.hasNext();) {
			Entry<DatabaseObject, ObjectDifferences> changedObject = iterator
					.next();
			List<String> differencesToRemove = new ArrayList<String>();
			for (Iterator<Difference> iterator2 = changedObject.getValue()
					.getDifferences().iterator(); iterator2.hasNext();) {
				Difference difference = iterator2.next();
				if ((difference.getReferenceValue().toString().equals("bigint") && difference
						.getComparedValue().toString().equals("BIGINT(19)"))
						|| (difference.getReferenceValue().toString()
								.equals("integer") && difference
								.getComparedValue().toString()
								.startsWith("INTEGER(10)"))
						|| (difference.getReferenceValue().toString()
								.equals("varchar") && difference
								.getComparedValue().toString()
								.startsWith("VARCHAR(2147483647)"))
						|| (difference.getReferenceValue().toString()
								.equals("datetime") && difference
								.getComparedValue().toString()
								.startsWith("TIMESTAMP(23, 10)"))
						|| (difference.getReferenceValue().toString()
								.equals("float") && difference
								.getComparedValue().toString()
								.startsWith("DOUBLE(17)"))) {
					log.info("Ignoring difference "
							+ changedObject.getKey().toString() + " "
							+ difference.toString());
					differencesToRemove.add(difference.getField());
				}
			}
			for (Iterator<String> iterator2 = differencesToRemove.iterator(); iterator2
					.hasNext();) {
				String differenceToRemove = iterator2.next();
				changedObject.getValue().removeDifference(differenceToRemove);
			}
			if (!changedObject.getValue().hasDifferences())
				objectsToRemove.add(changedObject.getKey());
		}
		for (Iterator<DatabaseObject> iterator = objectsToRemove.iterator(); iterator
				.hasNext();) {
			DatabaseObject objectToRemove = iterator.next();
			differences.remove(objectToRemove);
		}
	}
}
