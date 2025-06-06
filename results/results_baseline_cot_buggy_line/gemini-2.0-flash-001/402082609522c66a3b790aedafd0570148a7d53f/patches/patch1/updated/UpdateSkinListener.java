package com.github.games647.changeskin.sponge.bungee;

import com.github.games647.changeskin.core.message.SkinUpdateMessage;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.task.SkinApplier;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;

import java.util.Optional;

import org.spongepowered.api.Platform.Type;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.network.ChannelBinding.RawDataChannel;
import org.spongepowered.api.network.ChannelBuf;
import org.spongepowered.api.network.MessageReader;
import org.spongepowered.api.network.RemoteConnection;
import org.spongepowered.api.scheduler.Task;

import java.io.IOException;

public class UpdateSkinListener  {

    @Inject
    private ChangeSkinSponge plugin;

    public void handlePayload(RawDataChannel channel, RemoteConnection connection, ChannelBuf data) {
        byte[] rawData = data.array();
        ByteArrayDataInput dataInput = ByteStreams.newDataInput(rawData);
        SkinUpdateMessage updateMessage = new SkinUpdateMessage();
        updateMessage.readFrom(dataInput);

        String playerName = updateMessage.getPlayerName();
        Optional<Player> receiver = Sponge.getServer().getPlayer(playerName);
        if (receiver.isPresent()) {
            Runnable skinUpdater = new SkinApplier(plugin, receiver.get(), receiver.get(), null, false);
            Task.builder().execute(skinUpdater).submit(plugin);
        }
    }
}