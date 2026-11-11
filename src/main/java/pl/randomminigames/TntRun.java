package pl.randomminigames;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class TntRun extends Minigame {

    private static final int HALF = 12;                 // arena 25x25
    private static final int[] LAYERS = {0, -10, -20};  // 3 poziomy piasku
    private static final double[] OFFS = {-0.3, 0.3};
    private final Set<Block> scheduled = new HashSet<>();

    public TntRun(RandomMinigames plugin) {
        super(plugin);
    }

    @Override public String id() { return "tntrun"; }
    @Override public String name() { return "TNT Run"; }
    @Override public String description() { return "Biegnij! Bloki znikają pod Twoimi stopami. Ostatni żywy wygrywa."; }

    @Override
    protected void buildArena() {
        scheduled.clear();
        fill(Material.SAND);
    }

    @Override
    protected void clearArena() {
        fill(Material.AIR);
    }

    private void fill(Material m) {
        int ox = origin.getBlockX(), oy = origin.getBlockY(), oz = origin.getBlockZ();
        for (int layer : LAYERS)
            for (int x = -HALF; x <= HALF; x++)
                for (int z = -HALF; z <= HALF; z++)
                    world.getBlockAt(ox + x, oy + layer, oz + z).setType(m, false);
    }

    @Override
    protected Location spawnFor(int index) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return new Location(world,
                origin.getBlockX() + 0.5 + r.nextInt(-HALF + 2, HALF - 1),
                origin.getBlockY() + 1,
                origin.getBlockZ() + 0.5 + r.nextInt(-HALF + 2, HALF - 1));
    }

    @Override
    protected void handleMove(Player p, Location to) {
        if (to.getY() < origin.getBlockY() + LAYERS[LAYERS.length - 1] - 4) {
            eliminate(p, "spadł");
            return;
        }
        int by = (int) Math.floor(to.getY() - 0.01);
        for (double dx : OFFS) {
            for (double dz : OFFS) {
                Block b = world.getBlockAt((int) Math.floor(to.getX() + dx), by, (int) Math.floor(to.getZ() + dz));
                if (b.getType() == Material.SAND && scheduled.add(b)) {
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        b.setType(Material.AIR, false);
                        scheduled.remove(b);
                    }, 12L);
                }
            }
        }
    }
}
