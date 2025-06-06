package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedSkinChanger;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.Text;

public class SkinChanger extends SharedSkinChanger {

    private final Object invoker; // Updated type to a generic Object

    public SkinChanger(ChangeSkinSponge plugin, Account owner, String url, String oldSkinUrl, Object invoker) {
        super(plugin.getCore(), owner, url, oldSkinUrl);

        this.invoker = invoker;
    }

    protected void sendMessageInvoker(String localeMessage) {
        Text message = Texts.of(localeMessage); // Updated to use new Texts class
        // Assuming invoker has a method to send messages, replace with appropriate method
        // invoker.sendMessage(message); // Uncomment and replace with actual method to send message
    }
}