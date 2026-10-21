package necesse.inventory.item.toolItem.swordToolItem;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.SwordCleanSliceAttackMobEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MobHealthChangeEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ToolItemMobAbilityEvent;
import necesse.entity.mobs.AttackAnimMob;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.attackHandler.VoidClawDashAttackHandler;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.trails.Trail;
import necesse.entity.trails.TrailVector;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.itemAttack.HumanAttackDrawOptions;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class VoidClawSwordToolItem extends KatanaToolItem {
   public GameTexture clawsTexture;
   public GameTexture clawsTextureInverted;

   public VoidClawSwordToolItem() {
      super(2000, null);
      this.rarity = Item.Rarity.UNIQUE;
      this.attackAnimTime.setBaseValue(120);
      this.attackDamage.setBaseValue(160.0F).setUpgradedValue(1.0F, 186.66672F);
      this.attackRange.setBaseValue(150);
      this.knockback.setBaseValue(75);
      this.canBeUsedForRaids = false;
      this.resilienceGain.setBaseValue(0.3F).setUpgradedValue(1.0F, 0.3F).setUpgradedValue(10.0F, 0.5F);
      this.attackXOffset = 4;
      this.attackYOffset = 4;
      this.dashRange.setBaseValue(500);
   }

   @Override
   protected void loadAttackTexture() {
      super.loadAttackTexture();

      try {
         this.clawsTexture = GameTexture.fromFileRaw("player/weapons/voidclaw_front");
         this.clawsTextureInverted = GameTexture.fromFileRaw("player/weapons/voidclaw_inverted_front");
      } catch (FileNotFoundException e) {
         this.clawsTexture = null;
         this.clawsTextureInverted = null;
      }
   }

   @Override
   public ListGameTooltips getKatanaTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(Localization.translate("itemtooltip", "voidclawprimarytip"), 400);
      tooltips.add(Localization.translate("itemtooltip", "voidclawsecondarytip"), 400);
      return tooltips;
   }

   @Override
   public boolean canDash(ItemAttackerMob attackerMob) {
      return attackerMob.isPlayer ? !attackerMob.isRiding() : attackerMob.getHealthPercent() > 0.7F && !attackerMob.isRiding();
   }

   @Override
   public boolean animDrawBehindHand(InventoryItem item) {
      return false;
   }

   @Override
   public float getSecondSliceAttackCooldownModifier() {
      return 1.5F;
   }

   @Override
   public void hitMob(InventoryItem item, ToolItemMobAbilityEvent event, Level level, Mob target, Mob attacker) {
      super.hitMob(item, event, level, target, attacker);
      restoreHealthOnHit(null, level, null, attacker);
   }

   public static void restoreHealthOnHit(InventoryItem item, Level level, Mob target, Mob attacker) {
      if (attacker != null && !attacker.isClient()) {
         float healthToAdd = (attacker.getMaxHealth() - attacker.getHealth()) * 0.03F;
         LevelEvent healthEvent = new MobHealthChangeEvent(attacker, GameMath.max(1, (int)healthToAdd));
         level.entityManager.events.add(healthEvent);
      }
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      if (item.getGndData().getBoolean("chargeUp")) {
         float chargePercent = Math.min(item.getGndData().getFloat("chargePercent"), 1.0F);
         if (drawOptions.dir == 2) {
            drawOptions.rotation(GameMath.lerp(chargePercent, -20.0F, 0.0F));
         } else {
            drawOptions.rotation(GameMath.lerp(chargePercent, 80.0F, 100.0F));
         }
      } else if (item.getGndData().getBoolean("sliceDash")) {
         if (drawOptions.dir == 2) {
            drawOptions.rotation(GameMath.lerp(attackProgress, 0.0F, -170.0F));
         } else {
            drawOptions.rotation(GameMath.lerp(attackProgress, 70.0F, -100.0F));
         }
      } else if (item.getGndData().getBoolean("slash")) {
         drawOptions.rotation(this.getSwingRotation(item, drawOptions.dir, attackProgress) - 60.0F - drawOptions.dir * 10);
      } else {
         drawOptions.rotation(this.getSwingRotation(item, drawOptions.dir, attackProgress) - 10.0F - drawOptions.dir * 10);
      }
   }

   @Override
   public ItemAttackDrawOptions setupItemSpriteAttackDrawOptions(
      ItemAttackDrawOptions options,
      InventoryItem item,
      PlayerMob player,
      int mobDir,
      float attackDirX,
      float attackDirY,
      float attackProgress,
      Color itemColor
   ) {
      ItemAttackDrawOptions.AttackItemSprite itemSprite = options.itemSprite(this.getAttackSprite(item, player));
      return this.applySpriteOffsets(options, item, itemSprite);
   }

   protected ItemAttackDrawOptions applySpriteOffsets(ItemAttackDrawOptions options, InventoryItem item, ItemAttackDrawOptions.AttackItemSprite itemSprite) {
      boolean chargeUp = item.getGndData().getBoolean("chargeUp");
      boolean sliceDash = item.getGndData().getBoolean("sliceDash");
      if (chargeUp || sliceDash) {
         if (options.dir == 0) {
            itemSprite.itemRotatePoint(12, 12);
         } else if (options.dir == 2) {
            itemSprite.itemRotatePoint(14, 14);
         } else {
            itemSprite.itemRotatePoint(14, 14);
         }
      }

      if (chargeUp) {
         float chargePercent = Math.min(item.getGndData().getFloat("chargePercent"), 1.0F);
         if (options.dir == 0) {
            itemSprite.itemRotateOffsetAdd(GameMath.lerp(chargePercent, 0.0F, 10.0F));
         } else if (options.dir == 2) {
            itemSprite.itemRotateOffsetAdd(GameMath.lerp(chargePercent, 0.0F, 70.0F));
            options.addedArmRotationOffset(GameMath.lerp(chargePercent, 0.0F, 70.0F));
         } else {
            itemSprite.itemRotateOffsetAdd(GameMath.lerp(chargePercent, 0.0F, 60.0F));
         }

         return itemSprite.itemEnd();
      } else if (sliceDash) {
         if (options.dir == 0) {
            itemSprite.itemRotateOffsetAdd(10.0F);
         } else if (options.dir == 2) {
            itemSprite.itemRotateOffsetAdd(70.0F);
            options.addedArmRotationOffset(70.0F);
         } else {
            itemSprite.itemRotateOffsetAdd(60.0F);
         }

         return itemSprite.itemEnd();
      } else {
         if (item.getGndData().getBoolean("slash")) {
            itemSprite.itemRotatePoint(14, 14);
         } else {
            options.addedArmRotationOffset(-50.0F);
            if (options.dir == 0 || options.dir == 1) {
               itemSprite.itemRotatePoint(18, 8);
               itemSprite.itemRotateOffsetAdd(-45.0F);
            } else if (options.dir == 2) {
               itemSprite.itemRotatePoint(22, 0);
               itemSprite.itemRotateOffsetAdd(20.0F);
            } else {
               itemSprite.itemRotatePoint(18, 8);
               itemSprite.itemRotateOffsetAdd(-25.0F);
            }
         }

         return itemSprite.itemEnd();
      }
   }

   @Override
   public void showKatanaAttack(Level level, final AttackAnimMob mob, final int seed, final InventoryItem item) {
      level.entityManager
         .events
         .addHidden(
            new SwordCleanSliceAttackMobEvent(mob, seed, 12) {
               Trail[] trails = null;
               final float maxTrailThickness = 25.0F;
               final float minTrailThickness = 10.0F;

               @Override
               public void tick(float angle, float currentAttackProgress) {
                  int attackRange = VoidClawSwordToolItem.this.getAttackRange(item);
                  Float base = new Float(mob.x, mob.y);
                  int attackDir = mob.getDir();
                  if (attackDir == 0) {
                     base.x += 8.0F;
                  } else if (attackDir == 2) {
                     base.x -= 8.0F;
                  }

                  int distancePerTrail = 15;
                  boolean strictTrailAngles = item.getGndData().getBoolean("sliceDash");
                  if (strictTrailAngles) {
                     attackRange -= 20;
                     angle = VoidClawSwordToolItem.this.getSwingDirection(item, mob).apply(currentAttackProgress);
                  } else {
                     angle = VoidClawSwordToolItem.this.getSwingDirection(item, mob).apply(currentAttackProgress);
                  }

                  Float dir = GameMath.getAngleDir(angle);
                  int sliceDirOffset = VoidClawSwordToolItem.this.getAnimInverted(item) ? -90 : 90;
                  if (attackDir == 3) {
                     sliceDirOffset = -sliceDirOffset;
                  }

                  Float sliceDir = GameMath.getAngleDir(angle + sliceDirOffset);
                  if (this.trails == null) {
                     int fadeTime = strictTrailAngles ? 500 : 250;
                     int trailCount = 3;
                     this.trails = new Trail[trailCount];

                     for (int i = 0; i < this.trails.length; i++) {
                        Trail trail = new Trail(
                           this.getVector(currentAttackProgress, attackRange, i, distancePerTrail, base, dir, sliceDir),
                           this.level,
                           new Color(244, 177, 255),
                           fadeTime
                        ) {
                           @Override
                           public void addDrawables(OrderableDrawables list, int startTileY, int endTileY, TickManager tickManager, GameCamera camera) {
                              super.addDrawables(list, startTileY, endTileY, tickManager, camera);
                           }

                           @Override
                           public void addDrawables(List<LevelSortedDrawable> list, int startTileY, int endTileY, TickManager tickManager, GameCamera camera) {
                              super.addDrawables(list, startTileY, endTileY, tickManager, camera);
                           }
                        };
                        this.trails[i] = trail;
                        trail.removeOnFadeOut = false;
                        trail.sprite = new GameSprite(GameResources.chains, 11, 0, 32);
                        trail.lightLevel = 100;
                        trail.lightHue = 300.0F;
                        this.level.entityManager.addTrail(trail);
                     }
                  } else {
                     for (int i = 0; i < this.trails.length; i++) {
                        if (strictTrailAngles) {
                           this.trails[i]
                              .addPointIfSameDirection(
                                 this.getVector(currentAttackProgress, attackRange, i, distancePerTrail, base, dir, sliceDir), 0.2F, 20.0F, 50.0F
                              );
                        } else {
                           this.trails[i].addPoint(this.getVector(currentAttackProgress, attackRange, i, distancePerTrail, base, dir, sliceDir));
                        }
                     }
                  }
               }

               public TrailVector getVector(
                  float currentAttackProgress, int attackRange, int index, int distancePerTrail, Float base, Float dir, Float sliceDir
               ) {
                  float thickness = GameMath.lerp((float)index / (this.trails.length - 1), 25.0F, 10.0F);
                  if (currentAttackProgress < 0.33F) {
                     thickness *= 3.0F * currentAttackProgress;
                  } else if (currentAttackProgress > 0.66F) {
                     thickness *= 3.0F * (1.0F - currentAttackProgress);
                  }

                  int distanceOffset = attackRange - index * distancePerTrail;
                  GameRandom random = new GameRandom(seed).nextSeeded(index + 5);
                  float xOffset = random.getFloatOffset(0.0F, 10.0F);
                  float yOffset = random.getFloatOffset(0.0F, 10.0F);
                  Float edgePos = new Float(base.x + dir.x * distanceOffset + xOffset, base.y + dir.y * distanceOffset + yOffset);
                  return new TrailVector(edgePos.x, edgePos.y, sliceDir.x, sliceDir.y, thickness, 0.0F);
               }

               @Override
               public void onDispose() {
                  super.onDispose();
                  if (this.trails != null) {
                     for (Trail trail : this.trails) {
                        trail.removeOnFadeOut = true;
                     }
                  }
               }
            }
         );
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
      if (!attackerMob.isPlayer && this.canDash(attackerMob)) {
         int animTime = this.getAttackAnimTime(item, attackerMob);
         mapContent.setBoolean("chargeUp", true);
         attackerMob.startAttackHandler(new VoidClawDashAttackHandler(attackerMob, slot, item, this, animTime, new Color(221, 99, 255), seed, x, y));
         return item;
      }

      boolean isSlash = item.getGndData().getBoolean("slash");
      item.getGndData().setBoolean("slash", !isSlash);
      item.getGndData().setBoolean("chargeUp", false);
      item.getGndData().setBoolean("sliceDash", false);
      if (animAttack == 0) {
         int animTime = this.getAttackAnimTime(item, attackerMob);
         ToolItemMobAbilityEvent event = new ToolItemMobAbilityEvent(
            attackerMob, seed, item, x - attackerMob.getX(), y - attackerMob.getY() + attackHeight, animTime, animTime, isSlash ? new HashMap<>() : null
         );
         attackerMob.addAndSendAttackerLevelEvent(event);
      }

      return item;
   }

   @Override
   public int getItemAttackerAttackRange(ItemAttackerMob mob, InventoryItem item) {
      return !mob.isPlayer && this.canDash(mob)
         ? (int)(this.dashRange.getValue(this.getUpgradeTier(item)).intValue() * 0.8F)
         : super.getItemAttackerAttackRange(mob, item);
   }

   @Override
   public InventoryItem onLevelInteract(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, ItemAttackSlot slot, int seed, GNDItemMap mapContent
   ) {
      int animTime = this.getAttackAnimTime(item, attackerMob);
      mapContent.setBoolean("chargeUp", true);
      attackerMob.startAttackHandler(
         new VoidClawDashAttackHandler(attackerMob, slot, item, this, animTime, new Color(215, 82, 255), seed, x, y).startFromInteract()
      );
      return item;
   }

   public ItemAttackDrawOptions getClawDrawOptions(InventoryItem item, PlayerMob player, int mobDir, float attackDirX, float attackDirY, float attackProgress) {
      ItemAttackDrawOptions options = ItemAttackDrawOptions.start(mobDir);
      ItemAttackDrawOptions.AttackItemSprite itemSprite;
      if ((this.getAnimInverted(item) || item.getGndData().getBoolean("chargeUp") || item.getGndData().getBoolean("sliceDash"))
         && this.invertedAttackTexture != null) {
         itemSprite = options.itemSprite(new GameSprite(this.clawsTextureInverted));
      } else {
         itemSprite = options.itemSprite(new GameSprite(this.clawsTexture));
      }

      options = this.applySpriteOffsets(options, item, itemSprite);
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
      PlayerMob player,
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
      final HumanAttackDrawOptions clawOverlayOptions = this.getClawDrawOptions(item, player, mobDir, attackDirX, attackDirY, attackProgress);
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
            clawOverlayOptions.setOffsets(
               centerX, centerY, armPosX, armPosY, armRotationOffset, armRotateX, armRotateY, armLength, armCenterHeight, itemYOffset
            );
            return this;
         }

         @Override
         public HumanAttackDrawOptions light(Level level, boolean hasGlowEffect, int glowHash, GameLight light) {
            attackDrawOptions.light(level, hasGlowEffect, glowHash, light);
            clawOverlayOptions.light(level, hasGlowEffect, glowHash, light.minLevelCopy(150.0F));
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
            DrawOptions options2 = clawOverlayOptions.pos(drawX, drawY);
            return () -> {
               options1.draw();
               options2.draw();
            };
         }
      };
   }
}
