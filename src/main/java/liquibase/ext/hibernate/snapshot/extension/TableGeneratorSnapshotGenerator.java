package liquibase.ext.hibernate.snapshot.extension;

import liquibase.structure.core.Column;
import liquibase.structure.core.DataType;
import liquibase.structure.core.PrimaryKey;
import liquibase.structure.core.Table;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.enhanced.TableGenerator;

public class TableGeneratorSnapshotGenerator implements ExtendedSnapshotGenerator<IdentifierGenerator, Table> {

    private static final String PK_DATA_TYPE = "varchar";
    private static final String VALUE_DATA_TYPE = "bigint";

    @Override
    public Table snapshot(IdentifierGenerator ig) {
        TableGenerator tableGenerator = (TableGenerator) ig;
        Table table = new Table().setName(tableGenerator.getTableName());

        Column pkColumn = new Column();
        pkColumn.setName(tableGenerator.getSegmentColumnName());
        DataType pkDataType = new DataType(PK_DATA_TYPE);
        pkDataType.setColumnSize(tableGenerator.getSegmentValueLength());
        pkColumn.setType(pkDataType);
        pkColumn.setCertainDataType(false);
        pkColumn.setRelation(table);
        table.getColumns().add(pkColumn);

        PrimaryKey primaryKey = new PrimaryKey();
        primaryKey.setName(tableGenerator.getTableName() + "PK");
        primaryKey.addColumnName(0, pkColumn.getName());
        primaryKey.setTable(table);
        table.setPrimaryKey(primaryKey);

        Column valueColumn = new Column();
        valueColumn.setName(tableGenerator.getValueColumnName());
        valueColumn.setType(new DataType(VALUE_DATA_TYPE));
        valueColumn.setNullable(false);
        valueColumn.setCertainDataType(false);
        valueColumn.setRelation(table);
        table.getColumns().add(valueColumn);

        return table;
    }

    @Override
    public boolean supports(IdentifierGenerator ig) {
        return ig instanceof TableGenerator;
    }

}
