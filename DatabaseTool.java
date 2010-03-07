import java.io.*;
import java.sql.*;
import java.util.Properties;

/**
 * Provides database connectivity.
 */
public class DatabaseTool {

    private String dbURL;
    private Properties dbConfig = new Properties();
    private Connection conn; // database connection for reading data

    /**
     * Constructor. Opens a database connection, for example.
     */
    public DatabaseTool() throws Exception {

	final String dbConfigFile = "database.conf";

	// Some default values for the db
	this.dbConfig.setProperty("allowMultiQueries","true");

	// Reading config (overriding defaults if needed)
	this.dbConfig.load(new InputStreamReader(new FileInputStream(dbConfigFile),
						 "UTF-8"));
	// Building URI for database
	this.dbURL = "jdbc:mysql://" + dbConfig.getProperty("hostname") + 
	    "/" + dbConfig.getProperty("database");
	
	// Opening the database connection
	this.newConnection();
    }

    /**
     * Gives a new connection statement. Establishes new connections to the
     * database and prepares statements et cetera.
     */
    private void newConnection() throws SQLException {
	// A simple database connection
	this.conn = DriverManager.getConnection(dbURL,dbConfig);
    }
    
    private Connection getConnection() {
	return this.conn;
    }

    /**
     * A convenience method for preparing statements.
     * @param stmt An SQL statement to prepare.
     * @returns A prepared statement.
     */
    public PreparedStatement prepareStatement(String stmt) throws SQLException {
	return this.conn.prepareStatement(stmt);
    }
}
