package hu.kag.yavoter;

import java.util.List;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;

public class KTVote {
	private Logger log = LogManager.getLogger();

	KTRoom kr;
	TextChannel voteChannel;
	String command;
	
	VoteMessage vm;
	String voteTitle;
	boolean anon;
	
	boolean voteActive;
	Guild guild;
	
	public KTVote(Guild guild,KTRoom _kr, TextChannel _voteChannel,String _command,String title,String[] data) {
		this.kr = _kr;
		this.voteChannel = _voteChannel;
		this.guild = guild;
		
		anon = _command.contains("anon");
		voteTitle = title;
		
		voteActive=true;

		if (anon) {
			vm = new VoteMessage(voteTitle,1);
			vm.addFields(data);
			vm.sendtoChannel(voteChannel);
			kr.createPrivVotes(voteTitle,data);
		} else {
			vm = new VoteMessage(voteTitle,0);
			vm.addFields(data);
			vm.sendtoChannel(voteChannel);
		}
		
	}

	public synchronized void refreshVoteState() {
		int needVotes = kr.getSumVote();
		while (voteActive) {
			int sumEffectiveVote = kr.getSumEffectiveVote();
			if (needVotes>sumEffectiveVote*4/3&&needVotes-sumEffectiveVote>3) {  
				vm.updateFooter(sumEffectiveVote + " szavazat érkezett a " + needVotes + "-ból.");
			} else if (needVotes-sumEffectiveVote==0) {
				vm.updateFooter("Minden szavazó szavazott.");
				voteActive=false; // Nincs további frissítés
			} else { // Kiirjuk a renitens még nem szavazók listáját jól
				StringBuilder sb = new StringBuilder();
				sb.append("Még szavazniuk kell:");
				for (String vd:kr.getNoVoteList()) {
					sb.append("\n").append(vd);
				}
				vm.updateFooter(sb.toString());
			}
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
		String voteKey = vm.getVoteKey(event.getReactionEmote());
		if (voteKey!=null) {
			kr.registerVote(voteTitle,event.getMember(),voteKey);
		}
	}

	public void handleReaction(PrivateMessageReactionAddEvent event) {
		log.info("reaction received "+event.getUserId());
		String voteKey = vm.getVoteKey(event.getReactionEmote());
		Member m = guild.getMember(event.getUser());
		if (voteKey!=null&&m!=null) {
			kr.registerVote(voteTitle,m,voteKey);
		}
		//event.getReaction().removeReaction(event.getUser()).queue();
	}

	public String getMessageId() {
		return vm.getMessageId();
	}

	public synchronized void stopVote() {
		voteActive = false;
		this.notifyAll();
		List<String> allvotes=kr.getAllEffectiveVote();
		TreeMap<String,Integer> voteaggr = new TreeMap<>();
		for (String v : allvotes) {
			Integer vc = voteaggr.get(v);
			if (vc==null) {
				voteaggr.put(v, 1);
			} else {
				voteaggr.put(v, vc+1);
			}
		}
		vm.stopVote(voteaggr,!anon);
		if (anon) kr.clearPrivVotes(voteaggr);
	}
	
}
