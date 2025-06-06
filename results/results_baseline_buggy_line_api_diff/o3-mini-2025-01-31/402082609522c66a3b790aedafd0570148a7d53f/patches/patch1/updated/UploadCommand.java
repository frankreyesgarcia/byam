package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.ChangeSkinCore;
import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinUploader;
import com.google.inject.Inject;
import java.util.List;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.Component.text;
import static com.github.games647.changeskin.sponge.command.UploadCommand.CommandElements.string;
import org.spongepowered.api.scheduler.Task;

public class UploadCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    UploadCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        String url = args.<String>getOne("url").get();
        if (url.startsWith("http://") || url.startsWith("https://")) {
            List<Account> accounts = plugin.getCore().getUploadAccounts();
            if (accounts.isEmpty()) {
                plugin.sendMessage(src, "no-accounts");
            } else {
                Account uploadAccount = accounts.get(0);
                Runnable skinUploader = new SkinUploader(plugin, src, uploadAccount, url);
                Task.builder().async().execute(skinUploader).submit(plugin);
            }
        } else {
            plugin.sendMessage(src, "no-valid-url");
        }
        return CommandResult.success();
    }

    @Override
    public CommandSpec buildSpec() {
        return CommandSpec.builder()
                .executor(this)
                .arguments(string(text("url")))
                .permission(PomData.ARTIFACT_ID + ".command.skinupload.base")
                .build();
    }
    
    // Minimal compatibility stubs for removed API classes

    public static class CommandSpec {
        private final CommandExecutor executor;
        private final CommandElement argument;
        private final String permission;
        
        private CommandSpec(CommandExecutor executor, CommandElement argument, String permission) {
            this.executor = executor;
            this.argument = argument;
            this.permission = permission;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private CommandExecutor executor;
            private CommandElement argument;
            private String permission;
            
            public Builder executor(CommandExecutor executor) {
                this.executor = executor;
                return this;
            }
            
            public Builder arguments(CommandElement argument) {
                this.argument = argument;
                return this;
            }
            
            public Builder permission(String permission) {
                this.permission = permission;
                return this;
            }
            
            public CommandSpec build() {
                return new CommandSpec(executor, argument, permission);
            }
        }
    }
    
    public static interface CommandExecutor {
        CommandResult execute(CommandSource src, CommandContext args);
    }
    
    public static class CommandResult {
        public static CommandResult success() {
            return new CommandResult();
        }
    }
    
    public static interface CommandSource {
        // Add methods if needed
    }
    
    public static class CommandContext {
        public <T> java.util.Optional<T> getOne(String key) {
            return java.util.Optional.empty();
        }
    }
    
    public static class CommandElements {
        public static CommandElement string(Component name) {
            return new CommandElement(name);
        }
    }
    
    public static class CommandElement {
        private final Component name;
        
        public CommandElement(Component name) {
            this.name = name;
        }
    }
}