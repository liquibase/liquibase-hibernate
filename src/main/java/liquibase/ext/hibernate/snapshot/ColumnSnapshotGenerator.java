package liquibase.ext.hibernate.snapshot;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import liquibase.snapshot.SnapshotGenerator;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.id.ExportableColumn;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.type.SqlTypes;

import liquibase.Scope;
import liquibase.datatype.DataTypeFactory;
import liquibase.datatype.core.UnknownType;
import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.statement.DatabaseFunction;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Column;
import liquibase.structure.core.DataType;
import liquibase.structure.core.Relation;
import liquibase.structure.core.Table;
import liquibase.util.SqlUtil;
import liquibase.util.StringUtil;


/**
 * Columns are snapshotted along with Tables in {@link TableSnapshotGenerator} but this class needs to be here to keep the default ColumnSnapshotGenerator from running.
 * Ideally the column logic would be moved out of the TableSnapshotGenerator to better work in situations where the object types to snapshot are being controlled, but that is not the case yet.
 */
public class ColumnSnapshotGenerator extends HibernateSnapshotGenerator {

    private static final String SQL_TIMEZONE_SUFFIX = "with time zone";
    private static final String LIQUIBASE_TIMEZONE_SUFFIX = "with timezone";

    private final static Pattern pattern = Pattern.compile("([^\\(]*)\\s*\\(?\\s*(\\d*)?\\s*,?\\s*(\\d*)?\\s*([^\\(]*?)\\)?");

    public ColumnSnapshotGenerator() {
        super(Column.class, new Class[]{Table.class});
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        Column column = (Column) example;
        if (column.getType() == null) { //not the actual full version found with the table
            if (column.getRelation() == null) {
                throw new InvalidExampleException("No relation set on " + column);
            }
            Relation relation = snapshot.get(column.getRelation());
            if (relation != null) {
                for (Column columnSnapshot : relation.getColumns()) {
                    if (columnSnapshot.getName().equalsIgnoreCase(column.getName())) {
                        return columnSnapshot;
                    }
                }
            }
            snapshotColumn((Column) example, snapshot);
            return example; //did not find it
        } else {
            return example;
        }
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        if (foundObject instanceof Table) {
            org.hibernate.mapping.Table hibernateTable = findHibernateTable(foundObject, snapshot);
            if (hibernateTable == null) {
                return;
            }

            for (org.hibernate.mapping.Column hibernateColumn: hibernateTable.getColumns()) {
                Column column = new Column();
                column.setName(hibernateColumn.getName());
                column.setRelation((Table) foundObject);

                snapshotColumn(column, snapshot);


                ((Table) foundObject).getColumns().add(column);

            }
        }
    }

    protected void snapshotColumn(Column column, DatabaseSnapshot snapshot) throws DatabaseException {
        HibernateDatabase database = (HibernateDatabase) snapshot.getDatabase();

        org.hibernate.mapping.Table hibernateTable = findHibernateTable(column.getRelation(), snapshot);
        if (hibernateTable == null) {
            return;
        }

        Dialect dialect = database.getDialect();
        MetadataImplementor metadata = (MetadataImplementor) database.getMetadata();

        for (org.hibernate.mapping.Column hibernateColumn: hibernateTable.getColumns()) {
            if (hibernateColumn.getName().equalsIgnoreCase(column.getName())) {

                String defaultValue = null;
                String hibernateType = hibernateColumn.getSqlType(metadata);

                Matcher defaultValueMatcher = Pattern.compile("(?i) DEFAULT\\s+(.*)").matcher(hibernateType);
                if (defaultValueMatcher.find()) {
                    defaultValue = defaultValueMatcher.group(1);
                    hibernateType = hibernateType.replace(defaultValueMatcher.group(0), "");
                }

                DataType dataType = toDataType(hibernateType, hibernateColumn.getSqlTypeCode());
                if (dataType == null) {
                    throw new DatabaseException("Unable to find column data type for column " + hibernateColumn.getName());
                }

                column.setType(dataType);
                Scope.getCurrentScope().getLog(getClass()).info("Found column " + column.getName() + " " + column.getType().toString());

                column.setRemarks(hibernateColumn.getComment());

                // DataTypeFactory.from and SqlUtil.parseValue rely on the database type however,
                // the liquibase-core does not know about the fake hibernate database so not all conditions
                // are handled correctly for enums.
                boolean isEnumType =  Optional.ofNullable(dataType.getDataTypeId())
                        .map(SqlTypes::isEnumType)
                        .orElse(false);

                if (!isEnumType && hibernateColumn.getValue() instanceof SimpleValue) {
                    DataType parseType;
                    if (DataTypeFactory.getInstance().from(dataType, database) instanceof UnknownType) {
                        parseType = new DataType(((SimpleValue) hibernateColumn.getValue()).getTypeName());
                    } else {
                        parseType = dataType;
                    }

                    if (defaultValue == null) {
                        defaultValue = hibernateColumn.getDefaultValue();
                    }

                    column.setDefaultValue(SqlUtil.parseValue(
                            snapshot.getDatabase(),
                            defaultValue,
                            parseType));
                } else {
                    column.setDefaultValue(hibernateColumn.getDefaultValue());
                }
                column.setNullable(hibernateColumn.isNullable());
                column.setCertainDataType(false);

                org.hibernate.mapping.PrimaryKey hibernatePrimaryKey = hibernateTable.getPrimaryKey();
                if (hibernatePrimaryKey != null) {
                    boolean isPrimaryKeyColumn = false;
                    for (org.hibernate.mapping.Column pkColumn : (List<org.hibernate.mapping.Column>) hibernatePrimaryKey.getColumns()) {
                        if (pkColumn.getName().equalsIgnoreCase(hibernateColumn.getName())) {
                            isPrimaryKeyColumn = true;
                            break;
                        }
                    }

                    if (isPrimaryKeyColumn) {
                        String identifierGeneratorStrategy;

                        if (hibernateColumn instanceof ExportableColumn) {
                            //nothing
                        } else {

                            identifierGeneratorStrategy = hibernateColumn.getValue().isSimpleValue() ? ((SimpleValue) hibernateColumn.getValue()).getIdentifierGeneratorStrategy() : null;

                            if (("native".equalsIgnoreCase(identifierGeneratorStrategy) || "identity".equalsIgnoreCase(identifierGeneratorStrategy))) {
                                if (PostgreSQLDialect.class.isAssignableFrom(dialect.getClass())) {
                                    column.setAutoIncrementInformation(new Column.AutoIncrementInformation());
                                    String sequenceName = (column.getRelation().getName() + "_" + column.getName() + "_seq").toLowerCase();
                                    column.setDefaultValue(new DatabaseFunction("nextval('" + sequenceName + "'::regclass)"));
                                } else if (database.supportsAutoIncrement()) {
                                    column.setAutoIncrementInformation(new Column.AutoIncrementInformation());
                                }
                            } else if ("org.hibernate.id.enhanced.SequenceStyleGenerator".equals(identifierGeneratorStrategy)) {
                                Properties prop = ((SimpleValue) hibernateColumn.getValue()).getIdentifierGeneratorProperties();
                                if (prop.get("sequence_name") == null)
                                    column.setAutoIncrementInformation(new Column.AutoIncrementInformation());
                            }
                        }
                        column.setNullable(false);
                    }
                }
                return;
            }
        }
    }

    protected DataType toDataType(String hibernateType, Integer sqlTypeCode) {
        Matcher matcher = pattern.matcher(hibernateType);
        if (!matcher.matches()) {
            return null;
        }

        DataType dataType;

        // Small hack for enums until DataType adds support for them
        if (Optional.ofNullable(sqlTypeCode).map(SqlTypes::isEnumType).orElse(false)) {
            dataType = new DataType(hibernateType);
        } else {
            String typeName = matcher.group(1);

            // Liquibase seems to use 'with timezone' instead of 'with time zone',
            // so we remove any 'with time zone' suffixes here.
            // The corresponding 'with timezone' suffix will then be added below,
            // because in that case hibernateType also ends with 'with time zone'.
            if (typeName.toLowerCase().endsWith(SQL_TIMEZONE_SUFFIX)) {
                typeName = typeName.substring(0, typeName.length() - SQL_TIMEZONE_SUFFIX.length()).stripTrailing();
            }

            // If hibernateType ends with 'with time zone' we need to add the corresponding
            // 'with timezone' suffix to the Liquibase type.
            if (hibernateType.toLowerCase().endsWith(SQL_TIMEZONE_SUFFIX)) {
                typeName += (" " + LIQUIBASE_TIMEZONE_SUFFIX);
            }

            dataType = new DataType(typeName);
            if (matcher.group(3).isEmpty()) {
                if (!matcher.group(2).isEmpty()) {
                    dataType.setColumnSize(Integer.parseInt(matcher.group(2)));
                }
            } else {
                dataType.setColumnSize(Integer.parseInt(matcher.group(2)));
                dataType.setDecimalDigits(Integer.parseInt(matcher.group(3)));
            }

            String extra = StringUtil.trimToNull(matcher.group(4));
            if (extra != null) {
                if (extra.equalsIgnoreCase("char")) {
                    dataType.setColumnSizeUnit(DataType.ColumnSizeUnit.CHAR);
                }
            }
        }

        Scope.getCurrentScope().getLog(getClass()).info("Converted column data type - hibernate type: " + hibernateType + ", SQL type: " + sqlTypeCode + ", type name: " + dataType.getTypeName());

        dataType.setDataTypeId(sqlTypeCode);
        return dataType;
    }

    @Override
    public Class<? extends SnapshotGenerator>[] replaces() {
        return new Class[]{liquibase.snapshot.jvm.ColumnSnapshotGenerator.class};
    }

}
