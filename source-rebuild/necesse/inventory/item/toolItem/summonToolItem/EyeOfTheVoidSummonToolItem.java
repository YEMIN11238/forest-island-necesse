package necesse.inventory.item.toolItem.summonToolItem;

import java.awt.geom.Point2D.Float;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.ToolItemSummonedMob;
import necesse.entity.projectile.EyeOfTheVoidSpawnProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;

public class EyeOfTheVoidSummonToolItem extends SummonToolItem {
   protected int spawnRange = 480;

   public EyeOfTheVoidSummonToolItem() {
      super("wanderbotfollowingmob", FollowPosition.CIRCLE_FAR, 1.0F, 2000, null);
      this.rarity = Item.Rarity.UNIQUE;
      this.attackAnimTime.setBaseValue(400);
      this.attackCooldownTime.setBaseValue(1000);
      this.attackDamage.setBaseValue(20.0F).setUpgradedValue(1.0F, 23.33334F);
      this.knockback.setBaseValue(0);
      this.attackXOffset = 12;
      this.attackYOffset = 12;
      this.canBeUsedForRaids = false;
   }

   @Override
   public GameSprite getAttackSprite(InventoryItem item, PlayerMob player) {
      return null;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "eyeofthevoidtip"));
      return tooltips;
   }

   @Override
   public void showAttack(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, int animAttack, int seed, GNDItemMap mapContent
   ) {
      if (level.isClient()) {
         SoundManager.playSound(
            GameResources.magicbolt4, SoundEffect.effect(attackerMob).volume(0.3F).pitch(GameRandom.globalRandom.getFloatBetween(1.6F, 1.8F))
         );
      }
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
      if (attackerMob.getDistance(x, y) > this.spawnRange) {
         Float normalized = GameMath.normalize(x - attackerMob.x, y - attackerMob.y);
         int newX = (int)(attackerMob.x + normalized.x * this.spawnRange);
         int newY = (int)(attackerMob.y + normalized.y * this.spawnRange);
         return super.onAttack(level, newX, newY, attackerMob, attackHeight, item, slot, animAttack, seed, mapContent);
      } else {
         return super.onAttack(level, x, y, attackerMob, attackHeight, item, slot, animAttack, seed, mapContent);
      }
   }

   @Override
   public void summonServerMob(ItemAttackerMob attackerMob, ToolItemSummonedMob mob, int x, int y, int attackHeight, InventoryItem item) {
      float actualDist = attackerMob.getDistance(x, y);
      EyeOfTheVoidSpawnProjectile projectile = new EyeOfTheVoidSpawnProjectile(
         attackerMob.getLevel(), attackerMob, x, y, actualDist > this.spawnRange ? this.spawnRange : (int)actualDist, null, 0, this, item
      );
      attackerMob.getLevel().entityManager.projectiles.add(projectile);
   }

   @Override
   public GameSprite getWorldItemSprite(InventoryItem item, PlayerMob perspective) {
      return this.getItemSprite(item, perspective);
   }
}
