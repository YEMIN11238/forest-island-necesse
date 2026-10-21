package necesse.gfx.fairType;

import java.awt.Color;
import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import necesse.engine.input.Control;
import necesse.engine.input.Input;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.network.NetworkClient;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameUtils;
import necesse.engine.util.LevelIdentifier;
import necesse.gfx.GameColor;
import necesse.gfx.fairType.parsers.GameEmoteParser;
import necesse.gfx.fairType.parsers.HelpIconParser;
import necesse.gfx.fairType.parsers.TeleportParser;
import necesse.gfx.fairType.parsers.TypeItemParser;
import necesse.gfx.fairType.parsers.TypeMobParser;
import necesse.gfx.fairType.parsers.TypeParser;
import necesse.gfx.fairType.parsers.TypeParserMatcherResult;
import necesse.gfx.fairType.parsers.TypeParserResult;
import necesse.gfx.fairType.parsers.TypeRegexParser;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.InventoryItem;

public class TypeParsers {
   public static final TypeRegexParser GAME_COLOR = new TypeRegexParser("§([!0-9a-z]|#([0-9a-fA-F]{6}|[0-9a-fA-F]{3}))") {
      public FairGlyph[] parse(TypeParserMatcherResult result, FairGlyph[] oldGlyphs) {
         String s = result.matcher.group();
         Supplier<Color> colorSupplier = GameColor.parseColorSupplierString(s);
         return new FairGlyph[]{new FairColorChangeGlyph(s, colorSupplier)};
      }
   };
   public static final TypeRegexParser STRIP_GAME_COLOR = new TypeRegexParser("§([!0-9a-z]|#([0-9a-fA-F]{6}|[0-9a-fA-F]{3}))") {
      public FairGlyph[] parse(TypeParserMatcherResult result, FairGlyph[] oldGlyphs) {
         return new FairGlyph[0];
      }
   };
   private static final Pattern URL_PATTERN = Pattern.compile(
      "(?i)\\b((?:https?|ftp)://)?(?:\\S+(?::\\S*)?@)?(?:(?!(?:10|127)(?:\\.\\d{1,3}){3})(?!(?:169\\.254|192\\.168)(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)(?:\\.(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)*(?:\\.(?:[a-z\\u00a1-\\uffff]{2,}))\\.?)(?::\\d{2,5})?(?:[/?#]\\S*)?\\b"
   );
   public static final TypeRegexParser URL_OPEN = new TypeRegexParser(URL_PATTERN) {
      public FairGlyph[] parse(TypeParserMatcherResult result, FairGlyph[] oldGlyphs) {
         String s = result.matcher.group();
         if (result.matcher.groupCount() > 0 && result.matcher.group(1) == null) {
            s = "http://" + s;
         }

         try {
            URI uri = URI.create(s);
            FairGlyph[] out = new FairGlyph[oldGlyphs.length];

            for (int i = 0; i < out.length; i++) {
               if (oldGlyphs[i] instanceof FairURLLinkGlyph) {
                  FairURLLinkGlyph old = (FairURLLinkGlyph)oldGlyphs[i];
                  out[i] = new FairURLLinkGlyph(old.glyph, uri, true);
               } else {
                  out[i] = new FairURLLinkGlyph(oldGlyphs[i], uri, true);
               }
            }

            return out;
         } catch (Exception e) {
            e.printStackTrace();
            return oldGlyphs;
         }
      }
   };
   public static final TypeParser<TypeParserResult> REMOVE_URL = new TypeParser<TypeParserResult>() {
      @Override
      public TypeParserResult getMatchResult(FairGlyph[] glyphs, int startIndex) {
         for (int i = startIndex; i < glyphs.length; i++) {
            if (glyphs[i] instanceof FairURLLinkGlyph) {
               int length = 1;

               while (i + length < glyphs.length && glyphs[i + length] instanceof FairURLLinkGlyph && ((FairURLLinkGlyph)glyphs[i + length]).parsedGlyph) {
                  length++;
               }

               return new TypeParserResult(i, i + length);
            }
         }

         return null;
      }

      @Override
      public FairGlyph[] parse(TypeParserResult result, FairGlyph[] oldGlyphs) {
         FairGlyph[] out = new FairGlyph[oldGlyphs.length];

         for (int i = 0; i < oldGlyphs.length; i++) {
            out[i] = ((FairURLLinkGlyph)oldGlyphs[i]).glyph;
         }

         return out;
      }
   };
   private static final Pattern MARKDOWN_URL_PATTERN = Pattern.compile("\\[(.+)]\\((" + URL_PATTERN + ")\\)");
   public static final TypeRegexParser MARKDOWN_URL = new TypeRegexParser(MARKDOWN_URL_PATTERN) {
      public FairGlyph[] parse(TypeParserMatcherResult result, FairGlyph[] oldGlyphs) {
         String textGroup = result.matcher.group(1);
         String urlGroup = result.matcher.group(2);
         if (result.matcher.groupCount() > 2 && result.matcher.group(3) == null) {
            urlGroup = "http://" + urlGroup;
         }

         try {
            int textStartIndex = result.matcher.start(1) - result.start;
            URI uri = URI.create(urlGroup);
            FairGlyph[] out = new FairGlyph[textGroup.length()];

            for (int i = 0; i < out.length; i++) {
               FairGlyph last = oldGlyphs[i + textStartIndex];
               if (last instanceof FairURLLinkGlyph) {
                  FairURLLinkGlyph lastURL = (FairURLLinkGlyph)last;
                  out[i] = new FairURLLinkGlyph(lastURL.glyph, uri, false);
               } else {
                  out[i] = new FairURLLinkGlyph(last, uri, false);
               }
            }

            return out;
         } catch (Exception e) {
            e.printStackTrace();
            return oldGlyphs;
         }
      }
   };
   public static final Pattern INPUT_PATTERN = Pattern.compile("\\[input=(?:(-?\\d+)|(\\w+))]");
   public static final Pattern LINE_HEIGHT_PATTERN = Pattern.compile("\\[lh=(\\d+)]");

   public static TypeParser<?>[] BasicParsers(FontOptions fontOptions) {
      return new TypeParser[]{
         GAME_COLOR,
         GameEmote(fontOptions.getSize()),
         ItemIcon(fontOptions.getSize()),
         MobIcon(fontOptions.getSize()),
         InputIcon(fontOptions),
         HelpIcon(fontOptions)
      };
   }

   public static TypeParser<?>[] BasicAndURLParsers(FontOptions fontOptions) {
      return new TypeParser[]{
         GAME_COLOR,
         GameEmote(fontOptions.getSize()),
         ItemIcon(fontOptions.getSize()),
         MobIcon(fontOptions.getSize()),
         InputIcon(fontOptions),
         HelpIcon(fontOptions),
         URL_OPEN,
         MARKDOWN_URL
      };
   }

   public static TypeItemParser ItemIcon(int size) {
      return ItemIcon(size, true);
   }

   public static TypeItemParser ItemIcon(int size, boolean allowGND) {
      return ItemIcon(size, allowGND, null);
   }

   public static TypeItemParser ItemIcon(int size, boolean allowGND, Function<FairItemGlyph, FairItemGlyph> modder) {
      return new TypeItemParser(size, allowGND, modder);
   }

   public static String getItemParseString(String itemStringID) {
      return "[item=" + itemStringID + "]";
   }

   public static String getItemParseString(InventoryItem item) {
      StringBuilder builder = new StringBuilder();
      builder.append("[item=").append(item.item.getStringID()).append("]");
      if (item.getGndData().getMapSize() > 0) {
         SaveData data = new SaveData("");
         item.getGndData().addSaveData(data);
         builder.append(data.getScript(true));
      }

      return builder.toString();
   }

   public static String getItemsParseString(List<InventoryItem> items) {
      String itemsString = GameUtils.join(items.toArray(new InventoryItem[0]), item -> {
         String gndString = null;
         if (item.getGndData().getMapSize() > 0) {
            SaveData data = new SaveData("");
            item.getGndData().addSaveData(data);
            gndString = data.getScript(true);
         }

         return item.item.getStringID() + (gndString == null ? "" : gndString);
      }, ",");
      return "[items=" + itemsString + "]";
   }

   public static GameMessageBuilder getItemAndNameParseString(String itemStringID, String wrap) {
      GameMessageBuilder builder = new GameMessageBuilder();
      if (wrap != null) {
         builder.append(wrap);
      }

      builder.append(getItemParseString(itemStringID) + " ");
      builder.append(ItemRegistry.getLocalization(itemStringID));
      if (wrap != null) {
         builder.append(wrap);
      }

      return builder;
   }

   public static GameMessageBuilder getItemAndNameParseString(String itemStringID) {
      return getItemAndNameParseString(itemStringID, null);
   }

   public static GameMessageBuilder getItemAndNameParseString(InventoryItem item, String wrap, boolean addAmount) {
      GameMessageBuilder builder = new GameMessageBuilder();
      if (wrap != null) {
         builder.append(wrap);
      }

      if (addAmount && item.getAmount() > 1) {
         builder.append(item.getAmount() + "");
      }

      builder.append(getItemParseString(item) + " ");
      builder.append(item.getItemLocalization());
      if (wrap != null) {
         builder.append(wrap);
      }

      return builder;
   }

   public static GameMessageBuilder getItemAndNameParseString(InventoryItem item, String wrap) {
      return getItemAndNameParseString(item, wrap, true);
   }

   public static GameMessageBuilder getItemAndNameParseString(InventoryItem item, boolean addAmount) {
      return getItemAndNameParseString(item, null, addAmount);
   }

   public static GameMessageBuilder getItemAndNameParseString(InventoryItem item) {
      return getItemAndNameParseString(item, true);
   }

   public static TypeMobParser MobIcon(int size) {
      return MobIcon(size, null);
   }

   public static TypeMobParser MobIcon(int size, Function<FairMobGlyph, FairMobGlyph> modder) {
      return new TypeMobParser(size, modder);
   }

   public static String getMobParseString(String mobStringID) {
      return "[mob=" + mobStringID + "]";
   }

   public static String getMobParseString(int mobID) {
      return getMobParseString(MobRegistry.getMobStringID(mobID));
   }

   public static GameMessageBuilder getMobAndNameParseString(String mobStringID, String wrap) {
      GameMessageBuilder builder = new GameMessageBuilder();
      if (wrap != null) {
         builder.append(wrap);
      }

      builder.append(getMobParseString(mobStringID) + " ");
      builder.append(MobRegistry.getLocalization(mobStringID));
      if (wrap != null) {
         builder.append(wrap);
      }

      return builder;
   }

   public static GameMessageBuilder getMobAndNameParseString(String mobStringID) {
      return getMobAndNameParseString(mobStringID, null);
   }

   public static String getMobsParseString(List<String> mobStringIDs) {
      return "[mobs=" + GameUtils.join(mobStringIDs.toArray(), ",") + "]";
   }

   public static TypeRegexParser InputIcon(final FontOptions fontOptions) {
      return new TypeRegexParser(INPUT_PATTERN) {
         public FairGlyph[] parse(TypeParserMatcherResult result, FairGlyph[] oldGlyphs) {
            String inputKeyGroup = result.matcher.group(1);
            if (inputKeyGroup != null) {
               try {
                  int inputKey = Integer.parseInt(inputKeyGroup);
                  String name = Input.getName(inputKey);
                  return name.equals("N/A") ? oldGlyphs : new FairGlyph[]{new FairInputKeyGlyph(fontOptions, inputKey, name)};
               } catch (NumberFormatException e) {
                  return oldGlyphs;
               }
            } else {
               Control control = Control.getControl(result.matcher.group(2));
               return control != null ? new FairGlyph[]{new FairControlKeyGlyph(fontOptions, control)} : oldGlyphs;
            }
         }
      };
   }

   public static String getInputParseString(int inputKey) {
      return "[input=" + inputKey + "]";
   }

   public static String getInputParseString(Control control) {
      return "[input=" + control.id + "]";
   }

   public static HelpIconParser HelpIcon(FontOptions fontOptions) {
      return new HelpIconParser(fontOptions);
   }

   public static String getHelpParseString(String helpKey, String displayName) {
      StringBuilder builder = new StringBuilder();
      builder.append("[help=").append(helpKey).append("]");
      if (displayName != null && !displayName.isEmpty()) {
         builder.append("{").append(displayName).append("}");
      }

      return builder.toString();
   }

   public static String getHelpParseString(String helpKey) {
      return getHelpParseString(helpKey, null);
   }

   public static GameEmoteParser GameEmote(int size) {
      return new GameEmoteParser(size);
   }

   public static String getGameEmoteParseString(String stringID) {
      return ":" + stringID + ":";
   }

   public static TypeParser<TypeParserResult> replaceParser(final String match, final FairGlyph... replace) {
      return new TypeParser<TypeParserResult>() {
         @Override
         public TypeParserResult getMatchResult(FairGlyph[] glyphs, int startIndex) {
            StringBuilder builder = new StringBuilder();

            for (FairGlyph glyph : glyphs) {
               builder.append(glyph.getCharacter());
            }

            int nextIndex = builder.toString().indexOf(match);
            return nextIndex == -1 ? null : new TypeParserResult(nextIndex, nextIndex + match.length());
         }

         @Override
         public FairGlyph[] parse(TypeParserResult result, FairGlyph[] oldGlyphs) {
            return replace;
         }
      };
   }

   public static TypeRegexParser replaceParserRegex(String match, FairGlyph... replace) {
      return replaceParserRegex(Pattern.compile(match), replace);
   }

   public static TypeRegexParser replaceParserRegex(Pattern match, FairGlyph... replace) {
      return replaceParserRegex(match, result -> replace);
   }

   public static TypeRegexParser replaceParserRegex(String match, Function<TypeParserMatcherResult, FairGlyph[]> replaceFunction) {
      return replaceParserRegex(Pattern.compile(match), replaceFunction);
   }

   public static TypeRegexParser replaceParserRegex(Pattern match, final Function<TypeParserMatcherResult, FairGlyph[]> replaceFunction) {
      return new TypeRegexParser(match) {
         @Override
         public TypeParserMatcherResult getMatchResult(FairGlyph[] glyphs, int startIndex) {
            return super.getMatchResult(glyphs, startIndex);
         }

         public FairGlyph[] parse(TypeParserMatcherResult result, FairGlyph[] oldGlyphs) {
            return replaceFunction.apply(result);
         }
      };
   }

   public static TypeParser<TypeParserResult> headerParser(
      final String startTag, final String endTag, final boolean removeEndTag, final FontOptions fontOptions
   ) {
      return new TypeParser<TypeParserResult>() {
         @Override
         public TypeParserResult getMatchResult(FairGlyph[] glyphs, int startIndex) {
            int start = getIndexOf(glyphs, startTag, startIndex);
            if (start == -1) {
               return null;
            }

            int end = getIndexOf(glyphs, endTag, start + startTag.length());
            return end == -1 ? null : new TypeParserResult(start, end + endTag.length());
         }

         @Override
         public FairGlyph[] parse(TypeParserResult result, FairGlyph[] oldGlyphs) {
            FairGlyph[] out = new FairGlyph[oldGlyphs.length - startTag.length() + 1 - (removeEndTag ? endTag.length() - 1 : 0)];
            out[0] = new FairInvisibleTextParseGlyph(startTag);
            if (removeEndTag) {
               out[out.length - 1] = new FairInvisibleTextParseGlyph(endTag);
            }

            for (int i = 1; i < out.length - (removeEndTag ? 1 : 0); i++) {
               out[i] = new FairCharacterGlyph(fontOptions, oldGlyphs[i - 1 + startTag.length()].getCharacter());
            }

            return out;
         }
      };
   }

   public static TypeParser<TypeParserResult> highlightParser(
      final String startTag, final String endTag, final boolean removeEndTag, final Supplier<Color> highlightColor
   ) {
      return new TypeParser<TypeParserResult>() {
         @Override
         public TypeParserResult getMatchResult(FairGlyph[] glyphs, int startIndex) {
            int start = getIndexOf(glyphs, startTag, startIndex);
            if (start == -1) {
               return null;
            }

            int end = getIndexOf(glyphs, endTag, start + startTag.length());
            return end == -1 ? null : new TypeParserResult(start, end + endTag.length());
         }

         @Override
         public FairGlyph[] parse(TypeParserResult result, FairGlyph[] oldGlyphs) {
            FairGlyph[] out = new FairGlyph[oldGlyphs.length - startTag.length() + 1 - (removeEndTag ? endTag.length() - 1 : 0)];
            out[0] = new FairInvisibleTextParseGlyph(startTag);
            if (removeEndTag) {
               out[out.length - 1] = new FairInvisibleTextParseGlyph(endTag);
            }

            for (int i = 1; i < out.length - (removeEndTag ? 1 : 0); i++) {
               out[i] = new FairDefaultColorGlyph(oldGlyphs[i - 1 + startTag.length()], highlightColor);
            }

            return out;
         }
      };
   }

   public static TypeRegexParser lineHeightParser(final int startHeight) {
      return new TypeRegexParser(LINE_HEIGHT_PATTERN) {
         public FairGlyph[] parse(TypeParserMatcherResult result, FairGlyph[] oldGlyphs) {
            String heightGroup = result.matcher.group(1);
            if (heightGroup != null) {
               try {
                  int height = Integer.parseInt(heightGroup);
                  return new FairGlyph[]{new FairSpacerGlyph(0.0F, startHeight + height)};
               } catch (NumberFormatException e) {
                  return oldGlyphs;
               }
            } else {
               return oldGlyphs;
            }
         }
      };
   }

   public static String getLineHeightString(int height) {
      return "[lh=" + height + "]";
   }

   public static TeleportParser Teleport(FontOptions fontOptions) {
      return new TeleportParser(fontOptions);
   }

   public static String getTeleportParseString(LevelIdentifier levelIdentifier) {
      return "[setlevel=" + levelIdentifier.stringID + "]";
   }

   public static String getTeleportParseString(LevelIdentifier levelIdentifier, int tileX, int tileY) {
      return "[setposition=" + levelIdentifier.stringID + "," + tileX + "x" + tileY + "]";
   }

   public static String getTeleportParseString(NetworkClient client) {
      return "[teleport=" + client.getName() + "]";
   }
}
