package com.rurd4me.realestatecraft;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import cosine.boseconomy.BOSEconomy;

import org.bukkit.plugin.Plugin;

public final class RealEstateCraft extends JavaPlugin {
    @Override
    public void onEnable(){
        // TODO Insert logic to be performed when the plugin is enabled
    	getLogger().info("onEnable has been invoked!");
    }
 
    @Override
    public void onDisable() {
        // TODO Insert logic to be performed when the plugin is disabled
    	getLogger().info("onDisable has been invoked!");
    }

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
//TODO: Make this less crappy...its my first plugin..hacked together in several hours...gimme a break, yo!

/*
 * sellregion Command
 */
    	if (cmd.getName().equalsIgnoreCase("sellregion")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can only be run by a player.");
    		} else {
    			Player player = (Player) sender;
    			// do something
    			if(args.length == 2)
    			{
    				//region exists?
    				World currentWorld = player.getWorld();
    				ProtectedRegion myRegion = getRegionIfExists(currentWorld, args[0]);
    				if(myRegion != null)
    				{
    					//player is owner?
    					if(myRegion.isOwner(player.getName()))
    					{
    						//save in config
							int price = Integer.parseInt(args[1]);
							
    						this.getConfig().set(currentWorld.getName() + "." + myRegion.getId() + ".price", price);
    						this.getConfig().set(currentWorld.getName() + "." + myRegion.getId() + ".seller", player.getName());
    						this.saveConfig();
    						player.sendMessage("You have listed region '" + myRegion.getId() + "' for sale for $" + args[1]);
    					}
    					else
    					{
    						player.sendMessage("Fraud Alert: You don't own this region!");
    					}
    				}
    				else
    				{
    					player.sendMessage("Region not found!");
    				}
    			}
    			else
    			{
    				player.sendMessage("You did not specify correctly!");
    			}
    		}
    		return true;
		}
/*
 * buyregion Command
 */
		else if (cmd.getName().equalsIgnoreCase("buyregion")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("This command can only be run by a player.");
			} else {
				Player player = (Player) sender;
    			// do something
    			if(args.length == 1)
    			{
    				//region exists?
    				World currentWorld = player.getWorld();
    				ProtectedRegion myRegion = getRegionIfExists(currentWorld, args[0]);
    				if(myRegion != null)
    				{
    					//for sale?
    					if(isRegionForSale(currentWorld, myRegion))
    					{
    						//player is owner?
    						if(!myRegion.isOwner(player.getName()))
    						{
    							//has enough monies?
    							int price = this.getConfig().getInt(currentWorld.getName() + "." + myRegion.getId() + ".price");
    							String sellerName = this.getConfig().getString(currentWorld.getName() + "." + myRegion.getId() + ".seller");

    							if(playerCanAfford(player.getName(), price))
    							{
    								if(transact(player.getName(), sellerName, price))
    								{
										//remove previous owners and set new owner
		    							DefaultDomain newOwners = new DefaultDomain();
		    							newOwners.addPlayer(player.getName());
		    							myRegion.setOwners(newOwners);
		    							try {
											getRegionManager(currentWorld).save();
										} catch (ProtectionDatabaseException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
		    							//remove from config
		    							this.getConfig().set(currentWorld.getName() + "." + myRegion.getId(), null);
		    							this.saveConfig();
		    							player.sendMessage("You just bought a region for: $" + price);
		    							Player seller = Bukkit.getServer().getPlayer(sellerName);
		    							if(seller != null)
		    							{
		    								seller.sendMessage(player.getName() + " just purchased your region, '" + myRegion.getId() + "' for $" + price);
		    							}
    								}
    								else
    								{
    									player.sendMessage("Bank transfer failed. Please try again later.");
    								}

    							}
    							else
    							{
    								player.sendMessage("You do not have enough money to buy this.");
    							}

    						}
    						else
    						{
    							player.sendMessage("You already own this region!");
    						}
    					}
    					else
    					{
    						player.sendMessage("Region is not for sale!");
    					}
    				}
    				else
    				{
    					player.sendMessage("Region not found!");
    				}
    			}
    			else
    			{
    				player.sendMessage("You did not specify correctly!");
    			}
			}
			return true;
		}
/*
 * regioninfo Command
 */
		else if (cmd.getName().equalsIgnoreCase("regioninfo")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("This command can only be run by a player.");
			} else {
				Player player = (Player) sender;
    			// do something
    			if(args.length == 1)
    			{
    				//region exists?
    				World currentWorld = player.getWorld();
    				ProtectedRegion myRegion = getRegionIfExists(currentWorld, args[0]);
    				if(myRegion != null)
    				{
    					//for sale?
    					if(isRegionForSale(currentWorld, myRegion))
    					{
    						int price = this.getConfig().getInt(currentWorld.getName() + "." + myRegion.getId()+".price");
    						String sellerName = this.getConfig().getString(currentWorld.getName() + "." + myRegion.getId() + ".seller");
    						player.sendMessage("Region is for sale for $" + price + " by user " + sellerName);
    					}
    					else
    					{
    						player.sendMessage("Region is not for sale!");
    					}
    				}
    				else
    				{
    					player.sendMessage("Region not found!");
    				}
    				
    				//player.sendMessage("You would like info on region: " + args[0]);
    			}
    			else
    			{
    				player.sendMessage("You did not specify correctly!");
    			}
			}
			return true;
		}
/*
 * stopsellingregion Command
 */
		else if (cmd.getName().equalsIgnoreCase("stopsellingregion")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can only be run by a player.");
    		} else {
    			Player player = (Player) sender;
    			// do something
    			if(args.length == 1)
    			{
    				//region exists?
    				World currentWorld = player.getWorld();
    				ProtectedRegion myRegion = getRegionIfExists(currentWorld, args[0]);
    				if(myRegion != null)
    				{
    					//player is owner?
    					if(myRegion.isOwner(player.getName()))
    					{
							//stop the sale!
    						this.getConfig().set(currentWorld.getName() + "." + myRegion.getId(), null);
    						this.saveConfig();
    						player.sendMessage("Your region '" + myRegion.getId() + "' is no longer for sale.");
    					}
    					else
    					{
    						player.sendMessage("Fraud Alert: You don't own this region!");
    					}
    				}
    				else
    				{
    					player.sendMessage("Region not found!");
    				}
    			}
    			else
    			{
    				player.sendMessage("You did not specify correctly!");
    			}
    		}
    		return true;
    	}
    	else{
    
    	}
    	return false;
    }

    private WorldGuardPlugin getWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
        getLogger().info("plugins loaded: " + getServer().getPluginManager().getPlugins().length);
     
        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
        	getLogger().info("Couldn't load world guard plugin...");
            return null; // Maybe you want throw an exception instead
        }
     
        return (WorldGuardPlugin) plugin;
    }

    private RegionManager getRegionManager(World world){
    	return getWorldGuard().getRegionManager(world);
    }

    private ProtectedRegion getRegionIfExists(World world, String regionName){
		RegionManager regionManager = getRegionManager(world);
		return regionManager.getRegion(regionName);
    }
    
    private boolean isRegionForSale (World world, ProtectedRegion region){
    	String price = this.getConfig().getString(world.getName() + "." + region.getId());
    	if(price != null){
    		return true;
    	}
    	return false;
    	
		//RegionManager regionManager = getRegionManager(world);
		//ProtectedRegion region = regionManager.getRegion(r);
		//if(region != null)
		//{
			//do stuff with with it
		//}
		//else
		//{
			//player.sendMessage("Region " + args[0] + "doeth not existeth!");
		//}
    	//this.getConfig().getConfigurationSection("path.to.map").getValues(false)
    //	return false;
    }

    private boolean transact(String buyer, String seller, int price){
    	double amount = Double.valueOf(price);
    	if(playerCanAfford(buyer, amount))
    	{
    		debit(seller, amount);
    		credit(buyer, amount);
    		return true;
    	}
    	return false;

    }

    private void debit(String p, double amount){
    	BOSEconomy econ = getBOSEconomy();
    	econ.addPlayerMoney(p, amount, false);
    }

    private void credit(String p, double amount){
    	BOSEconomy econ = getBOSEconomy();
    	econ.addPlayerMoney(p, (amount * -1), false);
    }
    
    private boolean playerCanAfford(String player, double amount){
    	BOSEconomy econ = getBOSEconomy();
    	double bal = econ.getPlayerMoneyDouble(player);
    	
    	if(bal - amount >= 0)
    	{
    		return true;
    	}
    	return false;
    }

    public BOSEconomy getBOSEconomy(){
        Plugin plugin = getServer().getPluginManager().getPlugin("BOSEconomy");
        getLogger().info("plugins loaded: " + getServer().getPluginManager().getPlugins().length);
     
        // BOSEconomy may not be loaded
        if (plugin == null || !(plugin instanceof BOSEconomy)) {
        	getLogger().info("Couldn't load BOSEconomy plugin...");
            return null; // Maybe you want throw an exception instead
        }
     
        return (BOSEconomy) plugin;
    }

}
