package necesse.level.gameObject;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.regionSystem.SimulatePriorityList;

public class ThornsObject extends GrassObject {
   public static double spreadChance = GameMath.getAverageSuccessRuns(600.0);

   public ThornsObject() {
      super("thorns", -1);
      this.setItemCategory("materials", "flowers");
      this.playerCanPlaceOnOrganic = false;
      this.grassValidTileIDs.add(TileRegistry.mudID);
      this.mapColor = new Color(191, 90, 62);
      this.stackSize = 250;
      this.displayMapTooltip = true;
      this.weaveAmount = 0.05F;
      this.extraWeaveSpace = 32;
      this.randomYOffset = 3.0F;
      this.randomXOffset = 10.0F;
   }

   @Override
   public int getLightLevelMod(Level level, int x, int y) {
      return 30;
   }

   @Override
   public void tick(Mob mob, Level level, int x, int y) {
      super.tick(mob, level, x, y);
      if (level.isServer() && mob.canTakeDamage() && !mob.isBoss() && !mob.isHostile && !mob.isCritter) {
         if (!mob.isOnGenericCooldown("thornsdamage")) {
            int maxHealth = mob.getMaxHealth();
            float damage = Math.max((float)Math.pow(maxHealth, 0.5) + maxHealth / 30.0F, 10.0F);
            if (damage != 0.0F) {
               mob.isServerHit(new GameDamage(DamageTypeRegistry.TRUE, damage), 0.0F, 0.0F, 0.0F, new ThornsObject.ThornsAttacker());
               mob.startGenericCooldown("thornsdamage", 500L);
            }
         }

         level.sendObjectChangePacket(level.getServer(), x, y, 0, 0);
      }
   }

   public GrassSpreadOptions getSpreadOptions(Level level) {
      return GrassSpreadOptions.init(this, level).maxSpread(2, 10, 1);
   }

   @Override
   public void tick(Level level, int x, int y) {
      super.tick(level, x, y);
      if (level.isServer() && GameRandom.globalRandom.getChance(spreadChance)) {
         this.getSpreadOptions(level).tickSpread(x, y, true);
      }
   }

   @Override
   public void addSimulateLogic(Level level, int x, int y, long ticks, SimulatePriorityList list, boolean sendChanges) {
      super.addSimulateLogic(level, x, y, ticks, list, sendChanges);
      this.getSpreadOptions(level).addSimulateSpread(x, y, spreadChance, ticks, list, sendChanges);
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      Level level,
      int tileX,
      int tileY,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      Performance.record(tickManager, "thornsSetup", () -> {
         Integer[] adj = level.getAdjacentObjectsInt(tileX, tileY);
         int objs = 0;

         for (Integer id : adj) {
            if (id == this.getID()) {
               objs++;
            }
         }

         int minTextureIndex;
         int maxTextureIndex;
         if (objs < 4) {
            minTextureIndex = 4;
            maxTextureIndex = 7;
         } else {
            minTextureIndex = 0;
            maxTextureIndex = 3;
         }

         int drawX = camera.getTileDrawX(tileX);
         int drawY = camera.getTileDrawY(tileY);
         GameLight light = level.getLightLevel(tileX, tileY);
         this.addGrassDrawable(list, tileList, level, tileX, tileY, drawX, drawY, light, 6, -5, 0, minTextureIndex, maxTextureIndex);
         this.addGrassDrawable(list, tileList, level, tileX, tileY, drawX, drawY, light, 12, -5, 1);
         this.addGrassDrawable(list, tileList, level, tileX, tileY, drawX, drawY, light, 26, -5, 2, minTextureIndex, maxTextureIndex);
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      Integer[] adj = level.getAdjacentObjectsInt(tileX, tileY);
      int objs = 0;

      for (Integer id : adj) {
         if (id == this.getID()) {
            objs++;
         }
      }

      int minTextureIndex;
      int maxTextureIndex;
      if (objs < 4) {
         minTextureIndex = 4;
         maxTextureIndex = 7;
      } else {
         minTextureIndex = 0;
         maxTextureIndex = 3;
      }

      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      LinkedList<LevelSortedDrawable> list = new LinkedList<>();
      OrderableDrawables tileList = new OrderableDrawables(new TreeMap<>());
      this.addGrassDrawable(list, tileList, level, tileX, tileY, drawX, drawY, null, 6, -5, 0, minTextureIndex, maxTextureIndex, 0.5F);
      this.addGrassDrawable(list, tileList, level, tileX, tileY, drawX, drawY, null, 12, -5, 1, 0.5F);
      this.addGrassDrawable(list, tileList, level, tileX, tileY, drawX, drawY, null, 26, -5, 2, minTextureIndex, maxTextureIndex, 0.5F);
      tileList.forEach(d -> d.draw(level.tickManager()));
      list.forEach(d -> d.draw(level.tickManager()));
   }

   private static class ThornsAttacker implements Attacker {
      public ThornsAttacker() {
      }

      @Override
      public GameMessage getAttackerName() {
         return new LocalMessage("object", "thorns");
      }

      @Override
      public DeathMessageTable getDeathMessages() {
         return this.getDeathMessages("thorns", 2);
      }

      @Override
      public Mob getFirstAttackOwner() {
         return null;
      }
   }
}
