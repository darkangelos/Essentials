package com.earth2me.essentials;

import static com.earth2me.essentials.I18n._;
import com.earth2me.essentials.api.IJails;
import com.earth2me.essentials.storage.AsyncStorageObjectHolder;
import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.PluginManager;


public class Jails extends AsyncStorageObjectHolder<com.earth2me.essentials.settings.Jails> implements IJails
{
	private static final transient Logger LOGGER = Bukkit.getLogger();

	public Jails(final IEssentials ess)
	{
		super(ess, com.earth2me.essentials.settings.Jails.class);
		reloadConfig();
		registerListeners();
	}

	private void registerListeners()
	{
		final PluginManager pluginManager = ess.getServer().getPluginManager();
		final JailBlockListener blockListener = new JailBlockListener();
		final JailPlayerListener playerListener = new JailPlayerListener();
		pluginManager.registerEvents(blockListener, ess);
		pluginManager.registerEvents(playerListener, ess);
	}

	@Override
	public File getStorageFile()
	{
		return new File(ess.getDataFolder(), "jail.yml");
	}

	@Override
	public Location getJail(final String jailName) throws Exception
	{
		acquireReadLock();
		try
		{
			if (getData().getJails() == null || jailName == null
				|| !getData().getJails().containsKey(jailName.toLowerCase(Locale.ENGLISH)))
			{
				throw new Exception(_("jailNotExist"));
			}
			Location loc = getData().getJails().get(jailName.toLowerCase(Locale.ENGLISH));
			if (loc == null || loc.getWorld() == null)
			{
				throw new Exception(_("jailNotExist"));
			}
			return loc;
		}
		finally
		{
			unlock();
		}
	}

	@Override
	public Collection<String> getList() throws Exception
	{
		acquireReadLock();
		try
		{
			if (getData().getJails() == null)
			{
				return Collections.emptyList();
			}
			return new ArrayList<String>(getData().getJails().keySet());
		}
		finally
		{
			unlock();
		}
	}

	@Override
	public void removeJail(final String jail) throws Exception
	{
		acquireWriteLock();
		try
		{
			if (getData().getJails() == null)
			{
				return;
			}
			getData().getJails().remove(jail.toLowerCase(Locale.ENGLISH));
		}
		finally
		{
			unlock();
		}
	}

	@Override
	public void sendToJail(final IUser user, final String jail) throws Exception
	{
		acquireReadLock();
		try
		{
			if (!(user.getBase() instanceof OfflinePlayer))
			{
				Location loc = getJail(jail);
				user.getTeleport().now(loc, false, TeleportCause.COMMAND);
			}
			user.setJail(jail);
		}
		finally
		{
			unlock();
		}
	}

	@Override
	public void setJail(final String jailName, final Location loc) throws Exception
	{
		acquireWriteLock();
		try
		{
			if (getData().getJails() == null)
			{
				getData().setJails(new HashMap<String, Location>());
			}
			getData().getJails().put(jailName.toLowerCase(Locale.ENGLISH), loc);
		}
		finally
		{
			unlock();
		}
	}


	private class JailBlockListener implements Listener
	{
		@EventHandler(priority = EventPriority.LOW)
		public void onBlockBreak(final BlockBreakEvent event)
		{
			final User user = ess.getUser(event.getPlayer());
			if (user.isJailed())
			{
				event.setCancelled(true);
			}
		}

		@EventHandler(priority = EventPriority.LOW)
		public void onBlockPlace(final BlockPlaceEvent event)
		{
			final User user = ess.getUser(event.getPlayer());
			if (user.isJailed())
			{
				event.setCancelled(true);
			}
		}

		@EventHandler(priority = EventPriority.LOW)
		public void onBlockDamage(final BlockDamageEvent event)
		{
			final User user = ess.getUser(event.getPlayer());
			if (user.isJailed())
			{
				event.setCancelled(true);
			}
		}
	}


	private class JailPlayerListener implements Listener
	{
		@EventHandler(priority = EventPriority.LOW)
		public void onPlayerInteract(final PlayerInteractEvent event)
		{
			final User user = ess.getUser(event.getPlayer());
			if (user.isJailed())
			{
				event.setCancelled(true);
			}
		}

		@EventHandler(priority = EventPriority.HIGH)
		public void onPlayerRespawn(final PlayerRespawnEvent event)
		{
			final User user = ess.getUser(event.getPlayer());
			if (!user.isJailed() || user.getJail() == null || user.getJail().isEmpty())
			{
				return;
			}

			try
			{
				event.setRespawnLocation(getJail(user.getJail()));
			}
			catch (Exception ex)
			{
				if (ess.getSettings().isDebug())
				{
					LOGGER.log(Level.INFO, _("returnPlayerToJailError", user.getName(), ex.getLocalizedMessage()), ex);
				}
				else
				{
					LOGGER.log(Level.INFO, _("returnPlayerToJailError", user.getName(), ex.getLocalizedMessage()));
				}
			}
		}

		@EventHandler(priority = EventPriority.HIGH)
		public void onPlayerTeleport(final PlayerTeleportEvent event)
		{
			final User user = ess.getUser(event.getPlayer());
			if (!user.isJailed() || user.getJail() == null || user.getJail().isEmpty())
			{
				return;
			}

			try
			{
				event.setTo(getJail(user.getJail()));
			}
			catch (Exception ex)
			{
				if (ess.getSettings().isDebug())
				{
					LOGGER.log(Level.INFO, _("returnPlayerToJailError", user.getName(), ex.getLocalizedMessage()), ex);
				}
				else
				{
					LOGGER.log(Level.INFO, _("returnPlayerToJailError", user.getName(), ex.getLocalizedMessage()));
				}
			}
			user.sendMessage(_("jailMessage"));
		}

		@EventHandler(priority = EventPriority.HIGH)
		public void onPlayerJoin(final PlayerJoinEvent event)
		{
			final User user = ess.getUser(event.getPlayer());
			if (!user.isJailed() || user.getJail() == null || user.getJail().isEmpty())
			{
				return;
			}

			try
			{
				sendToJail(user, user.getJail());
			}
			catch (Exception ex)
			{
				if (ess.getSettings().isDebug())
				{
					LOGGER.log(Level.INFO, _("returnPlayerToJailError", user.getName(), ex.getLocalizedMessage()), ex);
				}
				else
				{
					LOGGER.log(Level.INFO, _("returnPlayerToJailError", user.getName(), ex.getLocalizedMessage()));
				}
			}
			user.sendMessage(_("jailMessage"));
		}
	}
}
