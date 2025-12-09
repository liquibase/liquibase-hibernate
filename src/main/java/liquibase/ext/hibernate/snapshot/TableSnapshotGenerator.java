package liquibase.ext.hibernate.snapshot;

import liquibase.Scope;
import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.SnapshotGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.*;

import java.util.Collection;
import java.util.Iterator;

public class TableSnapshotGenerator extends HibernateSnapshotGenerator {


    public TableSnapshotGenerator() {
        super(Table.class, new Class[]{Schema.class});
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

        if (foundObject instanceof Schema) {

            Schema schema = (Schema) foundObject;
            HibernateDatabase database = (HibernateDatabase) snapshot.getDatabase();
            MetadataImplementor metadata = (MetadataImplementor) database.getMetadata();

            Collection<PersistentClass> entityBindings = metadata.getEntityBindings();
//            Iterator<PersistentClass> tableMappings = entityBindings.iterator();
//
//            while (tableMappings.hasNext()) {
//                PersistentClass pc = tableMappings.next();
//
//                org.hibernate.mapping.Table hibernateTable = pc.getTable();
//                if (hibernateTable.isPhysicalTable()) {
//                    addDatabaseObjectToSchema(hibernateTable, schema, snapshot);
//
//                    Collection<Join> joins = pc.getJoins();
//                    Iterator<Join> joinMappings = joins.iterator();
//                    while (joinMappings.hasNext()) {
//                        Join join = joinMappings.next();
//                        addDatabaseObjectToSchema(join.getTable(), schema, snapshot);
//                    }
//                }
//            }

            for (Namespace namespace : metadata.getDatabase().getNamespaces()) {
                for (org.hibernate.mapping.Table hibernateTable : namespace.getTables()) {
                    if (hibernateTable.isPhysicalTable()) {
                        addDatabaseObjectToSchema(hibernateTable, schema, snapshot);
                        for (ForeignKey fk : hibernateTable.getForeignKeyCollection()) {
                            addDatabaseObjectToSchema(fk.getTable(), schema, snapshot);
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
        Scope.getCurrentScope().getLog(getClass()).info("Found table " + joinTable.getName());
        schema.addDatabaseObject(snapshotObject(joinTable, snapshot));
    }

    @Override
    public Class<? extends SnapshotGenerator>[] replaces() {
        return new Class[]{liquibase.snapshot.jvm.TableSnapshotGenerator.class};
    }
}
