package hu.kag.yavoter;

import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import emoji4j.EmojiUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

public class KTVote {
	private static final String[] chooserEmojis = new String[] {"🇦", "🇧", "🇨", "🇩", "🇪", "🇫", "🇬", "🇭", "🇮", "🇯", "🇰", "🇱", "🇲", "🇳", "🇴", "🇵", "🇶", "🇷", "🇸", "🇹", "🇺", "🇻", "🇼", "🇽", "🇾", "🇿"};
	private static final String FOOTERBASE = "Kérlek amennyiben van szavazójogod, a lenti ikonok egyikének megnyomásával szavazz.\nAmennyiben meggondoltad magad, az új véleményed ikonjának gombjával szavazhatsz.\nTörölni szavazatot nem lehet, csak tartózkodni.";

	private Logger log = LogManager.getLogger();

	
	KTRoom kr;
	TextChannel voteChannel;
	String command;
	String voteTitle;
	boolean anon;
	
	EmbedBuilder eb;
	Message m;
	LinkedHashMap<String,String> options;
	boolean voteActive;
	
	public KTVote(KTRoom _kr, TextChannel _voteChannel,String _command) {
		this.kr = _kr;
		this.voteChannel = _voteChannel;
		
		anon = _command.substring(0,_command.indexOf(" ")).contains("anon");
		String[] data = _command.substring(_command.indexOf(" ") + 1).split(";");
		voteTitle = data[0];
		options = new LinkedHashMap<>();
		
		eb = new EmbedBuilder();
		eb.setTitle(anon?"TITKOS SZAVAZÁS - "+voteTitle:voteTitle);
		eb.setFooter(FOOTERBASE);
		eb.setColor(0xFF0000);
		eb.setThumbnail("https://www.mediafire.com/convkey/6b94/q2jiww8rly7xn585g.jpg");
		if (data.length==1) {
			eb.addField("Elfogadom", "<:igen:789260936439267338>", true);
			eb.addField("Elutasítom", "<:nem:789260935978025001>", true);
			eb.addField("Tartózkodom", "<:tart:789261746464358450>", true);
		} else {
			boolean inline=data.length<4;
			for (int i=1;i<data.length;i++) {
				eb.addField(data[i], chooserEmojis[i-1], inline);
			}
		}
		voteActive=true;
		
		MessageEmbed embed = eb.build();
		m = voteChannel.sendMessage(embed).complete();
		for (Field f:eb.getFields()) {
			if (EmojiUtils.isEmoji(f.getValue())) {
				options.put(EmojiUtils.getEmoji(f.getValue()).getEmoji(),f.getName());
			} else {
				options.put(f.getValue(),f.getName());
			}
			m.addReaction(f.getValue()).submit(); // A sorrendiseg miatt megvarjuk
		}
	}

	public synchronized void refreshVoteState() {
		int needVotes = kr.getSumVote();
		while (voteActive) {
			eb.setFooter(
					FOOTERBASE + "\n" + kr.getSumEffectiveVote() + " szavazat érkezett a " + needVotes + "-ból.");
			m.editMessage(eb.build()).queue();
			try {
				wait(2000);
			} catch (InterruptedException e) {
				log.warn("message refresher thread interrupted", e);
			}
		}
	}

	public void handleReaction(GuildMessageReactionAddEvent event) {
		log.info("reaction received "+event.getUserId());
		event.getReaction().removeReaction(event.getUser()).queue();
		String reactionCode = event.getReactionEmote().getAsReactionCode();
		if (options.containsKey(reactionCode)) {
			kr.registerVote(voteTitle,event.getMember(),options.get(reactionCode));
		} else {
			log.info("reaction not found in vote;"+reactionCode+" "+options.keySet());
		}
	}

	public String getMessageId() {
		return m!=null?m.getId():"";
	}

	public synchronized void stopVote() {
		voteActive = false;
		this.notifyAll();
		m.clearReactions().complete();
	}
	
}
