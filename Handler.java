import java.util.Set;
import org.jibble.pircbot.*;

/**
 * An abstract class for handling multiple events. Ideally, this
 * should handle only a single task like notice the user or change
 * topic etc.
 */

public abstract class Handler {
    
    PircBot bot;
    String channel;
    
    /**
     * Constructor.
     * @param bot The bot this thing should interact with.
     * @param channel The channel to talk to.
     */
    public Handler(PircBot bot, String channel) {
	this.bot = bot;
	this.channel = channel;
    }

    /**
     * Called when the bot has brand new visitor list ready.
     * @param visitors Most likely an ordered set of current visitors.
     */
    public void newData(int site, Set<String> visitors) {
    }

    /**
     * Called when the bot has got a new channel topic. Topic changes
     * made by our bot are filtered out. This is called when the bot
     * joins a channel, too.
     * @param channel IRC channel.
     * @param topic New topic.
     */
    public void newTopic(String channel, String topic) {
    }
}
