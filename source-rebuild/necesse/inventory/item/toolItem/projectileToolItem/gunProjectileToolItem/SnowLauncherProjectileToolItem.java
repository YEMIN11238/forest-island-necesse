package necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem;

import necesse.engine.GameLog;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.ProjectileRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.itemAttacker.AmmoUserMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.Projectile;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemStatTipList;
import necesse.level.maps.Level;

public class SnowLauncherProjectileToolItem extends GunProjectileToolItem {
   public SnowLauncherProjectileToolItem() {
      super("snowball", 350, null);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackAnimTime.setBaseValue(100);
      this.attackXOffset = 23;
      this.attackYOffset = 13;
      this.attackRange.setBaseValue(800);
      this.velocity.setBaseValue(400);
      this.ammoConsumeChance = 0.5F;
      this.addGlobalIngredient("bulletuser");
      this.moveDist = 55;
      this.canBeUsedForRaids = false;
   }

   @Override
   protected void addAmmoTooltips(ListGameTooltips tooltips, InventoryItem item) {
      tooltips.add(Localization.translate("itemtooltip", "snowlaunchertip1"));
      tooltips.add(Localization.translate("itemtooltip", "snowlaunchertip2"));
   }

   @Override
   public void addAttackDamageTip(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem, Attacker attacker, boolean forceAdd) {
   }

   @Override
   public void addCritChanceTip(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem, Attacker attacker, boolean forceAdd) {
   }

   @Override
   public void addKnockbackTip(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem, Attacker attacker) {
   }

   @Override
   public void addResilienceGainTip(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem, Mob mob, boolean forceAdd) {
   }

   @Override
   public void addAttackRangeTip(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem) {
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
      int bulletID = mapContent.getShortUnsigned("bulletID", 65535);
      if (bulletID != 65535) {
         Item bullet = ItemRegistry.getItem(bulletID);
         if (bullet != null) {
            GameRandom random = new GameRandom(seed + 5);
            float ammoConsumeChance = this.getAmmoConsumeChance(attackerMob, item);
            boolean consumeAmmo = ammoConsumeChance >= 1.0F || ammoConsumeChance > 0.0F && random.getChance(ammoConsumeChance);
            boolean shouldFire;
            if (!consumeAmmo) {
               shouldFire = true;
            } else if (attackerMob instanceof AmmoUserMob) {
               shouldFire = ((AmmoUserMob)attackerMob).removeAmmo(bullet, 1, "bulletammo").amount >= 1;
            } else {
               shouldFire = true;
            }

            if (shouldFire) {
               Projectile projectile = ProjectileRegistry.getProjectile(
                  "playersnowball",
                  level,
                  attackerMob.x,
                  attackerMob.y,
                  x,
                  y,
                  this.getProjectileVelocity(item, attackerMob),
                  this.getAttackRange(item),
                  this.getAttackDamage(item),
                  this.getKnockback(item, attackerMob),
                  attackerMob
               );
               projectile.resetUniqueID(new GameRandom(seed));
               attackerMob.addAndSendAttackerProjectile(projectile, 55, random.getIntBetween(-5, 5));
            }
         } else {
            GameLog.warn.println(attackerMob.getDisplayName() + " tried to use item " + (bullet == null ? bulletID : bullet.getStringID()) + " as bullet.");
         }
      }

      return item;
   }

   @Override
   public float getItemAttackerWeaponValueFlat(InventoryItem item) {
      return 0.0F;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.flick);
   }
}
