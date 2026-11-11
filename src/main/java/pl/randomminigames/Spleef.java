package pl.randomminigames;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.concurrent.ThreadLocalRandom;

public class Spleef extends Minigame {

    private static final int HALF = 12;
    private static final int[] LAYERS = {0, -6};

    public Spleef(RandomMinigames plugin) {
        super(plugin);
    }

    @Override public String id() { return "spleef"; }
    @Override public String name() { return "Spleef"; }
    @Override public String description() { return "Kop śnieg pod rywalami łopatą. Kto spadnie, odpada."; }

    @Override
    protected void buildArena() {
        fill(Material.SNOW_BLOCK);
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
    protected void giveKit(Player p) {
        ItemStack shovel = new ItemStack(Material.DIAMOND_SHOVEL);
        ItemMeta meta = shovel.getItemMeta();
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.setUnbreakable(true);
        meta.displayName(Component.text("Łopata Spleef"));
        shovel.setItemMeta(meta);
        p.getInventory().addItem(shovel);
    }

    @Override
    protected boolean canBreak(Block b) {
        return b.getType() == Material.SNOW_BLOCK;
    }

    @Override
    protected void handleMove(Player p, Location to) {
        if (to.getY() < origin.getBlockY() + LAYERS[LAYERS.length - 1] - 4) {
            eliminate(p, "spadł");
        }
    }
}
