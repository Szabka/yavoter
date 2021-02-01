package hu.kag.yavoter;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import emoji4j.EmojiUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;

public class VoteMessage {
	private static final String[] chooserEmojis = new String[] {"🇦", "🇧", "🇨", "🇩", "🇪", "🇫", "🇬", "🇭", "🇮", "🇯", "🇰", "🇱", "🇲", "🇳", "🇴", "🇵", "🇶", "🇷", "🇸", "🇹", "🇺", "🇻", "🇼", "🇽", "🇾", "🇿"};
	private static final String FOOTERBASE = "Kérlek amennyiben van szavazójogod, a lenti ikonok egyikének megnyomásával szavazz.\nAmennyiben meggondoltad magad, az új véleményed ikonjának gombjával szavazhatsz.\nTörölni szavazatot nem lehet, csak tartózkodni.";

	private Logger log = LogManager.getLogger();

	EmbedBuilder eb;
	Message m;
	LinkedHashMap<String,String> options;
	
	public VoteMessage(String title) {
		options = new LinkedHashMap<>();
		eb = new EmbedBuilder();
		eb.setTitle(title);
		eb.setFooter(FOOTERBASE);
		eb.setColor(0xFF0000);
		eb.setThumbnail("https://www.mediafire.com/convkey/6b94/q2jiww8rly7xn585g.jpg");
	}

	public void addFields(String[] data) {
		if (data.length==0) {
			eb.addField("Elfogadom", "<:igen:789260936439267338>", true);
			eb.addField("Elutasítom", "<:nem:789260935978025001>", true);
			eb.addField("Tartózkodom", "<:tart:789261746464358450>", true);
		} else {
			boolean inline=data.length<4;
			for (int i=1;i<data.length;i++) {
				eb.addField(data[i], chooserEmojis[i-1], inline);
			}
		}
	}

	public void sendtoChannel(TextChannel voteChannel) {
		MessageEmbed embed = eb.build();
		m = voteChannel.sendMessage(embed).complete();
		for (Field f:eb.getFields()) {
			if (EmojiUtils.isEmoji(f.getValue())) {
				options.put(f.getValue(),f.getName());
			} else {
				options.put(f.getValue(),f.getName());
			}
			m.addReaction(f.getValue()).submit(); // A sorrendiseg miatt megvarjuk
		}
	}
	
	public String getVoteKey(MessageReaction.ReactionEmote reaction ) {
		String reactionCode = reaction.getAsReactionCode();
		if (options.containsKey(reactionCode)) {
			return options.get(reactionCode);
		} else if (reaction.isEmote()&&options.containsKey(reaction.getEmote().getAsMention())) {
			return options.get(reaction.getEmote().getAsMention());
		} else {
			log.info("reaction not found in vote;"+reactionCode+" "+options.keySet());
		}
		return null;
	}
	
	public void updateFooter(String customMessage) {
		eb.setFooter(FOOTERBASE + "\n" + customMessage);
		m.editMessage(eb.build()).queue();
	}
	
	public String getMessageId() {
		return m!=null?m.getId():"";
	}

	public void stopVote(TreeMap<String,AtomicInteger> voteaggr) {
		eb.getFields().clear();
		for (Entry<String, String> o:options.entrySet()) {
			eb.addField(o.getValue(), o.getKey()+"\n"+voteaggr.get(o.getValue()).get(), false);
		}
		m.editMessage(eb.build()).complete();
		m.clearReactions().queue();
	}

}
