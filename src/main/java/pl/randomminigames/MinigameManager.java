package pl.randomminigames;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MinigameManager {

    private final RandomMinigames plugin;
    private final Map<String, Minigame> games = new LinkedHashMap<>();
    private final Set<UUID> joined = new LinkedHashSet<>();

    private Minigame pending;   // gra w fazie zapisow
    private Minigame current;   // gra w trakcie
    private BukkitTask schedulerTask;
    private BukkitTask lobbyTask;

    public MinigameManager(RandomMinigames plugin) {
        this.plugin = plugin;
    }

    public void register(Minigame game) {
        games.put(game.id(), game);
    }

    public Minigame get(String id) {
        return games.get(id.toLowerCase());
    }

    public Set<String> ids() {
        return games.keySet();
    }

    public void startScheduler() {
        long ticks = Math.max(1, plugin.getConfig().getLong("interval-minutes", 5)) * 60L * 20L;
        schedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> openLobby(null, false), ticks, ticks);
    }

    public boolean isBusy() {
        return pending != null || current != null;
    }

    public boolean hasPending() {
        return pending != null;
    }

    /** Otwiera zapisy na gre. forced == null -> losowa. */
    public boolean openLobby(Minigame forced, boolean ignoreMinPlayers) {
        if (isBusy() || games.isEmpty()) return false;

        Minigame game = forced;
        if (game == null) {
            List<Minigame> list = new ArrayList<>(games.values());
            game = list.get(ThreadLocalRandom.current().nextInt(list.size()));
        }
        if (!ignoreMinPlayers && Bukkit.getOnlinePlayers().size() < game.minPlayers()) return false;

        pending = game;
        joined.clear();
        int seconds = Math.max(5, plugin.getConfig().getInt("lobby-seconds", 30));

        Component join = Component.text(" [KLIKNIJ, ABY DOŁĄCZYĆ]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.runCommand("/minigame join"));
        Bukkit.broadcast(RandomMinigames.msg("Za " + seconds + " s startuje: " + game.name() + "! " + game.description())
                .append(join));

        lobbyTask = Bukkit.getScheduler().runTaskLater(plugin, this::launch, seconds * 20L);
        return true;
    }

    public void forceStart() {
        if (pending == null) return;
        if (lobbyTask != null) lobbyTask.cancel();
        launch();
    }

    private void launch() {
        Minigame game = pending;
        pending = null;
        lobbyTask = null;
        if (game == null) return;

        Set<Player> players = new LinkedHashSet<>();
        for (UUID id : joined) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) players.add(p);
        }
        joined.clear();

        if (players.size() < game.minPlayers()) {
            Bukkit.broadcast(RandomMinigames.msg("Za mało graczy na " + game.name() + ". Runda odwołana."));
            return;
        }
        current = game;
        game.start(players);
    }

    public boolean join(Player p) {
        if (pending == null) return false;
        joined.add(p.getUniqueId());
        return true;
    }

    public void leave(Player p) {
        joined.remove(p.getUniqueId());
        if (current != null) current.eliminate(p, "opuścił grę");
    }

    public void stop() {
        if (lobbyTask != null) lobbyTask.cancel();
        lobbyTask = null;
        pending = null;
        joined.clear();
        if (current != null) current.forceStop();
    }

    public void gameEnded() {
        current = null;
    }

    public void shutdown() {
        if (schedulerTask != null) schedulerTask.cancel();
        stop();
    }

    public Minigame pending() {
        return pending;
    }
}
