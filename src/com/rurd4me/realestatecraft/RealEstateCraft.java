package com.rurd4me.realestatecraft;

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldedit.BlockVector;

import cosine.boseconomy.BOSEconomy;

import org.bukkit.plugin.Plugin;

public final class RealEstateCraft extends JavaPlugin {
    @Override
    public void onEnable(){
        // TODO Insert logic to be performed when the plugin is enabled
    	getLogger().info("onEnable has been invoked!");
    	Listener myListener = new Listener(){
    	    @EventHandler
    	    public void onPlayerInteract(PlayerInteractEvent e)
    	    {
    	        if(e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
    	        {
    	            Block b = e.getClickedBlock();
    	            Player p = e.getPlayer();
    	            World w = p.getWorld();
    	    		if(b.getTypeId() == 63 || b.getTypeId() == 68)
    	    		{	//WE HAVE A SIGN! YEAH! WOOHOO! PART@Y!
    	    			Sign s = (Sign)b.getState();
    	    			getLogger().info("Sign clicked and it says: " + s.getLine(0));
    	    			if(s.getLine(0).equalsIgnoreCase("[FOR SALE]"))
    	    			{
    	    				ApplicableRegionSet regions = getRegionsByBlock(w, b);
    	    				Iterator<ProtectedRegion> i = regions.iterator();
    	    				while(i.hasNext())
    	    				{
    	    					getLogger().info("in loop!");
    	    					ProtectedRegion r = i.next();
    	    					if(isRegionForSale(w, r))
    	    					{
    	    						p.performCommand("realtor buy " + r.getId());
    	    					}
    	    				}
    	    			}

    	    		}
    	        }
    	    }
    	};
    	getServer().getPluginManager().registerEvents(myListener, this);
    }
 
    @Override
    public void onDisable() {
        // TODO Insert logic to be performed when the plugin is disabled
    	getLogger().info("onDisable has been invoked!");
    	HandlerList.unregisterAll(this);
    }

    //TODO: Make this less crappy...its my first plugin..hacked together in several hours...gimme a break, yo!
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if (cmd.getName().equalsIgnoreCase("realtor"))
		{
			if(args.length == 0)
			{
				sender.sendMessage("Welcome to Minecraft Realty!");
				sender.sendMessage("----------------------------");
				sender.sendMessage("/realtor sell <region id> <price>");
				sender.sendMessage("/realtor buy <region id>");
				sender.sendMessage("/realtor withdraw <region id>");
				sender.sendMessage("/realtor info <region id>");
				return true;
			}
			//Main Command
			if (!(sender instanceof Player))
			{	//Only players can list regions for now
    			sender.sendMessage("This command can only be run by a player.");
    		}
			else
			{	//decide which command they ran
				Player player = (Player) sender;
				String command = args[0];
		    	if (command.equalsIgnoreCase("sell"))
		    	{	//We are selling, due diligence time.
		    			// do something
		    			if(args.length == 3)
		    			{
		    				//region exists?
		    				World currentWorld = player.getWorld();
		    				ProtectedRegion myRegion = getRegionIfExists(currentWorld, args[1]);
		    				if(myRegion != null)
		    				{
		    					//player is owner?
		    					if(myRegion.isOwner(player.getName()))
		    					{
		    						//save in config
									int price = Integer.parseInt(args[2]);
									
		    						this.getConfig().set(currentWorld.getName() + "." + myRegion.getId() + ".price", price);
		    						this.getConfig().set(currentWorld.getName() + "." + myRegion.getId() + ".seller", player.getName());
		    						this.saveConfig();
		    						player.sendMessage("You have listed region '" + myRegion.getId() + "' for sale for $" + args[2]);
		    						markRegion(myRegion, currentWorld, price);
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
		    			return true;
		    		}
					else if (command.equalsIgnoreCase("buy"))
					{	//We are buying
			    			// do something
			    			if(args.length == 2)
			    			{
			    				//region exists?
			    				World currentWorld = player.getWorld();
			    				ProtectedRegion myRegion = getRegionIfExists(currentWorld, args[1]);
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
			    								}else{
			    									player.sendMessage("Bank transfer failed. Please try again later.");
			    								}
			    							}else{
			    								player.sendMessage("You do not have enough money to buy this.");
			    							}
			    						}else{
			    							player.sendMessage("You already own this region!");
			    						}
			    					}else{
			    						player.sendMessage("Region is not for sale!");
			    					}
			    				}else{
			    					player.sendMessage("Region not found!");
			    				}
			    			}else{
			    				player.sendMessage("You did not specify correctly!");
			    			}
						return true;
					}
					else if (command.equalsIgnoreCase("info"))
					{	//Get them some infos
			    			if(args.length == 2)
			    			{
			    				//region exists?
			    				World currentWorld = player.getWorld();
			    				ProtectedRegion myRegion = getRegionIfExists(currentWorld, args[1]);
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
			    			}
			    			else
			    			{
			    				player.sendMessage("You did not specify correctly!");
			    			}
						return true;
					}
					else if (command.equalsIgnoreCase("withdraw"))
					{
			    			// do something
			    			if(args.length == 2)
			    			{
			    				//region exists?
			    				World currentWorld = player.getWorld();
			    				ProtectedRegion myRegion = getRegionIfExists(currentWorld, args[1]);
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
			    		return true;
			    	}
					else
					{
			    		sender.sendMessage("You did not specify correctly!");
			    	}
			    }
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

    private ApplicableRegionSet getRegionsByBlock(World world, Block b){
		RegionManager regionManager = getRegionManager(world);
		return regionManager.getApplicableRegions(b.getLocation());
    }   

    private boolean isRegionForSale (World world, ProtectedRegion region){
    	String price = this.getConfig().getString(world.getName() + "." + region.getId());
    	if(price != null){
    		return true;
    	}
    	return false;
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

    public void markRegion(ProtectedRegion region, World w, int price) {
    	BlockVector minPoint = region.getMinimumPoint();
    	BlockVector maxPoint = region.getMaximumPoint();

    	int minx = Double.valueOf(minPoint.getX()).intValue();
    	int miny = Double.valueOf(minPoint.getY()).intValue();
    	int minz = Double.valueOf(minPoint.getZ()).intValue();

    	int maxx = Double.valueOf(maxPoint.getX()).intValue();
    	int maxy = Double.valueOf(maxPoint.getY()).intValue();
    	int maxz = Double.valueOf(maxPoint.getZ()).intValue();

    	for(int x = minx; x<=maxx; x++){
        	for(int y = miny; y<=maxy; y++){
            	for(int z = minz; z<=maxz; z++){
            		Block b = w.getBlockAt(x, y, z);
            		if(b.getTypeId() == 63 || b.getTypeId() == 68)
            		{	//WE HAVE A SIGN! YEAH! WOOHOO! PART@Y!
            			Sign s = (Sign)b.getState();
            			getLogger().info("Sign found and it says: " + s.getLine(0));
            			if(s.getLine(0).equalsIgnoreCase("[FOR SALE]"))
            			{
                			getLogger().info("Sign found in region and it says at teh top: " + s.getLine(0));
                			s.setLine(0,  "[FOR SALE]");
                			s.setLine(1,  "Minecraft");
                			s.setLine(2,  "Realty");
                			s.setLine(3,  "$" + price);
                			s.update(true);
            			}

            		}
            	}
        	}
    	}
    }
    
}
