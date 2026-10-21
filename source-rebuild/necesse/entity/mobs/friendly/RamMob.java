package necesse.entity.mobs.friendly;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.HusbandryImpregnateWandererAI;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.HumanGender;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class RamMob extends HusbandryMob {
   public static LootTable lootTable = new LootTable(LootItem.between("rawmutton", 2, 3), LootItem.between("wool", 1, 2));
   public long nextShearTime;

   public RamMob() {
      super(50);
      this.setSpeed(12.0F);
      this.setFriction(3.0F);
      this.collision = new Rectangle(-12, -9, 24, 18);
      this.hitBox = new Rectangle(-16, -12, 32, 24);
      this.selectBox = new Rectangle(-18, -30, 36, 36);
      this.swimMaskMove = 16;
      this.swimMaskOffset = 0;
      this.swimSinkOffset = 0;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new HusbandryImpregnateWandererAI<>(30000));
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addLong("nextShearTime", this.nextShearTime);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.nextShearTime = save.getLong("nextShearTime", 0L, false);
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.nextShearTime = reader.getNextLong();
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextLong(this.nextShearTime);
   }

   @Override
   public LootTable getLootTable() {
      return !this.isGrown() ? new LootTable() : lootTable;
   }

   @Override
   public GameMessage getLocalization() {
      return this.isGrown() ? super.getLocalization() : new LocalMessage("mob", "lamb");
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      GameTexture texture = this.getTexture();

      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), texture, GameRandom.globalRandom.nextInt(5), 8, 32, this.x, this.y, 10.0F, knockbackX, knockbackY),
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
      int drawX = camera.getDrawX(x) - 22 - 10;
      int drawY = camera.getDrawY(y) - 44 - 7;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      TextureDrawOptions shadow = this.getShadowTexture().initDraw().sprite(0, dir, 64).light(light).pos(drawX, drawY);
      tileList.add(tm -> shadow.draw());
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions options = this.getTexture()
         .initDraw()
         .sprite(sprite.x, sprite.y, 64)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .addMaskShader(swimMask)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            options.draw();
            swimMask.stop();
         }
      });
   }

   private GameTexture getTexture() {
      if (this.isGrown()) {
         return this.hasWool() ? MobRegistry.Textures.ram : MobRegistry.Textures.ram_sheared;
      } else {
         return MobRegistry.Textures.lamb;
      }
   }

   private GameTexture getShadowTexture() {
      return this.isGrown() ? MobRegistry.Textures.sheep_shadow : MobRegistry.Textures.lamb_shadow;
   }

   @Override
   protected int getRockSpeed() {
      return this.isGrown() ? 10 : 7;
   }

   @Override
   public HumanGender getGender() {
      return HumanGender.MALE;
   }

   @Override
   public boolean canImpregnateMob(HusbandryMob other) {
      return other.getStringID().equals("sheep");
   }

   public boolean hasWool() {
      return this.nextShearTime <= this.getWorldEntity().getWorldTime();
   }

   @Override
   public boolean canShear(InventoryItem item) {
      return this.isGrown() && this.hasWool() && this.buyPrice == null;
   }

   @Override
   public InventoryItem onShear(InventoryItem item, List<InventoryItem> products) {
      this.nextShearTime = this.getWorldEntity().getWorldTime() + GameRandom.globalRandom.getIntBetween(1200000, 1800000);
      int items = GameRandom.globalRandom.getIntBetween(1, 3);

      for (int i = 0; i < items; i++) {
         products.add(new InventoryItem("wool"));
      }

      if (this.isClient()) {
         SoundManager.playSound(GameResources.shears, SoundEffect.effect(this).volume(0.4F));
      }

      this.sendMovementPacket(false);
      return item;
   }

   @Override
   protected void addDebugTooltips(ListGameTooltips tooltips) {
      super.addDebugTooltips(tooltips);
      if (this.isGrown()) {
         tooltips.add("Wool grown in: " + GameUtils.getTimeStringMillis(this.nextShearTime - this.getWorldTime()));
      }
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.ramAmbient).volume(0.4F);
   }

   @Override
   protected SoundSettings getHurtSound() {
      return new SoundSettings(GameResources.ramHurt).volume(0.4F);
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.ramDeath).volume(0.2F);
   }
}
