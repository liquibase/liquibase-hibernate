package liquibase.ext.hibernate.snapshot;

import liquibase.Scope;
import liquibase.exception.DatabaseException;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.SnapshotGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Catalog;
import liquibase.structure.core.Schema;

/**
 * Hibernate doesn't really support Schemas, so just return the passed example back as if it had all the info it needed.
 */
public class SchemaSnapshotGenerator extends HibernateSnapshotGenerator {

    public SchemaSnapshotGenerator() {
        super(Schema.class, new Class[]{Catalog.class});
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        Schema schema = new Schema(example.getSchema().getCatalog(), example.getSchema().getName());
        if (snapshot.getDatabase().getDefaultSchemaName() != null && snapshot.getDatabase().getDefaultSchemaName().equalsIgnoreCase(schema.getName())) {
            schema.setDefault(true);
        }

        return schema;
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        // Nothing to do
    }

    @Override
    public Class<? extends SnapshotGenerator>[] replaces() {
        return new Class[]{liquibase.snapshot.jvm.SchemaSnapshotGenerator.class};
    }

}
