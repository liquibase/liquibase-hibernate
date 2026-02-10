package liquibase.ext.hibernate.snapshot;

import liquibase.Scope;
import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.ext.hibernate.snapshot.extension.ExtendedSnapshotGenerator;
import liquibase.ext.hibernate.snapshot.extension.TableGeneratorSnapshotGenerator;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.SnapshotGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.mapping.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TableSnapshotGenerator extends HibernateSnapshotGenerator {

    private final List<ExtendedSnapshotGenerator<Generator, Table>> tableIdGenerators = new ArrayList<>();

    public TableSnapshotGenerator() {
        super(Table.class, new Class[]{Schema.class});
        tableIdGenerators.add(new TableGeneratorSnapshotGenerator());
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        if (example.getSnapshotId() != null) {
            return example;
        }
        var hibernateTable = findHibernateTable(example, snapshot);
        if (hibernateTable == null) {
            return example;
        }

        var table = new Table().setName(hibernateTable.getName());
        Scope.getCurrentScope().getLog(getClass()).info("Found table " + table.getName());
        table.setSchema(example.getSchema());
        if (hibernateTable.getComment() != null && !hibernateTable.getComment().isEmpty()) {
            table.setRemarks(hibernateTable.getComment());
        }

        return table;
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        if (!snapshot.getSnapshotControl().shouldInclude(Table.class)) {
            return;
        }

        if (foundObject instanceof Schema schema) {

            var database = (HibernateDatabase) snapshot.getDatabase();
            var metadata = (MetadataImplementor) database.getMetadata();

            var entityBindings = metadata.getEntityBindings();

            for (var persistentClass : entityBindings) {
                var hibernateTable = persistentClass.getTable();
                if (hibernateTable.isPhysicalTable()) {
                    addDatabaseObjectToSchema(hibernateTable, schema, snapshot);

                    var joins = persistentClass.getJoins();
                    for (var join : joins) {
                        addDatabaseObjectToSchema(join.getTable(), schema, snapshot);
                    }
                }
            }

            for (var persistentClass : entityBindings) {
                if (!persistentClass.isInherited() && persistentClass.getIdentifier() instanceof SimpleValue simpleValue) {
                    var generator = simpleValue.createGenerator(
                        database.getDialect(),
                        persistentClass.getRootClass()
                    );
                    for (var tableIdGenerator : tableIdGenerators) {
                        if (tableIdGenerator.supports(generator)) {
                            var idTable = tableIdGenerator.snapshot(generator);
                            idTable.setSchema(schema);
                            schema.addDatabaseObject(snapshotObject(idTable, snapshot));
                            break;
                        }
                    }
                }
            }

            Collection<org.hibernate.mapping.Collection> collectionBindings = metadata.getCollectionBindings();
            for (org.hibernate.mapping.Collection coll : collectionBindings) {
                org.hibernate.mapping.Table hTable = coll.getCollectionTable();
                if (hTable.isPhysicalTable()) {
                    addDatabaseObjectToSchema(hTable, schema, snapshot);
                }
            }
        }
    }

    private void addDatabaseObjectToSchema(org.hibernate.mapping.Table join, Schema schema, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        Table joinTable = new Table().setName(join.getName());
        joinTable.setSchema(schema);
        Scope.getCurrentScope().getLog(getClass()).info("Found table " + joinTable.getName());
        schema.addDatabaseObject(snapshotObject(joinTable, snapshot));
    }

    @Override
    public Class<? extends SnapshotGenerator>[] replaces() {
        return new Class[]{liquibase.snapshot.jvm.TableSnapshotGenerator.class};
    }
}
