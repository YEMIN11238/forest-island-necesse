package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PathDoorOption;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedItemAttackerPlayerChaserWandererAI;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.itemAttacker.CheckSlotType;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameSkin;
import necesse.gfx.HumanLook;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.cosmetics.misc.ShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.ShoesArmorItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class PalworldSyndicateThugMob extends HostileItemAttackerMob {
   public static LootTable lootTable = new LootTable(LootItem.between("coin", 100, 200).splitItems(5));
   public HumanLook look = new HumanLook();
   public InventoryItem helmet;
   public InventoryItem chest;
   public InventoryItem boots;

   public PalworldSyndicateThugMob() {
      super(500);
      this.setSpeed(30.0F);
      this.setFriction(3.0F);
      this.setKnockbackModifier(0.5F);
      this.setArmor(25);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-14, -41, 28, 48);
      this.swimMaskMove = 16;
      this.swimMaskOffset = -2;
      this.swimSinkOffset = -4;
   }

   @Override
   public void init() {
      super.init();
      this.updateLook();
      this.ai = new BehaviourTreeAI<>(this, new ConfusedItemAttackerPlayerChaserWandererAI<>(null, 640, this.getWeapon(), 40000));
   }

   public InventoryItem getWeapon() {
      GameRandom random = new GameRandom(this.getUniqueID()).nextSeeded(24);
      return random.getOneOf(() -> {
         InventoryItem inventoryItem = new InventoryItem("handgun");
         inventoryItem.getGndData().setFloat("damage", 80.0F);
         inventoryItem.getGndData().setInt("attackAnimTime", 1000);
         return inventoryItem;
      }, () -> {
         InventoryItem inventoryItem = new InventoryItem("syndicatebat");
         inventoryItem.getGndData().setFloat("damage", 110.0F);
         inventoryItem.getGndData().setInt("attackAnimTime", 500);
         return inventoryItem;
      });
   }

   @Override
   public PathDoorOption getPathDoorOption() {
      if (this.getLevel() != null) {
         return this.buffManager.getModifier(BuffModifiers.CAN_BREAK_OBJECTS)
            ? this.getLevel().regionManager.CAN_BREAK_OBJECTS_OPTIONS
            : this.getLevel().regionManager.CAN_OPEN_DOORS_OPTIONS;
      } else {
         return null;
      }
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("syndicatethug", 4);
   }

   public void updateLook() {
      GameRandom random = new GameRandom(this.getUniqueID()).nextSeeded(16);
      this.look.randomizeLook(random, true);
      this.helmet = new InventoryItem("syndicatethughood");
      this.chest = new InventoryItem("syndicatethugshirt");
      this.boots = new InventoryItem("syndicatethugboots");
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      GameSkin gameSkin = this.look.getGameSkin(true);

      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), gameSkin, GameRandom.globalRandom.nextInt(5), 8, 32, this.x, this.y, 10.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   public void addDrawables(
      List<MobDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      Level level,
      int x,
      int y,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 44 - 7;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      boolean inLiquid = this.inLiquid(x, y);
      if (inLiquid) {
         sprite.x = 0;
      }

      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      HumanDrawOptions humanOptions = new HumanDrawOptions(level, this.look, false)
         .sprite(sprite)
         .mask(swimMask)
         .dir(dir)
         .light(light)
         .applyEnemyTracker(this, perspective);
      if (inLiquid) {
         humanOptions.armSprite(2);
         humanOptions.mask(MobRegistry.Textures.boat_mask[sprite.y % 4], 0, -7);
      }

      if (this.helmet != null) {
         humanOptions.helmet(this.helmet);
      }

      if (this.chest != null) {
         humanOptions.chestplate(this.chest);
      } else {
         humanOptions.chestplate(ShirtArmorItem.addColorData(new InventoryItem("shirt"), this.look.getShirtColor()));
      }

      if (this.boots != null) {
         humanOptions.boots(this.boots);
      } else {
         humanOptions.boots(ShoesArmorItem.addColorData(new InventoryItem("shoes"), this.look.getShoesColor()));
      }

      this.setupAttackDraw(humanOptions);
      final DrawOptions drawOptions = humanOptions.pos(drawX, drawY);
      final DrawOptions boat = inLiquid ? MobRegistry.Textures.steelBoat.initDraw().sprite(0, sprite.y, 64).light(light).pos(drawX, drawY + 7) : null;
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            if (boat != null) {
               boat.draw();
            }

            drawOptions.draw();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   public float getAttackingMovementModifier() {
      return 0.0F;
   }

   @Override
   public ItemAttackSlot getCurrentSelectedAttackSlot() {
      return null;
   }

   @Override
   public boolean hasValidSummonItem(Item item, CheckSlotType slotType) {
      return true;
   }
}
