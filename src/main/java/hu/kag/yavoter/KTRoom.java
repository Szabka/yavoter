package hu.kag.yavoter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class KTRoom {
	private Logger log = LogManager.getLogger();
	
	Guild guild;
	VoiceChannel vc;
	String teacherRole;
	String voteControlRole;
	Set<String> allroles;
	Set<String> classroles;
	
	TreeMap<String,KTRepr> votersByRole;
	TreeMap<String,KTRepr> votersByUserId;
	
	public KTRoom(Guild _guild, VoiceChannel _vc) {
		this.guild = _guild;
		this.vc = _vc;
		this.teacherRole = Config.get("role.teacher", "690988963976183818");
		allroles = new HashSet<>();
		allroles.add(teacherRole);
		allroles.add(Config.get("role.parent", "690894061330235422"));
		allroles.add(Config.get("role.student", "700059776008192080"));
		classroles = new HashSet<>(Arrays.asList(Config.get("role.classes").split(",")));
		votersByRole = new TreeMap<>();
		votersByUserId = new TreeMap<>();
		
		voteControlRole = Config.get("role.votecontrol","805803639057940550");
		log.info("vc:" + vc.getId() + ":" + vc.getName());
		for (Member vm : vc.getMembers()) {
			log.info("vcm:" + vm.getId() + ":" + vm.getEffectiveName() + ":" + vm.getRoles());
			List<Role> vmrl = vm.getRoles();
			Role voterRole = getVoterRole(vmrl);
			if (voterRole!=null) {
				String roleKey = getRoleKey(voterRole, vm);
				KTRepr pr = votersByRole.get(roleKey);
				if (pr==null) {
					pr = isTeacherRole(voterRole)?new KTRepr(true,3,voterRole,null):new KTRepr(false,1,voterRole,getClassRole(vmrl));
					votersByRole.put(roleKey, pr);
				}
				pr.addRepr(vm);
				votersByUserId.put(vm.getId(), pr);
			}
		}
	}
	
	public boolean isTeacherRole(Role r) {
		return teacherRole.equals(r.getId());
	}
	
	public String getRoleKey(Role vr,Member vm) {
		if (isTeacherRole(vr)) {
			return teacherRole+"."+vm.getId();
		} else {
			return vr.getId()+"."+getClassRole(vm.getRoles());
		}
		
	}
	
	public Role getVoterRole(List<Role> vmrl) {
		for (Role role : vmrl) {
			if (allroles.contains(role.getId())) {
				return role;
			}
		}
		return null;
	}
	public Role getClassRole(List<Role> vmrl) {
		for (Role role : vmrl) {
			if (classroles.contains(role.getId())) {
				return role;
			}
		}
		return null;
	}
	
	public int getSumVote() {
		int sum = 0;
		for (KTRepr r : votersByRole.values()) {
			sum+=r.getVotes();
		}
		return sum;
	}

	public int getSumEffectiveVote() {
		int sum = 0;
		for (KTRepr r : votersByRole.values()) {
			sum+=r.getEffectiveVotes().size();
		}
		return sum;
	}

	public List<String> getAllEffectiveVote() {
		List<String> aev = new LinkedList<>();
		for (KTRepr r : votersByRole.values()) {
			aev.addAll(r.getEffectiveVotes());
		}
		return aev;
	}
	
	public List<String> getNoVoteList() {
		List<String> aev = new LinkedList<>();
		for (KTRepr r : votersByRole.values()) {
			if (r.getVotes()>r.getEffectiveVotes().size()) {
				aev.add(r.getVoteDetail());
			}
		}
		return aev;
	}
	
	
	public String getVoterDetails() {
		StringBuilder sb = new StringBuilder();
		int sum = 0;
		for (KTRepr r : votersByRole.values()) {
			sum+=r.getVotes();
			sb.append(r.getDetail());
			sb.append('\n');
		}
		sb.append("Összes szavazat: ").append(sum).append('\n');
		return sb.toString();
	}

	public void registerVote(String voteTitle,Member m, String vote) {
		KTRepr krepr = votersByUserId.get(m.getId());
		if (krepr!=null) {
			krepr.registerVote(voteTitle,m,vote);
		} else {
			log.info("no right to vote: "+m);
			m.getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage("NEM SZAVAZHATSZ! Ha szerinted ez hiba, akkor szólj.")).queue();
		}
	}

	public void createPrivVotes(String voteTitle, String[] data) {
		for (KTRepr r : votersByRole.values()) {
			r.createPrivVotes(voteTitle,data);
		}
	}

	public void clearPrivVotes(TreeMap<String, Integer> voteaggr) {
		for (KTRepr r : votersByRole.values()) {
			r.createPrivVotes(voteaggr,voteControlRole);
		}
	}
}
