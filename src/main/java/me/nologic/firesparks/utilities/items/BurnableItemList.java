package me.nologic.firesparks.utilities.items;

import me.nologic.firesparks.Firesparks;
import me.nologic.firesparks.utilities.Configuration;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Set;

 /**
  * BurnableItemList class.
  * With the BurnableItemList#build() method all the BurnableItems from config get built into a list.
  * Made to collect all the BurnableItem from config for further utils.
  **/

public class BurnableItemList {

    private final ArrayList<BurnableItem> burnableItems;
    private static final FileConfiguration config = Configuration.getConfig();

    private BurnableItemList(final ArrayList<BurnableItem> burnableItems) {
        this.burnableItems = burnableItems;
    }

    @Nullable
    public ArrayList<BurnableItem> getItems() {
        return burnableItems;
    }

    @Nullable
    public ArrayList<Material> getMaterials() {
        ArrayList<Material> materials = new ArrayList<>();
        this.burnableItems.forEach(burnableItem -> materials.add(Material.getMaterial(burnableItem.getName())));
        return materials;
    }

    @NotNull
    public static BurnableItemList build() {

        ArrayList<BurnableItem> burnableItemArrayList = new ArrayList<>();
        Set<String> itemNames = Firesparks.getPlugin().getConfig().getConfigurationSection("item-burn.item-list").getKeys(false);

        for (String name : itemNames) {
            String effect = config.getString("item-burn.item-list." + name + ".effect");
            String potionColor = config.getString("item-burn.item-list." + name + ".color");

            int duration = config.getInt("item-burn.item-list." + name + ".duration");
            int timeToBurn = config.getInt("item-burn.item-list." + name + ".time-to-burn");

            BurnableItem burnableItem = new BurnableItem(timeToBurn, duration, name, effect, potionColor);
            burnableItemArrayList.add(burnableItem);
        }
        return new BurnableItemList(burnableItemArrayList);
    }

}
