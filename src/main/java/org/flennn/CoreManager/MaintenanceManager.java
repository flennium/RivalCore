package org.flennn.CoreManager;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.flennn.Config.ConfigManager;

import java.util.List;
import java.util.UUID;

public class MaintenanceManager {
    private final ProxyServer proxy;
    private final ConfigManager config;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private Component normalMotd;
    private Component maintenanceMotd;

    public MaintenanceManager(ProxyServer proxy, ConfigManager config) {
        this.proxy = proxy;
        this.config = config;
        reloadMOTD();
    }

    public void reloadMOTD() {
        this.normalMotd = mm.deserialize(config.getMOTD());
        this.maintenanceMotd = mm.deserialize(config.getMaintenanceMOTD());
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing.Builder builder = event.getPing().asBuilder();
        boolean isMaintenance = config.isMaintenanceMode();

        builder.description(isMaintenance ? maintenanceMotd : normalMotd);

        builder.onlinePlayers(isMaintenance ? 0 : config.getFakeOnlinePlayers());
        builder.maximumPlayers(isMaintenance ? 404 : config.getMaxPlayers());

        builder.samplePlayers(getSamplePlayers(isMaintenance));

        builder.version(isMaintenance
                ? new ServerPing.Version(0, PlainTextComponentSerializer.plainText().serialize(mm.deserialize(config.getMaintenanceVersionMessage())))
                : null);


        event.setPing(builder.build());
    }


    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        if (config.isMaintenanceMode() && !config.getBypassPlayers().contains(event.getUsername())) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                    mm.deserialize(config.getMaintenanceModeMessage())
            ));
        }
    }

    private ServerPing.SamplePlayer[] getSamplePlayers(boolean isMaintenance) {
        MiniMessage mm = MiniMessage.miniMessage();
        PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

        List<String> descriptions = isMaintenance ? config.getMaintenanceMotdDescription() : config.getMotdDescription();

        return descriptions.stream()
                .map(line -> new ServerPing.SamplePlayer(serializer.serialize(mm.deserialize(line)), UUID.randomUUID()))
                .toArray(ServerPing.SamplePlayer[]::new);
    }
}
