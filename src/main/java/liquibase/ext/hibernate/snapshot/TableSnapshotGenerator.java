package liquibase.ext.hibernate.snapshot;

import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.*;
import liquibase.util.StringUtils;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TableSnapshotGenerator extends HibernateSnapshotGenerator {

    private final static Pattern pattern = Pattern.compile("([^\\(]*)\\s*\\(?\\s*(\\d*)?\\s*,?\\s*(\\d*)?\\s*([^\\(]*?)\\)?");

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
        PrimaryKey primaryKey = null;
        int pkColumnPosition = 0;
        LOG.info("Found table " + table.getName());

        table.setSchema(example.getSchema());

        Iterator columnIterator = hibernateTable.getColumnIterator();
        while (columnIterator.hasNext()) {
            org.hibernate.mapping.Column hibernateColumn = (org.hibernate.mapping.Column) columnIterator.next();
            Column column = new Column();
            column.setName(hibernateColumn.getName());

            String hibernateType = hibernateColumn.getSqlType(dialect, mapping);
            DataType dataType = toDataType(hibernateType, hibernateColumn.getSqlTypeCode());
            if (dataType == null) {
                throw new DatabaseException("Unable to find column data type for column " + hibernateColumn.getName());
            }

            column.setType(dataType);
            LOG.info("Found column " + column.getName() + " " + column.getType().toString());

            column.setRemarks(hibernateColumn.getComment());
            column.setDefaultValue(hibernateColumn.getDefaultValue());
            column.setNullable(hibernateColumn.isNullable());
            column.setCertainDataType(false);

            org.hibernate.mapping.PrimaryKey hibernatePrimaryKey = hibernateTable.getPrimaryKey();
            if (hibernatePrimaryKey != null) {
                boolean isPrimaryKeyColumn = false;
                for (org.hibernate.mapping.Column pkColumn : (List<org.hibernate.mapping.Column>) hibernatePrimaryKey.getColumns()) {
                    if (pkColumn.getName().equals(hibernateColumn.getName())) {
                        isPrimaryKeyColumn = true;
                        break;
                    }
                }

                if (isPrimaryKeyColumn) {
                    if (primaryKey == null) {
                        primaryKey = new PrimaryKey();
                        primaryKey.setName(hibernatePrimaryKey.getName());
                    }
                    primaryKey.addColumnName(pkColumnPosition++, column.getName());

                    Value value = hibernateColumn.getValue();
                    if (value instanceof SimpleValue && ((SimpleValue) value).getIdentifierGeneratorStrategy().equals("identity")) {
                        column.setAutoIncrementInformation(new Column.AutoIncrementInformation());
                    }
                }
            }
            column.setRelation(table);

            table.setPrimaryKey(primaryKey);
            table.getColumns().add(column);

        }

        return table;
    }

    protected DataType toDataType(String hibernateType, Integer sqlTypeCode) throws DatabaseException {
        Matcher matcher = pattern.matcher(hibernateType);
        if (!matcher.matches()) {
            return null;
        }
        DataType dataType = new DataType(matcher.group(1));
        if (matcher.group(3).isEmpty()) {
            if (!matcher.group(2).isEmpty())
                dataType.setColumnSize(Integer.parseInt(matcher.group(2)));
        } else {
            dataType.setColumnSize(Integer.parseInt(matcher.group(2)));
            dataType.setDecimalDigits(Integer.parseInt(matcher.group(3)));
        }

        String extra = StringUtils.trimToNull(matcher.group(4));
        if (extra != null) {
            if (extra.equalsIgnoreCase("char")) {
                dataType.setColumnSizeUnit(DataType.ColumnSizeUnit.CHAR);
            }
        }

        dataType.setDataTypeId(sqlTypeCode);
        return dataType;
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
