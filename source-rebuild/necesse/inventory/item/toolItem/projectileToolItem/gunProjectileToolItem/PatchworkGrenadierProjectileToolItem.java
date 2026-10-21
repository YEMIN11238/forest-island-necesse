package necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.PatchworkGrenadeProjectile;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.bulletItem.BulletItem;
import necesse.inventory.lootTable.presets.IncursionGunWeaponsLootTable;
import necesse.level.maps.Level;

public class PatchworkGrenadierProjectileToolItem extends GunProjectileToolItem {
   public PatchworkGrenadierProjectileToolItem() {
      super(NORMAL_AMMO_TYPES, 2000, IncursionGunWeaponsLootTable.incursionGunWeapons);
      this.rarity = Item.Rarity.EPIC;
      this.attackAnimTime.setBaseValue(500);
      this.attackXOffset = 23;
      this.attackYOffset = 23;
      this.attackDamage.setBaseValue(130.0F).setUpgradedValue(1.0F, 151.6667F);
      this.attackRange.setBaseValue(800);
      this.velocity.setBaseValue(150);
      this.addGlobalIngredient("bulletuser");
      this.canBeUsedForRaids = false;
   }

   @Override
   protected void addAmmoTooltips(ListGameTooltips tooltips, InventoryItem item) {
      tooltips.add(Localization.translate("itemtooltip", "patchworkgrenadiertip"), 400);
   }

   @Override
   public boolean shouldRunOnAttackedBuffEvent(
      Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item, ItemAttackSlot slot, int animAttack, int seed, GNDItemMap mapContent
   ) {
      return false;
   }

   @Override
   protected void fireProjectiles(
      Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item, int seed, BulletItem bullet, boolean dropItem, GNDItemMap mapContent
   ) {
      int distance = GameMath.min((int)attackerMob.getDistance(x, y), 800);
      GameRandom random = new GameRandom(seed);
      GameRandom spreadRandom = new GameRandom(seed + 10);
      Projectile projectile = new PatchworkGrenadeProjectile(
         level,
         attackerMob,
         attackerMob.x,
         attackerMob.y,
         x,
         y,
         this.velocity.getValue(this.getUpgradeTier(item)).intValue(),
         distance,
         this.getAttackDamage(item)
      );
      projectile.setModifier(new ResilienceOnHitProjectileModifier(this.getResilienceGain(item)));
      projectile.getUniqueID(random);
      attackerMob.addAndSendAttackerProjectile(projectile, this.moveDist, spreadRandom.getFloatOffset(0.0F, 3.0F));
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      super.setDrawAttackRotation(item, drawOptions, attackDirX, attackDirY, attackProgress);
      drawOptions.forEachItemSprite(i -> i.itemRotateOffset(Math.abs(attackDirX) * -40.0F));
   }

   @Override
   protected void playAttackSound(Mob source) {
      SoundManager.playSound(GameResources.pop, SoundEffect.effect(source).volume(1.0F).pitch(0.25F));
   }
}
