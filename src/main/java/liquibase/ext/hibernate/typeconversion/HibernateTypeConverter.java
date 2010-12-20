package liquibase.ext.hibernate.typeconversion;

import liquibase.database.Database;
import liquibase.database.typeconversion.core.AbstractTypeConverter;

public class HibernateTypeConverter extends AbstractTypeConverter {

    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    public boolean supports(Database database) {
        return database instanceof HibernateTypeConverter;
    }
}
