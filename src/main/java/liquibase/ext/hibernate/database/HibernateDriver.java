package liquibase.ext.hibernate.database;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class HibernateDriver implements Driver {

    public Connection connect(String url, Properties info) throws SQLException {
        return new HibernateConnection(url);
    }

    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith("hibernate:");
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getMajorVersion() {
        return 0;
    }

    public int getMinorVersion() {
        return 0;
    }

    public boolean jdbcCompliant() {
        return false;
    }

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}
}
