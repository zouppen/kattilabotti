import java.io.*;
import java.util.Properties;
import org.jibble.pircbot.*;
import java.sql.*;

public class KattilaBot extends PircBot {
    
    private static final String configFile = "irc.conf";
    private static final int maxReconnects = 3;
    private static final String lastStr = "zzzzzzzz";
    private static final String sqlQuery =
	"select nick from visitor_public as v, device_public as d where d.id=v.id and jointime < date_sub(utc_timestamp(), interval 9 hour) and leavetime > date_sub(utc_timestamp(), interval 9 hour) and site=? order by nick;";

    // when not simulating: select * from visitor_public as v, device_public as d where d.id=v.id and leavetime is null and site=1 order by nick;

    private Properties config = new Properties(); 
    private int reconnectsLeft = maxReconnects;
    private DatabaseTool dbTool = null;
    private PreparedStatement query;

    private ResultSet oldResult = null;
    
    public KattilaBot() throws Exception {
	
	// Reading the settings
	config.load(new InputStreamReader(new FileInputStream(configFile),"UTF-8"));

        this.setName(config.getProperty("nick"));

        // Enable debugging output.
        this.setVerbose(true);
        
        // Open connection to IRC
        this.connect(config.getProperty("server"));
	this.joinChannel(config.getProperty("channel"));
	
	// Puts in a signal handler which alerts about new data in the
        // database. This uses Oracle's Java extension which may not
        // be a part of your JRE. If so, implement a timer which runs
        // check() periodically.
        ProgressSignalHandler.install(this);

    }
    
    public void check() throws Exception {
	ResultSet res;
	
	if (this.reconnectsLeft == 0) {
	    System.err.println("No more reconnects!");
	    return;
	}

	try {
	    if (dbTool == null) {
		// Connect the database if needed
		this.dbTool = new DatabaseTool();
		System.err.println("Got a new connection to the database.");

		this.query = dbTool.prepareStatement(sqlQuery);
		this.query.setInt(1,Integer.parseInt(config.getProperty("site_id")));
	    }
	    
	    // Bind fresh params if needed...

	    res = this.query.executeQuery();
	} catch (SQLException sql_e) {
	    System.err.println("Database connection has been lost.");
	    sql_e.printStackTrace();
	    this.reconnectsLeft--;
	    this.dbTool = null;
	    return;
	}

	// No errors if this is reached
	this.reconnectsLeft = this.maxReconnects;

	// Now msg to IRC
	if (this.oldResult != null) { // Do not report on startup
	    String old = getNext(oldResult);
	    String cur = getNext(res);
	    String channel = config.getProperty("channel");
	    
	    while (!(lastStr.equals(old) && lastStr.equals(cur))) {
		int diff = old.compareTo(cur);

		if (diff < 0) {
		    // Leave of 'old'
		    sendMessage(channel, "Lähti: "+old);
		    old = getNext(oldResult);
		} else if (diff > 0) {
		    // Join of 'cur'
		    sendMessage(channel, "Saapui: "+cur);
		    cur = getNext(res);
		} else {
		    // If still present, report nothing but debug.
		    sendMessage(channel, "Paikalla vielä: "+cur);
		}
	    }
	}

	res.beforeFirst(); // roll to start
	this.oldResult = res;
    }
    
    public static String getNext(ResultSet res) throws SQLException {
	if (!res.next()) return lastStr;
	return res.getString(1);
    }

    public static void main(String[] args) throws Exception {
	KattilaBot bot = new KattilaBot();
    }
}