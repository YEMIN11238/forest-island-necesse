package necesse.engine.network.packet;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.gfx.GameColor;
import necesse.gfx.gameFont.FontOptions;
import necesse.level.maps.hudManager.floatText.ChatBubbleText;
import necesse.level.maps.hudManager.floatText.FloatTextFade;

public class PacketMobChat extends Packet {
   public final int mobUniqueID;
   public final GameMessage message;
   public final boolean isFloatTextElement;

   public PacketMobChat(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.mobUniqueID = reader.getNextInt();
      this.message = GameMessage.fromContentPacket(reader.getNextContentPacket());
      this.isFloatTextElement = reader.getNextBoolean();
   }

   public PacketMobChat(int mobUniqueID, GameMessage message, boolean isFloatTextElement) {
      this.mobUniqueID = mobUniqueID;
      this.message = message;
      this.isFloatTextElement = isFloatTextElement;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextInt(mobUniqueID);
      writer.putNextContentPacket(message.getContentPacket());
      writer.putNextBoolean(isFloatTextElement);
   }

   public PacketMobChat(int mobUniqueID, GameMessage message) {
      this(mobUniqueID, message, false);
   }

   public PacketMobChat(int mobUniqueID, String message) {
      this(mobUniqueID, new StaticMessage(message), false);
   }

   public PacketMobChat(int mobUniqueID, String category, String translationKey) {
      this(mobUniqueID, new LocalMessage(category, translationKey), false);
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      if (client.getLevel() != null) {
         final Mob chatter = GameUtils.getLevelMob(this.mobUniqueID, client.getLevel());
         if (chatter != null) {
            if (this.isFloatTextElement) {
               String stringMessage = GameColor.stripCodes(this.message.translate());
               FloatTextFade text = new FloatTextFade(chatter.getX(), chatter.getY(), stringMessage, new FontOptions(16).outline()) {
                  @Override
                  public int getAnchorX() {
                     return chatter.getX();
                  }

                  @Override
                  public int getAnchorY() {
                     return chatter.getY();
                  }
               };
               int hoverTime = stringMessage.length() * 100;
               if (hoverTime < 3000) {
                  hoverTime = 3000;
               } else if (hoverTime > 6000) {
                  hoverTime = 6000;
               }

               text.hoverTime = hoverTime;
               text.riseTime = 1000;
               chatter.getLevel().hudManager.addElement(text);
            } else {
               chatter.getLevel().hudManager.addElement(new ChatBubbleText(chatter, GameColor.stripCodes(this.message.translate())));
            }
         }
      }
   }
}
