package com.zachhof.catholicmotd;

import java.util.ArrayList;
import java.io.*;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Messager extends JavaPlugin implements Listener {
	private FileConfiguration config = getConfig();
	private ArrayList<String> playersJoined = new ArrayList<String>();
	private Calendar dailyCal;
	private String dailyVerse, dailyCatechism;
	
	//Fired when plugin is first enabled
	@Override
	public void onEnable() {
		//Configs
		//LINK FOR COLOR CODES!! https://www.digminecraft.com/lists/color_list_pc.php
		config.addDefault("motd_template",
				  "%greeting%\n"
				+ "%calendar%\n"
				+ "§6Verse of the day:\n"
				+ "%verse%\n"
				+ "§bCatechism passage of the day:\n"
				+ "%catechism%");
		config.addDefault("rejoin_template", "%greeting%");
		config.addDefault("verse_cmd_color", "§6");
		config.addDefault("bible_version", "RSVCE");
		config.addDefault("catechism_cmd_color", "§b");
		config.addDefault("catechism_max_length", 200);
		config.options().copyDefaults(true);
		saveConfig();
		//Register commands
        this.getCommand("catholic").setExecutor(new CommandCatholic(this));
        this.getCommand("calendar").setExecutor(new CommandCalendar(this));
        this.getCommand("verse").setExecutor(new CommandVerse(this));
        this.getCommand("catechism").setExecutor(new CommandCatechism(this));
        this.getCommand("reloadCatConfig").setExecutor(new CommandReload(this));
        //this.getCommand("quiz").setExecutor(new CommandQuiz(this));
		//Enable class
		getServer().getPluginManager().registerEvents(this, this);
		//Logs the daily calendar and verse
		dailyCal = Calendar.getDailyCalendar();
		try {
			dailyVerse = getDailyVerse(config.getString("bible_version"));
			dailyCatechism = getRandomCatechism(config.getInt("catechism_max_length"));
		} catch (IOException ex) {
			getLogger().warning("Exception thrown: " + ex.getStackTrace());
		}
		getLogger().info(buildMOTD("console", config.getString("motd_template")));
	}
	
	//Fired when plugin is disabled
	@Override
	public void onDisable() {}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		
		//Refresh calendar and verse if it's a new day
		if (!dailyCal.date.equals(getCurrentDateString("yyyy-MM-dd"))) {
			dailyCal = Calendar.getDailyCalendar();
			try {
				dailyVerse = getDailyVerse(config.getString("bible_version"));
				dailyCatechism = getRandomCatechism(config.getInt("catechism_max_length"));
			} catch (IOException ex) {
				getLogger().warning("Exception thrown: " + ex.getStackTrace());
			}
			playersJoined.clear();
		}
		
		//Send motd (if first join or always)
		if (isPlayerFirstLogin(player.getName())) {
			sendMOTD(player);
			playersJoined.add(player.getName());
		} else {
			sendRejoinMessage(player);
		}
	}
	
	public void sendMessage(Player player, String message) {
		final String finalMessage = new String(message);
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() { //Sends message after join message
			@Override
			public void run() {
				player.sendMessage(finalMessage);
			}
        }, 2);
	}
	
	public void sendMOTD(Player player) {
		sendMessage(player, buildMOTD(player.getName(), config.getString("motd_template")));
	}
	
	public void sendRejoinMessage(Player player) {
		sendMessage(player, buildMOTD(player.getName(), config.getString("rejoin_template")));
	}
	
	public void sendCalendar(Player player) {
		sendMessage(player, dailyCal.displayFormat());
	}
	
	public void sendVerse(Player player) {
		String color = config.getString("verse_cmd_color").replaceAll("(?i)%seasonColor%", dailyCal.color().toString());
		sendMessage(player, color + dailyVerse);
	}
	
	public void sendCatechism(Player player) {
		String color = config.getString("catechism_cmd_color").replaceAll("(?i)%seasonColor%", dailyCal.color().toString());
		sendMessage(player, color + dailyCatechism);
	}
	
	public String buildMOTD(String playerName, String messageTemplate) {
		String message = messageTemplate;
		message = message.replaceAll("(?i)%greeting%", buildGreeting(playerName, dailyCal.season));
		message = message.replaceAll("(?i)%player%", playerName);
		message = message.replaceAll("(?i)%calendar%", dailyCal.displayFormat());
		message = message.replaceAll("(?i)%verse%", dailyVerse);
		message = message.replaceAll("(?i)%catechism%", dailyCatechism);
		message = message.replaceAll("(?i)%seasonColor%", dailyCal.color().toString());
		return message;
	}
	
	public String buildGreeting(String playerName, String season) {
		String greeting = "";
		if (season.equalsIgnoreCase("christmas")) {
			greeting = ChatColor.RED + "Merry " + ChatColor.GREEN + "Christmas " + dailyCal.color() + playerName + "!";
		} else if (season.equalsIgnoreCase("easter")) {
			greeting = dailyCal.color() + "Happy Easter " + playerName + "! Christ is risen!";
		} else {
			greeting = dailyCal.color() + "Welcome " + playerName + "!";
		}
		return greeting.trim() + ChatColor.RESET;
	}
	
	public static String getDailyVerse(String bibleVersion) throws IOException {
		//Get the date
		String date = getCurrentDateString("yyyy/MM/dd");
		return getDailyVerse(bibleVersion, date);
	}
	
	public static String getDailyVerse(String bibleVersion, String date) throws IOException {
		//Fetch webpage from biblegateway via Jsoup
		Document doc = null;
		try {
			String url = "https://www.biblegateway.com/reading-plans/verse-of-the-day/" + date + "?version=" + bibleVersion;
			doc = Jsoup.connect(url).get();
		} catch (IOException ex) {
			throw ex;
		}
		String output = "";
		Elements passages = doc.select("div.rp-passage"); //Verses
		for (Element passage : passages) {
			for (Element child : passage.selectFirst("div.rp-passage-text").getAllElements()) {
				if (child.is("sup.versenum, span.chapternum, div.footnotes, sup.footnote,"
						+ "h1, h2, h3, h4, h5, .crossrefs, .hidden, .crossreference,"
						+ ".inline-h1, .inline-h2, .inline-h3, .inline-h4, .inline-h5") 
						&& !child.is(".poetry")) {
					child.remove();
				}
			}
			output += passage.selectFirst("div.rp-passage-text").text();
			output += " (" + passage.select("div.rp-passage-display").text() + ")\n";
		}
		return output.trim();
	}
	
	public static String getRandomCatechism(int maxLength) throws IOException {
		String url = "https://www.catholicculture.org/culture/library/catechism/randomcatechism.cfm";
		//Fetch webpage from catholicculture.org via Jsoup
		Document doc = null;
		String passage = "";
		try {
			do { //Loop to ensure that passage is within the desired length.
				doc = Jsoup.connect(url).get();
				passage = doc.selectFirst("div.content_wrapper").selectFirst("p").text();
			} while (passage.length() > maxLength);
		} catch (IOException ex) {
			throw ex;
		}
		
		return passage; //NOTE: if there is an exception in the above after the website has worked once, a really long passage may be returned here because an exception breaks the loop.
	}
	
	public boolean isPlayerFirstLogin(String name) { //Could replace this with a map or something for O(1) search?
		boolean first = true;
		for (String n : playersJoined) {
			if (name.equals(n)) first = false;
		}
		return first;
	}
	
	public static String getCurrentDateString(String pattern) {
		return getCurrentDateString(pattern, 0);
	}
	
	public static String getCurrentDateString(String pattern, int offset) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);  
		LocalDateTime now = LocalDateTime.now().plusDays(offset);
		return dtf.format(now);
	}
	
	public void reloadConfigFile() {
		reloadConfig();
		config = getConfig();
	}
}
