package com.zachhof.catholicmotd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

//import com.google.gson.Gson; //needed to read reward configs json file

public class CommandQuiz implements CommandExecutor {
	Messager mainMessager;
	
	public CommandQuiz(Messager messager) {
		mainMessager = messager;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        boolean returnStatus = true;
        
		if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args[0].equals("daily")) quizDailyVerse(player);
            else if (args[0].equals("random")) quizRandomVerse(player);
            else returnStatus = false;
        }
		
        return returnStatus; // If the player (or console) uses our command correct, we can return true
	}
	
	public void quizDailyVerse(Player player) {
		
	}
	
	public void quizRandomVerse(Player player) {
		
	}
}
