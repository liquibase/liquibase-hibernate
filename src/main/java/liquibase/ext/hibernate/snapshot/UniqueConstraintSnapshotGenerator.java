package liquibase.ext.hibernate.snapshot;

import java.util.Iterator;

import liquibase.exception.DatabaseException;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Table;
import liquibase.structure.core.UniqueConstraint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniqueConstraintSnapshotGenerator extends
		HibernateSnapshotGenerator {

	private static final Logger LOG = LoggerFactory
			.getLogger(UniqueConstraintSnapshotGenerator.class);

	public UniqueConstraintSnapshotGenerator() {
		super(UniqueConstraint.class, new Class[] { Table.class });
	}

	@Override
	protected DatabaseObject snapshotObject(DatabaseObject example,
			DatabaseSnapshot snapshot) throws DatabaseException,
			InvalidExampleException {
		return example;
	}

	@Override
	protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot)
			throws DatabaseException, InvalidExampleException {
		if (!snapshot.getSnapshotControl()
				.shouldInclude(UniqueConstraint.class)) {
			return;
		}

		if (foundObject instanceof Table) {
			Table table = (Table) foundObject;
			org.hibernate.mapping.Table hibernateTable = findHibernateTable(
					table, snapshot);
			Iterator uniqueIterator = hibernateTable.getUniqueKeyIterator();
			while (uniqueIterator.hasNext()) {
				org.hibernate.mapping.UniqueKey hibernateUnique = (org.hibernate.mapping.UniqueKey) uniqueIterator
						.next();

				UniqueConstraint uniqueConstraint = new UniqueConstraint();
				String name = "UC_" + table.getName().toUpperCase();
				uniqueConstraint.setTable(table);
				Iterator columnIterator = hibernateUnique.getColumnIterator();
				int i = 0;
				while (columnIterator.hasNext()) {
					org.hibernate.mapping.Column hibernateColumn = (org.hibernate.mapping.Column) columnIterator
							.next();
					name += "_" + hibernateColumn.getName().toUpperCase();
					uniqueConstraint.addColumn(i, hibernateColumn.getName());
					i++;
				}
				uniqueConstraint.setName(name);
				LOG.info("Found unique constraint "
						+ uniqueConstraint.toString());
				table.getUniqueConstraints().add(uniqueConstraint);
			}
		}
	}

}
