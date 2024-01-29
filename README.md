# Firesparks
> Compile: `mvn package`

![Imgur](https://i.imgur.com/JCzRE7B.png)
Small Bukkit plugin for Minecraft that tweaks natural hearth regeneration in interesting way.

..and not only regeneration, but also adds some other features.
## Coldness system
![Imgur](https://i.imgur.com/EbFrce3.png)
When a player don't stand next to the campfire or has no armor - he will get cold.

## Item burning feature
![Imgur](https://i.imgur.com/php5Ny6.png)
You can put items in campfire, like food. After an item burn - it spreads some potion effects. You can configure this feature in `config.yml`.

## Highly customizable config
Now you can configure **comfort growth**, that allows you to gain more comfort per data-update.

### Config settings
`growth-amount: 6`
> By default, player gains 6 comfort per 2 seconds.
> To fill the comfort bar - you need 1000 comfort.

`progressbar-name: "&lComfort"`
> The name speaks for itself

`coldness.enable: true`
> Enables coldness system.
> In cold biomes player can freeze to death, if it doesn't wear any armor.
> You can stand next to the campfire to keep warm.

`item-burn.enable: true`
> Enables item burning feature.
> You can right-click with item from config to burn it in campfire.
> You can use this template to add items:
> ```yaml
> MATERIAL: # https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html
>     effect: EFFECT # https://hub.spigotmc.org/javadocs/spigot/org/bukkit/potion/PotionEffectType.html
>     duration: INTEGER
>     time-to-burn: INTEGER
>     color: '#HEXCOLOR'
> ```
