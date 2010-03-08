import java.io.*;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import org.jibble.pircbot.*;
import java.sql.*;

public class KattilaBot extends PircBot {
    
    private static final String configFile = "irc.conf";
    private static final int maxReconnects = 3;
    private static final String sqlQuerySimulation =
	"select nick from visitor_public as v, device_public as d where d.id=v.id and site=? and jointime < date_sub(utc_timestamp(), interval ? hour) and leavetime > date_sub(utc_timestamp(), interval ? hour) order by nick;";
    
    private static final String sqlQueryReal =
	"select nick from visitor_public as v, device_public as d where d.id=v.id and leavetime is null and site=? order by nick";

    private Properties config = new Properties(); 
    private int reconnectsLeft = maxReconnects;
    private DatabaseTool dbTool = null;
    private PreparedStatement query;
    
    private Set<String> visitorsOld1 = new TreeSet<String>(); //  5 min ago
    private Set<String> visitorsOld2 = new TreeSet<String>(); // 10 min ago
    
    public KattilaBot() throws Exception {
	
	// Reading the settings
	config.load(new InputStreamReader(new FileInputStream(configFile),"UTF-8"));

        this.setName(config.getProperty("nick"));

        // Disable debugging output. Otherwise it prints all msgs to stdout.
        this.setVerbose(false);
        
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

		int history_hours =
		    Integer.parseInt(config.getProperty("history_hours","0"));
		
		if (history_hours == 0) {
		    // Real time data
		    this.query = dbTool.prepareStatement(sqlQueryReal);
		    this.query.setInt(1,Integer.parseInt(config.getProperty("site_id")));
		} else {
		    // Historical data
		    System.err.println("Back in time for "+history_hours+" hours.");
		    this.query = dbTool.prepareStatement(sqlQuerySimulation);
		    this.query.setInt(1,Integer.parseInt(config.getProperty("site_id")));
		    this.query.setInt(2,history_hours);
		    this.query.setInt(3,history_hours);
		}
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

	// Read current visitors
	Set<String> curVisitors = new TreeSet<String>();
	while (res.next()) {
	    curVisitors.add(res.getString(1));
	}
	
	// Joins: cur \ (old1 ∪ old2) = cur \ old1 \ old2
	Set<String> joins = new TreeSet<String>(curVisitors);
	joins.removeAll(visitorsOld1);
	joins.removeAll(visitorsOld2);
	
	// Leaves: old2 \ old1 \ cur
	// before it's recorded as a leave, one must be out of range two times.
	Set<String> leaves = new TreeSet<String>(visitorsOld2);
	leaves.removeAll(visitorsOld1);
	leaves.removeAll(curVisitors);

	// Now msg to IRC
	String channel = config.getProperty("channel");
	
	String joinText = beautifulOut(joins,
				       "Kattilaan saapui ",
				       "Kattilaan saapuivat ");
	String leaveText = beautifulOut(leaves,
					"Kattilasta lähti ",
					"Kattilasta lähtivät ");
	
	String extraText = "";
	if (curVisitors.size() == 0) extraText = " Kattila on nyt tyhjä.";

	if (joinText != null) sendNotice(channel, joinText);
	if (leaveText != null) sendNotice(channel, leaveText + extraText);
	
	// Pushing old visitor list down
	this.visitorsOld2 = visitorsOld1;
	this.visitorsOld1 = curVisitors;
    }
    
    /**
     * Formats string to be a beautiful list in Finnish language.
     * @param set A set of nicks
     * @returns A human friendly string.
     */
    public static String beautifulOut(Set<String> set, String singular, String plural) {
	StringBuilder sb = new StringBuilder();
	int left = set.size();

	if (left == 0 ) return null;
	if (left == 1 ) sb = new StringBuilder(singular);
	else sb = new StringBuilder(plural);

	for (String nick : set) {
	    sb.append(nick);
	    left--;

	    if (left > 1) sb.append(", ");
	    else if (left == 1) sb.append(" ja ");
	    else sb.append(".");
	}
	
	return sb.toString();
    }

    public static void main(String[] args) throws Exception {
	KattilaBot bot = new KattilaBot();
    }
}