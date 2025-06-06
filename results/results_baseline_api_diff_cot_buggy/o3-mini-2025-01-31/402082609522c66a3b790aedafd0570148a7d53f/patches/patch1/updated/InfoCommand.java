package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.model.UserPreference;
import com.github.games647.changeskin.core.model.skin.SkinModel;
import com.github.games647.changeskin.core.shared.SkinFormatter;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.google.inject.Inject;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.entity.living.player.Player;

public class InfoCommand implements CommandExecutor, ChangeSkinCommand {

    @Inject
    private ChangeSkinSponge plugin;

    @Inject
    private SkinFormatter formatter;

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (!Player.class.isInstance(src)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        UUID uniqueId = ((Player) src).getUniqueId();
        Sponge.server().scheduler().executor(plugin).submit(() -> {
            UserPreference preferences = plugin.getCore().getStorage().getPreferences(uniqueId);
            Sponge.server().scheduler().executor(plugin).submit(() -> sendSkinDetails(uniqueId, preferences));
        });

        return CommandResult.success();
    }

    public CommandSpec buildSpec() {
        Command cmd = Command.builder()
                .executor(this)
                .permission(PomData.ARTIFACT_ID + ".command.skininfo.base")
                .build();
        return new CommandSpecWrapper(cmd);
    }

    private void sendSkinDetails(UUID uuid, UserPreference preference) {
        Optional<Player> optPlayer = Sponge.server().player(uuid);
        if (optPlayer.isPresent()) {
            Player player = optPlayer.get();

            Optional<SkinModel> optSkin = preference.getTargetSkin();
            if (optSkin.isPresent()) {
                String template = plugin.getCore().getMessage("skin-info");
                String formatted = formatter.apply(template, optSkin.get());

                Component text = LegacyComponentSerializer.legacyAmpersand().deserialize(formatted);
                player.sendMessage(text);
            } else {
                plugin.sendMessage(player, "skin-not-found");
            }
        }
    }

    // Compatibility stub to replace the removed CommandSource type.
    public static interface CommandSource {
    }

    // Minimal compatibility implementation for CommandResult.
    public static class CommandResult {
        private final boolean success;

        private CommandResult(boolean success) {
            this.success = success;
        }

        public static CommandResult empty() {
            return new CommandResult(false);
        }

        public static CommandResult success() {
            return new CommandResult(true);
        }
    }

    // Stub interface for CommandSpec that extends the new Command interface.
    public static interface CommandSpec extends Command {
    }

    // Wrapper to adapt a Command from the new API into our CommandSpec.
    private static class CommandSpecWrapper implements CommandSpec {
        private final Command command;

        CommandSpecWrapper(Command command) {
            this.command = command;
        }

        @Override
        public CommandResult process(CommandContext context) throws CommandException {
            return command.process(context);
        }
    }
}
