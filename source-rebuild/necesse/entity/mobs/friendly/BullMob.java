package necesse.entity.mobs.friendly;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.journal.JournalChallenge;
import necesse.engine.journal.JournalChallengeUtils;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.JournalChallengeRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
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
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class BullMob extends HusbandryMob {
   public static LootTable lootTable = new LootTable(LootItem.between("beef", 2, 3), LootItem.between("leather", 2, 5));

   public BullMob() {
      super(50);
      this.setSpeed(12.0F);
      this.setFriction(3.0F);
      this.collision = new Rectangle(-12, -9, 24, 18);
      this.hitBox = new Rectangle(-16, -12, 32, 24);
      this.selectBox = new Rectangle(-18, -40, 36, 46);
      this.swimMaskMove = 16;
      this.swimMaskOffset = 0;
      this.swimSinkOffset = 0;
   }

   @Override
   public GameMessage getLocalization() {
      return this.isGrown() ? super.getLocalization() : new LocalMessage("mob", "calf");
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new HusbandryImpregnateWandererAI<>(30000));
   }

   @Override
   public LootTable getLootTable() {
      return !this.isGrown() ? new LootTable() : lootTable;
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.bullAmbient);
   }

   @Override
   protected SoundSettings getHurtSound() {
      return new SoundSettings(GameResources.bullHurt);
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.bullDeath).volume(0.3F);
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
      DrawOptions shadow = this.getShadowTexture().initDraw().sprite(0, dir, 64).light(light).pos(drawX, drawY);
      tileList.add(tm -> shadow.draw());
      drawY -= 4;
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

   @Override
   public int getRockSpeed() {
      return this.isGrown() ? 10 : 7;
   }

   private GameTexture getTexture() {
      return this.isGrown() ? MobRegistry.Textures.bull : MobRegistry.Textures.calf;
   }

   private GameTexture getShadowTexture() {
      return this.isGrown() ? MobRegistry.Textures.cow_shadow : MobRegistry.Textures.calf_shadow;
   }

   @Override
   public HumanGender getGender() {
      return HumanGender.MALE;
   }

   @Override
   public boolean canImpregnateMob(HusbandryMob other) {
      return other.getStringID().equals("cow");
   }

   @Override
   public InventoryItem onRope(int fromUniqueID, InventoryItem item) {
      InventoryItem out = super.onRope(fromUniqueID, item);
      Mob ropeMob = this.getRopeMob();
      if (ropeMob != null
         && ropeMob.isPlayer
         && ((PlayerMob)ropeMob).isServerClient()
         && JournalChallengeUtils.isForestBiome(this.getLevel().getBiome(this.getTileX(), this.getTileY()))) {
         ServerClient serverClient = ((PlayerMob)ropeMob).getServerClient();
         JournalChallenge challenge = JournalChallengeRegistry.getChallenge(JournalChallengeRegistry.CAPTURE_COW_ID);
         if (!challenge.isCompleted(serverClient) && challenge.isJournalEntryDiscovered(serverClient)) {
            challenge.markCompleted(serverClient);
            serverClient.forceCombineNewStats();
         }
      }

      return out;
   }
}
