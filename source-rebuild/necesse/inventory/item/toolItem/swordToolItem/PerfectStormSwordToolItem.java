package necesse.inventory.item.toolItem.swordToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MobManaChangeEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ToolItemMobAbilityEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.WeaponChargeSmiteLevelEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.itemAttack.HumanAttackDrawOptions;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemInteractAction;
import necesse.inventory.lootTable.presets.IncursionCloseRangeWeaponsLootTable;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class PerfectStormSwordToolItem extends SwordToolItem implements ItemInteractAction {
   protected float abilityCooldown = 30.0F;

   public PerfectStormSwordToolItem() {
      super(2000, IncursionCloseRangeWeaponsLootTable.incursionCloseRangeWeapons);
      this.rarity = Item.Rarity.EPIC;
      this.attackAnimTime.setBaseValue(350);
      this.attackDamage.setBaseValue(75.0F).setUpgradedValue(1.0F, 87.50002F);
      this.attackRange.setBaseValue(50);
      this.knockback.setBaseValue(75);
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
      this.attackXOffset = 16;
      this.attackYOffset = 16;
      this.changeDir = true;
      this.raidTicketsModifier = 0.25F;
   }

   @Override
   public int getFlatAttackRange(InventoryItem item) {
      return 85;
   }

   @Override
   public GameSprite getItemSprite(InventoryItem item, PlayerMob perspective) {
      return perspective != null && perspective.buffManager.hasBuff(BuffRegistry.PERFECT_STORM)
         ? new GameSprite(this.itemTexture, 1, 0, this.itemTexture.getHeight())
         : new GameSprite(this.itemTexture, 0, 0, this.itemTexture.getHeight());
   }

   @Override
   public GameSprite getAttackSprite(InventoryItem item, PlayerMob player) {
      return new GameSprite(this.attackTexture, 0, 0, this.attackTexture.getHeight());
   }

   public ItemAttackDrawOptions getLightningDrawOptions(
      InventoryItem item, PlayerMob player, int mobDir, float attackDirX, float attackDirY, float attackProgress
   ) {
      int spriteRes = this.attackTexture.getHeight();
      int timePerFrame = 300;
      int spriteCount = 3;
      int sprite = GameUtils.getAnim(player == null ? System.currentTimeMillis() : player.getLocalTime(), spriteCount, spriteCount * timePerFrame) + 1;
      GameSprite lightningSprite = new GameSprite(this.attackTexture, sprite, 0, spriteRes);
      ItemAttackDrawOptions options = ItemAttackDrawOptions.start(mobDir);
      ItemAttackDrawOptions.AttackItemSprite itemSprite = options.itemSprite(lightningSprite);
      itemSprite.itemRotatePoint(this.attackXOffset, this.attackYOffset);
      if (!this.animDrawBehindHand(item)) {
         options.itemAfterHand();
      }

      this.setDrawAttackRotation(item, options, attackDirX, attackDirY, attackProgress);
      return options;
   }

   @Override
   public HumanAttackDrawOptions getAttackDrawOptions(
      InventoryItem item,
      Level level,
      final PlayerMob player,
      InventoryItem headItem,
      InventoryItem chestItem,
      InventoryItem feetItem,
      int mobDir,
      float attackDirX,
      float attackDirY,
      float attackProgress,
      GameSprite armSprite
   ) {
      final HumanAttackDrawOptions attackDrawOptions = super.getAttackDrawOptions(
         item, level, player, headItem, chestItem, feetItem, mobDir, attackDirX, attackDirY, attackProgress, armSprite
      );
      final HumanAttackDrawOptions lightningDrawOptions = this.getLightningDrawOptions(item, player, mobDir, attackDirX, attackDirY, attackProgress);
      return new HumanAttackDrawOptions() {
         @Override
         public HumanAttackDrawOptions setOffsets(
            int centerX,
            int centerY,
            int armPosX,
            int armPosY,
            float armRotationOffset,
            int armRotateX,
            int armRotateY,
            int armLength,
            int armCenterHeight,
            int itemYOffset
         ) {
            attackDrawOptions.setOffsets(centerX, centerY, armPosX, armPosY, armRotationOffset, armRotateX, armRotateY, armLength, armCenterHeight, itemYOffset);
            lightningDrawOptions.setOffsets(
               centerX, centerY, armPosX, armPosY, armRotationOffset, armRotateX, armRotateY, armLength, armCenterHeight, itemYOffset
            );
            return this;
         }

         @Override
         public HumanAttackDrawOptions light(Level level, boolean hasGlowEffect, int glowHash, GameLight light) {
            attackDrawOptions.light(level, hasGlowEffect, glowHash, light);
            lightningDrawOptions.light(level, hasGlowEffect, glowHash, light.minLevelCopy(150.0F));
            return this;
         }

         @Override
         public HumanAttackDrawOptions skinColor(int skinIndex) {
            attackDrawOptions.skinColor(skinIndex);
            return this;
         }

         @Override
         public DrawOptions pos(int drawX, int drawY) {
            DrawOptions options1 = attackDrawOptions.pos(drawX, drawY);
            DrawOptions options2 = lightningDrawOptions.pos(drawX, drawY);
            return () -> {
               options1.draw();
               if (player != null && player.buffManager.hasBuff(BuffRegistry.PERFECT_STORM)) {
                  options2.draw();
               }
            };
         }
      };
   }

   @Override
   public boolean canLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item) {
      return !attackerMob.buffManager.hasBuff(BuffRegistry.Debuffs.PERFECT_STORM_ABILITY_COOLDOWN);
   }

   @Override
   public InventoryItem onLevelInteract(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, ItemAttackSlot slot, int seed, GNDItemMap mapContent
   ) {
      float fortyPercentMissingMana = attackerMob.getMissingMana() * 0.4F;
      if (!level.isClient() && fortyPercentMissingMana >= 1.0F) {
         LevelEvent event = new MobManaChangeEvent(attackerMob, fortyPercentMissingMana);
         level.entityManager.addLevelEvent(event);
      }

      float maxmana = attackerMob.getMaxMana();
      float percManaRestored = fortyPercentMissingMana / maxmana;
      float restorationFactor = percManaRestored / 0.4F;
      float cooldownScale = GameMath.limit(restorationFactor, 0.0F, 1.0F);
      attackerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.PERFECT_STORM, attackerMob, 5.0F + 15.0F * cooldownScale, null), true);
      attackerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.PERFECT_STORM_ABILITY_COOLDOWN, attackerMob, this.abilityCooldown, null), true);
      item.getGndData().setBoolean("attackIsRightClick", true);
      return ItemInteractAction.super.onLevelInteract(level, x, y, attackerMob, attackHeight, item, slot, seed, mapContent);
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      if (item.getGndData().getBoolean("attackIsRightClick")) {
         drawOptions.rotation(-35.0F);
      } else {
         super.setDrawAttackRotation(item, drawOptions, attackDirX, attackDirY, attackProgress);
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
      if (item.getGndData().getBoolean("attackIsRightClick")) {
         item.getGndData().setBoolean("attackIsRightClick", false);
      }

      return super.onAttack(level, x, y, attackerMob, attackHeight, item, slot, animAttack, seed, mapContent);
   }

   @Override
   public boolean showAttackAllDirections(ItemAttackerMob attackerMob, InventoryItem item) {
      return !item.getGndData().getBoolean("attackIsRightClick");
   }

   @Override
   public void showLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, int seed, GNDItemMap mapContent) {
      if (attackerMob.isClient()) {
         level.getClient().startCameraShake(x, y, 400, 40, 3.0F, 3.0F, true);
         SoundManager.playSound(GameResources.electricExplosion, SoundEffect.effect(attackerMob).volume(1.0F));
      }

      if (!attackerMob.isClient()) {
         int smites = 7;
         float radius = 300.0F;

         for (int i = 0; i < smites; i++) {
            double angle = (Math.PI * 2) * i / smites;
            float cX = attackerMob.x + (float)(radius * Math.cos(angle));
            float cY = attackerMob.y + (float)(radius * Math.sin(angle));
            LevelEvent smiteEvent = new WeaponChargeSmiteLevelEvent(attackerMob, new GameRandom(attackerMob.getUniqueID()), cX, cY);
            attackerMob.getLevel().entityManager.addLevelEvent(smiteEvent);
         }
      }
   }

   @Override
   public Attacker getToolItemEventAttacker(InventoryItem item, ToolItemMobAbilityEvent event, Level level, Mob target, Mob attacker) {
      return new PerfectStormSwordToolItem.PerfectStormSwordAttacker(attacker);
   }

   @Override
   protected SoundSettings getSwingSound() {
      return new SoundSettings(GameResources.lightningHammer).pitchVariance(0.2F).volume(0.6F);
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "perfectstormtip"), 400);
      return tooltips;
   }

   public static class PerfectStormSwordAttacker implements Attacker {
      protected Mob attackerMob;

      public PerfectStormSwordAttacker(Mob attackerMob) {
         this.attackerMob = attackerMob;
      }

      @Override
      public GameMessage getAttackerName() {
         return this.attackerMob.getAttackerName();
      }

      @Override
      public DeathMessageTable getDeathMessages() {
         return this.attackerMob.getDeathMessages();
      }

      @Override
      public Mob getFirstAttackOwner() {
         return this.attackerMob.getFirstAttackOwner();
      }
   }
}
