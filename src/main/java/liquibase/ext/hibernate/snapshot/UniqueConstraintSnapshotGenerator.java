package liquibase.ext.hibernate.snapshot;

import java.util.Iterator;

import liquibase.exception.DatabaseException;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Index;
import liquibase.structure.core.Table;
import liquibase.structure.core.UniqueConstraint;

public class UniqueConstraintSnapshotGenerator extends HibernateSnapshotGenerator {

    public UniqueConstraintSnapshotGenerator() {
        super(UniqueConstraint.class, new Class[] { Table.class });
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        return example;
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        if (!snapshot.getSnapshotControl().shouldInclude(UniqueConstraint.class)) {
            return;
        }

        if (foundObject instanceof Table) {
            Table table = (Table) foundObject;
            org.hibernate.mapping.Table hibernateTable = findHibernateTable(table, snapshot);
            Iterator uniqueIterator = hibernateTable.getUniqueKeyIterator();
            while (uniqueIterator.hasNext()) {
                org.hibernate.mapping.UniqueKey hibernateUnique = (org.hibernate.mapping.UniqueKey) uniqueIterator.next();

                Index index = new Index();
                index.setTable(table);

                // FIXME
                index.setName(table.getName() + "_" + hibernateUnique.getName());

                UniqueConstraint uniqueConstraint = new UniqueConstraint();
                uniqueConstraint.setTable(table);

                uniqueConstraint.setName(hibernateUnique.getName());

                uniqueConstraint.setBackingIndex(index);

                Iterator columnIterator = hibernateUnique.getColumnIterator();
                int i = 0;
                while (columnIterator.hasNext()) {
                    org.hibernate.mapping.Column hibernateColumn = (org.hibernate.mapping.Column) columnIterator.next();
                    index.getColumns().add(hibernateColumn.getName());
                    uniqueConstraint.addColumn(i, hibernateColumn.getName());
                    i++;
                }
                table.getUniqueConstraints().add(uniqueConstraint);
                table.getIndexes().add(index);
            }
        }
    }

}
