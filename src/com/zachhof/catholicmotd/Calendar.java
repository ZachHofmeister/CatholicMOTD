package com.zachhof.catholicmotd;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import org.bukkit.ChatColor;

import com.google.gson.Gson;

//{"date":"2020-09-11",
//"season":"ordinary",
//"season_week":23,
//"celebrations":[
//	{"title":"Friday, 23rd week in Ordinary Time",
//	"colour":"green",
//	"rank":"ferial",
//	"rank_num":3.13}
//	],
//"weekday":"friday"}
public class Calendar {
	public String date;
	public String season;
	public int season_week;
	public List<Celebration> celebrations;
	public String weekday;
	
	public class Celebration {
		public String title;
		public String colour; //useful for colored text
		public String rank; //useful for 
		public float rank_num;
	}
	
	public String displayFormat() {
		String formatted = "";
		//Celebrations
		for(Celebration c : this.celebrations) {
			if (c.title.equalsIgnoreCase("The Memorial of the Blessed Virgin Mary on Saturday")) continue;
			formatted += color(c.colour);
			switch(c.rank.toLowerCase()) {
				case "optional memorial":
				case "memorial":
					formatted += "Today is the Memorial of ";
					break;
				case "feast":
					formatted += "Today is the Feast of ";
					break;
				case "commemoration":
					formatted += "Today is the Commemoration of ";
					break;
				case "solemnity":
					formatted += "Today is the Solemnity of ";
					break;
				case "ferial":
				case "easter triduum":
				case "sunday":
				case "primary liturgical days":
				default:
					formatted += "Today is ";
					break;
			}
			if (c.title.startsWith("Chair")
				|| c.title.startsWith("Presentation")
				|| c.title.startsWith("Holy")
				|| c.title.startsWith("Dedication")
				|| c.title.startsWith("Guardian")
				|| c.title.startsWith("Triumph")
				|| Character.isDigit(c.title.charAt(0))
			) formatted += "the ";
			formatted += c.title + ".\n";
		}
		return formatted.trim();
	}
	
	//STATICS
	public static Calendar getDailyCalendar() {
		//Get the date
		String date = Messager.getCurrentDateString("yyyy/MM/dd");
		//Fetch JSON from calapi
		String json = "";
		try {
			URL calendar = new URL("http://calapi.inadiutorium.cz/api/v0/en/calendars/default/" + date);
	        BufferedReader in = new BufferedReader(
	        	new InputStreamReader(calendar.openStream()));
	        
	        String inputLine;
	        while ((inputLine = in.readLine()) != null) json += inputLine;
	        in.close();
		} catch (Exception ex) {
			//getLogger().warning("Exception thrown from function getDailyInfo: " + ex.getMessage());
			// Seems like this exception needs to be thrown to where it can be logged (Messager or CommandQuiz)
		}
		//Format JSON as CalendarInfo with GSON
		Gson gson = new Gson();
		Calendar info = gson.fromJson(json, Calendar.class);
		return info;
	}

	public static ChatColor color(String color) {
		ChatColor col = ChatColor.DARK_GREEN;
		switch(color.toLowerCase()) {
			case "white":
				col = ChatColor.WHITE;
				break;
			case "red":
				col = ChatColor.DARK_RED;
				break;
			case "violet":
				col = ChatColor.DARK_PURPLE;
				break;
			case "green":
			default:
				col = ChatColor.DARK_GREEN;
				break;
		}
		return col;
	}

	public static ChatColor seasonColor(String season) {
		ChatColor col = ChatColor.DARK_GREEN;
		switch(season.toLowerCase()) {
			case "advent":
			case "lent":
				col = ChatColor.DARK_PURPLE;
				break;
			case "christmas":
			case "easter":
				col = ChatColor.WHITE;
				break;
			case "ordinary":
			default:
				col = ChatColor.DARK_GREEN;
				break;
		}
		return col;
	}
}