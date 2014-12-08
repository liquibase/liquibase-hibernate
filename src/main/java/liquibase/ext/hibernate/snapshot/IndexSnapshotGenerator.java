package liquibase.ext.hibernate.snapshot;

import liquibase.exception.DatabaseException;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.SnapshotGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.*;

import java.util.Iterator;

public class IndexSnapshotGenerator extends HibernateSnapshotGenerator {

    public IndexSnapshotGenerator() {
        super(Index.class, new Class[]{Table.class, ForeignKey.class, UniqueConstraint.class});
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        if (example.getSnapshotId() != null) {
            return example;
        }
        Table table = ((Index) example).getTable();
        org.hibernate.mapping.Table hibernateTable = findHibernateTable(table, snapshot);
        if (hibernateTable == null) {
            return example;
        }
        Iterator indexIterator = hibernateTable.getIndexIterator();
        while (indexIterator.hasNext()) {
            org.hibernate.mapping.Index hibernateIndex = (org.hibernate.mapping.Index) indexIterator.next();
            Index index = new Index();
            index.setTable(table);
            index.setName(hibernateIndex.getName());
            Iterator columnIterator = hibernateIndex.getColumnIterator();
            while (columnIterator.hasNext()) {
                org.hibernate.mapping.Column hibernateColumn = (org.hibernate.mapping.Column) columnIterator.next();
                index.getColumns().add(new Column(hibernateColumn.getName()).setRelation(table));
            }

            if (index.getColumnNames().equalsIgnoreCase(((Index) example).getColumnNames())) {
                LOG.info("Found index " + index.getName());
                table.getIndexes().add(index);
                return index;
            }
        }
        return example;

    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        if (!snapshot.getSnapshotControl().shouldInclude(Index.class)) {
            return;
        }
        if (foundObject instanceof Table) {
            Table table = (Table) foundObject;
            org.hibernate.mapping.Table hibernateTable = findHibernateTable(table, snapshot);
            if (hibernateTable == null) {
                return;
            }
            Iterator indexIterator = hibernateTable.getIndexIterator();
            while (indexIterator.hasNext()) {
                org.hibernate.mapping.Index hibernateIndex = (org.hibernate.mapping.Index) indexIterator.next();
                Index index = new Index();
                index.setTable(table);
                index.setName(hibernateIndex.getName());
                Iterator columnIterator = hibernateIndex.getColumnIterator();
                while (columnIterator.hasNext()) {
                    org.hibernate.mapping.Column hibernateColumn = (org.hibernate.mapping.Column) columnIterator.next();
                    index.getColumns().add(new Column(hibernateColumn.getName()).setRelation(table));
                }
                LOG.info("Found index " + index.getName());
                table.getIndexes().add(index);
            }
        }
    }

}
