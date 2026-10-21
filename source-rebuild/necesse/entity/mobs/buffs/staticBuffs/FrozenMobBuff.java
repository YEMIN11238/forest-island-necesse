package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.io.FileNotFoundException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.stream.Collectors;
import necesse.engine.CameraShakeValues;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.gameSound.GameSound;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.ObjectValue;
import necesse.entity.levelEvent.mobAbilityLevelEvent.FrozenEnemyShatterLevelEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffManager;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.MobExtraDrawBuff;
import necesse.gfx.GameResources;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.gameObject.ObjectDamagedTextureArray;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class FrozenMobBuff extends Buff implements MobExtraDrawBuff {
   private static final Dimension[] TEXTURE_DIMENSIONS = new Dimension[]{
      new Dimension(32, 32), new Dimension(32, 64), new Dimension(64, 64), new Dimension(64, 96), new Dimension(96, 96)
   };
   public static int DELAY_TIME_PER_TILE = 150;
   public static int THAW_TIME_MIN = 3000;
   public static int THAW_TIME_MAX = 6000;
   public static float[] CRACK_SOUND_AT_PROGRESS = new float[]{0.2F, 0.4F, 0.6F, 0.8F};
   public static CameraShakeValues CRACK_SHAKE = new CameraShakeValues(200, 50, 1.5F, 1.5F, true);
   private static GameTexture[][] FROZEN_TEXTURES;
   private final GameRandom drawRandom;

   public FrozenMobBuff() {
      this.isVisible = true;
      this.isImportant = true;
      this.canCancel = false;
      this.drawRandom = new GameRandom();
   }

   @Override
   public void onOverridden(ActiveBuff buff, ActiveBuff other) {
      super.onOverridden(buff, other);
      other.getGndData().copyKeysToTarget(buff.getGndData(), "lastCrackSound", "shakeStartTime");
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.PARALYZED, true);
      buff.setModifier(BuffModifiers.GROUNDED, true);
      if (this.getID() != BuffRegistry.FROZEN_MOB_IMMUNE.getID()) {
         buff.getGndData().setLong("thawStartTime", buff.getStackTimes().getLast().startTime);
         buff.getGndData().setLong("thawTime", buff.getDuration() - 100);
      }
   }

   @Override
   public void loadTextures() {
      try {
         this.iconTexture = GameTexture.fromFileRaw("buffs/frozensolid");
      } catch (FileNotFoundException e) {
         this.iconTexture = GameTexture.fromFile("buffs/unknown");
      }

      GameTexture overlay = GameTexture.fromFile("objects/breakobjectoverlay", true);
      FROZEN_TEXTURES = new GameTexture[TEXTURE_DIMENSIONS.length][0];

      for (int i = 0; i < TEXTURE_DIMENSIONS.length; i++) {
         Dimension dimension = TEXTURE_DIMENSIONS[i];
         GameTexture texture = GameTexture.fromFile("particles/frozentomb" + dimension.width + "x" + dimension.height);
         FROZEN_TEXTURES[i] = ObjectDamagedTextureArray.applyOverlay(texture, overlay);
      }
   }

   public static int getAppropriateSpriteIndex(int width, int height) {
      Dimension best = null;
      int bestIndex = -1;

      for (int i = 0; i < TEXTURE_DIMENSIONS.length; i++) {
         Dimension dimension = TEXTURE_DIMENSIONS[i];
         if (dimension.width >= width && dimension.height >= height) {
            if (best != null) {
               int bestSize = best.width * best.height;
               int nextSize = dimension.width * dimension.height;
               if (bestSize < nextSize) {
                  continue;
               }
            }

            best = dimension;
            bestIndex = i;
         }
      }

      return bestIndex;
   }

   private GameSprite getAppropriateSprite(int width, int height, int seed, float breakPercent) {
      int index = getAppropriateSpriteIndex(width, height);
      if (index != -1) {
         Dimension dimension = TEXTURE_DIMENSIONS[index];
         GameTexture[] textures = FROZEN_TEXTURES[index];
         int variation = Math.min((int)(breakPercent * textures.length), textures.length - 1);
         GameTexture texture = textures[variation];
         int sprites = Math.max(texture.getWidth() / (dimension.width + 32), 1);
         int sprite;
         synchronized (this.drawRandom) {
            sprite = this.drawRandom.seeded(seed).nextInt(sprites);
         }

         return new GameSprite(texture, sprite, 0, dimension.width + 32, dimension.height + 32, dimension.width + 32, dimension.height + 32);
      } else {
         return null;
      }
   }

   public static GameTextureSection getAppropriateDebrisSprites(int width, int height) {
      int index = getAppropriateSpriteIndex(width, height);
      if (index != -1) {
         Dimension dimension = TEXTURE_DIMENSIONS[index];
         GameTexture texture = FROZEN_TEXTURES[index][0];
         int spriteStart = dimension.height + 32;
         int spriteHeight = spriteStart - texture.getHeight();
         return new GameTextureSection(texture).section(0, spriteHeight * 5, spriteStart, texture.getHeight());
      } else {
         return null;
      }
   }

   public static GameTextureSection getAppropriateRandomDebrisSprite(int width, int height) {
      GameTextureSection sprites = getAppropriateDebrisSprites(width, height);
      if (sprites == null) {
         return null;
      }

      int spriteRes = sprites.getHeight();
      int sprite = GameRandom.globalRandom.nextInt(Math.max(sprites.getWidth() / spriteRes, 1));
      return sprites.sprite(sprite, 0, spriteRes);
   }

   @Override
   public void serverTick(ActiveBuff buff) {
      super.serverTick(buff);
      long startTime = buff.getGndData().getLong("thawStartTime");
      if (startTime != 0L) {
         long thawTime = startTime + buff.getGndData().getInt("thawTime");
         if (buff.owner.getTime() >= thawTime) {
            this.onThawFinished(buff);
         }
      }
   }

   protected void onThawFinished(ActiveBuff activeBuff) {
      activeBuff.owner.getLevel().entityManager.addLevelEvent(new FrozenEnemyShatterLevelEvent(activeBuff.owner));
      activeBuff.owner.buffManager.removeBuff(activeBuff.buff, true);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      this.tickPlayCrack(buff);
      long startTime = buff.getGndData().getLong("thawStartTime");
      if (startTime != 0L && startTime <= buff.owner.getTime()) {
         buff.owner.getLevel().lightManager.refreshParticleLightFloat(buff.owner.x, buff.owner.y, 210.0F, 1.0F, 50);
      }
   }

   public void tickPlayCrack(ActiveBuff buff) {
      float lastCrackSound = buff.getGndData().getFloat("lastCrackSound", -1.0F);
      float nextCrackSound = -1.0F;

      for (float crackSoundAtProgress : CRACK_SOUND_AT_PROGRESS) {
         if (crackSoundAtProgress > lastCrackSound) {
            nextCrackSound = crackSoundAtProgress;
            break;
         }
      }

      if (nextCrackSound != -1.0F) {
         float breakPercent = 0.0F;
         long startTime = buff.getGndData().getLong("thawStartTime");
         if (startTime != 0L) {
            long thawProgressTime = buff.owner.getTime() - startTime;
            breakPercent = GameMath.limit((float)thawProgressTime / buff.getGndData().getInt("thawTime"), 0.0F, 1.0F);
         }

         if (breakPercent > nextCrackSound) {
            GameSound sound = GameRandom.globalRandom.getOneOf(GameResources.iceCrack1, GameResources.iceCrack2, GameResources.iceCrack3);
            SoundManager.playSound(sound, SoundEffect.effect(buff.owner).pitch(GameRandom.globalRandom.getFloatBetween(0.9F, 1.1F)));
            buff.getGndData().setFloat("lastCrackSound", breakPercent);
            buff.getGndData().setLong("shakeStartTime", buff.owner.getLocalTime());
         }
      }
   }

   protected void startThawMob(Mob mob, int delayTime, int nextThawTime) {
      ActiveBuff ab = mob.buffManager.getBuff(this);
      if (ab != null) {
         long thawStartTime = ab.getGndData().getLong("thawStartTime");
         if (thawStartTime == 0L) {
            if (nextThawTime == -1) {
               nextThawTime = GameRandom.globalRandom.getIntBetween(THAW_TIME_MIN, THAW_TIME_MAX);
            }

            ab.getGndData().setLong("thawStartTime", delayTime + mob.getTime());
            ab.getGndData().setLong("thawTime", nextThawTime);
         } else {
            long thawTime = ab.getGndData().getLong("thawTime");
            ab.getGndData().setLong("thawTime", thawTime - 1000L);
         }

         BuffManager.sendUpdatePacket(ab);
      }
   }

   protected void startThawMobAndFindNext(Mob baseMob, Mob targetMob, int maxTargets) {
      if (targetMob.isServer()) {
         this.startThawMob(targetMob, 0, -1);
         this.findAndThawNextTargets(baseMob, maxTargets);
      }
   }

   protected void findAndThawNextTargets(Mob from, int maxTargets) {
      from.getLevel()
         .entityManager
         .mobs
         .streamInRegionsInRange(from.x, from.y, 384)
         .filter(m -> !m.removed() && m != from)
         .filter(m -> {
            ActiveBuff buff = m.buffManager.getBuff(this);
            return buff != null && buff.getGndData().getLong("thawStartTime") == 0L;
         })
         .sorted(Comparator.comparingDouble(m -> GameMath.diagonalMoveDistance(from.getX(), from.getY(), m.getX(), m.getY())))
         .limit(maxTargets)
         .forEach(m -> this.startThawMobAndFindNext(from, m, 1));
   }

   protected boolean findAndThawAll(Mob from) {
      LinkedList<ObjectValue<Mob, Double>> list = from.getLevel()
         .entityManager
         .mobs
         .streamInRegionsInRange(from.x, from.y, 384)
         .filter(m -> !m.removed())
         .filter(m -> {
            ActiveBuff buff = m.buffManager.getBuff(this);
            return buff != null && buff.getGndData().getLong("thawStartTime") == 0L;
         })
         .map(m -> new ObjectValue<>(m, m == from ? 0.0 : GameMath.diagonalMoveDistance(from.getX(), from.getY(), m.getX(), m.getY())))
         .sorted(Comparator.comparingDouble(ox -> (Double)ox.value))
         .collect(Collectors.toCollection(LinkedList::new));
      if (!list.isEmpty()) {
         boolean out = false;
         double first = (Double)list.getFirst().value;
         double last = (Double)list.getLast().value;

         for (ObjectValue<Mob, Double> o : list) {
            if (o.object == from) {
               out = true;
            }

            int delayTime = (int)(DELAY_TIME_PER_TILE * o.value / 32.0);
            double distancePercent = first == last ? 0.0 : GameMath.clamp(o.value, first, last);
            this.startThawMob(o.object, delayTime, (int)GameMath.lerp(distancePercent, THAW_TIME_MIN, THAW_TIME_MAX));
         }

         return out;
      } else {
         return false;
      }
   }

   @Override
   public void addBackDrawOptions(
      ActiveBuff buff, LinkedList<DrawOptions> list, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective
   ) {
   }

   @Override
   public void addFrontDrawOptions(
      ActiveBuff buff, LinkedList<DrawOptions> list, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective
   ) {
      Level level = buff.owner.getLevel();
      if (level != null) {
         GameLight light = level.getLightLevel(buff.owner);
         Rectangle selectBox = buff.owner.getMountUnionSelectBox(x, y);
         float breakPercent = 0.0F;
         long startTime = buff.getGndData().getLong("thawStartTime");
         if (startTime != 0L) {
            long thawProgressTime = buff.owner.getTime() - startTime;
            breakPercent = GameMath.limit((float)thawProgressTime / buff.getGndData().getInt("thawTime"), 0.0F, 1.0F);
         }

         Float shake = new Float();
         long shakeStartTime = buff.getGndData().getLong("shakeStartTime");
         if (shakeStartTime != 0L) {
            shake = CRACK_SHAKE.getCurrentShake(shakeStartTime, buff.owner.getLocalTime());
         }

         GameSprite sprite = this.getAppropriateSprite(selectBox.width, selectBox.height, buff.owner.getUniqueID(), breakPercent);
         if (sprite != null) {
            int deltaWidth = sprite.width - selectBox.width;
            int deltaHeight = sprite.height - selectBox.height;
            int drawX = camera.getDrawX(selectBox.x + shake.x) - deltaWidth / 2;
            int drawY = camera.getDrawY(selectBox.y + shake.y) - deltaHeight + 18;
            TextureDrawOptionsEnd drawOptions = sprite.initDraw()
               .startGlowOptions(level, buff.owner.getID())
               .light(light)
               .applyEnemyTracker(buff.owner, perspective)
               .pos(drawX, drawY);
            list.add(drawOptions);
         } else {
            int drawX = camera.getDrawX(selectBox.x + shake.x) - 5;
            int drawY = camera.getDrawY(selectBox.y + shake.y) - 10;
            TextureDrawOptionsEnd quadOptions = Renderer.initQuadDraw(selectBox.width + 10, selectBox.height + 10)
               .startGlowOptions(level, buff.owner.getID())
               .colorLight(new Color(150, 150, 255, 150), light)
               .applyEnemyTracker(buff.owner, perspective)
               .pos(drawX, drawY);
            list.add(quadOptions);
         }
      }
   }

   public static boolean isBuffValidForTarget(Mob target) {
      Rectangle selectBox = target.getSelectBox();
      return getAppropriateSpriteIndex(selectBox.width, selectBox.height) != -1;
   }
}
