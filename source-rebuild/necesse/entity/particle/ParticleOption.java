package necesse.entity.particle;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import necesse.engine.sound.SoundEmitter;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.IntersectionPoint;
import necesse.entity.Entity;
import necesse.entity.trails.Trail;
import necesse.entity.trails.TrailVector;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class ParticleOption implements SoundEmitter {
   protected boolean removed;
   protected int lifeTime = 500;
   protected Float pos;
   protected float currentHeight;
   protected Supplier<Float> snapPosition;
   protected ParticleOption.Mover mover = null;
   protected int lightLevel;
   protected float lightHue;
   protected float lightSat;
   protected ParticleOption.SpriteSelector sprite = (options, lifeTime, timeAlive, lifePercent) -> options.add(GameResources.particles.sprite(0, 1, 8));
   protected ParticleOption.HeightGetter height = null;
   protected ParticleOption.FloatGetter rotation = (lifeTime, timeAlive, lifePercent) -> 0.0F;
   protected ParticleOption.DrawModifier size = (options, lifeTime, timeAlive, lifePercent) -> {};
   protected ParticleOption.DrawModifier color = (options, lifeTime, timeAlive, lifePercent) -> {};
   protected ParticleOption.TrailVectorGetter trailVectorGetter;
   protected int minDrawLightLevel;
   protected boolean ignoreLight;
   protected Trail trail;
   protected Color trailColor;
   protected int trailFadeInTime;
   protected int trailFadeOutTime;
   protected LinkedList<ParticleOption.DrawModifier> extraModifiers = new LinkedList<>();
   protected LinkedList<Supplier<Boolean>> removeIf = new LinkedList<>();
   protected GameLinkedList<ParticleOption.TimeAliveEvent> timeAliveEvents = new GameLinkedList<>();
   protected GameLinkedList<ParticleOption.ProgressEvent> progressEvents = new GameLinkedList<>();
   protected GameLinkedList<ParticleOption.TickEvent> tickEvents = new GameLinkedList<>();
   public static float defaultFlameHue = 43.0F;
   public static float defaultSmokeHue = 0.0F;

   public static ParticleOption base(float x, float y) {
      return new ParticleOption(x, y);
   }

   public static ParticleOption standard(float x, float y) {
      return base(x, y).sizeFades().rotates();
   }

   protected ParticleOption(float x, float y) {
      this.pos = new Float(x, y);
   }

   public ParticleOption changePos(float x, float y) {
      this.pos.x = x;
      this.pos.y = y;
      return this;
   }

   public Float getPos() {
      return this.pos;
   }

   public Float getLevelPos() {
      if (this.snapPosition != null) {
         Float snapPos = this.snapPosition.get();
         if (snapPos != null) {
            return new Float(this.pos.x + snapPos.x, this.pos.y + snapPos.y);
         }
      }

      return new Float(this.pos.x, this.pos.y);
   }

   public ParticleOption snapPosition(Supplier<Float> snapPosition) {
      this.snapPosition = snapPosition;
      return this;
   }

   public ParticleOption snapPosition(Entity entity) {
      return this.snapPosition(() -> new Float(entity.x, entity.y));
   }

   public ParticleOption moves(ParticleOption.Mover mover) {
      this.mover = mover;
      return this;
   }

   public ParticleOption movesFriction(float dx, float dy, float friction) {
      return this.moves(new ParticleOption.FrictionMover(dx, dy, friction));
   }

   public ParticleOption movesFrictionAngle(float angle, float speed, float friction) {
      Float dir = GameMath.getAngleDir(angle);
      return this.movesFriction(dir.x * speed, dir.y * speed, friction);
   }

   public ParticleOption movesConstant(float dx, float dy) {
      return this.movesFriction(dx, dy, 0.0F);
   }

   public ParticleOption movesConstantAngle(float angle, float speed) {
      Float dir = GameMath.getAngleDir(angle);
      return this.movesConstant(dir.x * speed, dir.y * speed);
   }

   public ParticleOption lifeTime(int millis) {
      this.lifeTime = millis;
      return this;
   }

   public ParticleOption lifeTimeBetween(int minMillis, int maxMillis) {
      this.lifeTime = GameRandom.globalRandom.getIntBetween(minMillis, maxMillis);
      return this;
   }

   public ParticleOption givesLight(int lightLevel) {
      this.lightLevel = lightLevel;
      return this;
   }

   public ParticleOption givesLight(boolean givesLight) {
      return givesLight ? this.givesLight(100) : this.givesLight(this.lightLevel);
   }

   public ParticleOption givesLight() {
      return this.givesLight(true);
   }

   public ParticleOption givesLight(float hue, float sat) {
      this.lightHue = hue;
      this.lightSat = sat;
      return this.givesLight(true);
   }

   public ParticleOption height(ParticleOption.HeightGetter getter) {
      this.height = getter;
      return this;
   }

   public ParticleOption heightMoves(float startHeight, float dh, float gravity, float friction, float minHeight, float bouncy) {
      return this.height(new ParticleOption.HeightMover(startHeight, dh, gravity, friction, minHeight, bouncy));
   }

   public ParticleOption height(ParticleOption.FloatGetter getter) {
      return this.height((delta, lifeTime, timeAlive, lifePercent) -> getter.get(lifeTime, timeAlive, lifePercent));
   }

   public ParticleOption height(float height) {
      return this.height((lifeTime, timeAlive, lifePercent) -> height);
   }

   public ParticleOption heightMoves(float startHeight, float endHeight) {
      float delta = endHeight - startHeight;
      return this.height((lifeTime, timeAlive, lifePercent) -> startHeight + delta * lifePercent);
   }

   public float getCurrentHeight() {
      return this.currentHeight;
   }

   public ParticleOption sprite(ParticleOption.SpriteSelector selector) {
      this.sprite = selector;
      return this;
   }

   public ParticleOption sprite(GameTextureSection section) {
      return this.sprite((options, lifeTime, timeAlive, lifePercent) -> options.add(section));
   }

   public ParticleOption sprite(int spriteX, int spriteY) {
      return this.sprite(GameResources.particles.sprite(spriteX, spriteY, 8));
   }

   public ParticleOption size(ParticleOption.DrawModifier modifier) {
      this.size = modifier;
      return this;
   }

   public ParticleOption sizeFades(int minSize, int maxSize) {
      int startSize = GameRandom.globalRandom.getIntBetween(minSize, maxSize);
      return this.size((options, lifeTime, timeAlive, lifePercent) -> {
         int size = (int)(startSize * Math.abs(lifePercent - 1.0F));
         options.size(size, size);
      });
   }

   public ParticleOption sizeFadesInAndOut(int minMidSize, int maxMidSize, float fadeInLifePercent) {
      int midSize = GameRandom.globalRandom.getIntBetween(minMidSize, maxMidSize);
      return this.size((options, lifeTime, timeAlive, lifePercent) -> {
         int size = midSize;
         if (lifePercent < fadeInLifePercent) {
            double percFontSize = lifePercent / fadeInLifePercent;
            size = (int)(percFontSize * size);
         } else {
            float fadeOutLifePercent = 1.0F - fadeInLifePercent;
            double percFontSize = Math.abs((lifePercent - fadeOutLifePercent) / fadeOutLifePercent - 1.0F);
            size = (int)(percFontSize * size);
         }

         options.size(size, size);
      });
   }

   public ParticleOption sizeFadesInAndOut(int minMidSize, int maxMidSize, int fadeInMS, int fadeOutMS) {
      int midSize = GameRandom.globalRandom.getIntBetween(minMidSize, maxMidSize);
      return this.size((options, lifeTime, timeAlive, lifePercent) -> {
         int size = midSize;
         if (timeAlive < fadeInMS) {
            double percFontSize = (double)timeAlive / fadeInMS;
            size = (int)(percFontSize * size);
         }

         if (timeAlive > lifeTime - fadeOutMS) {
            double percFontSize = Math.abs((double)(timeAlive + fadeOutMS - lifeTime) / fadeOutMS - 1.0);
            size = (int)(percFontSize * size);
         }

         options.size(size, size);
      });
   }

   public ParticleOption sizeFades() {
      return this.sizeFades(10, 18);
   }

   public ParticleOption rotation(ParticleOption.FloatGetter getter) {
      this.rotation = getter;
      return this;
   }

   public ParticleOption dontRotate() {
      return this.rotation((lifeTime, timeAlive, lifePercent) -> 0.0F);
   }

   public ParticleOption rotates(float minSpeed, float maxSpeed) {
      float offset = GameRandom.globalRandom.nextInt(360);
      float speed = minSpeed + GameRandom.globalRandom.nextFloat() * maxSpeed;
      if (GameRandom.globalRandom.nextBoolean()) {
         speed = -speed;
      }

      float finalSpeed = speed;
      return this.rotation((lifeTime, timeAlive, lifePercent) -> offset + lifePercent * finalSpeed);
   }

   public ParticleOption rotates() {
      return this.rotates(50.0F, 150.0F);
   }

   public ParticleOption alpha(float alpha) {
      return this.modify((options, lifeTime, timeAlive, lifePercent) -> options.alpha(alpha));
   }

   public ParticleOption fadesAlpha(float fadeInPercent, float fadeOutPercent) {
      return this.modify((options, lifeTime, timeAlive, lifePercent) -> {
         if (fadeInPercent != 0.0F && lifePercent <= fadeInPercent) {
            options.alpha(lifePercent / fadeInPercent);
         } else if (fadeOutPercent != 0.0F && lifePercent >= 1.0F - fadeOutPercent) {
            options.alpha(Math.abs((lifePercent - (1.0F - fadeOutPercent)) / fadeOutPercent - 1.0F));
         }
      });
   }

   public ParticleOption fadesAlphaTime(int fadeInMS, int fadeOutMS) {
      return this.fadesAlphaTimeToCustomAlpha(fadeInMS, fadeOutMS, 1.0F);
   }

   public ParticleOption fadesAlphaTimeToCustomAlpha(int fadeInMS, int fadeOutMS, float alpha) {
      return this.modify((options, lifeTime, timeAlive, lifePercent) -> {
         float newAlpha = alpha;
         if (timeAlive < fadeInMS) {
            float perc = (float)timeAlive / fadeInMS;
            newAlpha = perc * newAlpha;
         }

         if (timeAlive > lifeTime - fadeOutMS) {
            float perc = Math.abs((float)(timeAlive + fadeOutMS - lifeTime) / fadeOutMS - 1.0F);
            newAlpha = perc * newAlpha;
         }

         options.alpha(newAlpha);
      });
   }

   public ParticleOption color(ParticleOption.DrawModifier modifier) {
      this.color = modifier;
      return this;
   }

   public ParticleOption color(float red, float green, float blue, float alpha) {
      return this.color((options, lifeTime, timeAlive, lifePercent) -> options.color(red, green, blue, alpha));
   }

   public ParticleOption color(Color color) {
      float red = color.getRed() / 255.0F;
      float green = color.getGreen() / 255.0F;
      float blue = color.getBlue() / 255.0F;
      float alpha = color.getAlpha() / 255.0F;
      return this.color(red, green, blue, alpha);
   }

   public ParticleOption colorRandom(Color color, float hueOffset, float saturationOffset, float brightnessOffset) {
      return this.color(randomizeColor(color, hueOffset, saturationOffset, brightnessOffset));
   }

   public ParticleOption colorRandom(float hue, float saturation, float brightness, float hueOffset, float saturationOffset, float brightnessOffset) {
      return this.color(randomizeColor(hue, saturation, brightness, hueOffset, saturationOffset, brightnessOffset));
   }

   public ParticleOption flameColor(float hue) {
      return this.color(randomFlameColor(hue));
   }

   public ParticleOption flameColor() {
      return this.color(randomFlameColor());
   }

   public ParticleOption smokeColor(float hue) {
      return this.color(randomSmokeColor(hue));
   }

   public ParticleOption smokeColor() {
      return this.color(randomSmokeColor());
   }

   public static Color randomFlameColor(GameRandom random, float hue) {
      return randomizeColor(random, hue, 1.0F, 1.0F, 7.0F, 0.1F, 0.1F);
   }

   public static Color randomFlameColor(float hue) {
      return randomFlameColor(GameRandom.globalRandom, hue);
   }

   public static Color randomFlameColor(GameRandom random) {
      return randomFlameColor(random, defaultFlameHue);
   }

   public static Color randomFlameColor() {
      return randomFlameColor(defaultFlameHue);
   }

   public static Color randomSmokeColor(GameRandom random, float hue) {
      return randomizeColor(random, hue, 0.0F, 0.24F, 0.0F, 0.0F, 0.1F);
   }

   public static Color randomSmokeColor(float hue) {
      return randomSmokeColor(GameRandom.globalRandom, hue);
   }

   public static Color randomSmokeColor(GameRandom random) {
      return randomSmokeColor(random, defaultSmokeHue);
   }

   public static Color randomSmokeColor() {
      return randomSmokeColor(defaultSmokeHue);
   }

   public static Color randomizeColor(Color color, float hueOffset, float saturationOffset, float brightnessOffset) {
      float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
      return randomizeColor(hsb[0] * 360.0F, hsb[1], hsb[2], hueOffset, saturationOffset, brightnessOffset);
   }

   public static Color randomizeColor(float hue, float saturation, float brightness, float hueOffset, float saturationOffset, float brightnessOffset) {
      return randomizeColor(GameRandom.globalRandom, hue, saturation, brightness, hueOffset, saturationOffset, brightnessOffset);
   }

   public static Color randomizeColor(
      GameRandom random, float hue, float saturation, float brightness, float hueOffset, float saturationOffset, float brightnessOffset
   ) {
      if (hueOffset != 0.0F) {
         hue = (hue + random.getFloatOffset(0.0F, hueOffset)) % 360.0F;
      }

      if (saturationOffset != 0.0F) {
         saturation += random.getFloatOffset(0.0F, saturationOffset);
         if (saturation > 1.0F) {
            saturation -= saturation % 1.0F;
         } else if (saturation < 0.0F) {
            saturation -= saturation % 1.0F;
         }
      }

      if (brightnessOffset != 0.0F) {
         brightness += random.getFloatOffset(0.0F, brightnessOffset);
         if (brightness > 1.0F) {
            brightness -= brightness % 1.0F;
         } else if (brightness < 0.0F) {
            brightness -= brightness % 1.0F;
         }
      }

      return Color.getHSBColor(hue / 360.0F, saturation, brightness);
   }

   public ParticleOption minDrawLight(int level) {
      this.minDrawLightLevel = level;
      return this;
   }

   public ParticleOption ignoreLight(boolean ignoreLight) {
      this.ignoreLight = ignoreLight;
      return this;
   }

   public ParticleOption trail(Color color, int fadeInTime, int fadeOutTime, ParticleOption.TrailVectorGetter trailVectorGetter) {
      this.trailColor = color;
      this.trailFadeInTime = fadeInTime;
      this.trailFadeOutTime = fadeOutTime;
      this.trailVectorGetter = trailVectorGetter;
      return this;
   }

   public ParticleOption trail(Color color, int fadeInTime, int fadeOutTime, float thickness) {
      return this.trail(
         color, fadeInTime, fadeOutTime, (x, y, dx, dy, height, lifeTime, timeAlive, lifePercent) -> new TrailVector(x, y, dx, dy, thickness, height)
      );
   }

   public ParticleOption trail(Color color, int fadeInTime, int fadeOutTime, float startThickness, float endThickness) {
      return this.trail(color, fadeInTime, fadeOutTime, (x, y, dx, dy, height, lifeTime, timeAlive, lifePercent) -> {
         float currentThickness = GameMath.lerp(lifePercent, startThickness, endThickness);
         return new TrailVector(x, y, dx, dy, currentThickness, height);
      });
   }

   public ParticleOption modify(ParticleOption.DrawModifier modifier) {
      this.extraModifiers.add(modifier);
      return this;
   }

   public ParticleOption removeIf(Supplier<Boolean> test) {
      this.removeIf.add(test);
      return this;
   }

   public ParticleOption onTimeAlive(int timeAlive, ParticleOption.SnapshotEvent listener) {
      ParticleOption.TimeAliveEvent e = new ParticleOption.TimeAliveEvent(timeAlive, listener);

      for (GameLinkedList<ParticleOption.TimeAliveEvent>.Element el : this.timeAliveEvents.elements()) {
         if (((ParticleOption.TimeAliveEvent)el.object).timeAlive > timeAlive) {
            el.insertBefore(e);
            return this;
         }
      }

      this.timeAliveEvents.addLast(e);
      return this;
   }

   public ParticleOption onTimeAlive(int timeAlive, Consumer<Float> positionConsumer) {
      return this.onTimeAlive(timeAlive, (lifeTime, timeAlive2, lifePercent, pos) -> positionConsumer.accept(pos));
   }

   public ParticleOption onProgress(float lifeProgress, ParticleOption.SnapshotEvent listener) {
      ParticleOption.ProgressEvent e = new ParticleOption.ProgressEvent(lifeProgress, listener);

      for (GameLinkedList<ParticleOption.ProgressEvent>.Element el : this.progressEvents.elements()) {
         if (((ParticleOption.ProgressEvent)el.object).lifeProgress > lifeProgress) {
            el.insertBefore(e);
            return this;
         }
      }

      this.progressEvents.addLast(e);
      return this;
   }

   public ParticleOption onProgress(float lifeProgress, Consumer<Float> positionConsumer) {
      return this.onProgress(lifeProgress, (lifeTime, timeAlive, lifePercent, pos) -> positionConsumer.accept(pos));
   }

   public ParticleOption onDied(Consumer<Float> positionConsumer) {
      return this.onProgress(1.0F, positionConsumer);
   }

   public ParticleOption onMoveTick(ParticleOption.TickEvent event) {
      this.tickEvents.addLast(event);
      return this;
   }

   public void remove() {
      if (this.trail != null) {
         this.trail.removeOnFadeOut = true;
      }

      this.removed = true;
   }

   public boolean isRemoved() {
      return this.removed;
   }

   protected void tick(Level level, float delta, int lifeTime, int timeAlive, float lifePercent) {
      float lastX = this.pos.x;
      float lastY = this.pos.y;
      if (this.mover != null) {
         this.mover.tick(this.pos, delta, lifeTime, timeAlive, lifePercent);
      }

      if (this.height != null) {
         this.currentHeight = this.height.tick(delta, lifeTime, timeAlive, lifePercent);
      }

      this.tickEvents.forEach(c -> c.tick(delta, lifeTime, timeAlive, lifePercent));
      if (this.trailVectorGetter != null) {
         Float dir = GameMath.normalize(this.pos.x - lastX, this.pos.y - lastY);
         TrailVector trailVector = this.trailVectorGetter.get(this.pos.x, this.pos.y, dir.x, dir.y, this.currentHeight, lifeTime, timeAlive, lifePercent);
         if (trailVector != null) {
            if (this.trail == null) {
               this.trail = new Trail(trailVector, level, this.trailColor, this.trailFadeInTime, this.trailFadeOutTime);
               if (lifePercent < 1.0F && !this.isRemoved()) {
                  this.trail.removeOnFadeOut = false;
               }

               level.entityManager.addTrail(this.trail);
            } else {
               this.trail.addPoint(trailVector);
            }
         }
      }

      this.tickProgress(lifeTime, timeAlive, lifePercent);
   }

   protected void tickProgress(int lifeTime, int timeAlive, float lifeCyclePercent) {
      if (this.trail != null && lifeCyclePercent >= 1.0F) {
         this.trail.removeOnFadeOut = true;
      }

      if (this.removeIf.stream().anyMatch(Supplier::get)) {
         this.remove();
      }

      while (!this.timeAliveEvents.isEmpty()) {
         GameLinkedList<ParticleOption.TimeAliveEvent>.Element firstEl = this.timeAliveEvents.getFirstElement();
         if (timeAlive < ((ParticleOption.TimeAliveEvent)firstEl.object).timeAlive) {
            break;
         }

         ((ParticleOption.TimeAliveEvent)firstEl.object).listener.snap(lifeTime, timeAlive, lifeCyclePercent, this.pos);
         firstEl.remove();
      }

      while (!this.progressEvents.isEmpty()) {
         GameLinkedList<ParticleOption.ProgressEvent>.Element firstEl = this.progressEvents.getFirstElement();
         if (!(lifeCyclePercent >= ((ParticleOption.ProgressEvent)firstEl.object).lifeProgress)) {
            break;
         }

         ((ParticleOption.ProgressEvent)firstEl.object).listener.snap(lifeTime, timeAlive, lifeCyclePercent, this.pos);
         firstEl.remove();
      }
   }

   public void addDrawOptions(SharedTextureDrawOptions options, Level level, int lifeTime, int timeAlive, float lifePercent, GameCamera camera) {
      if (!this.isRemoved()) {
         float posX = this.pos.x;
         float posY = this.pos.y;
         if (this.snapPosition != null) {
            Float snapPos = this.snapPosition.get();
            if (snapPos != null) {
               posX += snapPos.x;
               posY += snapPos.y;
            }
         }

         SharedTextureDrawOptions.Wrapper o = this.sprite.get(options, lifeTime, timeAlive, lifePercent);
         this.size.modify(o, lifeTime, timeAlive, lifePercent);
         this.color.modify(o, lifeTime, timeAlive, lifePercent);
         float rotation = this.rotation.get(lifeTime, timeAlive, lifePercent);
         if (rotation != 0.0F) {
            o.rotate(rotation);
         }

         if (!this.ignoreLight) {
            GameLight light = level.getLightLevel(GameMath.getTileCoordinate(posX), GameMath.getTileCoordinate(posY));
            if (this.minDrawLightLevel > 0) {
               light = light.minLevelCopy(this.minDrawLightLevel);
            }

            o.light(light);
         }

         this.extraModifiers.forEach(m -> m.modify(o, lifeTime, timeAlive, lifePercent));
         o.posMiddle(camera.getDrawX(posX), camera.getDrawY(posY - this.currentHeight));
      }
   }

   @Override
   public float getSoundPositionX() {
      return this.pos.x;
   }

   @Override
   public float getSoundPositionY() {
      return this.pos.y;
   }

   public static class CollisionMover implements ParticleOption.Mover {
      public final Level level;
      public final CollisionFilter collisionFilter;
      public final ParticleOption.Mover parentMover;
      public boolean stopped;
      public float minRandomWallPosOffset = -10.0F;
      public float maxRandomWallPosOffset = 10.0F;

      public CollisionMover(Level level, ParticleOption.Mover parentMover, CollisionFilter collisionFilter) {
         this.level = level;
         this.parentMover = parentMover;
         this.collisionFilter = collisionFilter;
      }

      public CollisionMover(Level level, ParticleOption.Mover parentMover) {
         this(level, parentMover, new CollisionFilter().mobCollision());
      }

      @Override
      public void tick(Float pos, float delta, int lifeTime, int timeAlive, float lifePercent) {
         if (!this.stopped) {
            Float startPos = new Float(pos.x, pos.y);
            this.parentMover.tick(pos, delta, lifeTime, timeAlive, lifePercent);
            java.awt.geom.Line2D.Float moveLine = new java.awt.geom.Line2D.Float(startPos, pos);
            ArrayList<LevelObjectHit> collisions = this.level.getCollisions(moveLine, this.collisionFilter);
            IntersectionPoint<LevelObjectHit> collisionPoint = this.level.getCollisionPoint(collisions, moveLine, false);
            if (collisionPoint != null) {
               float offset = GameRandom.globalRandom.getFloatBetween(this.minRandomWallPosOffset, this.maxRandomWallPosOffset);
               Float dir = GameMath.normalize(moveLine.x2 - moveLine.x1, moveLine.y2 - moveLine.y1);
               pos.x = (float)collisionPoint.x + dir.x * offset;
               pos.y = (float)collisionPoint.y + dir.y * offset;
               this.stopped = true;
            }
         }
      }
   }

   public interface DrawModifier {
      void modify(SharedTextureDrawOptions.Wrapper var1, int var2, int var3, float var4);
   }

   public interface FloatGetter {
      float get(int var1, int var2, float var3);
   }

   public static class FrictionMover implements ParticleOption.Mover {
      public float dx;
      public float dy;
      public float friction;

      public FrictionMover(float dx, float dy, float friction) {
         this.dx = dx;
         this.dy = dy;
         this.friction = friction;
      }

      @Override
      public void tick(Float pos, float delta, int lifeTime, int timeAlive, float lifePercent) {
         float deltaSpeed = delta / 250.0F;
         if (this.friction != 0.0F) {
            this.dx = this.dx + -this.friction * this.dx * deltaSpeed;
            this.dy = this.dy + -this.friction * this.dy * deltaSpeed;
         }

         pos.x = pos.x + this.dx * deltaSpeed;
         pos.y = pos.y + this.dy * deltaSpeed;
      }
   }

   public interface HeightGetter {
      float tick(float var1, int var2, int var3, float var4);
   }

   public static class HeightMover implements ParticleOption.HeightGetter {
      public float currentHeight;
      public float dh;
      public float gravity;
      public float minHeight;
      public float bouncy;
      public float friction;

      public HeightMover(float startHeight, float dh, float gravity, float friction, float minHeight, float bouncy) {
         this.currentHeight = startHeight;
         this.dh = dh;
         this.gravity = gravity;
         this.minHeight = minHeight;
         this.bouncy = bouncy;
         this.friction = friction;
      }

      @Override
      public float tick(float delta, int lifeTime, int timeAlive, float lifePercent) {
         float deltaSpeed = delta / 250.0F;
         if (this.friction != 0.0F) {
            this.dh = this.dh + (-this.gravity * this.friction - this.friction * this.dh) * deltaSpeed;
         } else {
            this.dh = this.dh + (this.gravity - this.dh) * deltaSpeed;
         }

         this.currentHeight = this.currentHeight + this.dh * deltaSpeed;
         if (this.currentHeight < this.minHeight) {
            if (this.bouncy > 0.0F && Math.abs(this.dh) > Math.abs(this.gravity / 20.0F)) {
               this.dh = -this.dh * this.bouncy;
               this.currentHeight = this.minHeight + Math.abs(this.currentHeight - this.minHeight);
            } else {
               this.currentHeight = this.minHeight;
               this.dh = 0.0F;
            }
         }

         return this.currentHeight;
      }
   }

   public interface Mover {
      void tick(Float var1, float var2, int var3, int var4, float var5);
   }

   protected static class ProgressEvent {
      public final float lifeProgress;
      public final ParticleOption.SnapshotEvent listener;

      public ProgressEvent(float lifeProgress, ParticleOption.SnapshotEvent listener) {
         this.lifeProgress = lifeProgress;
         this.listener = listener;
      }
   }

   public interface SnapshotEvent {
      void snap(int var1, int var2, float var3, Float var4);
   }

   public interface SpriteSelector {
      SharedTextureDrawOptions.Wrapper get(SharedTextureDrawOptions var1, int var2, int var3, float var4);
   }

   public interface TickEvent {
      void tick(float var1, int var2, int var3, float var4);
   }

   protected static class TimeAliveEvent {
      public final int timeAlive;
      public final ParticleOption.SnapshotEvent listener;

      public TimeAliveEvent(int timeAlive, ParticleOption.SnapshotEvent listener) {
         this.timeAlive = timeAlive;
         this.listener = listener;
      }
   }

   public interface TrailVectorGetter {
      TrailVector get(float var1, float var2, float var3, float var4, float var5, int var6, int var7, float var8);
   }
}
