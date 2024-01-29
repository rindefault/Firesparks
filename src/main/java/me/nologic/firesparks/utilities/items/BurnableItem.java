package me.nologic.firesparks.utilities.items;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

 /**
  * BurnableItem class. An instance of this class is a reflection of an item from config.
  * Made to simplify collection of information about config items.
  **/

public class BurnableItem {

    private final String name;
    private final String potionEffect;
    private final String potionColor;

    private final int timeToBurn;
    private final int duration;

    public BurnableItem(final int timeToBurn, final int duration, final String name, final String potionEffect, final String potionColor) {
        this.name = name;
        this.timeToBurn = timeToBurn;
        this.potionEffect = potionEffect;
        this.potionColor = potionColor;
        this.duration = duration;
    }

    public String getName() {
        return name;
    }
    public Material getMaterial() {
        return Material.getMaterial(name);
    }
    public int getTimeToBurn() {
        return timeToBurn;
    }
    public PotionEffectType getPotionEffect() {
        return PotionEffectType.getByName(potionEffect);
    }
    public int getDuration() {
        return duration;
    }
    public String getPotionColor() {
        return potionColor;
    }
}
