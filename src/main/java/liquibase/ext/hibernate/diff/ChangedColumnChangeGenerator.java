package liquibase.ext.hibernate.diff;

import liquibase.change.Change;
import liquibase.database.Database;
import liquibase.diff.Difference;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.output.DiffOutputControl;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.statement.DatabaseFunction;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Column;
import liquibase.structure.core.DataType;

import java.util.List;

/**
 * Hibernate and database types tend to look different even though they are not.
 * The only change that we are handling it size change, and even for this one there are exceptions.
 */
public class ChangedColumnChangeGenerator extends liquibase.diff.output.changelog.core.ChangedColumnChangeGenerator {

    private static final List<String> TYPES_TO_IGNORE_SIZE = List.of("TIMESTAMP", "TIME");

    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (Column.class.isAssignableFrom(objectType)) {
            return PRIORITY_ADDITIONAL;
        }
        return PRIORITY_NONE;
    }

    @Override
    protected void handleTypeDifferences(Column column, ObjectDifferences differences, DiffOutputControl control, List<Change> changes, Database referenceDatabase, Database comparisonDatabase) {
        if (referenceDatabase instanceof HibernateDatabase || comparisonDatabase instanceof HibernateDatabase) {
            handleSizeChange(column, differences, control, changes, referenceDatabase, comparisonDatabase);
        } else {
            super.handleTypeDifferences(column, differences, control, changes, referenceDatabase, comparisonDatabase);
        }
    }

    private void handleSizeChange(Column column, ObjectDifferences differences, DiffOutputControl control, List<Change> changes, Database referenceDatabase, Database comparisonDatabase) {
        if (TYPES_TO_IGNORE_SIZE.stream().anyMatch(s -> s.equalsIgnoreCase(column.getType().getTypeName()))) {
            return;
        }
        Difference difference = differences.getDifference("type");
        if (difference != null) {
            if (difference.getReferenceValue() instanceof DataType referencedType) {
                Integer originalSize = referencedType.getColumnSize();
                Integer newSize = difference.getComparedValue() instanceof DataType comparedType
                        ? comparedType.getColumnSize() : null;
                if (newSize == null || originalSize == null || newSize.equals(originalSize)) {
                    differences.removeDifference("type");
                }
            }
            super.handleTypeDifferences(column, differences, control, changes, referenceDatabase, comparisonDatabase);
        }
    }

    @Override
    protected void handleDefaultValueDifferences(Column column, ObjectDifferences differences, DiffOutputControl control, List<Change> changes, Database referenceDatabase, Database comparisonDatabase) {
        if (referenceDatabase instanceof HibernateDatabase || comparisonDatabase instanceof HibernateDatabase) {
            Difference difference = differences.getDifference("defaultValue");
            if (difference != null && difference.getReferenceValue() == null && difference.getComparedValue() instanceof DatabaseFunction) {
                //database sometimes adds a function default value, like for timestamp columns
                return;
            }
            difference = differences.getDifference("defaultValue");
            if (difference != null) {
                super.handleDefaultValueDifferences(column, differences, control, changes, referenceDatabase, comparisonDatabase);
            }
            // do nothing, types tend to not match with hibernate
        }
        super.handleDefaultValueDifferences(column, differences, control, changes, referenceDatabase, comparisonDatabase);
    }
}
