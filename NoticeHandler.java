import java.util.Set;
import java.util.TreeSet;
import org.jibble.pircbot.*;
import java.util.Properties;

/**
 * Class for sending notices to a channel . Ideally, this
 * should handle only a single task like notice the user or change
 * topic etc.
 */

public class NoticeHandler extends Handler {
    
    private Set<String> visitorsOld1 = new TreeSet<String>(); //  5 min ago
    private Set<String> visitorsOld2 = new TreeSet<String>(); // 10 min ago
    private Properties config;

    /**
     * Constructor.
     * @param bot The bot this thing should interact with.
     * @param channel The channel to talk to.
     */
    public NoticeHandler(PircBot bot, String channel,Properties config) {
	super(bot,channel);
	this.config = config;
    }

    /**
     * Called when the bot has brand new visitor list ready.
     * @param visitors Most likely an ordered set of current visitors.
     */
    public void newData(int site, Set<String> curVisitors) {
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
	String joinText = KattilaBot.toHumanList(joins,
						 config.getProperty("join_s")+" ",
						 config.getProperty("join_p")+" ");
	String leaveText = KattilaBot.toHumanList(leaves,
						  config.getProperty("leave_s")+" ",
						  config.getProperty("leave_p")+" ");
	
	String extraText = "";
	if (curVisitors.size() == 0) extraText = " "+config.getProperty("empty");

	if (joinText != null) bot.sendNotice(this.channel, joinText);
	if (leaveText != null) bot.sendNotice(this.channel, leaveText + extraText);
	
	// Pushing old visitor list down
	this.visitorsOld2 = visitorsOld1;
	this.visitorsOld1 = curVisitors;
    }
}
