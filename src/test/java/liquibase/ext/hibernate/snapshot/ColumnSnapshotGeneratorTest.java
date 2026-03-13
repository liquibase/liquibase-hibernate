package liquibase.ext.hibernate.snapshot;

import liquibase.exception.DatabaseException;
import liquibase.structure.core.Column;
import liquibase.structure.core.DataType;
import liquibase.structure.core.Table;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.type.SqlTypes;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.*;

public class ColumnSnapshotGeneratorTest {

    private ColumnSnapshotGenerator columnSnapshotGenerator;

    @Before
    public void setUp() {
        columnSnapshotGenerator = new ColumnSnapshotGenerator();
    }

    /**
     * Verifies that PK columns are forced to NOT NULL.
     * Matches the 5-argument signature of snapshotColumn in the source.
     */
    @Test
    public void snapshotColumn_WhenIsPrimaryKey_SetsNullableFalse() {
        // 1. Setup Hibernate Objects
        org.hibernate.mapping.Table hibTable = new org.hibernate.mapping.Table("test_table");
        org.hibernate.mapping.Column hibColumn = new org.hibernate.mapping.Column("id");
        hibColumn.setNullable(true);

        PrimaryKey hibPk = new PrimaryKey(hibTable);
        hibPk.addColumn(hibColumn);
        hibTable.setPrimaryKey(hibPk);

        // 2. Setup Liquibase Objects
        Table liquibaseTable = new Table().setName("test_table");
        Column liquibaseColumn = new Column("id");

        // 3. Execute Snapshot Logic (Passing all 5 required arguments)
        columnSnapshotGenerator.snapshotColumn(hibColumn, liquibaseColumn);

        // 4. Verification
        assertFalse("Primary key columns must be non-nullable in the snapshot",
                liquibaseColumn.isNullable());
    }

    @Test
    public void snapshotColumn_MapsRemarksAndDefaultValue() {
        org.hibernate.mapping.Column hibColumn = new org.hibernate.mapping.Column("status");
        hibColumn.setComment("Current status");
        hibColumn.setDefaultValue("'ACTIVE'");

        Column liquibaseColumn = new Column("status");
        Table liquibaseTable = new Table().setName("test_table");

        // Using 5 arguments to match class signature
        columnSnapshotGenerator.snapshotColumn(hibColumn, liquibaseColumn);

        assertEquals("Current status", liquibaseColumn.getRemarks());
        assertEquals("'ACTIVE'", liquibaseColumn.getDefaultValue().toString());
    }

    @Test
    public void toDataType() throws DatabaseException {
        // Every call below uses EXACTLY 2 arguments: (String, int)

        DataType varchar = columnSnapshotGenerator.toDataType("varchar(255)", Types.VARCHAR);
        assertEquals("varchar", varchar.getTypeName());
        assertEquals(255, varchar.getColumnSize().intValue());

        DataType intType = columnSnapshotGenerator.toDataType("integer", Types.INTEGER);
        assertEquals("integer", intType.getTypeName());

        DataType varcharChar = columnSnapshotGenerator.toDataType("varchar2(30 char)", Types.VARCHAR);
        assertEquals("varchar2", varcharChar.getTypeName());
        assertEquals(30, varcharChar.getColumnSize().intValue());

        DataType enumType = columnSnapshotGenerator.toDataType("enum ('a', 'b', 'c')", SqlTypes.ENUM);
        assertEquals("enum ('a', 'b', 'c')", enumType.getTypeName());

        DataType timestamp6WithTz = columnSnapshotGenerator.toDataType("timestamp(6) with time zone", Types.TIMESTAMP_WITH_TIMEZONE);
        assertEquals("timestamp with timezone", timestamp6WithTz.getTypeName());

        // Line 46 fix
        DataType decimalType = columnSnapshotGenerator.toDataType("decimal(10,2)", Types.DECIMAL);
        assertEquals("decimal", decimalType.getTypeName());
        assertEquals(10, decimalType.getColumnSize().intValue());
        assertEquals(2, decimalType.getDecimalDigits().intValue());

        DataType numericType = columnSnapshotGenerator.toDataType("numeric(10)", Types.NUMERIC);
        assertEquals("numeric", numericType.getTypeName());

        // Line 64 fix
        DataType nvarcharType = columnSnapshotGenerator.toDataType("nvarchar2(255)", Types.NVARCHAR);
        assertEquals("nvarchar2", nvarcharType.getTypeName());

        DataType nvarcharMax = columnSnapshotGenerator.toDataType("nvarchar(max)", Types.NVARCHAR);
        assertEquals("nvarchar max", nvarcharMax.getTypeName());

        DataType int11 = columnSnapshotGenerator.toDataType("int(11)", Types.INTEGER);
        assertEquals("int", int11.getTypeName().toLowerCase());
    }
}