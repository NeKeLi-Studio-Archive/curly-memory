package pl.randomminigames;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class RandomMinigames extends JavaPlugin {

    private MinigameManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new MinigameManager(this);

        List<String> enabled = getConfig().getStringList("enabled-games");
        if (enabled.contains("tntrun")) manager.register(new TntRun(this));
        if (enabled.contains("spleef")) manager.register(new Spleef(this));
        if (enabled.contains("elytra")) manager.register(new ElytraRace(this));

        manager.startScheduler();

        PluginCommand cmd = getCommand("minigame");
        if (cmd != null) {
            MinigameCommand handler = new MinigameCommand(this, manager);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }
        getLogger().info("RandomMinigames wlaczony. Gier: " + manager.ids());
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.shutdown();
    }

    public MinigameManager manager() {
        return manager;
    }

    public static Component msg(String text) {
        return Component.text("[Minigry] ", NamedTextColor.GOLD)
                .append(Component.text(text, NamedTextColor.YELLOW));
    }
}
