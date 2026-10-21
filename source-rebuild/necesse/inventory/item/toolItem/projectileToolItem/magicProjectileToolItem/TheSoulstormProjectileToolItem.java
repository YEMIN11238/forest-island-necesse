package necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.LinkedList;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.LineHitbox;
import necesse.engine.util.Ray;
import necesse.engine.util.RayLinkedList;
import necesse.entity.levelEvent.WaitForSecondsEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.SingleSpiritBeamLevelEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffManager;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemControllerInteract;
import necesse.inventory.item.ItemInteractAction;
import necesse.inventory.lootTable.presets.MagicWeaponsLootTable;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;

public class TheSoulstormProjectileToolItem extends MagicProjectileToolItem implements ItemInteractAction {
   public TheSoulstormProjectileToolItem() {
      super(1575, MagicWeaponsLootTable.magicWeapons);
      this.rarity = Item.Rarity.EPIC;
      this.attackAnimTime.setBaseValue(450);
      this.attackDamage.setBaseValue(133.0F).setUpgradedValue(1.0F, 155.40004F);
      this.knockback.setBaseValue(10);
      this.attackRange.setBaseValue(500);
      this.attackXOffset = 18;
      this.attackYOffset = 20;
      this.manaCost.setBaseValue(5.0F).setUpgradedValue(1.0F, 5.0F);
      this.itemAttackerProjectileCanHitWidth = 5.0F;
      this.canBeUsedForRaids = false;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "thesoulstormtip"), 400);
      tooltips.add(Localization.translate("itemtooltip", "thesoulstormsecondarytip"), 400);
      return tooltips;
   }

   @Override
   public InventoryItem onAttack(
      Level level,
      int x,
      int y,
      ItemAttackerMob attackerMob,
      int attackHeight,
      InventoryItem item,
      ItemAttackSlot slot,
      int animAttack,
      int seed,
      GNDItemMap mapContent
   ) {
      if (!attackerMob.isPlayer
         && attackerMob.buffManager.hasBuff(BuffRegistry.SOULSTORM_SOULS)
         && attackerMob.buffManager.getBuff(BuffRegistry.SOULSTORM_SOULS).getStacks() >= 5) {
         this.onLevelInteract(level, x, y, attackerMob, attackHeight, item, slot, seed, mapContent);
      } else {
         Point targetPoints = new Point(x, y);
         Float normalizedVector = GameMath.normalize(targetPoints.x - attackerMob.x, targetPoints.y - attackerMob.y);
         RayLinkedList<LevelObjectHit> hits = GameUtils.castRay(
            level,
            attackerMob.x,
            attackerMob.y,
            normalizedVector.x,
            normalizedVector.y,
            targetPoints.distance(attackerMob.x, attackerMob.y),
            0,
            new CollisionFilter().projectileCollision().addFilter(tp -> tp.object().object.isWall || tp.object().object.isRock)
         );
         if (!hits.isEmpty()) {
            Ray<LevelObjectHit> first = hits.getLast();
            targetPoints.x = (int)first.x2;
            targetPoints.y = (int)first.y2;
         }

         this.consumeMana(attackerMob, item);
         SingleSpiritBeamLevelEvent event = new SingleSpiritBeamLevelEvent(
            attackerMob, new GameRandom(seed), targetPoints, this.getAttackDamage(item), 0.25F, true
         );
         attackerMob.addAndSendAttackerLevelEvent(event);
      }

      return item;
   }

   @Override
   public boolean canLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item) {
      return attackerMob.buffManager.hasBuff(BuffRegistry.SOULSTORM_SOULS);
   }

   @Override
   public InventoryItem onLevelInteract(
      Level level,
      final int x,
      final int y,
      final ItemAttackerMob attackerMob,
      int attackHeight,
      final InventoryItem item,
      ItemAttackSlot slot,
      int seed,
      GNDItemMap mapContent
   ) {
      BuffManager buffManager = attackerMob.buffManager;
      GameRandom random = new GameRandom(seed);
      if (buffManager.hasBuff(BuffRegistry.SOULSTORM_SOULS)) {
         this.consumeMana(attackerMob, item);
         int soulStacks = buffManager.getBuff(BuffRegistry.SOULSTORM_SOULS).getStacks();
         buffManager.removeBuff(BuffRegistry.SOULSTORM_SOULS, level.isServer());

         for (int i = 0; i < soulStacks; i++) {
            final GameRandom eventRandom = random.nextSeeded();
            level.entityManager
               .events
               .addHidden(
                  new WaitForSecondsEvent(i * 0.25F) {
                     @Override
                     public void onWaitOver() {
                        Point targetPoints = new Point(x, y);
                        int rndX = eventRandom.getIntBetween(-50, 50);
                        int rndY = eventRandom.getIntBetween(-50, 50);
                        targetPoints.x += rndX;
                        targetPoints.y += rndY;
                        RayLinkedList<LevelObjectHit> hits = GameUtils.castRay(
                           this.level,
                           attackerMob.x,
                           attackerMob.y,
                           targetPoints.x - attackerMob.x,
                           targetPoints.y - attackerMob.y,
                           targetPoints.distance(attackerMob.x, attackerMob.y),
                           0,
                           new CollisionFilter().projectileCollision().addFilter(tp -> tp.object().object.isWall || tp.object().object.isRock)
                        );
                        if (!hits.isEmpty()) {
                           Ray<LevelObjectHit> first = hits.getLast();
                           targetPoints.x = (int)first.x2;
                           targetPoints.y = (int)first.y2;
                        }

                        SingleSpiritBeamLevelEvent event = new SingleSpiritBeamLevelEvent(
                           attackerMob, eventRandom, targetPoints, TheSoulstormProjectileToolItem.this.getAttackDamage(item), 0.25F, false
                        );
                        attackerMob.addAndSendAttackerLevelEvent(event);
                     }
                  }
               );
         }
      }

      return item;
   }

   @Override
   public boolean canItemAttackerHitTarget(ItemAttackerMob attackerMob, float fromX, float fromY, Mob target, InventoryItem item) {
      float distance = attackerMob.getDistance(target);
      int attackRange = this.getAttackRange(item);
      if (distance < attackRange) {
         return super.canItemAttackerHitTarget(attackerMob, fromX, fromY, target, item);
      } else {
         return distance < attackRange * 5
            ? !attackerMob.getLevel()
               .collides(
                  new LineHitbox(fromX, fromY, target.x, target.y, 45.0F),
                  attackerMob.modifyChasingCollisionFilter(new CollisionFilter().projectileCollision(), target)
               )
            : false;
      }
   }

   @Override
   public ItemControllerInteract getControllerInteract(
      Level level,
      PlayerMob player,
      InventoryItem item,
      boolean beforeObjectInteract,
      int interactDir,
      LinkedList<Rectangle> mobInteractBoxes,
      LinkedList<Rectangle> tileInteractBoxes
   ) {
      Float controllerAimDir = player.getControllerAimDir();
      Point levelPos = this.getControllerAttackLevelPos(level, controllerAimDir.x, controllerAimDir.y, player, item);
      return new ItemControllerInteract(levelPos.x, levelPos.y) {
         @Override
         public DrawOptions getDrawOptions(GameCamera camera) {
            return null;
         }

         @Override
         public void onCurrentlyFocused(GameCamera camera) {
         }
      };
   }

   @Override
   public Point getControllerAttackLevelPos(Level level, float aimDirX, float aimDirY, PlayerMob player, InventoryItem item) {
      float range = 500.0F;
      return new Point((int)(player.x + aimDirX * range), (int)(player.y + aimDirY * range));
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.magicSwing).volume(0.2F);
   }
}
