package pl.randomminigames;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ElytraRace extends Minigame {

    private static final int COUNT = 8;
    private static final double RADIUS_SQ = 36; // 6 bloków

    private final List<Location> checkpoints = new ArrayList<>();
    private final Map<UUID, Integer> next = new HashMap<>();
    private int platformY;
    private double minY;

    public ElytraRace(RandomMinigames plugin) {
        super(plugin);
    }

    @Override public String id() { return "elytra"; }
    @Override public String name() { return "Elytra Race"; }
    @Override public String description() {
        return "Skocz z platformy, w powietrzu wciśnij spację, lataj przez obręcze. Fajerwerk = przyspieszenie. Kto pierwszy przeleci wszystkie, wygrywa.";
    }

    @Override
    protected void buildArena() {
        checkpoints.clear();
        next.clear();
        int ox = origin.getBlockX(), oz = origin.getBlockZ();
        platformY = Math.min(origin.getBlockY() + 60, world.getMaxHeight() - 30);

        for (int x = -3; x <= 3; x++)
            for (int z = -3; z <= 3; z++)
                world.getBlockAt(ox + x, platformY, oz + z).setType(Material.GLASS, false);

        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 1; i <= COUNT; i++) {
            checkpoints.add(new Location(world,
                    ox + i * 45.0,
                    platformY - i * 3 + r.nextInt(-8, 9),
                    oz + r.nextInt(-25, 26)));
        }
        minY = platformY - COUNT * 3 - 70;

        repeat(() -> {
            for (Location c : checkpoints) {
                for (int k = 0; k < 24; k++) {
                    double a = k * Math.PI * 2 / 24;
                    world.spawnParticle(Particle.END_ROD,
                            c.getX(), c.getY() + Math.cos(a) * 7, c.getZ() + Math.sin(a) * 7,
                            1, 0, 0, 0, 0);
                }
            }
        }, 10L);
    }

    @Override
    protected void clearArena() {
        int ox = origin.getBlockX(), oz = origin.getBlockZ();
        for (int x = -3; x <= 3; x++)
            for (int z = -3; z <= 3; z++)
                world.getBlockAt(ox + x, platformY, oz + z).setType(Material.AIR, false);
    }

    private Location platformSpawn(int index) {
        return new Location(world,
                origin.getBlockX() + 0.5 + (index % 3) - 1,
                platformY + 1,
                origin.getBlockZ() + 0.5 + ((index / 3) % 3) - 1,
                -90f, 0f); // patrzy na wschod (+X)
    }

    @Override
    protected Location spawnFor(int index) {
        return platformSpawn(index);
    }

    @Override
    protected void giveKit(Player p) {
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        ItemMeta em = elytra.getItemMeta();
        em.setUnbreakable(true);
        elytra.setItemMeta(em);
        p.getInventory().setChestplate(elytra);

        ItemStack fw = new ItemStack(Material.FIREWORK_ROCKET, 64);
        FireworkMeta fm = (FireworkMeta) fw.getItemMeta();
        fm.setPower(2);
        fw.setItemMeta(fm);
        p.getInventory().addItem(fw);
    }

    @Override
    protected void handleMove(Player p, Location to) {
        UUID id = p.getUniqueId();
        int n = next.getOrDefault(id, 0);

        if (n < checkpoints.size() && to.getWorld() == world
                && to.distanceSquared(checkpoints.get(n)) <= RADIUS_SQ) {
            n++;
            next.put(id, n);
            if (n >= checkpoints.size()) {
                win(p);
                return;
            }
            p.sendActionBar(Component.text("Checkpoint " + n + "/" + checkpoints.size()));
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }

        if (to.getY() < minY) {
            final int reached = n;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finished || !alive.contains(id)) return;
                Location back = reached == 0 ? platformSpawn(0) : checkpoints.get(reached - 1).clone();
                back.setYaw(-90f);
                back.setPitch(0f);
                p.teleport(back);
                p.setFallDistance(0f);
                if (reached > 0) p.setGliding(true);
            });
        }
    }
}
