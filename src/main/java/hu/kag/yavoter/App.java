package hu.kag.yavoter;

import java.io.File;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.properties.PropertiesConfiguration;
import org.apache.logging.log4j.core.config.properties.PropertiesConfigurationFactory;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;



/**
 * KT szavazobot
 *
 */
public class App {
	private static Logger log = LogManager.getLogger(App.class);

    public static void main( String[] args )  throws Exception  {
    	File configdir = new File("config"); 
    	if (args.length>0) {
    		configdir = new File(args[0]);
    	}
    	configdir = configdir.getCanonicalFile().getAbsoluteFile();
    	
    	URI configLocation = new File(configdir,"log4j2.properties").toURI();
    	System.out.println("Using log4j2 config file:"+configLocation);
    	
    	PropertiesConfigurationFactory factory = new PropertiesConfigurationFactory();
        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
		PropertiesConfiguration lconfig = factory.getConfiguration(lc, ConfigurationSource.fromUri(configLocation));
		lc.setConfiguration(lconfig);

    	log.info("logsystem initialized");

    	Config.load(new File(configdir,"app.properties").getPath());

    	log.info("configuration loaded");

    	new App();
    }
    
    JDA client;
    KTVote kv;
    ExecutorService es;
    
    public App() throws Exception {
    	es = Executors.newCachedThreadPool();
    	JDABuilder b = JDABuilder.create(Config.get("token"),GatewayIntent.getIntents(GatewayIntent.DEFAULT))
    			.enableIntents(GatewayIntent.GUILD_MEMBERS)
    			.disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS)
    			.addEventListeners(new AppListenerAdapter());
    	client = b.build();
    	client.awaitReady();
    	log.info("app is ready");
    }
    
	void handleChatMessage(GuildMessageReceivedEvent event) {
		log.info("Message received from: "+event.getAuthor().getName()+" on "+event.getChannel()+" "+event.getMessage().getContentDisplay());
		String contentRaw = event.getMessage().getContentRaw();
		try {
			Guild guild = event.getGuild();
			if (contentRaw.startsWith(".yadiscover")) {
				log.info("g:" + guild.getId() + ":" + guild.getName());
				for (VoiceChannel vc : guild.getVoiceChannels()) {
					log.info("vc:" + vc.getId() + ":" + vc.getName());
					for (Member vm : vc.getMembers()) {
						log.info("vcm:" + vm.getId() + ":" + vm.getEffectiveName() + ":" + vm.getRoles());
					}
				}
				for (Role r : guild.getRoles()) {
					log.info("r:" + r.getId() + ":" + r.getName());
				}
				for (Member t : guild.loadMembers().get()) {
					log.info("agm:" + t.getId() + ":" + t.getEffectiveName() + ":" + t.getRoles());
				}
				event.getChannel().sendMessage("Pong!").complete();
			} else if (contentRaw.startsWith(".yapresence")) {
				KTRoom kr = new KTRoom(guild,guild.getVoiceChannelById(Config.get("votevcs", "696432415064850582")));
				if (contentRaw.contains("detail")) {
					event.getChannel().sendMessage("Jelenlévő képviselők listája: \n"+kr.getVoterDetails()).complete();
				} else {
					event.getChannel().sendMessage("Jelenlévő képviselők szavazószáma: "+kr.getSumVote()).complete();
				}
			} else if (contentRaw.startsWith(".yavote")) {
					//event.getMessage().delete().queue();
					String fullCommand = contentRaw.indexOf(" ")<0?contentRaw:contentRaw.substring(0,contentRaw.indexOf(" "));
					if (fullCommand.startsWith(".yavotestart")) {
						if (kv!=null) {
							kv.stopVote();
							kv=null;
						}
						String[] data = contentRaw.substring(contentRaw.indexOf(" ") + 1).split(";");
						String[] choicedata = new String[data.length - 1];
						System.arraycopy(data, 1, choicedata, 0, choicedata.length);

						KTRoom kr = new KTRoom(guild,
								guild.getVoiceChannelById(Config.get("votevcs", "696432415064850582")));
						kv = new KTVote(guild,kr, guild.getTextChannelById(Config.get("votetch", "690893435854389278")),
								fullCommand, data[0], choicedata);
						kv.refreshVoteState(); // blokkol a vote vegeig
					} else if (fullCommand.startsWith(".yavotestop")) {
						if (kv!=null) {
							kv.stopVote();
							kv=null;
						}
						event.getMessage().delete().queue();
					}
			}
		} catch (Exception e) {
			log.warn("mse:", e);
		}
	}
    
    public class AppListenerAdapter extends ListenerAdapter {

		@Override
		public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
			if (event.getAuthor().isBot()) return;
			// switch processing thread
			es.execute(new Runnable() {
				@Override
				public void run() {
					handleChatMessage(event );
				}
			});
			
		}

		@Override
		public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
			if (kv!=null&&!event.getUserId().equals(Config.get("botuserid", "803237413542428722"))&&event.getMessageId().equals(kv.getMessageId())) {
				es.execute(new Runnable() {
					@Override
					public void run() {
						kv.handleReaction(event );
					}
				});
			}
		}
		
		// privat uzenetes szupertitkos szavazas
		@Override
		public void onPrivateMessageReactionAdd(PrivateMessageReactionAddEvent event) {
			if (kv!=null&&!event.getUserId().equals(Config.get("botuserid", "803237413542428722"))) {
				es.execute(new Runnable() {
					@Override
					public void run() {
						kv.handleReaction(event );
					}
				});
			}
		}

		@Override
		public void onPrivateMessageReactionRemove(PrivateMessageReactionRemoveEvent event) {
			if (kv!=null&&!event.getUserId().equals(Config.get("botuserid", "803237413542428722"))) {
				es.execute(new Runnable() {
					@Override
					public void run() {
						kv.handleReaction(event );
					}
				});
			}
		}

    }
    
}
