package necesse.gfx.fairType.parsers;

import java.util.function.Function;
import necesse.engine.GlobalData;
import necesse.engine.network.client.Client;
import necesse.engine.network.client.ClientClient;
import necesse.engine.network.packet.PacketChatMessage;
import necesse.engine.state.MainGame;
import necesse.engine.state.State;
import necesse.gfx.fairType.FairCharacterGlyph;
import necesse.gfx.fairType.FairGlyph;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.StringTooltips;

public class TeleportParser extends TypeParser<TeleportParser.TargetTypeParserResult> {
   public final FontOptions fontOptions;

   public TeleportParser(FontOptions fontOptions) {
      this.fontOptions = fontOptions;
   }

   public TeleportParser.TargetTypeParserResult getMatchResult(FairGlyph[] glyphs, int startIndex) {
      TeleportParser.TargetTypeParserResult teleportResult = this.checkTeleportResult(glyphs, startIndex);
      if (teleportResult != null) {
         return teleportResult;
      }

      TeleportParser.TargetTypeParserResult setLevelResult = this.checkSetLevelResult(glyphs, startIndex);
      return setLevelResult != null ? setLevelResult : this.checkSetPositionResult(glyphs, startIndex);
   }

   protected TeleportParser.TargetTypeParserResult checkTeleportResult(FairGlyph[] glyphs, int startIndex) {
      String prefix = "[teleport=";
      int start = getIndexOf(glyphs, prefix, startIndex);
      if (start == -1) {
         return null;
      }

      int end = getIndexOf(glyphs, "]", startIndex + prefix.length());
      if (end == -1) {
         return null;
      }

      String target = subString(glyphs, start + prefix.length(), end - 1);
      Function<ClientClient, String> chatCommandGetter = client -> "/tp \"" + client.getName() + "\" " + target;
      return new TeleportParser.TargetTypeParserResult(start, end + 1, target, chatCommandGetter);
   }

   protected TeleportParser.TargetTypeParserResult checkSetLevelResult(FairGlyph[] glyphs, int startIndex) {
      String prefix = "[setlevel=";
      int start = getIndexOf(glyphs, prefix, startIndex);
      if (start == -1) {
         return null;
      }

      int end = getIndexOf(glyphs, "]", startIndex + prefix.length());
      if (end == -1) {
         return null;
      }

      String target = subString(glyphs, start + prefix.length(), end - 1);
      Function<ClientClient, String> chatCommandGetter = client -> "/setlevel \"" + client.getName() + "\" " + target;
      return new TeleportParser.TargetTypeParserResult(start, end + 1, target, chatCommandGetter);
   }

   protected TeleportParser.TargetTypeParserResult checkSetPositionResult(FairGlyph[] glyphs, int startIndex) {
      String prefix = "[setposition=";
      int start = getIndexOf(glyphs, prefix, startIndex);
      if (start == -1) {
         return null;
      }

      int positionSplit = getIndexOf(glyphs, ",", startIndex + prefix.length());
      if (positionSplit == -1) {
         return null;
      }

      int tileSplit = getIndexOf(glyphs, "x", positionSplit);
      if (tileSplit == -1) {
         return null;
      }

      int end = getIndexOf(glyphs, "]", positionSplit);
      if (end == -1) {
         return null;
      }

      String levelIdentifierString = subString(glyphs, start + prefix.length(), positionSplit - 1);
      String tileXString = subString(glyphs, positionSplit + 1, tileSplit - 1);
      String tileYString = subString(glyphs, tileSplit + 1, end - 1);
      Function<ClientClient, String> chatCommandGetter = client -> "/setposition \""
         + client.getName()
         + "\" "
         + levelIdentifierString
         + " "
         + tileXString
         + " "
         + tileYString;
      return new TeleportParser.TargetTypeParserResult(start, end + 1, levelIdentifierString + " tile " + tileXString + "x" + tileYString, chatCommandGetter);
   }

   public FairGlyph[] parse(TeleportParser.TargetTypeParserResult result, FairGlyph[] oldGlyphs) {
      return FairCharacterGlyph.fromString(this.fontOptions, result.displayString, e -> {
         if (e.getID() == -100) {
            if (!e.state) {
               State currentState = GlobalData.getCurrentState();
               if (currentState instanceof MainGame) {
                  Client client = ((MainGame)currentState).getClient();
                  if (client != null) {
                     client.network.sendPacket(new PacketChatMessage(client.getSlot(), result.chatCommandGetter.apply(client.getClient())));
                  }
               }
            }

            return true;
         } else {
            return false;
         }
      }, () -> new StringTooltips("Teleport to " + result.displayString));
   }

   public static class TargetTypeParserResult extends TypeParserResult {
      public final String displayString;
      public final Function<ClientClient, String> chatCommandGetter;

      public TargetTypeParserResult(int start, int end, String displayString, Function<ClientClient, String> chatCommandGetter) {
         super(start, end);
         this.displayString = displayString;
         this.chatCommandGetter = chatCommandGetter;
      }
   }
}
