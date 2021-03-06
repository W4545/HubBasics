package net.notfab.hubbasics.bungee.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.notfab.hubbasics.bungee.HubBasics;
import net.notfab.hubbasics.bungee.Module;
import net.notfab.hubbasics.bungee.managers.HBLogger;
import net.notfab.hubbasics.bungee.utils.Messages;
import net.notfab.spigot.simpleconfig.SimpleConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class LobbyCommand extends Command implements Module {

    private static final HBLogger logger = HBLogger.getLogger("HubBasics");
    private HubBasics hubBasics;

    private List<String> servers = new ArrayList<>();

    public LobbyCommand(HubBasics hubBasics) {
        super("lobby", null, "hub");
        this.hubBasics = hubBasics;
    }

    @Override
    public void setup(HubBasics hubBasics) {
        SimpleConfig config = hubBasics.getConfigManager().getNewConfig("config.yml");
        boolean isList = config.get("Lobby") instanceof List;
        if (isList) {
            for (String server : config.getStringList("Lobby")) {
                if (hubBasics.getProxy().getServerInfo(server) == null) {
                    logger.warn("Unknown Server: " + server);
                    continue;
                }
                this.servers.add(server);
            }
        } else {
            String server = config.getString("Lobby");
            if (hubBasics.getProxy().getServerInfo(server) == null) {
                logger.warn("Unknown Server: " + server);
                return;
            }
            this.servers.add(server);
        }
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        ServerInfo serverInfo = this.getLobby();
        if (serverInfo != null && commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            if (!player.getServer().getInfo().getName().equals(serverInfo.getName())) {
                player.connect(getLobby());
            } else {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("HubBasics");
                out.writeUTF("Lobby");
                player.sendData("BungeeCord", out.toByteArray());
            }
        } else if (serverInfo == null) {
            commandSender.sendMessage(new TextComponent(Messages.get(commandSender, "LOBBY_NOT_DEFINED")));
        } else {
            commandSender.sendMessage(new TextComponent(Messages.get(commandSender, "COMMAND_PLAYER")));
        }
    }

    private ServerInfo getLobby() {
        if (this.servers.size() == 1) {
            return hubBasics.getProxy().getServerInfo(this.servers.get(0));
        }
        int lowest = Integer.MAX_VALUE - 1;
        AtomicReference<ServerInfo> serverInfo = new AtomicReference<>();
        this.servers.forEach(lobby -> {
            ServerInfo info = hubBasics.getProxy().getServerInfo(lobby);
            if (info.getPlayers().size() < lowest) {
                serverInfo.set(info);
            }
        });
        return serverInfo.get();
    }

}
