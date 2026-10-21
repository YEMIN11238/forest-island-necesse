package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Color;
import necesse.engine.localization.Localization;
import necesse.engine.network.client.Client;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.gfx.gameFont.FontOptions;
import necesse.level.maps.hudManager.floatText.UniqueFloatText;

public class CoreKeeperCoreChatLevelEvent extends MobAbilityLevelEvent {
   public CoreKeeperCoreChatLevelEvent() {
   }

   public CoreKeeperCoreChatLevelEvent(Mob owner, GameRandom uniqueIDRandom) {
      super(owner, uniqueIDRandom);
   }

   @Override
   public void init() {
      super.init();
      if (this.owner != null && !this.isServer()) {
         Client client = this.getClient();
         boolean isMe = client != null && this.owner == client.getPlayer();
         FontOptions fontOptions = new FontOptions(16).outline().color(new Color(58, 211, 176));
         UniqueFloatText text = new UniqueFloatText(
            this.owner.getX(),
            this.owner.getY(),
            Localization.translate("ui", "thecoredamagetaken"),
            fontOptions,
            isMe ? "thought" : "thought" + this.owner.getUniqueID()
         ) {
            @Override
            public int getAnchorX() {
               return CoreKeeperCoreChatLevelEvent.this.owner.getX();
            }

            @Override
            public int getAnchorY() {
               return CoreKeeperCoreChatLevelEvent.this.owner.getY();
            }
         };
         text.hoverTime = 4500;
         text.riseTime = 500;
         this.level.hudManager.addElement(text);
      }

      this.over();
   }
}
