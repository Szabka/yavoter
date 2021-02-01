package hu.kag.yavoter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class KTRepr {
	private Logger log = LogManager.getLogger();

	boolean teacher;
	int votes;
	TreeMap<String,Member> representatives;
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
		representatives.put(vm.getId(),vm);
	}

	public Collection<Member> getRepr() {
		return representatives.values();
	}

	public int getVotes() {
		return votes;
	}

	public String getDetail() {
		if (teacher) {
			return vr.getName()+":"+representatives.values().iterator().next().getEffectiveName()+":"+votes+" szavazat";
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(vr.getName()).append("-").append(cr.getName()).append(":(");
			boolean first=true;
			for (Member r : representatives.values()) {
				if (!first) sb.append(',');
				sb.append(r.getEffectiveName());
				first=false;
			}
			sb.append("):"+votes+" szavazat");
			return sb.toString();
		}
	}
	
	public String getVoteDetail() {
		return getDetail()+" leadott "+getEffectiveVotes().size();
	}
	
	public synchronized void registerVote(String voteTitle,Member m,String vote) {
		log.info("incoming vote "+vote+" "+representatives);
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
		for (Member r : representatives.values()) {
			r.getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage(content)).queue();
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
}
