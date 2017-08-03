package liquibase.ext.hibernate.diff;

import liquibase.change.Change;
import liquibase.database.Database;
import liquibase.diff.Difference;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.ChangeGeneratorChain;
import liquibase.diff.output.changelog.ChangedObjectChangeGenerator;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.UniqueConstraint;

/**
 * Hibernate doesn't know about all the variations that occur with unique constraint but just whether they exists or not.
 * To prevent changing customized constraints, we suppress all changes with hibernate.
 */
public class ChangedUniqueConstraintChangeGenerator implements ChangedObjectChangeGenerator {
    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {

        if (UniqueConstraint.class.isAssignableFrom(objectType)) {
            return PRIORITY_ADDITIONAL;
        }
        return PRIORITY_NONE;
    }

    @Override
    public Class<? extends DatabaseObject>[] runBeforeTypes() {
        return null;
    }

    @Override
    public Class<? extends DatabaseObject>[] runAfterTypes() {
        return null;
    }

    @Override
    public Change[] fixChanged(DatabaseObject changedObject, ObjectDifferences differences, DiffOutputControl control, Database referenceDatabase, Database comparisonDatabase, ChangeGeneratorChain chain) {
        if (referenceDatabase instanceof HibernateDatabase || comparisonDatabase instanceof HibernateDatabase) {
            Difference difference = differences.getDifference("unique");
            if ( unique != null && isNullOrFalse( unique.getReferenceValue() ) && isNullOrFalse( unique.getComparedValue() ) )
            {
                differences.removeDifference("unique");
                if (!differences.hasDifferences()) {
                    return null;
                }
            }
        }
        return chain.fixChanged(changedObject, differences, control, referenceDatabase, comparisonDatabase);
    }

    @Override
    public Change[] fixSchema(Change[] changes, CompareControl.SchemaComparison[] schemaComparisons) {
        return changes;
    }

    @Override
    public Change[] fixOutputAsSchema(Change[] changes, CompareControl.SchemaComparison[] schemaComparisons) {
        return changes;
    }
}
