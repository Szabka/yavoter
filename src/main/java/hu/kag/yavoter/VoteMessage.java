package hu.kag.yavoter;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import emoji4j.EmojiUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.MessageReaction;

public class VoteMessage {
	private static final String[] chooserEmojis = new String[] {"🇦", "🇧", "🇨", "🇩", "🇪", "🇫", "🇬", "🇭", "🇮", "🇯", "🇰", "🇱", "🇲", "🇳", "🇴", "🇵", "🇶", "🇷", "🇸", "🇹", "🇺", "🇻", "🇼", "🇽", "🇾", "🇿"};
	private static final String FOOTERBASE = "Kérlek amennyiben van szavazójogod, a lenti ikonok egyikének megnyomásával szavazz.\nAmennyiben meggondoltad magad, az új véleményed ikonjának gombjával szavazhatsz.\nTörölni szavazatot nem lehet, csak tartózkodni.";
	
	private Logger log = LogManager.getLogger();

	EmbedBuilder eb;
	Message m;
	LinkedHashMap<String,String> options;
	String lastcustomMessage;
	String title;
	int mode; // 0==publikus, 1==titkos kozoscsatornai, 2== titkos privatuzenet
	
	public VoteMessage(String title, int mode) {
		this.title = title;
		this.mode = mode;
		
		options = new LinkedHashMap<>();
		eb = new EmbedBuilder();
		eb.setTitle(title);
		if (mode!=1) eb.setFooter(FOOTERBASE);
		eb.setColor(0xFF0000);
		eb.setThumbnail("https://www.mediafire.com/convkey/6b94/q2jiww8rly7xn585g.jpg");
		
	}

	public void addFields(String[] data) {
		if (data.length==0) {
			eb.addField("Elfogadom", "<:igen:789260936439267338>", true);
			eb.addField("Elutasítom", "<:nem:789260935978025001>", true);
			eb.addField("Tartózkodom", "<:tart:789261746464358450>", true);
		} else {
			boolean inline=data.length<3;
			for (int i=0;i<data.length;i++) {
				eb.addField(data[i], chooserEmojis[i], inline);
			}
			eb.addField("Tartózkodom", "<:tart:789261746464358450>", inline);
		}
	}

	public void sendtoChannel(MessageChannel voteChannel) {
		
		for (Field f:eb.getFields()) {
			String fv = f.getValue();
			if (EmojiUtils.isEmoji(fv)||fv.length()<4) {
				options.put(fv,f.getName());
			} else { // emote
				fv = fv.substring(2, fv.length()-1);
				options.put(fv,f.getName());
			}
		}
		if (mode==1) eb.getFields().clear();
		
		MessageEmbed embed = eb.build();
		m = voteChannel.sendMessage(embed).complete();
		
		if (mode!=1) {
			for (String fv:options.keySet()) {
				m.addReaction(fv).submit(); // A sorrendiseg miatt megvarjuk
			}
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
		if (lastcustomMessage==null||!lastcustomMessage.equals(customMessage)) {
			eb.setDescription(customMessage);
			m.editMessage(eb.build()).queue();
			lastcustomMessage=customMessage;
		}
	}
	
	public String getMessageId() {
		return m!=null?m.getId():"";
	}

	public void stopVote(TreeMap<String,Integer> voteaggr,boolean voteControl) {
		Integer min=null,max=null;
		int sum=0,mincount=0;
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> o:options.entrySet()) {
			Integer value = voteaggr.get(o.getValue());
			if (value==null) {
				value=Integer.valueOf(0);
				voteaggr.put(o.getValue(),value);
			}
			if (mode==0||voteControl) sb.append("**"+o.getValue()+"** "+value+" szavazat\n");
			if (!"Tartózkodom".equals(o.getValue())) {
				if (min==null||value<min) {
					min=value;
					mincount=1;
				} else if (value==min) {
					mincount++;
				}
				
				if (max==null||value>max) max=value;
			}
			sum=sum+value;
		}
		if (max!=null&&max>=(sum/2+1)) { // van nyertes ag
			sb.append("van nyertes\n"); 
		} else if (mincount==1) {
			if (options.size()>3) {
				sb.append("van egyértelmű kieső\n");
				sb.append(".yavotestart");
				if (mode>0) sb.append("anon");
				sb.append(' ').append(title).append(";");
				LinkedList<String> printFields = new LinkedList<>(options.values());
				if (printFields.size()>0) printFields.removeLast();
				for (String f:printFields) {
					if (voteaggr.get(f)!=min) {
						sb.append(f).append(';');
					}
				}
			}
		} else {
			if (options.size()>3) {
				sb.append("nincs egyértelmű kieső, szétszavazás\n");
				sb.append(".yavotestart");
				if (mode>0) sb.append("anon");
				sb.append(' ').append(title).append(";");
				LinkedList<String> printFields = new LinkedList<>(options.values());
				if (printFields.size()>0) printFields.removeLast();
				for (String f:printFields) {
					if (voteaggr.get(f)==min) {
						sb.append(f).append(';');
					}
				}
			}
		}
		
		eb.getFields().clear();
		eb.setFooter(null);
		eb.setDescription(sb.toString());
		m.editMessage(eb.build()).complete();
		if (mode==0) m.clearReactions().queue();
	}

}
