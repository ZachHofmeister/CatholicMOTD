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
		config.addDefault("motd_on_every_join", false);
		config.addDefault("calendar_enabled", true);
		config.addDefault("verse_enabled", true);
		config.addDefault("verse_color", "YELLOW");
		config.addDefault("bible_version", "RSVCE");
		config.addDefault("catechism_enabled", true);
		config.addDefault("catechism_color", "AQUA");
		config.addDefault("catechism_max_length", 200);
		config.options().copyDefaults(true);
		saveConfig();
		//Register commands
        this.getCommand("catholic").setExecutor(new CommandCatholic(this));
        this.getCommand("calendar").setExecutor(new CommandCalendar(this));
        this.getCommand("verse").setExecutor(new CommandVerse(this));
        this.getCommand("catechism").setExecutor(new CommandCatechism(this));
        //this.getCommand("quiz").setExecutor(new CommandQuiz(this));
		//Enable class
		getServer().getPluginManager().registerEvents(this, this);
		//Logs the daily calendar and verse
		dailyCal = Calendar.getDailyCalendar();
		dailyVerse = getDailyVerse();
		dailyCatechism = getRandomCatechism();
		getLogger().info(dailyCal.displayFormat());
		getLogger().info(ChatColor.valueOf(config.getString("verse_color")) + dailyVerse);
		getLogger().info(ChatColor.valueOf(config.getString("catechism_color")) + dailyCatechism);
		//debugDump(365);
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
			dailyVerse = getDailyVerse();
			dailyCatechism = getRandomCatechism();
		}
		
		//Send message
		sendGreeting(player);
		//Send motd (if first join or always)
		if (config.getBoolean("motd_on_every_join") || isPlayerFirstLogin(player.getName())) sendMOTD(player);
		
		//Record player login
		if (isPlayerFirstLogin(player.getName())) playersJoined.add(player.getName());
	}
	
	public void sendGreeting(Player player) {
		final String message = new String(buildGreeting(player.getName(), dailyCal.season));
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() { //Sends message after join message
			@Override
			public void run() {
				player.sendMessage(message);
			}
        }, 2);
	}
	
	public void sendMOTD(Player player) {
		if (config.getBoolean("calendar_enabled")) sendCalendar(player); //Send calendar (if enabled)
		if (config.getBoolean("verse_enabled")) sendVerse(player, "Verse of the day:\n"); //Send verse (if enabled)
		if (config.getBoolean("catechism_enabled")) sendCatechism(player, "Catechism passage of the day:\n");
	}
	
	public void sendCalendar(Player player) {
		final String message = new String(dailyCal.displayFormat());
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() { //Sends message after join message
			@Override
			public void run() {
				player.sendMessage(message);
			}
        }, 2);
	}
	
	public void sendVerse(Player player) { sendVerse(player, ""); }
	public void sendVerse(Player player, String prefix) {
		final String message = new String(ChatColor.valueOf(config.getString("verse_color")) + prefix + dailyVerse);
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() { //Sends message after join message
			@Override
			public void run() {
				player.sendMessage(message);
			}
        }, 2);
	}
	
	public void sendCatechism(Player player) { sendCatechism(player, ""); }
	public void sendCatechism(Player player, String prefix) {
		final String message = new String(ChatColor.valueOf(config.getString("catechism_color")) + prefix + dailyCatechism);
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() { //Sends message after join message
			@Override
			public void run() {
				player.sendMessage(message);
			}
        }, 2);
	}
	
	public String buildGreeting(String playerName, String season) {
		String greeting = "";
		if (season.equalsIgnoreCase("christmas")) {
			greeting = ChatColor.RED + "Merry " + ChatColor.GREEN + "Christmas " + Calendar.seasonColor(season) + playerName + "!";
		} else if(season.equalsIgnoreCase("easter")) {
			greeting = Calendar.seasonColor(season) + "Happy Easter " + playerName + "! Christ is risen!";
		} else {
			greeting = Calendar.seasonColor(season) + "Welcome " + playerName + "!";
		}
		return greeting.trim();
	}
	
	public boolean isPlayerFirstLogin(String name) { //Could replace this with a map or something for O(1) search?
		boolean first = true;
		for (String n : playersJoined) {
			if (name.equals(n)) first = false;
		}
		return first;
	}
	
	public static String getCurrentDateString(String pattern) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);  
		LocalDateTime now = LocalDateTime.now();
		return dtf.format(now);
	}
	
	public String getDailyVerse() {
		//Get the date
		String date = getCurrentDateString("yyyy/MM/dd");
		//Fetch webpage from biblegateway via Jsoup
		Document doc = null;
		try {
			String url = "https://www.biblegateway.com/reading-plans/verse-of-the-day/" + date + "?version=" + config.getString("bible_version");
			doc = Jsoup.connect(url).get();
		} catch (IOException ex) {
			getLogger().warning("Exception thrown from function getDailyVerse: " + ex.getMessage());
		}
		String output = "";
		Elements passages = doc.select("div.rp-passage"); //Verses
		for (Element passage : passages) {
			for (Element child : passage.selectFirst("div.rp-passage-text").getAllElements()) {
				if (child.is("sup.versenum, span.chapternum, div.footnotes, sup.footnote, h1, h2, h3, h4, h5")) {
					child.remove();
				}
			}
			output += passage.selectFirst("div.rp-passage-text").text();
			output += " (" + passage.select("div.rp-passage-display").text() + ")\n";
		}
		return output.trim();
	}
	
	public String getRandomCatechism() {
		String url = "https://www.catholicculture.org/culture/library/catechism/randomcatechism.cfm";
		//Fetch webpage from catholicculture.org via Jsoup
		Document doc = null;
		String passage = "";
		try {
			do { //Loop to ensure that passage is within the desired length.
				doc = Jsoup.connect(url).get();
				passage = doc.selectFirst("div.content_wrapper").selectFirst("p").text();
			} while (passage.length() > config.getInt("catechism_max_length"));
		} catch (IOException ex) {
			System.out.println("Exception thrown from function getRandomCatechism: " + ex.getMessage());
		}
		
		return passage;
	}
	
//	public void debugDump(int days) {
//		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");  
//		LocalDateTime now = LocalDateTime.now();
//		for (int i = 0; i < days; ++i) {
//			String json = "";
//			try {
//				URL calendar = new URL("http://calapi.inadiutorium.cz/api/v0/en/calendars/default/" + dtf.format(now));
//		        BufferedReader in = new BufferedReader(
//		        new InputStreamReader(calendar.openStream()));
//		        
//		        String inputLine;
//		        while ((inputLine = in.readLine()) != null) json += inputLine;
//		        in.close();
//			} catch (Exception ex) {
//				getLogger().warning("Exception thrown from function getRawCalendar: " + ex.getMessage());
//			}
//			Gson gson = new Gson();
//			CalendarInfo info = gson.fromJson(json, CalendarInfo.class);
//			String message = "";
//			
//			//Greeting
//			message += playerGreeting("TestPlayer212", info.season);
//			message += formatCalendar(info);
//			getLogger().info(message);
//			now = now.plusDays(1L);
//		}
//	}
}
