package liquibase.ext.hibernate.snapshot;

import liquibase.exception.DatabaseException;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Catalog;
import liquibase.structure.core.Schema;

public class SchemaSnapshotGenerator extends HibernateSnapshotGenerator {

    public SchemaSnapshotGenerator() {
	super(Schema.class, new Class[] { Catalog.class });
    }

    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
	return example;
    }

    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
	// Nothing to do
    }

}
