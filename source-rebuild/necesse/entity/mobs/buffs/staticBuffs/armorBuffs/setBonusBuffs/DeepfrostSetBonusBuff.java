package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.awt.Point;
import java.util.ArrayList;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.localization.Localization;
import necesse.engine.network.client.Client;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketDeepFrostAimUpdate;
import necesse.engine.network.packet.PacketMobAttack;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ProjectileRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffManager;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob.GhostlyBowFollowingMob;
import necesse.entity.projectile.Projectile;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.arrowItem.ArrowItem;
import necesse.inventory.item.toolItem.ToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.AscendedBowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.BowProjectileToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.GunProjectileToolItem;
import necesse.level.maps.Level;

public class DeepfrostSetBonusBuff extends SetBonusBuff {
   public static ArrayList<Class<? extends ToolItem>> validRangeWeaponClasses = new ArrayList<>();
   private final int attackRange;
   private final String summonType;

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
   }

   public DeepfrostSetBonusBuff() {
      validRangeWeaponClasses.add(BowProjectileToolItem.class);
      validRangeWeaponClasses.add(GunProjectileToolItem.class);
      validRangeWeaponClasses.add(AscendedBowProjectileToolItem.class);
      this.attackRange = 480;
      this.summonType = "ghostlybow";
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      if (buff.owner.isClient()) {
         if (buff.owner.isPlayer) {
            PlayerMob player = (PlayerMob)buff.owner;
            Client client = player.getClient();
            if (client.getSlot() == player.getUniqueID()) {
               GameCamera camera = GlobalData.getCurrentState().getCamera();
               if (camera == null) {
                  return;
               }

               int newMouseLevelPosX = camera.getMouseLevelPosX();
               int newMouseLevelPosY = camera.getMouseLevelPosY();
               Point oldMouseLevelPos = getMousePos(buff);
               if (newMouseLevelPosX != oldMouseLevelPos.x || newMouseLevelPosY != oldMouseLevelPos.y) {
                  updateMousePos(buff, newMouseLevelPosX, newMouseLevelPosY);
                  client.network.sendPacket(new PacketDeepFrostAimUpdate(player, newMouseLevelPosX, newMouseLevelPosY));
               }
            }
         }
      }
   }

   @Override
   public void serverTick(ActiveBuff buff) {
      super.serverTick(buff);
      if (buff.owner.isItemAttacker) {
         ItemAttackerMob attackerMob = (ItemAttackerMob)buff.owner;
         float count = attackerMob.serverFollowersManager.getFollowerCount("ghostlybow");
         if (count <= 0.0F) {
            Level level = buff.owner.getLevel();
            Mob mob = MobRegistry.getMob("ghostlybow", level);
            attackerMob.serverFollowersManager.addFollower("ghostlybow", mob, FollowPosition.WALK_CLOSE, "summonedghostlybow", 1.0F, 1, null, false);
            Point spawnPoint = new Point(
               attackerMob.getX() + GameRandom.globalRandom.getIntBetween(-64, 64), attackerMob.getY() + GameRandom.globalRandom.getIntBetween(-64, 64)
            );
            level.entityManager.addMob(mob, spawnPoint.x, spawnPoint.y);
            buff.getGndData().setInt("ghostlybowUniqueID", mob.getUniqueID());
         }
      }
   }

   public static void updateMousePos(ActiveBuff activeBuff, int levelX, int levelY) {
      GNDItemMap gndData = activeBuff.getGndData();
      gndData.setInt("mouseLevelX", levelX);
      gndData.setInt("mouseLevelY", levelY);
   }

   public static Point getMousePos(ActiveBuff activeBuff) {
      GNDItemMap gndData = activeBuff.getGndData();
      return new Point(gndData.getInt("mouseLevelX"), gndData.getInt("mouseLevelY"));
   }

   @Override
   public void onItemAttacked(
      ActiveBuff buff,
      int targetX,
      int targetY,
      ItemAttackerMob attackerMob,
      int attackHeight,
      InventoryItem item,
      ItemAttackSlot slot,
      int animAttack,
      GNDItemMap attackMap
   ) {
      super.onItemAttacked(buff, targetX, targetY, attackerMob, attackHeight, item, slot, animAttack, attackMap);
      if (!attackerMob.isClient()) {
         if (this.isRangedItem(item)) {
            int ghostlyBowUniqueID = buff.getGndData().getInt("ghostlybowUniqueID");
            if (ghostlyBowUniqueID != 0) {
               Mob followingMob = attackerMob.getLevel().entityManager.mobs.get(ghostlyBowUniqueID, false);
               if (followingMob != null) {
                  GhostlyBowFollowingMob wisp = (GhostlyBowFollowingMob)followingMob;
                  GameDamage attackDamage;
                  if (item.item.isToolItem()) {
                     attackDamage = ((ToolItem)item.item).getAttackDamage(item).modFinalMultiplier(0.4F);
                     int arrowID = attackMap.getShortUnsigned("arrowID", 65535);
                     if (arrowID != 65535) {
                        Item arrow = ItemRegistry.getItem(arrowID);
                        if (arrow != null && arrow.type == Item.Type.ARROW) {
                           attackDamage = ((ArrowItem)arrow).modDamage(attackDamage);
                        }
                     }
                  } else {
                     attackDamage = new GameDamage(DamageTypeRegistry.RANGED, 25.0F);
                     GameLog.warn.println(item.item.getStringID() + " is not a toolitem. Setting default damage to 25");
                  }

                  this.spawnGhostlyArrow(wisp, targetX, targetY, null, attackDamage);
               }
            }
         }
      }
   }

   private void spawnGhostlyArrow(GhostlyBowFollowingMob wisp, int targetX, int targetY, Mob target, GameDamage damage) {
      Projectile projectile = ProjectileRegistry.getProjectile(
         "ghostarrow", wisp.getLevel(), wisp.x, wisp.y + (int)wisp.getDesiredHeight() + 20.0F, targetX, targetY, 200.0F, 608, damage, wisp
      );
      if (target != null) {
         projectile.setTargetPrediction(target, -20.0F);
      }

      projectile.moveDist(20.0);
      wisp.getLevel().entityManager.projectiles.add(projectile);
      wisp.showAttack(targetX, targetY, true);
      if (wisp.isServer()) {
         wisp.getServer().network.sendToClientsWithEntity(new PacketMobAttack(wisp, targetX, targetY, true), wisp);
      }
   }

   private boolean isRangedItem(InventoryItem item) {
      if (item != null && item.item instanceof ToolItem) {
         return ((ToolItem)item.item).getDamageType(item) != DamageTypeRegistry.RANGED
            ? false
            : validRangeWeaponClasses.stream().anyMatch(clazz -> clazz.isAssignableFrom(item.item.getClass()));
      } else {
         return false;
      }
   }

   @Override
   public void onRemoved(ActiveBuff buff) {
      super.onRemoved(buff);
      BuffManager buffManager = buff.owner.buffManager;
      if (buff.owner.isServer() && buffManager.hasBuff("summonedghostlybow")) {
         buffManager.removeBuff("summonedghostlybow", true);
      }
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "deepfrostset"), 400);
      return tooltips;
   }
}
