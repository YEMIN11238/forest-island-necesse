package necesse.entity.mobs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.shader.ShaderState;
import necesse.gfx.shader.SpriteMaskOptions;
import necesse.gfx.shader.SpriteMaskTextureOptions;

public class MaskShaderOptions implements ShaderState {
   private final ArrayList<SpriteMaskOptions> masks = new ArrayList<>();
   public final int drawXOffset;
   public final int drawYOffset;
   private final ArrayDeque<ShaderState> maskOffsets = new ArrayDeque<>();

   public MaskShaderOptions(int drawXOffset, int drawYOffset) {
      this.drawXOffset = drawXOffset;
      this.drawYOffset = drawYOffset;
   }

   public MaskShaderOptions(GameTexture mask, int drawXOffset, int drawYOffset, int maskXOffset, int maskYOffset) {
      this(drawXOffset, drawYOffset);
      this.addMask(mask, maskXOffset, maskYOffset);
   }

   private MaskShaderOptions(MaskShaderOptions copy) {
      this.drawXOffset = copy.drawXOffset;
      this.drawYOffset = copy.drawYOffset;
      this.masks.addAll(copy.masks);
   }

   public void addMaskOffset(final int xOffset, final int yOffset) {
      final AtomicInteger usedMasks = new AtomicInteger();
      final int[][] lastOffsets = new int[4][2];
      this.maskOffsets.add(new ShaderState() {
         @Override
         public void use() {
            usedMasks.set(MaskShaderOptions.this.masks.size());
            int masks = usedMasks.get();

            for (int i = 0; i < masks; i++) {
               int[] get = GameResources.sceneShader.getOffset(i);
               lastOffsets[i] = get;
               GameResources.sceneShader.passMaskOffset(i, get[0] + xOffset, get[1] + yOffset);
            }
         }

         @Override
         public void stop() {
            int masks = usedMasks.get();

            for (int i = 0; i < masks; i++) {
               int[] last = lastOffsets[i];
               GameResources.sceneShader.passMaskOffset(i, last[0], last[1]);
            }
         }
      });
   }

   public MaskShaderOptions addMask(GameTexture mask, int maskXOffset, int maskYOffset) {
      return this.addMask(new SpriteMaskTextureOptions(mask, maskXOffset, maskYOffset));
   }

   public MaskShaderOptions copyAndAddMask(GameTexture mask, int maskXOffset, int maskYOffset) {
      return new MaskShaderOptions(this).addMask(new SpriteMaskTextureOptions(mask, maskXOffset, maskYOffset));
   }

   public MaskShaderOptions addMask(SpriteMaskOptions options) {
      this.masks.add(options);
      return this;
   }

   public MaskShaderOptions copyAndAddMask(SpriteMaskOptions options) {
      return new MaskShaderOptions(this).addMask(options);
   }

   public TextureDrawOptionsEnd apply(TextureDrawOptionsEnd options) {
      options = options.addTranslatePos(this.drawXOffset, this.drawYOffset);
      options = options.addShaderState(this);

      for (int i = 0; i < this.masks.size(); i++) {
         SpriteMaskOptions mask = this.masks.get(i);
         options = mask.apply(options, i + 3);
      }

      for (ShaderState maskOffsetStage : this.maskOffsets) {
         options.addShaderState(maskOffsetStage);
      }

      this.maskOffsets.clear();
      return options;
   }

   @Override
   public void use() {
      GameResources.sceneShader.use();
      GameResources.sceneShader.passMaskCount(this.masks.size());

      for (int i = 0; i < this.masks.size(); i++) {
         this.masks.get(i).use(GameResources.sceneShader, i);
      }
   }

   @Override
   public void stop() {
      GameResources.sceneShader.passMaskCount(0);
      GameResources.sceneShader.stop();
   }
}
