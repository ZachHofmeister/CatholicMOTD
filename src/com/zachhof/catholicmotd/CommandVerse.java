package com.zachhof.catholicmotd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandVerse implements CommandExecutor {
	Messager mainMessager;
	
	public CommandVerse(Messager messager) {
		mainMessager = messager;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player) {
            Player player = (Player) sender;
            mainMessager.sendVerse(player);
        }
        return true; // If the player (or console) uses our command correct, we can return true
	}

}
