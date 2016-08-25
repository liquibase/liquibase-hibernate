package liquibase.ext.hibernate.snapshot;

import liquibase.datatype.DataTypeFactory;
import liquibase.datatype.LiquibaseDataType;
import liquibase.datatype.core.UnknownType;
import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.ext.hibernate.snapshot.extension.ExtendedSnapshotGenerator;
import liquibase.ext.hibernate.snapshot.extension.MultipleHiLoPerTableSnapshotGenerator;
import liquibase.ext.hibernate.snapshot.extension.TableGeneratorSnapshotGenerator;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.JdbcDatabaseSnapshot;
import liquibase.snapshot.SnapshotIdService;
import liquibase.statement.DatabaseFunction;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.*;
import liquibase.util.SqlUtil;
import liquibase.util.StringUtils;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TableSnapshotGenerator extends HibernateSnapshotGenerator {

    private List<ExtendedSnapshotGenerator<IdentifierGenerator, Table>> tableIdGenerators = new ArrayList<ExtendedSnapshotGenerator<IdentifierGenerator, Table>>();

    public TableSnapshotGenerator() {
        super(Table.class, new Class[]{Schema.class});
        tableIdGenerators.add(new MultipleHiLoPerTableSnapshotGenerator());
        tableIdGenerators.add(new TableGeneratorSnapshotGenerator());
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        if (example.getSnapshotId() != null) {
            return example;
        }
        org.hibernate.mapping.Table hibernateTable = findHibernateTable(example, snapshot);
        if (hibernateTable == null) {
            return example;
        }

        Table table = new Table().setName(hibernateTable.getName());
        LOG.info("Found table " + table.getName());
//        table.setSnapshotId(SnapshotIdService.getInstance().generateId());
        table.setSchema(example.getSchema());


        return table;
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        if (!snapshot.getSnapshotControl().shouldInclude(Table.class)) {
            return;
        }

        if (foundObject instanceof Schema) {

            Schema schema = (Schema) foundObject;
            HibernateDatabase database = (HibernateDatabase) snapshot.getDatabase();
            MetadataImplementor metadata = (MetadataImplementor) database.getMetadata();

            Collection<PersistentClass> entityBindings = metadata.getEntityBindings();
            Iterator<PersistentClass> tableMappings = entityBindings.iterator();

            while (tableMappings.hasNext()) {
                PersistentClass pc = tableMappings.next();

                org.hibernate.mapping.Table hibernateTable = pc.getTable();
                if (hibernateTable.isPhysicalTable()) {
                    Table table = new Table().setName(hibernateTable.getName());
                    table.setSchema(schema);
                    LOG.info("Found table " + table.getName());
                    schema.addDatabaseObject(snapshotObject(table, snapshot));
                }
            }

            Iterator<PersistentClass> classMappings = entityBindings.iterator();
            while (classMappings.hasNext()) {
                PersistentClass persistentClass = (PersistentClass) classMappings
                        .next();
                if (!persistentClass.isInherited()) {
                    IdentifierGenerator ig = persistentClass.getIdentifier().createIdentifierGenerator(
                            metadata.getIdentifierGeneratorFactory(),
                            database.getDialect(),
                            null,
                            null,
                            (RootClass) persistentClass
                    );
                    for (ExtendedSnapshotGenerator<IdentifierGenerator, Table> tableIdGenerator : tableIdGenerators) {
                        if (tableIdGenerator.supports(ig)) {
                            Table idTable = tableIdGenerator.snapshot(ig);
                            idTable.setSchema(schema);
                            schema.addDatabaseObject(snapshotObject(idTable, snapshot));
                            break;
                        }
                    }
                }
            }

            Collection<org.hibernate.mapping.Collection> collectionBindings = metadata.getCollectionBindings();
            Iterator<org.hibernate.mapping.Collection> collIter = collectionBindings.iterator();
            while (collIter.hasNext()) {
                org.hibernate.mapping.Collection coll = collIter.next();
                org.hibernate.mapping.Table hTable = coll.getCollectionTable();
                if (hTable.isPhysicalTable()) {
                    Table table = new Table().setName(hTable.getName());
                    table.setSchema(schema);
                    LOG.info("Found table " + table.getName());
                    schema.addDatabaseObject(snapshotObject(table, snapshot));
                }
            }
        }
    }
}
