package org.leralix.tan.api.external.papi.entries;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.leralix.tan.api.external.papi.PlaceHolderAPI;
import org.leralix.tan.dataclass.ITanPlayer;
import org.leralix.tan.lang.Lang;
import org.leralix.tan.storage.stored.PlayerDataStorage;

public class OtherPlayerTownTag extends PapiEntry {

    public OtherPlayerTownTag() {
        super("player_{}_town_tag");
    }

    @Override
    public String getData(OfflinePlayer player, @NotNull String params) {

        ITanPlayer tanPlayer = PlayerDataStorage.getInstance().get(player.getUniqueId());

        if (tanPlayer == null) {
            return PLAYER_NOT_FOUND;
        }

        String[] values = extractValues(params);
        if (values.length < 1) {
            return PlaceHolderAPI.PLACEHOLDER_NOT_FOUND;
        }
        OfflinePlayer playerSelected = Bukkit.getOfflinePlayer(values[0]);

        ITanPlayer otherTanPlayer = PlayerDataStorage.getInstance().get(playerSelected);

        if (otherTanPlayer == null) {
            return PLAYER_NOT_FOUND;
        }

        return otherTanPlayer.hasTown() ?
                otherTanPlayer.getTown().getTownTag() :
                Lang.NO_TOWN.get(tanPlayer);
    }
}
