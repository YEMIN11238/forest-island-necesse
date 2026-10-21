package necesse.gfx.gameIcon;

import java.awt.Color;
import java.awt.Dimension;
import necesse.engine.input.InputEvent;
import necesse.engine.registries.IDData;
import necesse.engine.registries.IDDataContainer;

public abstract class GameEmote implements IDDataContainer {
   public final IDData idData = new IDData();

   @Override
   public final String getStringID() {
      return this.idData.getStringID();
   }

   @Override
   public final int getID() {
      return this.idData.getID();
   }

   @Override
   public IDData getIDData() {
      return this.idData;
   }

   public void loadTextures() {
   }

   public abstract Dimension getDrawBoundingBox(int var1);

   public abstract void drawIcon(int var1, int var2, boolean var3, Color var4, int var5);

   public void handleEvent(InputEvent event) {
   }

   public String getEmoteParseStringID() {
      return this.getStringID();
   }
}
