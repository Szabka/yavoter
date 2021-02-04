package hu.kag.yavoter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.Role;

public class KTRepr {
	private Logger log = LogManager.getLogger();

	boolean teacher;
	int votes;
	TreeMap<String,KTReprData> representatives;
	Role vr;
	Role cr;
	LinkedList<String> votelist;
	
	public KTRepr(boolean _teacher,int _votes,Role _vr,Role _cr) {
		this.teacher = _teacher;
		this.votes = _votes;
		this.vr = _vr;
		this.cr = _cr;
	
		this.votelist = new LinkedList<>();
		representatives = new TreeMap<>();
	}
	
	public void addRepr(Member vm) {
		if (!teacher&&cr==null) {
			log.warn("reps is not teacher but no class role set:"+vm);
		}
		representatives.put(vm.getId(),new KTReprData(vm));
	}

	public Collection<Member> getRepr() {
		LinkedList<Member> ret = new LinkedList<>();
		for (KTReprData r : representatives.values()) {
			ret.add(r.m);
		}
		return ret;
	}

	public int getVotes() {
		return votes;
	}

	public String getDetail() {
		if (teacher) {
			return vr.getName()+":"+representatives.values().iterator().next().m.getEffectiveName()+":"+votes+" szavazat";
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(vr.getName()).append("-").append(cr.getName()).append(":(");
			boolean first=true;
			for (KTReprData r : representatives.values()) {
				if (!first) sb.append(',');
				sb.append(r.m.getEffectiveName());
				first=false;
			}
			sb.append("):"+votes+" szavazat");
			return sb.toString();
		}
	}
	
	public String getVoteDetail() {
		if (teacher) {
			return vr.getName()+":"+representatives.values().iterator().next().m.getEffectiveName()+" leadott "+getEffectiveVotes().size();
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(vr.getName()).append("-").append(cr.getName()).append(":(");
			boolean first=true;
			for (KTReprData r : representatives.values()) {
				if (!first) sb.append(',');
				sb.append(r.m.getEffectiveName());
				first=false;
			}
			sb.append(")");
			return sb.toString();
		}
	}
	
	public synchronized void registerVote(String voteTitle,Member m,String vote) {
		if (log.isDebugEnabled()) log.debug("incoming vote "+vote+" "+representatives);
		StringBuilder mb = new StringBuilder();
		mb.append(voteTitle).append('\n');
		votelist.addLast(vote);
		if (votelist.size()>votes) votelist.removeFirst(); // a legregebbi kiszedese

		if (teacher) {
			switch (votelist.size()) {
			case 1:
				mb.append("Ezt a szavazatot kapta a rendszer tőled: ").append(vote).append("\nEgyéb szavazat hiányában ez automatikusan 3 szavazatként van kezelve.");
				break;
			default:
				mb.append("Ezeket a szavazatokat kapta a rendszer tőled: ").append(votelist);
				break;
			}
		} else {
			mb.append(m.getEffectiveName()).append(" ezt a szavazatot adta le a ")
			.append(vr.getName()).append("-").append(cr.getName()).append(" képviselőjeként : ** ").append(vote).append(" **");
		}
		String content = mb.toString();
		for (KTReprData r : representatives.values()) {
			if (r.vm == null) {
				if (r.rm == null) {
					r.pc = r.m.getUser().openPrivateChannel().complete();
					r.rm = r.pc.sendMessage(content).complete();
				} else {
					r.rm.editMessage(content).queue();
				}
			} else {
				r.vm.updateFooter(content.substring(voteTitle.length()+1)); // levagjuk a title-t.
			}
		}		
	}
	
	public synchronized List<String> getEffectiveVotes() {
		LinkedList<String> ret = new LinkedList<>(votelist);
		if (teacher&&votelist.size()==1) {
			ret.addAll(votelist);
			ret.addAll(votelist);
		}
		return ret;
	}
	
	public void createPrivVotes(String voteTitle, String[] data) {
		for (KTReprData r : representatives.values()) {
			r.vm = new VoteMessage(voteTitle, 2);
			r.vm.addFields(data);
			if (r.pc==null) {
				r.pc = r.m.getUser().openPrivateChannel().complete();
			}
			r.vm.sendtoChannel(r.pc);
		}		
	}

	private class KTReprData {
		Member m;
		PrivateChannel pc;
		Message rm;
		VoteMessage vm;
		
		public KTReprData(Member _m) {
			m=_m;
		}
	}

	public void createPrivVotes(TreeMap<String, Integer> voteaggr, String voteControlRole) {
		for (KTReprData r : representatives.values()) {
			boolean isVoteControl = r.m.getRoles().stream().anyMatch(role -> (role.getId().equals(voteControlRole)));
			if (r.vm!=null) {
				r.vm.stopVote(voteaggr,isVoteControl);
			}
		}		
	}

}
