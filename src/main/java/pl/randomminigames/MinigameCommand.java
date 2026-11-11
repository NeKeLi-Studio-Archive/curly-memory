package pl.randomminigames;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MinigameCommand implements CommandExecutor, TabCompleter {

    private final RandomMinigames plugin;
    private final MinigameManager manager;

    public MinigameCommand(RandomMinigames plugin, MinigameManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(RandomMinigames.msg("Użycie: /" + label + " <join|leave|list|start|stop|reload>"));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "join" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Tylko dla graczy."); return true; }
                if (manager.join(p)) p.sendMessage(RandomMinigames.msg("Dołączono! Poczekaj na start."));
                else p.sendMessage(RandomMinigames.msg("Teraz nie trwają żadne zapisy."));
            }
            case "leave" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Tylko dla graczy."); return true; }
                manager.leave(p);
                p.sendMessage(RandomMinigames.msg("Opuściłeś minigrę."));
            }
            case "list" -> sender.sendMessage(RandomMinigames.msg("Gry: " + String.join(", ", manager.ids())));
            case "start" -> {
                if (!sender.hasPermission("minigame.admin")) { sender.sendMessage("Brak uprawnień."); return true; }
                if (manager.hasPending()) {
                    manager.forceStart();
                    return true;
                }
                Minigame forced = null;
                if (args.length > 1) {
                    forced = manager.get(args[1]);
                    if (forced == null) { sender.sendMessage(RandomMinigames.msg("Nie ma takiej gry.")); return true; }
                }
                if (!manager.openLobby(forced, true)) sender.sendMessage(RandomMinigames.msg("Nie można teraz uruchomić gry."));
            }
            case "stop" -> {
                if (!sender.hasPermission("minigame.admin")) { sender.sendMessage("Brak uprawnień."); return true; }
                manager.stop();
                sender.sendMessage(RandomMinigames.msg("Zatrzymano."));
            }
            case "reload" -> {
                if (!sender.hasPermission("minigame.admin")) { sender.sendMessage("Brak uprawnień."); return true; }
                plugin.reloadConfig();
                sender.sendMessage(RandomMinigames.msg("Konfiguracja przeładowana (zmiana listy gier/interwału wymaga restartu)."));
            }
            default -> sender.sendMessage(RandomMinigames.msg("Nieznana podkomenda."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("join", "leave", "list", "start", "stop", "reload"))
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            for (String id : manager.ids()) if (id.startsWith(args[1].toLowerCase())) out.add(id);
        }
        return out;
    }
}
