import java.util.Set;
import java.util.TreeSet;
import org.jibble.pircbot.*;

/**
 * Class for sending notices to a channel . Ideally, this
 * should handle only a single task like notice the user or change
 * topic etc.
 */

public class TopicHandler extends Handler {
    
    // Topic head, nick list and tail.
    private String topicHead, topicList, topicTail; 

    private Set<String> oldRaw = new TreeSet<String>(); // "Raw" visitors 5 mins ago.
    private Set<String> cur = null; // Current "friendly" list of nicks.
    
    private final String heading;
    private final Pattern topicPattern;

    /**
     * Constructor.
     * @param bot The bot this thing should interact with.
     * @param channel The channel to talk to.
     */
    public TopicHandler(PircBot bot, String channel, String heading) {
	super(bot,channel);
	this.heading = heading;

	this.topicPattern = 
	    Pattern.compile("^(.*)" + Pattern.quote(heading)+"(.*?)\\.(.*)$");
    }

    /**
     * Called when the bot has brand new visitor list ready.
     * @param visitors Most likely an ordered set of current visitors.
     */
    public void newData(int site, Set<String> curRaw) {
	// "Clean" joins: old ∪ cur
	Set<String> cur = new TreeSet<String>(curRaw);
	cur.addAll(oldRaw);
		
	// Pushing visitor list as old
	this.oldRaw = curRaw;
	this.cur = cur;
    }

    public void updateTopic() {
	// Construct a new topic
	
	
	// Now msg to IRC
	String joinText = KattilaBot.toHumanList(joins,
						 "Kattilaan saapui ",
						 "Kattilaan saapuivat ");
	String leaveText = KattilaBot.toHumanList(leaves,
						  "Kattilasta lähti ",
						  "Kattilasta lähtivät ");
	
	String extraText = "";
	if (curVisitors.size() == 0) extraText = " Kattila on nyt tyhjä.";

	if (joinText != null) bot.sendNotice(this.channel, joinText);
	if (leaveText != null) bot.sendNotice(this.channel, leaveText + extraText);
    }
}
