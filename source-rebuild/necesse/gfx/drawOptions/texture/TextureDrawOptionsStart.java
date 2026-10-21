package necesse.gfx.drawOptions.texture;

public class TextureDrawOptionsStart extends TextureDrawOptionsEnd {
   TextureDrawOptionsStart(TextureDrawOptions other) {
      super(other);
   }

   public TextureDrawOptionsStart copy() {
      return new TextureDrawOptionsStart(super.copy());
   }

   public TextureDrawOptionsEnd section(int startX, int endX, int startY, int endY) {
      this.opts.spriteX1 = pixel(startX, this.texture.getWidth());
      this.opts.spriteX2 = pixel(endX, this.texture.getWidth());
      this.opts.spriteX3 = this.opts.spriteX2;
      this.opts.spriteX4 = this.opts.spriteX1;
      this.opts.spriteY1 = pixel(startY, this.texture.getHeight());
      this.opts.spriteY2 = this.opts.spriteY1;
      this.opts.spriteY3 = pixel(endY, this.texture.getHeight());
      this.opts.spriteY4 = this.opts.spriteY3;
      this.opts.spriteWidth = (float)Math.abs(endX - startX) / this.texture.getWidth();
      this.opts.spriteHeight = (float)Math.abs(endY - startY) / this.texture.getHeight();
      this.opts.width = Math.abs(endX - startX);
      this.opts.height = Math.abs(endY - startY);
      return new TextureDrawOptionsEnd(this);
   }

   public TextureDrawOptionsEnd sprite(int spriteX, int spriteY, int spriteRes) {
      return this.sprite(spriteX, spriteY, spriteRes, spriteRes);
   }

   public TextureDrawOptionsEnd sprite(int spriteX, int spriteY, int spriteWidth, int spriteHeight) {
      this.opts.spriteX1 = pixel(spriteX, spriteWidth, this.texture.getWidth());
      this.opts.spriteX2 = pixel(spriteX + 1, spriteWidth, this.texture.getWidth());
      this.opts.spriteX3 = this.opts.spriteX2;
      this.opts.spriteX4 = this.opts.spriteX1;
      this.opts.spriteY1 = pixel(spriteY, spriteHeight, this.texture.getHeight());
      this.opts.spriteY2 = this.opts.spriteY1;
      this.opts.spriteY3 = pixel(spriteY + 1, spriteHeight, this.texture.getHeight());
      this.opts.spriteY4 = this.opts.spriteY3;
      this.opts.spriteWidth = (float)spriteWidth / this.texture.getWidth();
      this.opts.spriteHeight = (float)spriteHeight / this.texture.getHeight();
      this.opts.width = spriteWidth;
      this.opts.height = spriteHeight;
      return new TextureDrawOptionsEnd(this);
   }

   public TextureDrawOptionsEnd spriteSection(
      int spriteX, int spriteY, int spriteWidth, int spriteHeight, int startX, int endX, int startY, int endY, boolean translatePos
   ) {
      this.opts.spriteX1 = pixel(spriteX, startX, spriteWidth, this.texture.getWidth());
      this.opts.spriteX2 = pixel(spriteX, endX, spriteWidth, this.texture.getWidth());
      this.opts.spriteX3 = this.opts.spriteX2;
      this.opts.spriteX4 = this.opts.spriteX1;
      this.opts.spriteY1 = pixel(spriteY, startY, spriteHeight, this.texture.getHeight());
      this.opts.spriteY2 = this.opts.spriteY1;
      this.opts.spriteY3 = pixel(spriteY, endY, spriteHeight, this.texture.getHeight());
      this.opts.spriteY4 = this.opts.spriteY3;
      this.opts.spriteWidth = (float)spriteWidth / this.texture.getWidth();
      this.opts.spriteHeight = (float)spriteHeight / this.texture.getHeight();
      if (translatePos) {
         this.opts.translateX = startX;
         this.opts.translateY = startY;
      }

      this.opts.width = Math.abs(endX - startX);
      this.opts.height = Math.abs(endY - startY);
      return new TextureDrawOptionsEnd(this);
   }

   public TextureDrawOptionsEnd spriteSection(int spriteX, int spriteY, int spriteRes, int startX, int endX, int startY, int endY, boolean translatePos) {
      return this.spriteSection(spriteX, spriteY, spriteRes, spriteRes, startX, endX, startY, endY, translatePos);
   }

   public TextureDrawOptionsEnd spriteSection(int spriteX, int spriteY, int spriteWidth, int spriteHeight, int startX, int endX, int startY, int endY) {
      return this.spriteSection(spriteX, spriteY, spriteWidth, spriteHeight, startX, endX, startY, endY, true);
   }

   public TextureDrawOptionsEnd spriteSection(int spriteX, int spriteY, int spriteRes, int startX, int endX, int startY, int endY) {
      return this.spriteSection(spriteX, spriteY, spriteRes, spriteRes, startX, endX, startY, endY);
   }

   public TextureDrawOptionsEnd next() {
      return new TextureDrawOptionsEnd(this);
   }
}
