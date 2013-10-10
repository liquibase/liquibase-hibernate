package liquibase.ext.hibernate.snapshot;

import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Column;
import liquibase.structure.core.DataType;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.Mapping;
import org.hibernate.mapping.PrimaryKey;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TableSnapshotGenerator extends HibernateSnapshotGenerator {

    private final static Pattern pattern = Pattern.compile("([^\\(]*)\\s*\\(?\\s*(\\d*)?\\s*,?\\s*(\\d*)?\\s*\\)?");

    public TableSnapshotGenerator() {
        super(Table.class, new Class[]{Schema.class});
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        HibernateDatabase database = (HibernateDatabase) snapshot.getDatabase();
        Configuration cfg = database.getConfiguration();

        Dialect dialect = database.getDialect();
        Mapping mapping = cfg.buildMapping();

        org.hibernate.mapping.Table hibernateTable = findHibernateTable(example, snapshot);
        if (hibernateTable == null) {
            return null;
        }

        Table table = new Table().setName(hibernateTable.getName());
        LOG.info("Found table " + table.getName());

        table.setSchema(example.getSchema());

        Iterator columnIterator = hibernateTable.getColumnIterator();
        while (columnIterator.hasNext()) {
            org.hibernate.mapping.Column hibernateColumn = (org.hibernate.mapping.Column) columnIterator.next();
            Column column = new Column();
            column.setName(hibernateColumn.getName());
            // TODO autoincrement

            String hibernateType = hibernateColumn.getSqlType(dialect, mapping);
            LOG.info("Found column " + column.getName() + " " + hibernateType);
            Matcher matcher = pattern.matcher(hibernateColumn.getSqlType(dialect, mapping));
            if (!matcher.matches())
                throw new DatabaseException("Unable to find column data type for column " + column.getName());
            DataType dataType = new DataType(matcher.group(1));
            if (matcher.group(3).isEmpty()) {
                if (!matcher.group(2).isEmpty())
                    dataType.setColumnSize(Integer.parseInt(matcher.group(2)));
            } else {
                dataType.setDecimalDigits(Integer.parseInt(matcher.group(2)));
                dataType.setRadix(Integer.parseInt(matcher.group(3)));
            }

            dataType.setDataTypeId(hibernateColumn.getSqlTypeCode());
            column.setType(dataType);

            column.setRemarks(hibernateColumn.getComment());
            column.setDefaultValue(hibernateColumn.getDefaultValue());
            column.setNullable(hibernateColumn.isNullable());
            column.setCertainDataType(false);

            PrimaryKey hibernatePrimaryKey = hibernateTable.getPrimaryKey();
            if (hibernatePrimaryKey != null) {
                if (hibernatePrimaryKey.getColumns().size() == 1 && hibernatePrimaryKey.getColumn(0).getName().equals(hibernateColumn.getName())) {
                    column.setAutoIncrementInformation(new Column.AutoIncrementInformation());
                }
            }
            column.setRelation(table);

            table.getColumns().add(column);

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
            Configuration cfg = database.getConfiguration();

            Iterator<org.hibernate.mapping.Table> tableMappings = cfg.getTableMappings();
            while (tableMappings.hasNext()) {
                org.hibernate.mapping.Table hibernateTable = (org.hibernate.mapping.Table) tableMappings.next();
                if (hibernateTable.isPhysicalTable()) {
                    Table table = new Table().setName(hibernateTable.getName());
                    table.setSchema(schema);
                    LOG.info("Found table " + table.getName());
                    schema.addDatabaseObject(table);
                }
            }
        }
    }

}
