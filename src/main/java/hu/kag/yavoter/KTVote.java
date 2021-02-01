package hu.kag.yavoter;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

public class KTVote {
	private Logger log = LogManager.getLogger();

	
	KTRoom kr;
	TextChannel voteChannel;
	String command;
	
	VoteMessage vm;
	String voteTitle;
	boolean anon;
	
	boolean voteActive;
	
	public KTVote(KTRoom _kr, TextChannel _voteChannel,String _command,String title,String[] data) {
		this.kr = _kr;
		this.voteChannel = _voteChannel;
		
		anon = _command.substring(0,_command.indexOf(" ")).contains("anon");
		voteTitle = title;
		
		voteActive=true;

		vm = new VoteMessage(anon?"TITKOS SZAVAZÁS - "+voteTitle:voteTitle);
		vm.addFields(data);
		vm.sendtoChannel(voteChannel);
		
	}

	public synchronized void refreshVoteState() {
		int needVotes = kr.getSumVote();
		while (voteActive) {
			int sumEffectiveVote = kr.getSumEffectiveVote();
			if (needVotes-sumEffectiveVote>3) { 
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

	public String getMessageId() {
		return vm.getMessageId();
	}

	public synchronized void stopVote() {
		voteActive = false;
		this.notifyAll();
		List<String> allvotes=kr.getAllEffectiveVote();
		TreeMap<String,AtomicInteger> voteaggr = new TreeMap<>();
		for (String v : allvotes) {
			AtomicInteger vc = voteaggr.get(v);
			if (vc==null) {
				vc = new AtomicInteger();
				voteaggr.put(v, vc);
			}
			vc.incrementAndGet();
		}
		vm.stopVote(voteaggr);
	}
	
}
