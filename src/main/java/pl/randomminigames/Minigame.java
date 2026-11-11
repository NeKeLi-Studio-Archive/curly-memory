package pl.randomminigames;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.function.Consumer;

public abstract class Minigame implements Listener {

    private record SavedState(Location loc, ItemStack[] inv, GameMode mode) {}

    protected final RandomMinigames plugin;
    protected final Set<UUID> alive = new HashSet<>();
    private final Set<UUID> participants = new HashSet<>();
    private final Map<UUID, SavedState> saved = new HashMap<>();
    private final List<BukkitTask> tasks = new ArrayList<>();

    protected boolean running;
    protected boolean started;
    protected boolean finished;
    protected World world;
    protected Location origin;

    protected Minigame(RandomMinigames plugin) {
        this.plugin = plugin;
    }

    // ---- do zaimplementowania w grach ----
    public abstract String id();
    public abstract String name();
    public abstract String description();
    protected abstract void buildArena();
    protected abstract void clearArena();
    protected abstract Location spawnFor(int index);

    protected void giveKit(Player p) {}
    protected void onStart() {}
    protected void handleMove(Player p, Location to) {}
    protected boolean canBreak(Block b) { return false; }

    public int minPlayers() {
        return plugin.getConfig().getInt("min-players", 2);
    }

    // ---- cykl zycia ----
    public void start(Set<Player> players) {
        running = true;
        finished = false;
        started = false;

        world = Bukkit.getWorld(plugin.getConfig().getString("arena.world", "world"));
        if (world == null) world = Bukkit.getWorlds().get(0);
        origin = new Location(world,
                plugin.getConfig().getInt("arena.x", 10000),
                plugin.getConfig().getInt("arena.y", 120),
                plugin.getConfig().getInt("arena.z", 10000));

        buildArena();
        Bukkit.getPluginManager().registerEvents(this, plugin);

        int i = 0;
        for (Player p : players) {
            ItemStack[] contents = p.getInventory().getContents();
            ItemStack[] copy = new ItemStack[contents.length];
            for (int k = 0; k < contents.length; k++) {
                copy[k] = contents[k] == null ? null : contents[k].clone();
            }
            saved.put(p.getUniqueId(), new SavedState(p.getLocation(), copy, p.getGameMode()));

            p.getInventory().clear();
            p.setGameMode(GameMode.SURVIVAL);
            p.setFoodLevel(20);
            p.setFireTicks(0);
            p.setFallDistance(0f);
            alive.add(p.getUniqueId());
            participants.add(p.getUniqueId());
            p.teleport(spawnFor(i++));
            giveKit(p);
            p.sendMessage(RandomMinigames.msg(name() + ": " + description()));
        }
        runCountdown();
    }

    private void runCountdown() {
        tasks.add(new BukkitRunnable() {
            int left = 5;

            @Override
            public void run() {
                if (finished) { cancel(); return; }
                if (left > 0) {
                    forEachAlive(p -> p.sendActionBar(Component.text("Start za " + left + " s")));
                    left--;
                } else {
                    started = true;
                    forEachAlive(p -> p.sendActionBar(Component.text("START!")));
                    onStart();
                    long max = plugin.getConfig().getLong("max-game-seconds", 300) * 20L;
                    later(() -> { if (!finished) win(null); }, max);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L));
    }

    public boolean isPlaying(Player p) {
        return running && alive.contains(p.getUniqueId());
    }

    public void eliminate(Player p, String reason) {
        if (finished || !alive.remove(p.getUniqueId())) return;
        sendToParticipants(RandomMinigames.msg(p.getName() + " odpada (" + reason + "). Zostało: " + alive.size()));
        restore(p);
        if (alive.size() <= 1) {
            Player winner = alive.isEmpty() ? null : Bukkit.getPlayer(alive.iterator().next());
            win(winner);
        }
    }

    public void win(Player winner) {
        if (finished) return;
        finished = true;
        if (winner != null) {
            Bukkit.broadcast(RandomMinigames.msg(winner.getName() + " wygrywa " + name() + "!"));
            for (String cmd : plugin.getConfig().getStringList("reward-commands")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", winner.getName()));
            }
        } else {
            Bukkit.broadcast(RandomMinigames.msg("Runda " + name() + " zakończona bez zwycięzcy."));
        }
        later(this::cleanup, 100L); // 5 s na celebracje
    }

    /** Natychmiastowe zatrzymanie (komenda / wylaczenie pluginu). */
    public void forceStop() {
        if (!running) return;
        finished = true;
        cleanup();
    }

    private void cleanup() {
        for (UUID id : new ArrayList<>(alive)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) restore(p);
        }
        for (BukkitTask t : tasks) t.cancel();
        tasks.clear();
        HandlerList.unregisterAll(this);
        clearArena();
        alive.clear();
        participants.clear();
        saved.clear();
        running = false;
        started = false;
        plugin.manager().gameEnded();
    }

    private void restore(Player p) {
        SavedState s = saved.remove(p.getUniqueId());
        if (s == null) return;
        p.setGliding(false);
        p.getInventory().clear();
        p.getInventory().setContents(s.inv());
        p.setGameMode(s.mode());
        p.setFallDistance(0f);
        p.teleport(s.loc());
    }

    // ---- helpery ----
    protected void forEachAlive(Consumer<Player> action) {
        for (UUID id : new ArrayList<>(alive)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) action.accept(p);
        }
    }

    protected void sendToParticipants(Component c) {
        for (UUID id : participants) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage(c);
        }
    }

    protected BukkitTask later(Runnable r, long ticks) {
        BukkitTask t = Bukkit.getScheduler().runTaskLater(plugin, r, ticks);
        tasks.add(t);
        return t;
    }

    protected BukkitTask repeat(Runnable r, long period) {
        BukkitTask t = Bukkit.getScheduler().runTaskTimer(plugin, r, 0L, period);
        tasks.add(t);
        return t;
    }

    // ---- zdarzenia ----
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (finished || !alive.contains(p.getUniqueId())) return;
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        if (!started) {
            if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
                e.setTo(new Location(from.getWorld(), from.getX(), to.getY(), from.getZ(), to.getYaw(), to.getPitch()));
            }
            return;
        }
        handleMove(p, to);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && alive.contains(p.getUniqueId())) e.setCancelled(true);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && alive.contains(p.getUniqueId())) e.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!alive.contains(e.getPlayer().getUniqueId())) return;
        if (started && !finished && canBreak(e.getBlock())) {
            e.setDropItems(false);
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (alive.contains(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (alive.contains(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        eliminate(e.getPlayer(), "wyszedł z serwera");
    }
}
