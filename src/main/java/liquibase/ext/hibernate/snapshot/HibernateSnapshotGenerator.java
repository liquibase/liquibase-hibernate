package liquibase.ext.hibernate.snapshot;

import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.ext.hibernate.database.HibernateDatabase;
import liquibase.logging.LogFactory;
import liquibase.logging.Logger;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.SnapshotGenerator;
import liquibase.snapshot.SnapshotGeneratorChain;
import liquibase.structure.DatabaseObject;
import org.hibernate.cfg.Configuration;

import java.util.Iterator;

/**
 * Base class for all Hibernate SnapshotGenerators
 */
public abstract class HibernateSnapshotGenerator implements SnapshotGenerator {

    private static final int PRIORITY_HIBERNATE_ADDITIONAL = 200;
    private static final int PRIORITY_HIBERNATE_DEFAULT = 100;

    private Class<? extends DatabaseObject> defaultFor = null;
    private Class<? extends DatabaseObject>[] addsTo = null;

    protected static final Logger LOG = LogFactory.getLogger("liquibase-hibernate");


    protected HibernateSnapshotGenerator(Class<? extends DatabaseObject> defaultFor) {
        this.defaultFor = defaultFor;
    }

    protected HibernateSnapshotGenerator(Class<? extends DatabaseObject> defaultFor, Class<? extends DatabaseObject>[] addsTo) {
        this.defaultFor = defaultFor;
        this.addsTo = addsTo;
    }

    public final int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (database instanceof HibernateDatabase) {
            if (defaultFor != null && defaultFor.isAssignableFrom(objectType)) {
                return PRIORITY_HIBERNATE_DEFAULT;
            }
            if (addsTo() != null) {
                for (Class<? extends DatabaseObject> type : addsTo()) {
                    if (type.isAssignableFrom(objectType)) {
                        return PRIORITY_HIBERNATE_ADDITIONAL;
                    }
                }
            }
        }
        return PRIORITY_NONE;

    }

    public final Class<? extends DatabaseObject>[] addsTo() {
        return addsTo;
    }

    public final DatabaseObject snapshot(DatabaseObject example, DatabaseSnapshot snapshot, SnapshotGeneratorChain chain) throws DatabaseException, InvalidExampleException {
        if (defaultFor != null && defaultFor.isAssignableFrom(example.getClass())) {
            DatabaseObject result = snapshotObject(example, snapshot);
            return result;
        }
        DatabaseObject chainResponse = chain.snapshot(example, snapshot);
        if (chainResponse == null) {
            return null;
        }
        if (addsTo() != null) {
            for (Class<? extends DatabaseObject> addType : addsTo()) {
                if (addType.isAssignableFrom(example.getClass())) {
                    if (chainResponse != null) {
                        addTo(chainResponse, snapshot);
                    }
                }
            }
        }
        return chainResponse;

    }

    protected abstract DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException;

    protected abstract void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException;

    protected org.hibernate.mapping.Table findHibernateTable(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException {
        HibernateDatabase database = (HibernateDatabase) snapshot.getDatabase();
        Configuration cfg = database.getConfiguration();
        Iterator<org.hibernate.mapping.Table> tableMappings = cfg.getTableMappings();
        while (tableMappings.hasNext()) {
            org.hibernate.mapping.Table hibernateTable = (org.hibernate.mapping.Table) tableMappings.next();
            if (hibernateTable.getName().equals(example.getName()))
                return hibernateTable;
        }
        return null;
    }
}
