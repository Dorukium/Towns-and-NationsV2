package org.leralix.tan.api.external.papi.entries;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.leralix.tan.api.external.papi.PlaceHolderAPI;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.lang.LangType;
import org.leralix.tan.storage.LocalChatStorage;
import org.leralix.tan.storage.stored.PlayerDataStorage;

public class OtherPlayerChatMode extends PapiEntry {

    public OtherPlayerChatMode () {
        super("chat_mode_{}");
    }

    @Override
    public String getData(OfflinePlayer player, @NotNull String params) {

        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().get(player.getUniqueId());

        if (tanPlayer == null) {
            return PLAYER_NOT_FOUND;
        }
        LangType langType = tanPlayer.getLang();
        String[] values = extractValues(params);
        if (values.length < 1) {
            return PlaceHolderAPI.PLACEHOLDER_NOT_FOUND;
        }
        OfflinePlayer playerSelected = Bukkit.getOfflinePlayer(values[0]);
        if(!playerSelected.isOnline()){
            return Lang.INVALID_PLAYER_NAME.get(langType);
        }
        var chatScope = LocalChatStorage.getPlayerChatScope(playerSelected.getUniqueId().toString());
        if (chatScope == null) {
            return PlaceHolderAPI.PLACEHOLDER_NOT_FOUND;
        }
        return chatScope.getName(langType);
    }
}
