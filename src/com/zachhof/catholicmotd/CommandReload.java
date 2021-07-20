package com.zachhof.catholicmotd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandReload implements CommandExecutor {
	Messager mainMessager;
	
	public CommandReload(Messager messager) {
		mainMessager = messager;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		mainMessager.reloadConfigFile();
        return true; // If the player (or console) uses our command correct, we can return true
	}

}