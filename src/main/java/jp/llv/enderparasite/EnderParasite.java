/* 
 * Copyright (C) 2015 Toyblocks,
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jp.llv.enderparasite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 *
 * @author Toyblocks
 */
public class EnderParasite extends JavaPlugin implements Listener {
    
    private static final Set<Material> TRIGGERS = new HashSet<>();
    private static final Set<Material> CHECK_ON_BREAK = new HashSet<>();
    private double spawnProbability = 0.005;
    
    static {
        TRIGGERS.add(Material.DIAMOND_ORE);
        TRIGGERS.add(Material.GOLD_ORE);
        TRIGGERS.add(Material.IRON_ORE);
        TRIGGERS.add(Material.EMERALD_ORE);
        CHECK_ON_BREAK.addAll(TRIGGERS);
        CHECK_ON_BREAK.add(Material.STONE);
        CHECK_ON_BREAK.add(Material.GRAVEL);
    }
    
    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.saveDefaultConfig();
        this.spawnProbability = this.getConfig().getDouble("spawn-probability", 0.005);
    }
    
    @EventHandler
    public void on(BlockBreakEvent eve) {
        Block b = eve.getBlock();
        if (!CHECK_ON_BREAK.contains(b.getType())) {
            return;
        }
        
        if (b.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        if (eve.getPlayer().getGameMode() == GameMode.CREATIVE
                || eve.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        
        if (Math.random() >= 0.01) {
            return;
        }
        Location loc = b.getLocation();
        boolean t = false;
        List<Location> spawnable = new ArrayList<>();
        outer:
        for (int x = loc.getBlockX() - 3; x < loc.getBlockX() + 4; x++) {
            for (int y = loc.getBlockY() > 3 ? loc.getBlockY() - 3 : 0; y < loc.getBlockY() + 4; y++) {
                for (int z = loc.getBlockZ() - 3; z < loc.getBlockZ() + 4; z++) {
                    Block ba = loc.getWorld().getBlockAt(x, y, z);
                    if (ba.getType() == Material.AIR) {
                        Block bu = ba.getRelative(BlockFace.DOWN);
                        if (bu.getType().isOccluding()) {
                            spawnable.add(ba.getLocation());
                        }
                    } else if (TRIGGERS.contains(ba.getType())) {
                        t = true;
                        break outer;
                    }
                }
            }
        }
        if (!t) {
            return;
        }
        if (spawnable.isEmpty()) {
            return;
        }
        
        Location pl = eve.getPlayer().getLocation();
        Vector pv = eve.getPlayer().getEyeLocation().getDirection();
        Collections.sort(spawnable, (l1, l2) -> {
            Vector v1 = getVector(pl, l1), v2 = getVector(pl, l2);
            float a1 = v1.angle(pv), a2 = v2.angle(pv);
            double d1 = pl.distanceSquared(l1), d2 = pl.distanceSquared(l2);
            double e1 = d1 + (a1 * 100), e2 = d2 + (a2 * 100);
            return Double.compare(e2, e1);
        });
        
        Location spawnAt = spawnable.get(0).add(0.5, 0.5, 0.5);
        Entity parasite = spawnAt.getWorld().spawnEntity(spawnAt, EntityType.ENDERMITE);
        parasite.setMetadata("parasite", new FixedMetadataValue(this, Boolean.TRUE));
        spawnAt.getWorld().playSound(spawnAt, Sound.SILVERFISH_IDLE, 2.0F, 0.5F);
    }
    
    @EventHandler
    public void on(EntityDamageByEntityEvent eve) {
        if (!(eve.getEntity() instanceof Player)) {
            return;
        }
        Player p = (Player) eve.getEntity();
        Entity d = eve.getDamager();
        if (d.getVehicle() != null) {
            return;
        }
        if (d.getMetadata("parasite").isEmpty()) {
            return;
        }
        p.setPassenger(d);
        new BukkitRunnable() {
            
            @Override
            public void run() {
                ItemStack h = p.getInventory().getHelmet();
                if (p.getPassenger() == null
                        || p.getPassenger().getType() != EntityType.ENDERMITE
                        || p.isDead()) {
                    this.cancel();
                } else if (h != null && h.getType() != Material.AIR) {
                    short dur = h.getDurability();
                    dur -= 2;
                    if (dur > 0) {
                        h.setDurability(dur);
                        p.getInventory().setHelmet(h);
                    } else {
                        p.getInventory().setHelmet(null);
                    }
                } else {
                    p.damage(1D, d);
                }
            }
            
        }.runTaskTimer(this, 10L, 10L);
    }
    
    private static Vector getVector(Location l1, Location l2) {
        return l2.toVector().subtract(l1.toVector());
    }
    
}
