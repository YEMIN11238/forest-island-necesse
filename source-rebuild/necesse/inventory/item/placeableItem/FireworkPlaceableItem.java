package necesse.inventory.item.placeableItem;

import java.awt.Color;
import java.util.function.Consumer;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketSpawnFirework;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.entity.particle.fireworks.FireworksExplosion;
import necesse.entity.particle.fireworks.FireworksPath;
import necesse.entity.particle.fireworks.FireworksRocketParticle;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;

public class FireworkPlaceableItem extends PlaceableItem {
   public FireworkPlaceableItem() {
      super(100, true);
      this.rarity = Item.Rarity.COMMON;
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "fireworkrockettip"));
      GNDItemMap gndData = item.getGndData();
      FireworkPlaceableItem.FireworksShape shape = getShape(gndData);
      tooltips.add(
         Localization.translate(
            "itemtooltip", "fireworkshape", "shape", shape == null ? Localization.translate("itemtooltip", "fireworkrandom") : shape.displayName.translate()
         )
      );
      FireworkPlaceableItem.FireworkColor color = getColor(gndData);
      tooltips.add(
         Localization.translate(
            "itemtooltip", "fireworkcolor", "color", color == null ? Localization.translate("itemtooltip", "fireworkrandom") : color.displayName.translate()
         )
      );
      FireworkPlaceableItem.FireworkCrackle crackle = getCrackle(gndData);
      tooltips.add(crackle == null ? Localization.translate("itemtooltip", "fireworkrandomcrackle") : crackle.displayName.translate());
      return tooltips;
   }

   @Override
   public boolean canCombineItem(Level level, PlayerMob player, InventoryItem me, InventoryItem them, String purpose) {
      return !super.canCombineItem(level, player, me, them, purpose) ? false : this.isSameGNDData(level, me, them, purpose);
   }

   @Override
   public boolean isSameGNDData(Level level, InventoryItem me, InventoryItem them, String purpose) {
      return me.getGndData().sameKeys(them.getGndData(), "shape", "color", "crackle");
   }

   public static void spawnFireworks(GNDItemMap gndData, Level level, float x, float y, int height, float size, int seed) {
      GameRandom random = new GameRandom(seed);
      level.entityManager
         .addParticle(new FireworksRocketParticle(level, x, y, 1200L, height, getExplosion(gndData, size, random), random), true, Particle.GType.CRITICAL);
   }

   public static FireworksExplosion getExplosion(GNDItemMap gndData, float size, GameRandom random) {
      FireworkPlaceableItem.FireworksShape shape = getShape(gndData);
      if (shape == null) {
         shape = random.getOneOf(FireworkPlaceableItem.FireworksShape.values());
      }

      FireworkPlaceableItem.FireworkColor color = getColor(gndData);
      if (color == null) {
         color = random.getOneOf(FireworkPlaceableItem.FireworkColor.values());
      }

      FireworkPlaceableItem.FireworkCrackle crackle = getCrackle(gndData);
      if (crackle == null) {
         crackle = random.getOneOf(FireworkPlaceableItem.FireworkCrackle.values());
      }

      FireworksExplosion explosion = new FireworksExplosion(null);
      shape.explosionModifier.play(explosion, size, random);
      color.explosionModifier.accept(explosion);
      crackle.explosionModifier.accept(explosion);
      return explosion;
   }

   @Override
   public InventoryItem onPlace(Level level, int x, int y, PlayerMob player, int seed, InventoryItem item, GNDItemMap mapContent) {
      if (level.isServer()) {
         int tileX = GameMath.getTileCoordinate(x);
         int tileY = GameMath.getTileCoordinate(y);
         level.getServer()
            .network
            .sendToClientsWithTile(
               new PacketSpawnFirework(
                  level,
                  x,
                  y,
                  GameRandom.globalRandom.getIntBetween(300, 400),
                  GameRandom.globalRandom.getIntBetween(150, 250),
                  item.getGndData(),
                  GameRandom.globalRandom.nextInt()
               ),
               level,
               tileX,
               tileY
            );
      }

      if (this.isSingleUse(player)) {
         item.setAmount(item.getAmount() - 1);
      }

      return item;
   }

   public static FireworkPlaceableItem.FireworksShape getShape(GNDItemMap gndData) {
      if (gndData.hasKey("shape")) {
         FireworkPlaceableItem.FireworksShape[] ar = FireworkPlaceableItem.FireworksShape.values();
         return ar[Math.abs(gndData.getByte("shape") - 1) % ar.length];
      } else {
         return null;
      }
   }

   public static FireworkPlaceableItem.FireworkColor getColor(GNDItemMap gndData) {
      if (gndData.hasKey("color")) {
         FireworkPlaceableItem.FireworkColor[] ar = FireworkPlaceableItem.FireworkColor.values();
         return ar[Math.abs(gndData.getByte("color") - 1) % ar.length];
      } else {
         return null;
      }
   }

   public static FireworkPlaceableItem.FireworkCrackle getCrackle(GNDItemMap gndData) {
      if (gndData.hasKey("crackle")) {
         FireworkPlaceableItem.FireworkCrackle[] ar = FireworkPlaceableItem.FireworkCrackle.values();
         return ar[Math.abs(gndData.getByte("crackle") - 1) % ar.length];
      } else {
         return null;
      }
   }

   public enum FireworkColor {
      Confetti(
         new LocalMessage("itemtooltip", "fireworkconfetti"), e -> e.colorGetter = (p, progress, random) -> Color.getHSBColor(random.nextFloat(), 1.0F, 1.0F)
      ),
      Flame(new LocalMessage("itemtooltip", "fireworkflame"), e -> e.colorGetter = (p, progress, random) -> ParticleOption.randomFlameColor(random)),
      Red(new LocalMessage("itemtooltip", "fireworkred"), e -> e.colorGetter = (p, progress, random) -> ParticleOption.randomFlameColor(random, 0.0F)),
      Green(new LocalMessage("itemtooltip", "fireworkgreen"), e -> e.colorGetter = (p, progress, random) -> ParticleOption.randomFlameColor(random, 110.0F)),
      Blue(new LocalMessage("itemtooltip", "fireworkblue"), e -> e.colorGetter = (p, progress, random) -> ParticleOption.randomFlameColor(random, 240.0F)),
      Pink(new LocalMessage("itemtooltip", "fireworkpink"), e -> e.colorGetter = (p, progress, random) -> ParticleOption.randomFlameColor(random, 310.0F));

      public final GameMessage displayName;
      public final Consumer<FireworksExplosion> explosionModifier;

      FireworkColor(GameMessage displayName, Consumer<FireworksExplosion> explosionModifier) {
         this.displayName = displayName;
         this.explosionModifier = explosionModifier;
      }
   }

   public enum FireworkCrackle {
      NoCrackle(new LocalMessage("itemtooltip", "fireworknocrackle"), e -> e.popChance = 0.0F),
      Crackle(new LocalMessage("itemtooltip", "fireworkyescrackle"), e -> e.popChance = 0.5F);

      public final GameMessage displayName;
      public Consumer<FireworksExplosion> explosionModifier;

      FireworkCrackle(GameMessage displayName, Consumer<FireworksExplosion> explosionModifier) {
         this.displayName = displayName;
         this.explosionModifier = explosionModifier;
      }
   }

   public static class FireworkItemCreator {
      FireworkPlaceableItem.FireworksShape shape;
      FireworkPlaceableItem.FireworkColor color;
      FireworkPlaceableItem.FireworkCrackle crackle;

      public GNDItemMap getGNDData() {
         GNDItemMap gndData = new GNDItemMap();
         this.applyToData(gndData);
         return gndData;
      }

      public FireworkPlaceableItem.FireworkItemCreator applyToData(GNDItemMap gndData) {
         if (this.shape != null) {
            gndData.setByte("shape", (byte)(this.shape.ordinal() + 1));
         }

         if (this.color != null) {
            gndData.setByte("color", (byte)(this.color.ordinal() + 1));
         }

         if (this.crackle != null) {
            gndData.setByte("crackle", (byte)(this.crackle.ordinal() + 1));
         }

         return this;
      }

      public FireworkPlaceableItem.FireworkItemCreator shape(FireworkPlaceableItem.FireworksShape shape) {
         this.shape = shape;
         return this;
      }

      public FireworkPlaceableItem.FireworkItemCreator color(FireworkPlaceableItem.FireworkColor color) {
         this.color = color;
         return this;
      }

      public FireworkPlaceableItem.FireworkItemCreator crackle(FireworkPlaceableItem.FireworkCrackle crackle) {
         this.crackle = crackle;
         return this;
      }

      public FireworkPlaceableItem.FireworkItemCreator applyToItem(InventoryItem item) {
         return this.applyToData(item.getGndData());
      }

      public InventoryItem getNewItem() {
         InventoryItem item = new InventoryItem("fireworkrocket");
         this.applyToItem(item);
         return item;
      }
   }

   public enum FireworksShape {
      Sphere(new LocalMessage("itemtooltip", "fireworksphere"), (e, size, random) -> e.pathGetter = FireworksPath.sphere(size)),
      Splash(new LocalMessage("itemtooltip", "fireworksplash"), (e, size, random) -> e.pathGetter = FireworksPath.splash(random.getIntBetween(0, 360), size)),
      Disc(new LocalMessage("itemtooltip", "fireworkdisc"), (e, size, random) -> {
         e.pathGetter = FireworksPath.disc(size);
         e.minSize = 20;
         e.maxSize = 30;
         e.trailSize = 15.0F;
         e.trailFadeTime = 1000;
         e.particles = 50;
         e.trailChance = 1.0F;
      }),
      Star(
         new LocalMessage("itemtooltip", "fireworkstar"),
         (e, size, random) -> e.pathGetter = FireworksPath.shape(FireworksPath.star, size, random2 -> Math.min(1.0F, random2.nextFloat() * 1.2F))
      ),
      Heart(
         new LocalMessage("itemtooltip", "fireworkheart"),
         (e, size, random) -> e.pathGetter = FireworksPath.shape(FireworksPath.heart, size, random2 -> Math.min(1.0F, random2.nextFloat() * 1.2F))
      );

      public final GameMessage displayName;
      public final FireworksRocketParticle.ExplosionModifier explosionModifier;

      FireworksShape(GameMessage displayName, FireworksRocketParticle.ExplosionModifier explosionModifier) {
         this.displayName = displayName;
         this.explosionModifier = explosionModifier;
      }
   }
}
