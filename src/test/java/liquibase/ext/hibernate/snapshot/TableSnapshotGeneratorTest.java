package liquibase.ext.hibernate.snapshot;

import junit.framework.Assert;
import liquibase.exception.DatabaseException;
import liquibase.structure.core.DataType;
import org.junit.Test;

import java.sql.Types;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNull;

public class TableSnapshotGeneratorTest {

    @Test
    public void toDataType() throws DatabaseException {
        TableSnapshotGenerator tableSnapshotGenerator = new TableSnapshotGenerator();
        DataType varchar = tableSnapshotGenerator.toDataType("varchar(255)", Types.VARCHAR);
        assertEquals("varchar", varchar.getTypeName());
        assertEquals(255, varchar.getColumnSize().intValue());
        assertEquals(Types.VARCHAR, varchar.getDataTypeId().intValue());
        assertNull(varchar.getColumnSizeUnit());

        DataType intType = tableSnapshotGenerator.toDataType("integer", Types.INTEGER);
        assertEquals("integer", intType.getTypeName());

        DataType varcharChar = tableSnapshotGenerator.toDataType("varchar2(30 char)", Types.INTEGER);
        assertEquals("varchar2", varcharChar.getTypeName());
        assertEquals(30, varcharChar.getColumnSize().intValue());
        assertEquals(DataType.ColumnSizeUnit.CHAR, varcharChar.getColumnSizeUnit());

    }
}
