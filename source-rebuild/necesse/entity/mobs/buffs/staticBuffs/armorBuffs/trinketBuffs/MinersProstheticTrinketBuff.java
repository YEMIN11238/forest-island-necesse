package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.HashMap;
import necesse.engine.GameTileRange;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobObjectDamagedEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.ToolDamageItem;
import necesse.inventory.item.trinketItem.TrinketItem;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.LevelObjectHit;

public class MinersProstheticTrinketBuff extends TrinketBuff {
   public static GameTileRange tileRange = new GameTileRange(3);

   @Override
   public ListGameTooltips getTrinketTooltip(TrinketItem trinketItem, InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getTrinketTooltip(trinketItem, item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "minersprosthetictip"), 350);
      return tooltips;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      eventSubscriber.subscribeEvent(
         MobObjectDamagedEvent.class,
         event -> {
            if (event.level.isServer()) {
               if (event.totalDamage > 0) {
                  if (event.attacker instanceof ToolDamageItem.ToolDamageItemAttacker) {
                     int centerTileX = event.result.getTileX();
                     int centerTileY = event.result.getTileY();
                     int hitX = event.result.showEffects ? event.result.mouseX : centerTileX * 32 + 16;
                     int hitY = event.result.showEffects ? event.result.mouseY : centerTileY * 32 + 16;
                     if (this.isValidObject(event.result.levelObject.object)) {
                        Float hitDir = GameMath.normalize(hitX - buff.owner.x, hitY - buff.owner.y);
                        float hitAngle = GameMath.getAngle(hitDir);
                        float angleOffset = 120.0F;
                        int arms = 2;
                        PlayerMob player = event.attacker.getFirstPlayerOwner();
                        ServerClient client = player != null && player.isServerClient() ? player.getServerClient() : null;
                        CollisionFilter collisionFilter = new CollisionFilter()
                           .customAdder((tp, rectangles) -> rectangles.add(new Rectangle(tp.tileX * 32, tp.tileY * 32, 32, 32)))
                           .addFilter(tp -> this.isValidObject(tp.object().object));
                        HashMap<Point, Integer> damageDealt = new HashMap<>();
                        damageDealt.put(new Point(centerTileX, centerTileY), event.totalDamage);

                        for (int i = 0; i < arms; i++) {
                           float currentAngle = GameRandom.globalRandom.getFloatOffset(hitAngle, angleOffset / 2.0F);
                           float range = 64.0F;
                           Float currentDir = GameMath.getAngleDir(currentAngle);
                           java.awt.geom.Line2D.Float line = new java.awt.geom.Line2D.Float(
                              hitX, hitY, hitX + currentDir.x * range, hitY + currentDir.y * range
                           );
                           ArrayList<LevelObjectHit> collisions = event.level.getCollisions(line, collisionFilter);
                           int damage = Math.max(1, (int)(event.totalDamage * GameRandom.globalRandom.getFloatBetween(0.3F, 1.0F)));

                           for (LevelObjectHit collision : collisions) {
                              int currentDamageDealt = damageDealt.getOrDefault(collision.getPoint(), 0);
                              if (currentDamageDealt < event.totalDamage) {
                                 int finalDamage = Math.min(event.totalDamage - currentDamageDealt, damage);
                                 damageDealt.put(collision.getPoint(), currentDamageDealt + finalDamage);
                                 event.level
                                    .entityManager
                                    .doObjectDamage(
                                       event.result.objectLayerID,
                                       collision.tileX,
                                       collision.tileY,
                                       finalDamage,
                                       event.toolTier,
                                       new MinersProstheticTrinketBuff.MinersProstheticAttacker(buff.owner),
                                       client,
                                       event.result.showEffects,
                                       collision.tileX * 32 + 16,
                                       collision.tileY * 32 + 16
                                    );
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      );
   }

   public boolean isValidObject(GameObject object) {
      return object.isRock;
   }

   public static class MinersProstheticAttacker implements Attacker {
      private final Mob owner;

      public MinersProstheticAttacker(Mob owner) {
         this.owner = owner;
      }

      @Override
      public GameMessage getAttackerName() {
         return this.owner.getAttackerName();
      }

      @Override
      public DeathMessageTable getDeathMessages() {
         return this.owner.getDeathMessages();
      }

      @Override
      public Mob getFirstAttackOwner() {
         return this.owner;
      }
   }
}
