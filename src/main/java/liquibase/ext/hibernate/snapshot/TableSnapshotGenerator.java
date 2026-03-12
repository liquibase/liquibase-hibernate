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
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class TableSnapshotGenerator extends HibernateSnapshotGenerator {

    private List<ExtendedSnapshotGenerator<Generator, Table>> tableIdGenerators = new ArrayList<>();

    public TableSnapshotGenerator() {
        super(Table.class, new Class[]{Schema.class});
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

        if (!schemaMatchesTable(example, hibernateTable)) {
            Scope.getCurrentScope().getLog(getClass()).info("Skipping table " + hibernateTable.getName() + " for schema " + example.getSchema().getName() + ", because it is part of another one.");
            return null;
        }

        Table table = new Table();
        table.setName(hibernateTable.getName());
        table.setSchema(example.getSchema());
        if (hibernateTable.getComment() != null && !hibernateTable.getComment().isEmpty()) {
            table.setRemarks(hibernateTable.getComment());
        }

        Scope.getCurrentScope().getLog(getClass()).info("Found table " + example.getSchema().getName() + "." + table.getName());
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
                    addDatabaseObjectToSchema(hibernateTable, schema, snapshot);

                    Collection<Join> joins = pc.getJoins();
                    Iterator<Join> joinMappings = joins.iterator();
                    while (joinMappings.hasNext()) {
                        Join join = joinMappings.next();
                        addDatabaseObjectToSchema(join.getTable(), schema, snapshot);
                    }
                }
            }

            Iterator<PersistentClass> classMappings = entityBindings.iterator();
            while (classMappings.hasNext()) {
                PersistentClass persistentClass = classMappings.next();
                if (!persistentClass.isInherited() && persistentClass.getIdentifier() instanceof SimpleValue) {
                    var simpleValue =  (SimpleValue) persistentClass.getIdentifier();
                    Generator ig = simpleValue.createGenerator(
                            metadata.getMetadataBuildingOptions().getIdentifierGeneratorFactory(),
                            database.getDialect(),
                            (RootClass) persistentClass
                    );
                    for (ExtendedSnapshotGenerator<Generator, Table> tableIdGenerator : tableIdGenerators) {
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
                    addDatabaseObjectToSchema(hTable, schema, snapshot);
                }
            }
        }
    }

    private void addDatabaseObjectToSchema(org.hibernate.mapping.Table join, Schema schema, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        Table joinTable = new Table().setName(join.getName());
        joinTable.setSchema(schema);
        schema.addDatabaseObject(snapshotObject(joinTable, snapshot));
    }

    @Override
    public Class<? extends SnapshotGenerator>[] replaces() {
        return new Class[]{liquibase.snapshot.jvm.TableSnapshotGenerator.class};
    }
}
