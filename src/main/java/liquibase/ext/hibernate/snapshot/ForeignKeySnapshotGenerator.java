package liquibase.ext.hibernate.snapshot;

import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Column;
import liquibase.structure.core.ForeignKey;
import liquibase.structure.core.Table;
import org.hibernate.cfg.Configuration;

import java.util.Iterator;

public class ForeignKeySnapshotGenerator extends HibernateSnapshotGenerator {

    public ForeignKeySnapshotGenerator() {
        super(ForeignKey.class, new Class[]{Table.class});
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        return example;
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        if (!snapshot.getSnapshotControl().shouldInclude(ForeignKey.class)) {
            return;
        }
        if (foundObject instanceof Table) {
            Table table = (Table) foundObject;
            HibernateDatabase database = (HibernateDatabase) snapshot.getDatabase();
            Configuration cfg = database.getConfiguration();
            Iterator<org.hibernate.mapping.Table> tableMappings = cfg.getTableMappings();
            while (tableMappings.hasNext()) {
                org.hibernate.mapping.Table hibernateTable = (org.hibernate.mapping.Table) tableMappings.next();
                Iterator<org.hibernate.mapping.ForeignKey> fkIterator = hibernateTable.getForeignKeyIterator();
                while (fkIterator.hasNext()) {
                    org.hibernate.mapping.ForeignKey hibernateForeignKey = fkIterator.next();
                    Table currentTable = new Table().setName(hibernateTable.getName());
                    currentTable.setSchema(hibernateTable.getCatalog(), hibernateTable.getSchema());
                    org.hibernate.mapping.Table hibernateReferencedTable = hibernateForeignKey.getReferencedTable();
                    Table referencedTable = new Table().setName(hibernateReferencedTable.getName());
                    referencedTable.setSchema(hibernateReferencedTable.getCatalog(), hibernateReferencedTable.getSchema());
                    if (hibernateForeignKey.isPhysicalConstraint()) {
                        ForeignKey fk = new ForeignKey();
                        fk.setName(hibernateForeignKey.getName());
                        fk.setPrimaryKeyTable(referencedTable);
                        for (Object hibernateColumn : hibernateForeignKey.getColumns()) {
                            fk.addForeignKeyColumn(toLiquibaseColumn(hibernateColumn));
                        }
                        fk.setForeignKeyTable(currentTable);
                        for (Object hibernateColumn : hibernateForeignKey.getReferencedColumns()) {
                            fk.addPrimaryKeyColumn(toLiquibaseColumn(hibernateColumn));
                        }
                        if (fk.getPrimaryKeyColumns() == null || fk.getPrimaryKeyColumns().isEmpty()) {
                            for (Object hibernateColumn : hibernateReferencedTable.getPrimaryKey().getColumns()) {
                                fk.addPrimaryKeyColumn(toLiquibaseColumn(hibernateColumn));
                            }
                        }

                        fk.setDeferrable(false);
                        fk.setInitiallyDeferred(false);

//			Index index = new Index();
//			index.setName("IX_" + fk.getName());
//			index.setTable(fk.getForeignKeyTable());
//			index.setColumns(fk.getForeignKeyColumns());
//			fk.setBackingIndex(index);
//			table.getIndexes().add(index);

                        if (currentTable.equals(table)) {
                            table.getOutgoingForeignKeys().add(fk);
                        }
                    }
                }
            }
        }
    }

    private Column toLiquibaseColumn(Object hibernateColumn) {
        return new Column(((org.hibernate.mapping.Column) hibernateColumn).getName());
    }

}
