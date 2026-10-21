package necesse.engine.util;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.client.Client;
import necesse.engine.network.client.ClientClient;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameColor;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.level.gameObject.ObjectHoverHitbox;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.LevelObjectHit;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

public final class GameUtils {
   public static final GameUtils.MetricValue[] normalMetricSystem = new GameUtils.MetricValue[]{
      new GameUtils.MetricValue("G", 9),
      new GameUtils.MetricValue("M", 6),
      new GameUtils.MetricValue("k", 3),
      new GameUtils.MetricValue("", 0),
      new GameUtils.MetricValue("m", -3),
      new GameUtils.MetricValue("u", -6),
      new GameUtils.MetricValue("n", -9)
   };
   public static Pattern playerNameSymbolsPattern = Pattern.compile("[\\p{L}\\p{N} ]+");
   public static final String[] invalidFileNameSequences = new String[]{"\\", "/", ":", "*", "?", "\"", "<", ">", "|", "\u0000"};
   public static final Pattern validFileNamePattern = Pattern.compile(
      "[^" + Stream.of(invalidFileNameSequences).map(c -> "\\" + c).reduce("", (s1, s2) -> s1 + s2) + "]+"
   );

   private GameUtils() {
      throw new IllegalStateException("GameUtils cannot be instantiated");
   }

   public static boolean deleteFileOrFolder(String path) {
      return deleteFileOrFolder(new File(path));
   }

   public static boolean deleteFileOrFolder(File file) {
      if (!file.isDirectory()) {
         return file.delete();
      }

      File[] files = file.listFiles();
      if (files != null) {
         for (File other : files) {
            if (!deleteFileOrFolder(other.getPath())) {
               return false;
            }
         }
      }

      return file.delete();
   }

   private static void _copyFileOrFolder(File file, File target, CopyOption... options) throws IOException {
      if (!file.isDirectory() || !target.isDirectory()) {
         Files.copy(file.toPath(), target.toPath(), options);
      }

      if (file.isDirectory()) {
         File[] files = file.listFiles();
         if (files != null) {
            for (File other : files) {
               _copyFileOrFolder(other, resolveFile(target, other.getName()), options);
            }
         }
      }
   }

   public static void copyFileOrFolder(File file, File target, CopyOption... options) throws IOException {
      if (file.isFile()) {
         File absoluteParent = target.getAbsoluteFile().getParentFile();
         if (absoluteParent != null && !absoluteParent.exists() && !absoluteParent.mkdirs()) {
            throw new IOException("Could not make target directories for move");
         }
      } else {
         File absoluteDir = target.getAbsoluteFile();
         if (!absoluteDir.exists() && !absoluteDir.mkdirs()) {
            throw new IOException("Could not make target directories for move");
         }
      }

      _copyFileOrFolder(file, target, options);
   }

   public static void copyFileOrFolderReplaceExisting(File file, File target) throws IOException {
      copyFileOrFolder(file, target, StandardCopyOption.REPLACE_EXISTING);
   }

   private static void _moveFileOrFolder(File file, File target, CopyOption... options) throws IOException {
      if (!file.isDirectory() || !target.isDirectory()) {
         Files.move(file.toPath(), target.toPath(), options);
      }

      if (file.isDirectory()) {
         File[] files = file.listFiles();
         if (files != null) {
            for (File other : files) {
               _moveFileOrFolder(other, resolveFile(target, other.getName()), options);
            }
         }
      }
   }

   public static void moveFileOrFolder(File file, File target, CopyOption... options) throws IOException {
      if (file.isFile()) {
         File absoluteParent = target.getAbsoluteFile().getParentFile();
         if (absoluteParent != null && !absoluteParent.exists() && !absoluteParent.mkdirs()) {
            throw new IOException("Could not make target directories for move");
         }
      } else {
         File absoluteDir = target.getAbsoluteFile();
         if (!absoluteDir.exists() && !absoluteDir.mkdirs()) {
            throw new IOException("Could not make target directories for move");
         }
      }

      _moveFileOrFolder(file, target, options);
   }

   public static void moveFileOrFolderReplaceExisting(File file, File target) throws IOException {
      moveFileOrFolder(file, target, StandardCopyOption.REPLACE_EXISTING);
   }

   public static File resolveFile(File parentFile, String siblingName) {
      return parentFile.toPath().resolve(siblingName).toFile();
   }

   public static void collectFiles(File file, Collection<File> files, Predicate<File> test) {
      collectFiles(file, files, test, false);
   }

   public static void collectFiles(File file, Collection<File> files, Predicate<File> test, boolean testFolders) {
      if (file.isDirectory()) {
         if (testFolders && test != null && !test.test(file)) {
            return;
         }

         File[] children = file.listFiles();
         if (children == null) {
            throw new NullPointerException("Could not read files inside folder " + file.getPath());
         }

         for (File child : children) {
            collectFiles(child, files, test, testFolders);
         }
      } else {
         if (test != null && !test.test(file)) {
            return;
         }

         files.add(file);
      }
   }

   public static String getNextUniqueFilename(String path, String suffix, String extension) {
      return getNextUniqueFilename(path, suffix, extension, s -> new File(s).exists());
   }

   public static String getNextUniqueFilename(String path, String suffix, String extension, Function<String, Boolean> fileExists) {
      int count = 1;
      String countSuffix = "";

      while (true) {
         String newPath = path + suffix + countSuffix + (extension != null && extension.length() != 0 ? "." + extension : "");
         if (!fileExists.apply(newPath)) {
            return newPath;
         }

         countSuffix = Integer.toString(++count);
      }
   }

   public static boolean mkDirs(File file) {
      if (file.isDirectory()) {
         return file.mkdirs();
      }

      File parent = file.getParentFile();
      return parent == null || parent.exists() || parent.mkdirs();
   }

   public static void saveByteFile(byte[] data, File file) throws IOException {
      if (!mkDirs(file)) {
         throw new IllegalArgumentException("Could not create folder for file: " + file.getAbsolutePath());
      }

      FileOutputStream saveFile = new FileOutputStream(file);
      saveFile.write(data);
      saveFile.close();
   }

   public static void saveByteFile(byte[] data, String filePath) throws IOException {
      saveByteFile(data, new File(filePath));
   }

   public static byte[] loadByteFile(File file) throws IOException {
      if (!file.exists()) {
         throw new FileNotFoundException("Could not find byte file");
      } else {
         FileInputStream readFile = new FileInputStream(file);
         long fileSize = file.length();
         if (fileSize > 2147483647L) {
            throw new IOException("Byte file too large");
         } else {
            return loadInputStream(readFile);
         }
      }
   }

   public static byte[] loadInputStream(InputStream is) throws IOException {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      byte[] data = new byte[16384];

      int nRead;
      while ((nRead = is.read(data, 0, data.length)) != -1) {
         buffer.write(data, 0, nRead);
      }

      is.close();
      return buffer.toByteArray();
   }

   public static byte[] loadByteFile(String filePath) throws IOException {
      return loadByteFile(new File(filePath));
   }

   public static byte[] getStringBytes(String string) {
      return string.getBytes(GlobalData.charset);
   }

   public static String getStringFromBytes(byte[] bytes) {
      for (Charset charset : GlobalData.decodeCharsets) {
         try {
            CharsetDecoder decoder;
            if (charset == GlobalData.charset) {
               decoder = GlobalData.defaultDecoder;
            } else {
               decoder = charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
            }

            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
         } catch (CharacterCodingException var6) {
         }
      }

      throw new RuntimeException("Could not load byte array: Unknown encoding");
   }

   public static byte[] encodeToPNG(ByteBuffer buffer, int imageWidth, int imageHeight, int imageBPP) {
      int bufferSize = imageWidth * imageHeight * imageBPP;
      ByteBuffer flippedBuffer = BufferUtils.createByteBuffer(bufferSize);

      for (int y = imageHeight - 1; y >= 0; y--) {
         int index = imageWidth * y * imageBPP;
         byte[] row = new byte[imageWidth * imageBPP];
         buffer.position(index);
         buffer.get(row, 0, row.length);
         flippedBuffer.put(row, 0, row.length);
      }

      flippedBuffer.position(0);
      ByteArrayOutputStream pngOutput = new ByteArrayOutputStream();
      STBImageWrite.stbi_write_png_to_func((context, data, size) -> {
         ByteBuffer pngChunk = MemoryUtil.memByteBuffer(data, size);
         byte[] chunkBytes = new byte[size];
         pngChunk.get(chunkBytes);

         try {
            pngOutput.write(chunkBytes);
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }, 0L, imageWidth, imageHeight, imageBPP, flippedBuffer, imageWidth * 4);
      return pngOutput.toByteArray();
   }

   public static byte[] encodeRGBToPNG(ByteBuffer buffer, int imageWidth, int imageHeight) {
      return encodeToPNG(buffer, imageWidth, imageHeight, 3);
   }

   public static byte[] encodeRGBAToPNG(ByteBuffer buffer, int imageWidth, int imageHeight) {
      return encodeToPNG(buffer, imageWidth, imageHeight, 4);
   }

   public static byte[] compressData(byte[] input) {
      return compressData(input, false);
   }

   public static byte[] compressData(byte[] input, boolean fast) {
      Deflater compressor = new Deflater(fast ? 1 : 9);
      compressor.setInput(input);
      compressor.finish();
      ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
      byte[] buffer = new byte[1024];

      while (!compressor.finished()) {
         int count = compressor.deflate(buffer);
         bos.write(buffer, 0, count);
      }

      compressor.end();
      return bos.toByteArray();
   }

   public static byte[] decompressData(byte[] compressedInput) throws DataFormatException {
      if (compressedInput.length == 0) {
         return new byte[0];
      }

      Inflater decompressor = new Inflater();
      decompressor.setInput(compressedInput);
      ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedInput.length);
      byte[] buffer = new byte[1024];

      while (!decompressor.finished()) {
         int count = decompressor.inflate(buffer);
         if (count == 0) {
            break;
         }

         bos.write(buffer, 0, count);
      }

      decompressor.end();
      return bos.toByteArray();
   }

   public static String toBase64(byte[] data) {
      return Base64.getUrlEncoder().encodeToString(data);
   }

   public static byte[] fromBase64(String data) throws DataFormatException {
      try {
         return Base64.getUrlDecoder().decode(data);
      } catch (IllegalArgumentException e) {
         throw new DataFormatException("Invalid base64 data: " + e.getMessage());
      }
   }

   public static String insertNewLines(String message, FontOptions fontOptions, int maxWidth, String ignoreRegex) {
      return insertNewLines(message, fontOptions, maxWidth, ignoreRegex, true);
   }

   public static String insertNewLines(String message, FontOptions fontOptions, int maxWidth, String ignoreRegex, boolean splitLines) {
      if (splitLines) {
         String[] newLineSplit = message.split("\\n");
         StringBuilder builder = new StringBuilder();

         for (String line : newLineSplit) {
            if (builder.length() != 0) {
               builder.append("\n");
            }

            builder.append(insertNewLines(line, fontOptions, maxWidth, ignoreRegex, false));
         }

         return builder.toString().trim();
      } else {
         String[] words = message.split(" ");
         if (words.length == 0) {
            return message;
         }

         float spaceWidth = FontManager.bit.getWidth(' ', fontOptions);
         float lineWidth = 0.0F;
         StringBuilder builder = new StringBuilder();

         for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.length() != 0) {
               String wordIgnoreNL = word.replace("\n", "").trim();
               String wordIgnoreRegex = ignoreRegex == null ? wordIgnoreNL : wordIgnoreNL.replaceAll(ignoreRegex, "");
               float nextWordWidth = FontManager.bit.getWidth(wordIgnoreRegex, fontOptions);
               if (maxWidth > 0 && lineWidth + nextWordWidth > maxWidth) {
                  if (lineWidth != 0.0F) {
                     builder = new StringBuilder(builder.toString().trim() + "\n");
                     lineWidth = 0.0F;
                     i--;
                     continue;
                  }

                  char[] wordChars = word.toCharArray();
                  char[] ignoredChars = wordIgnoreRegex.toCharArray();
                  int j = 0;
                  int k = 0;

                  while (j < wordChars.length) {
                     char c = wordChars[j];
                     boolean shouldCount;
                     if (k >= ignoredChars.length) {
                        shouldCount = false;
                     } else {
                        shouldCount = c == ignoredChars[k];
                     }

                     builder.append(c);
                     if (shouldCount) {
                        lineWidth += FontManager.bit.getWidth(c, fontOptions);
                        if (lineWidth > maxWidth) {
                           builder = new StringBuilder(builder.toString().trim() + "\n");
                           lineWidth = 0.0F;
                        }
                     }

                     if (shouldCount) {
                        k++;
                     }

                     j++;
                  }

                  word = null;
               }

               if (word != null) {
                  builder.append(word).append(" ");
                  lineWidth += nextWordWidth + spaceWidth;
               }
            }
         }

         return builder.toString().trim();
      }
   }

   public static ArrayList<String> breakString(String message, FontOptions fontOptions, int maxWidth) {
      return breakString(message, fontOptions, maxWidth, null);
   }

   public static ArrayList<String> breakString(String message, FontOptions fontOptions, int maxWidth, String ignoreRegex) {
      String[] lines = insertNewLines(message, fontOptions, maxWidth, ignoreRegex).split("\\n");
      ArrayList<String> out = new ArrayList<>();
      Collections.addAll(out, lines);
      return out;
   }

   public static String maxString(String str, FontOptions fontOptions, int maxWidth) {
      return maxString(str, fontOptions, maxWidth, null);
   }

   public static String maxString(String str, FontOptions fontOptions, int maxWidth, String ignoreRegex) {
      String cStr;
      for (cStr = str;
         FontManager.bit.getWidthCeil(ignoreRegex == null ? cStr : cStr.replaceAll(ignoreRegex, ""), fontOptions) >= maxWidth;
         cStr = str.substring(0, cStr.length() - 1)
      ) {
         if (cStr.length() == 1) {
            return "";
         }
      }

      return cStr;
   }

   public static Integer tryParseInt(String input) {
      if (input == null) {
         return null;
      }

      boolean negative = false;
      int i = 0;
      int len = input.length();
      int limit = -2147483647;
      if (len <= 0) {
         return null;
      }

      char firstChar = input.charAt(0);
      if (firstChar < '0') {
         if (firstChar == '-') {
            negative = true;
            limit = Integer.MIN_VALUE;
         } else if (firstChar != '+') {
            return 0;
         }

         if (len == 1) {
            return 0;
         }

         i++;
      }

      int multmin = limit / 10;
      int result = 0;

      while (i < len) {
         int digit = Character.digit(input.charAt(i++), 10);
         if (digit < 0 || result < multmin) {
            return null;
         }

         result *= 10;
         if (result < limit + digit) {
            return null;
         }

         result -= digit;
      }

      return negative ? result : -result;
   }

   public static String capitalize(String string) {
      return string.substring(0, 1).toUpperCase() + string.substring(1);
   }

   public static BigDecimal exponent(BigDecimal number, int pow) {
      return pow < 0
         ? number.divide(BigDecimal.valueOf(10L).pow(Math.abs(pow)), new MathContext(0, RoundingMode.DOWN))
         : number.multiply(BigDecimal.valueOf(10L).pow(pow));
   }

   public static String metricNumber(
      BigDecimal number, int decimalOffset, int decimals, boolean minimize, RoundingMode roundingMode, String spacer, GameUtils.MetricValue[] metricSystem
   ) {
      if (metricSystem.length == 0) {
         throw new IllegalArgumentException("metricSystem must have a least one value");
      }

      if (decimalOffset != 0) {
         number = exponent(number, decimalOffset);
      }

      if (spacer == null) {
         spacer = "";
      }

      int sign = number.signum();
      if (sign == 0) {
         return "0" + spacer;
      }

      String prefix = "";
      if (sign < 0) {
         prefix = "-";
         number = number.abs();
      }

      GameUtils.MetricValue lastMetric = null;

      for (GameUtils.MetricValue metric : metricSystem) {
         lastMetric = metric;
         if (number.compareTo(metric.divider) >= 0) {
            break;
         }
      }

      BigDecimal decimal = number.divide(lastMetric.divider, new MathContext(0, RoundingMode.DOWN));
      String roundString;
      if (decimal.remainder(BigDecimal.ONE).equals(BigDecimal.ZERO)) {
         roundString = decimal.toString();
      } else if (minimize) {
         BigDecimal abs = decimal.abs();
         int digitsLeftOfDecimalPoint = abs.toString().length() - abs.scale() - 1;
         if (!abs.equals(decimal)) {
            digitsLeftOfDecimalPoint++;
         }

         int maxDecimals = Math.max(0, 2 - digitsLeftOfDecimalPoint);
         roundString = decimal.setScale(Math.min(decimals, maxDecimals), roundingMode).toString();
      } else {
         roundString = decimal.setScale(decimals, roundingMode).toString();
      }

      return prefix + roundString + spacer + lastMetric.suffix;
   }

   public static String metricNumber(long number, int decimals, boolean minimize, RoundingMode roundingMode, String spacer) {
      return metricNumber(BigDecimal.valueOf(number), 0, decimals, minimize, roundingMode, spacer, normalMetricSystem);
   }

   public static String metricNumber(long number, int decimals, boolean minimize, String spacer) {
      return metricNumber(number, decimals, minimize, RoundingMode.HALF_UP, spacer);
   }

   public static String metricNumber(long number, int decimals, String spacer) {
      return metricNumber(number, decimals, false, spacer);
   }

   public static String metricNumber(long number, String spacer) {
      return metricNumber(number, 2, spacer);
   }

   public static String metricNumber(long number, int decimals) {
      return metricNumber(number, decimals, null);
   }

   public static String metricNumber(long number) {
      return metricNumber(number, null);
   }

   public static String getByteString(long bytes) {
      return metricNumber(bytes, " ") + "B";
   }

   public static String getTimeStringNano(long nanoSeconds) {
      long seconds = nanoSeconds / 1000000000L;
      String prefix = "";
      if (seconds > 60L) {
         prefix = formatSecondsMinutes(seconds, true) + " ";
         long cut = seconds / 60L * 60L;
         nanoSeconds -= cut * 1000000000L;
      }

      return prefix + metricNumber(BigDecimal.valueOf(nanoSeconds), -9, 2, false, RoundingMode.HALF_UP, " ", normalMetricSystem) + "s";
   }

   public static String getTimeStringMillis(long milliSeconds) {
      long seconds = milliSeconds / 1000L;
      String prefix = "";
      if (seconds > 60L) {
         prefix = formatSecondsMinutes(seconds, true) + " ";
         long cut = seconds / 60L * 60L;
         milliSeconds -= cut * 1000L;
      }

      return prefix + metricNumber(BigDecimal.valueOf(milliSeconds), -3, 2, false, RoundingMode.HALF_UP, " ", normalMetricSystem) + "s";
   }

   private static String formatSecondsMinutes(long seconds, boolean addSpaces) {
      long d = seconds / 3600L / 24L;
      long h = seconds / 3600L % 24L;
      long m = seconds / 60L % 60L;
      if (d > 0L) {
         return d + "d" + (addSpaces ? " " : "") + h + "h" + (addSpaces ? " " : "") + m + "m";
      } else if (h > 0L) {
         return h + "h" + (addSpaces ? " " : "") + m + "m";
      } else {
         return m > 0L ? m + "m" : "";
      }
   }

   public static String formatSeconds(long seconds, boolean addSpaces) {
      long s = seconds % 60L;
      return formatSecondsMinutes(seconds, addSpaces) + s + (addSpaces ? " " : "") + "s";
   }

   public static String formatSeconds(long seconds) {
      return formatSeconds(seconds, false);
   }

   public static String formatNumber(long value) {
      return NumberFormat.getNumberInstance(Locale.ENGLISH).format(value);
   }

   public static String formatNumber(double value) {
      return NumberFormat.getNumberInstance(Locale.ENGLISH).format(value);
   }

   public static String padString(String input, int length, char c) {
      if (input.length() >= length) {
         return input;
      }

      StringBuilder builder = new StringBuilder(input);

      while (builder.length() < length) {
         builder.append(c);
      }

      return builder.toString();
   }

   public static void forEachMatcherResult(Pattern pattern, String input, Consumer<MatchResult> forEach) {
      Matcher matcher = pattern.matcher(input);

      while (matcher.find()) {
         forEach.accept(matcher.toMatchResult());
      }
   }

   public static String matcherReplaceAll(Pattern pattern, String input, Function<MatchResult, String> replacer) {
      StringBuilder out = new StringBuilder();
      AtomicInteger currentPos = new AtomicInteger();
      forEachMatcherResult(pattern, input, result -> {
         String replace = replacer.apply(result);
         if (replace != null) {
            out.append(input, currentPos.get(), result.start());
            out.append(replace);
            currentPos.set(result.end());
         }
      });
      out.append(input, currentPos.get(), input.length());
      return out.toString();
   }

   public static Dimension getPlayerNameLength() {
      return new Dimension(1, 30);
   }

   public static GameMessage isValidPlayerName(String name) {
      name = name.trim();
      Dimension length = getPlayerNameLength();
      if (name.length() < length.width || name.length() > length.height) {
         return new LocalMessage("ui", "playernamesize");
      } else {
         return !playerNameSymbolsPattern.matcher(name).matches() ? new LocalMessage("ui", "playernamesymbols") : null;
      }
   }

   public static Stream<? extends NetworkClient> streamNetworkClients(Level level) {
      if (level.isServer()) {
         return streamServerClients(level);
      } else {
         return level.isClient() ? streamClientClients(level) : Stream.empty();
      }
   }

   public static Stream<ClientClient> streamClientClients(Client client, LevelIdentifier levelIdentifier) {
      return client == null
         ? Stream.empty()
         : client.streamClients().filter(c -> c != null && c.loadedPlayer && c.hasSpawned() && !c.isDead() && c.isSamePlace(levelIdentifier));
   }

   public static Stream<ClientClient> streamClientClients(Client client, Level level) {
      return streamClientClients(client, level.getIdentifier());
   }

   public static Stream<ClientClient> streamClientClients(Level level) {
      return streamClientClients(level.getClient(), level);
   }

   public static Stream<ServerClient> streamServerClients(Server server, LevelIdentifier levelIdentifier) {
      return server == null
         ? Stream.empty()
         : server.streamClients().filter(c -> c.playerMob != null && c.hasSpawned() && !c.playerMob.removed() && c.isSamePlace(levelIdentifier));
   }

   public static Stream<ServerClient> streamServerClients(Server server, Level level) {
      return streamServerClients(server, level.getIdentifier());
   }

   public static Stream<ServerClient> streamServerClients(Level level) {
      return streamServerClients(level.getServer(), level);
   }

   public static Stream<Point> streamTileCoordinatesBetweenPoints(final Level level, final Point2D start, final Point2D end) {
      Iterator<Point> iterator = new Iterator<Point>() {
         Point next;
         double x = start.getX();
         double y = start.getY();
         int tileX = GameMath.getTileCoordinate(this.x);
         int tileY = GameMath.getTileCoordinate(this.y);
         double distanceTraveled = 0.0;
         final double lineDistance = start.distance(end);
         final double normalizedDirX = (end.getX() - start.getX()) / this.lineDistance;
         final double normalizedDirY = (end.getY() - start.getY()) / this.lineDistance;
         final int signDirX = (int)Math.signum(this.normalizedDirX);
         final int signDirY = (int)Math.signum(this.normalizedDirY);
         final int debugIndex = 0;

         {
            this.next = this.computeNext();
         }

         @Override
         public boolean hasNext() {
            return this.next != null;
         }

         public Point next() {
            if (this.next == null) {
               throw new NoSuchElementException("Out of elements");
            }

            Point out = this.next;
            this.next = this.computeNext();
            return out;
         }

         private Point computeNext() {
            while (this.distanceTraveled <= this.lineDistance) {
               if (this.next == null || this.tileX != this.next.x || this.tileY != this.next.y) {
                  return new Point(this.tileX, this.tileY);
               }

               double xSign = Math.signum(this.x);
               double ySign = Math.signum(this.y);
               double toNextX = ((this.signDirX * xSign < 0.0 ? 0.0 : 32.0) - Math.abs(this.x) % 32.0) * xSign;
               double toNextY = ((this.signDirY * ySign < 0.0 ? 0.0 : 32.0) - Math.abs(this.y) % 32.0) * ySign;
               if (toNextX == 0.0) {
                  toNextX = this.signDirX * 32;
               }

               if (toNextY == 0.0) {
                  toNextY = this.signDirY * 32;
               }

               double distanceToMoveX = toNextX / this.normalizedDirX;
               double distanceToMoveY = toNextY / this.normalizedDirY;
               double biasX = 0.0;
               double biasY = 0.0;
               double distanceToMove;
               if (distanceToMoveX < distanceToMoveY) {
                  distanceToMove = distanceToMoveX;
                  biasX = this.signDirX;
               } else {
                  distanceToMove = distanceToMoveY;
                  biasY = this.signDirY;
               }

               this.x = this.x + (distanceToMove * this.normalizedDirX + biasX);
               this.y = this.y + (distanceToMove * this.normalizedDirY + biasY);
               this.tileX = GameMath.getTileCoordinate(this.x);
               this.tileY = GameMath.getTileCoordinate(this.y);
               if (!level.isTileWithinBounds(this.tileX, this.tileY)) {
                  break;
               }

               this.distanceTraveled += distanceToMove;
            }

            return null;
         }
      };
      Iterable<Point> iterable = () -> iterator;
      return StreamSupport.stream(iterable.spliterator(), false);
   }

   public static Stream<Point> streamRegionCoordinatesBetweenPoints(final Level level, final Point2D start, final Point2D end) {
      Iterator<Point> iterator = new Iterator<Point>() {
         Point next;
         double x = start.getX();
         double y = start.getY();
         int regionX = GameMath.getRegionCoordByTile(GameMath.getTileCoordinate(this.x));
         int regionY = GameMath.getRegionCoordByTile(GameMath.getTileCoordinate(this.y));
         double distanceTraveled = 0.0;
         final double lineDistance = start.distance(end);
         final double normalizedDirX = (end.getX() - start.getX()) / this.lineDistance;
         final double normalizedDirY = (end.getY() - start.getY()) / this.lineDistance;
         final int signDirX = (int)Math.signum(this.normalizedDirX);
         final int signDirY = (int)Math.signum(this.normalizedDirY);
         static final double regionSize = 512.0;

         {
            this.next = this.computeNext();
         }

         @Override
         public boolean hasNext() {
            return this.next != null;
         }

         public Point next() {
            if (this.next == null) {
               throw new NoSuchElementException("Out of elements");
            }

            Point out = this.next;
            this.next = this.computeNext();
            return out;
         }

         private Point computeNext() {
            while (this.distanceTraveled <= this.lineDistance) {
               if (this.next == null || this.regionX != this.next.x || this.regionY != this.next.y) {
                  return new Point(this.regionX, this.regionY);
               }

               double xSign = Math.signum(this.x);
               double ySign = Math.signum(this.y);
               double toNextX = ((this.signDirX * xSign < 0.0 ? 0.0 : 512.0) - Math.abs(this.x) % 512.0) * xSign;
               double toNextY = ((this.signDirY * ySign < 0.0 ? 0.0 : 512.0) - Math.abs(this.y) % 512.0) * ySign;
               if (toNextX == 0.0) {
                  toNextX = this.signDirX * 512.0;
               }

               if (toNextY == 0.0) {
                  toNextY = this.signDirY * 512.0;
               }

               double distanceToMoveX = toNextX / this.normalizedDirX;
               double distanceToMoveY = toNextY / this.normalizedDirY;
               double biasX = 0.0;
               double biasY = 0.0;
               double distanceToMove;
               if (distanceToMoveX < distanceToMoveY) {
                  distanceToMove = distanceToMoveX;
                  biasX = this.signDirX;
               } else {
                  distanceToMove = distanceToMoveY;
                  biasY = this.signDirY;
               }

               this.x = this.x + (distanceToMove * this.normalizedDirX + biasX);
               this.y = this.y + (distanceToMove * this.normalizedDirY + biasY);
               this.regionX = GameMath.getRegionCoordByTile(GameMath.getTileCoordinate(this.x));
               this.regionY = GameMath.getRegionCoordByTile(GameMath.getTileCoordinate(this.y));
               if (!level.regionManager.isRegionWithinBounds(this.regionX, this.regionY)) {
                  break;
               }

               this.distanceTraveled += distanceToMove;
            }

            return null;
         }
      };
      Iterable<Point> iterable = () -> iterator;
      return StreamSupport.stream(iterable.spliterator(), false);
   }

   public static Mob getLevelMob(int uniqueID, Level level) {
      return getLevelMob(uniqueID, level, false);
   }

   public static Mob getLevelMob(int uniqueID, Level level, boolean searchCache) {
      return getLevelMob(uniqueID, level, searchCache, false);
   }

   public static Mob getLevelMob(int uniqueID, Level level, boolean searchCache, boolean allowOutsideLevelPlayers) {
      if (level == null) {
         return null;
      }

      Mob out = null;
      if (level.isServer()) {
         if (uniqueID < level.getServer().getSlots() && uniqueID >= 0) {
            if (level.getServer().getClient(uniqueID) == null) {
               return null;
            }

            if (!allowOutsideLevelPlayers && !level.getServer().getClient(uniqueID).isSamePlace(level)) {
               return null;
            }

            out = level.getServer().getClient(uniqueID).playerMob;
         }
      } else if (level.isClient() && uniqueID < level.getClient().getSlots() && uniqueID >= 0) {
         if (level.getClient().getClient(uniqueID) == null) {
            return null;
         }

         if (!allowOutsideLevelPlayers && !level.getClient().getClient(uniqueID).isSamePlace(level)) {
            return null;
         }

         out = level.getClient().getClient(uniqueID).playerMob;
      }

      if (out == null) {
         out = level.entityManager.mobs.get(uniqueID, searchCache);
      }

      return out;
   }

   public static Attacker getLevelAttacker(int uniqueID, Level level) {
      if (level == null) {
         return null;
      }

      Attacker out = getLevelMob(uniqueID, level);
      if (out == null) {
         out = level.entityManager.projectiles.get(uniqueID, true);
      }

      return out;
   }

   public static Point getValidMobLocationAroundObject(GameRandom random, Level level, Mob mob, int tileX, int tileY, boolean allowDiagonal) {
      ArrayList<Point> priority1Spawns = new ArrayList<>(8);
      ArrayList<Point> priority2Spawns = new ArrayList<>(8);
      ArrayList<Point> priority3Spawns = new ArrayList<>(8);
      level.regionManager.ensureTileIsLoaded(tileX, tileY);

      for (Point tile : level.getLevelObject(tileX, tileY).getMultiTile().getAdjacentTiles(tileX, tileY, allowDiagonal)) {
         int posX = tile.x * 32 + 16;
         int posY = tile.y * 32 + 16;
         if (!mob.collidesWith(level, posX, posY)) {
            if (!mob.collidesWithAnyMob(level, posX, posY)) {
               priority1Spawns.add(new Point(posX, posY));
            }

            priority2Spawns.add(new Point(posX, posY));
         }

         priority3Spawns.add(new Point(posX, posY));
      }

      if (!priority1Spawns.isEmpty()) {
         return random.getOneOf(priority1Spawns);
      } else {
         return !priority2Spawns.isEmpty() ? random.getOneOf(priority2Spawns) : random.getOneOf(priority3Spawns);
      }
   }

   public static Rectangle rangeBounds(int x, int y, int range) {
      return new Rectangle(x - range, y - range, range * 2, range * 2);
   }

   public static Rectangle rangeBounds(float x, float y, int range) {
      return rangeBounds((int)x, (int)y, range);
   }

   public static Rectangle rangeTileBounds(int x, int y, int tileRange) {
      return rangeBounds(x, y, tileRange * 32);
   }

   public static Stream<Mob> streamTargetsRange(Mob attacker, int range) {
      return streamTargetsRange(attacker, attacker.getX(), attacker.getY(), range);
   }

   public static Stream<Mob> streamTargetsRange(Mob attacker, int posX, int posY, int range) {
      return streamTargets(attacker, rangeBounds(posX, posY, range));
   }

   public static Stream<Mob> streamTargetsTileRange(Mob attacker, int tileRange) {
      return streamTargetsTileRange(attacker, attacker.getX(), attacker.getY(), tileRange);
   }

   public static Stream<Mob> streamTargetsTileRange(Mob attacker, int posX, int posY, int tileRange) {
      return streamTargets(attacker, rangeTileBounds(posX, posY, tileRange));
   }

   public static Stream<Mob> streamTargets(Mob attacker, Shape hitBounds) {
      return streamTargets(attacker, hitBounds, 1);
   }

   public static Stream<Mob> streamTargets(Mob attacker, Level level, Shape hitBounds) {
      return streamTargets(attacker, level, hitBounds, 1);
   }

   public static <T> Stream<T> streamConcat(Stream<? extends T>... streams) {
      Stream<T> out = null;

      for (Stream<? extends T> stream : streams) {
         if (out == null) {
            out = (Stream<T>)stream;
         } else {
            out = Stream.concat(out, stream);
         }
      }

      return out == null ? Stream.empty() : out;
   }

   public static NetworkClient getAttackerClient(Mob attacker) {
      NetworkClient followingClient = attacker.getFollowingClient();
      if (followingClient != null) {
         return followingClient;
      } else {
         return attacker.isPlayer ? ((PlayerMob)attacker).getNetworkClient() : null;
      }
   }

   public static Stream<Mob> streamTargets(Mob attacker, Shape hitBounds, int extraRegionRange) {
      return streamTargets(attacker, attacker == null ? null : attacker.getLevel(), hitBounds, extraRegionRange);
   }

   public static Stream<Mob> streamTargets(Mob attacker, Level level, Shape hitBounds, int extraRegionRange) {
      if (attacker != null && level != null) {
         NetworkClient attackerClient = getAttackerClient(attacker);
         Stream<Mob> targets;
         if (hitBounds == null) {
            targets = level.entityManager.mobs.stream();
         } else {
            targets = level.entityManager.mobs.streamInRegionsShape(hitBounds, extraRegionRange);
         }

         targets = targets.filter(m -> m.canBeTargeted(attacker, attackerClient));
         if (attackerClient != null && !attackerClient.pvpEnabled()) {
            return targets;
         }

         Stream<PlayerMob> players;
         if (hitBounds == null) {
            players = streamNetworkClients(level).map(c -> c.playerMob);
         } else {
            players = level.entityManager.players.streamInRegionsShape(hitBounds, extraRegionRange);
         }

         players = players.filter(p -> {
            NetworkClient c = p.getNetworkClient();
            if (c == null || c.playerMob == null || c.playerMob.getLevel() == null) {
               return false;
            } else if (c.isDead() || !c.hasSpawned()) {
               return false;
            } else {
               return attackerClient != null && !c.pvpEnabled() ? false : c.playerMob.canBeTargeted(attacker, attackerClient);
            }
         });
         return Stream.concat(targets, players);
      } else {
         return Stream.empty();
      }
   }

   public static <T> String join(T[] objects, Function<T, String> toString, String join, String lastJoin) {
      StringBuilder out = new StringBuilder();

      for (int i = 0; i < objects.length; i++) {
         out.append(toString == null ? objects[i] : toString.apply(objects[i]));
         if (i == objects.length - 2) {
            out.append(lastJoin);
         } else if (i < objects.length - 1) {
            out.append(join);
         }
      }

      return out.toString();
   }

   public static String join(Object[] objects, String join, String lastJoin) {
      return join(objects, null, join, lastJoin);
   }

   public static <T> String join(T[] objects, Function<T, String> toString, String join) {
      return join(objects, toString, join, join);
   }

   public static String join(Object[] objects, String join) {
      return join(objects, null, join, join);
   }

   public static <T> void insertSortedList(List<T> list, T object, Comparator<T> comparator) {
      insertSortedList(list.listIterator(), object, comparator);
   }

   public static <T> void insertSortedList(ListIterator<T> iterator, T object, Comparator<T> comparator) {
      if (comparator == null) {
         if (!(object instanceof Comparable)) {
            throw new IllegalArgumentException("Must either supply comparator or object must be comparable.");
         }

         comparator = (o1, o2) -> ((Comparable)o1).compareTo(o2);
      }

      while (iterator.hasNext()) {
         T next = iterator.next();
         if (comparator.compare(next, object) > 0) {
            iterator.previous();
            iterator.add(object);
            return;
         }
      }

      iterator.add(object);
   }

   public static <T> GameLinkedList<T>.Element insertSortedList(GameLinkedList<T> list, T object, Comparator<T> comparator) {
      if (comparator == null) {
         if (!(object instanceof Comparable)) {
            throw new IllegalArgumentException("Must either supply comparator or object must be comparable.");
         }

         comparator = (o1, o2) -> ((Comparable)o1).compareTo(o2);
      }

      for (GameLinkedList<T>.Element next = list.getFirstElement(); next != null; next = next.next()) {
         if (comparator.compare(next.object, object) > 0) {
            return next.insertBefore(object);
         }
      }

      return list.addLast(object);
   }

   public static <T> GameLinkedList<T>.Element insertSortedListReversed(GameLinkedList<T> list, T object, Comparator<T> comparator) {
      if (comparator == null) {
         if (!(object instanceof Comparable)) {
            throw new IllegalArgumentException("Must either supply comparator or object must be comparable.");
         }

         comparator = (o1, o2) -> ((Comparable)o1).compareTo(o2);
      }

      for (GameLinkedList<T>.Element last = list.getLastElement(); last != null; last = last.prev()) {
         if (comparator.compare(last.object, object) < 0) {
            return last.insertAfter(object);
         }
      }

      return list.addFirst(object);
   }

   public static Color getBrighterColor(Color color, float factor) {
      int r = color.getRed();
      int g = color.getGreen();
      int b = color.getBlue();
      int i = (int)(1.0F / factor);
      if (r == 0 && g == 0 && b == 0) {
         return new Color(i, i, i, color.getAlpha());
      }

      if (r > 0 && r < i) {
         r = i;
      }

      if (g > 0 && g < i) {
         g = i;
      }

      if (b > 0 && b < i) {
         b = i;
      }

      return new Color(Math.min((int)(r / factor), 255), Math.min((int)(g / factor), 255), Math.min((int)(b / factor), 255), color.getAlpha());
   }

   public static Color getDarkerColor(Color color, float factor) {
      return new Color(
         Math.max((int)(color.getRed() * factor), 0),
         Math.max((int)(color.getGreen() * factor), 0),
         Math.max((int)(color.getBlue() * factor), 0),
         color.getAlpha()
      );
   }

   public static float getStatusColorHue(float percent) {
      return GameMath.limit(percent, 0.0F, 1.0F) / 3.0F;
   }

   public static Color getStatusColor(float percent, float saturation, float brightness) {
      return Color.getHSBColor(getStatusColorHue(percent), saturation, brightness);
   }

   public static Color getStatusColorRedPref(float percent, float saturation, float brightness, float exponent) {
      percent = (float)Math.pow(percent, exponent);
      return Color.getHSBColor(getStatusColorHue(percent), saturation, brightness);
   }

   public static Color getStatusColorGreenPref(float percent, float saturation, float brightness, float exponent) {
      percent = Math.abs((float)Math.pow(Math.abs(percent - 1.0F), exponent) - 1.0F);
      return Color.getHSBColor(getStatusColorHue(percent), saturation, brightness);
   }

   public static Color getStatusColorLerp(float percent, float lowerLimit, float upperLimit) {
      float percentInv = Math.abs(percent - 1.0F);
      float red = GameMath.limit(percentInv * 2.0F, 0.0F, 1.0F);
      float green = GameMath.limit(percent * 2.0F, 0.0F, 1.0F);
      return new Color(GameMath.lerp(red, lowerLimit, upperLimit), GameMath.lerp(green, lowerLimit, upperLimit), 0.0F);
   }

   public static Color getStatusColorLerpRedPref(float percent, float lowerLimit, float upperLimit, float exponent) {
      percent = (float)Math.pow(percent, exponent);
      return getStatusColorLerp(percent, lowerLimit, upperLimit);
   }

   public static Color getStatusColorLerpGreenPref(float percent, float lowerLimit, float upperLimit, float exponent) {
      percent = Math.abs((float)Math.pow(Math.abs(percent - 1.0F), exponent) - 1.0F);
      return getStatusColorLerp(percent, lowerLimit, upperLimit);
   }

   public static float getBobbing(long time, int bobbingTime) {
      int halfBobbingTime = bobbingTime / 2;
      long timeMod = time % bobbingTime;
      if (timeMod > halfBobbingTime) {
         timeMod = halfBobbingTime - timeMod % halfBobbingTime;
      }

      return (float)timeMod / halfBobbingTime;
   }

   public static float getTimeRotation(long time, int div, int accuracy) {
      return accuracy <= 1 ? (float)(time % 360L) : (float)(time * accuracy / div % (360 * accuracy)) / accuracy;
   }

   public static float getTimeRotation(long time, int div) {
      return getTimeRotation(time, div, 10);
   }

   public static float getAnimFloat(long time, int animTime) {
      return (float)(time % animTime) / animTime;
   }

   public static float getAnimFloatContinuous(long time, int animTime) {
      float anim = getAnimFloat(time, animTime);
      return anim > 0.5F ? Math.abs((anim - 0.5F) * 2.0F - 1.0F) : anim * 2.0F;
   }

   public static int getAnim(long time, int frames, int animTime) {
      float frameTime = (float)animTime / frames;
      return (int)Math.min((float)(time % animTime) / frameTime, frames - 1);
   }

   public static int getAnimContinuous(long time, int[] frameTimes) {
      if (time < 0L) {
         return -1;
      }

      int totalTime = 0;

      for (int i = 0; i < frameTimes.length; i++) {
         if (time < frameTimes[i]) {
            return i;
         }

         totalTime += frameTimes[i];
         time -= frameTimes[i];
      }

      time %= totalTime;
      return getAnim(time, frameTimes);
   }

   public static int getAnim(long time, int[] frameTimes) {
      if (time < 0L) {
         return -1;
      }

      for (int i = 0; i < frameTimes.length; i++) {
         if (time < frameTimes[i]) {
            return i;
         }

         time -= frameTimes[i];
      }

      return -1;
   }

   public static int getBounceAnim(long time, int bounceAmount, int bounceCooldown, int totalBounceTime, int firstBounceTime, int defaultNoBounce) {
      long animTime = time % (bounceCooldown + totalBounceTime);
      if (animTime > totalBounceTime) {
         return defaultNoBounce;
      }

      int calculatedBounce = 0;
      int bounceTime = firstBounceTime;
      int bounceTimeOffset = 0;

      while (bounceTime > 0) {
         float bouncePerc = (float)(animTime - bounceTimeOffset) / bounceTime;
         if (bouncePerc < 0.0F) {
            break;
         }

         float amountMod = (float)bounceTime / firstBounceTime;
         if (amountMod * bounceAmount < 1.0F) {
            return defaultNoBounce;
         }

         calculatedBounce = (int)(GameMath.sin(bouncePerc * 180.0F) * bounceAmount * amountMod);
         bounceTimeOffset += bounceTime;
         bounceTime /= 2;
      }

      return calculatedBounce;
   }

   public static int getBounceAnim(long time, int bounceAmount, int bounceCooldown, int totalBounceTime, int firstBounceTime) {
      return getBounceAnim(time, bounceAmount, bounceCooldown, totalBounceTime, firstBounceTime, 0);
   }

   public static float getMultiplayerScaling(int playerCount, int maxPlayerCount, float percentStart, float percentDec) {
      if (percentDec == 0.0F) {
         return 1.0F + (Math.min(playerCount, maxPlayerCount) - 1) * percentStart;
      }

      float multiplier = 1.0F;

      for (int i = 2; i <= Math.min(playerCount, maxPlayerCount); i++) {
         float inc = percentStart - percentDec * (i - 2);
         if (inc <= 0.0F) {
            break;
         }

         multiplier += inc;
      }

      return multiplier;
   }

   public static float getMultiplayerScaling(int playerCount, float percentStart, float percentDec) {
      return getMultiplayerScaling(playerCount, 20, percentStart, percentDec);
   }

   public static float getMultiplayerScaling(int playerCount) {
      return getMultiplayerScaling(playerCount, 0.8F, 0.04F);
   }

   public static <T> RayLinkedList<T> castRay(
      double x, double y, double dx, double dy, double distance, double checkDistance, int maxBounces, Function<Line2D, IntersectionPoint<T>> rayHandler
   ) {
      Double dir = GameMath.normalize(dx, dy);
      if (!java.lang.Double.isNaN(dir.x) && !java.lang.Double.isNaN(dir.y)) {
         Double currentPos = new Double(x, y);
         Line2D currentLine = new java.awt.geom.Line2D.Double(
            currentPos.x, currentPos.y, currentPos.x + dir.x * Math.min(checkDistance, distance), currentPos.y + dir.y * Math.min(checkDistance, distance)
         );
         double currentDistance = 0.0;
         RayLinkedList<T> out = new RayLinkedList<>();
         boolean lastLine = false;

         while (out.size() <= maxBounces) {
            IntersectionPoint<T> collisionPoint = rayHandler.apply(currentLine);
            if (collisionPoint == null) {
               if (lastLine) {
                  out.addLast(new Ray<>(currentPos.x, currentPos.y, currentPos.x + dir.x * distance, currentPos.y + dir.y * distance, null, null));
                  out.totalDist += distance;
                  break;
               }

               currentDistance += currentLine.getP1().distance(currentLine.getP2());
               double nextDist = distance - currentDistance;
               if (nextDist <= checkDistance) {
                  currentLine = new java.awt.geom.Line2D.Double(
                     currentLine.getX2(), currentLine.getY2(), currentLine.getX2() + dir.x * nextDist, currentLine.getY2() + dir.y * nextDist
                  );
                  lastLine = true;
               } else {
                  currentLine = new java.awt.geom.Line2D.Double(
                     currentLine.getX2(), currentLine.getY2(), currentLine.getX2() + dir.x * checkDistance, currentLine.getY2() + dir.y * checkDistance
                  );
               }
            } else {
               if (collisionPoint.dir == IntersectionPoint.Dir.UP) {
                  dir.y = -dir.y;
                  collisionPoint.y += 0.1;
               } else if (collisionPoint.dir == IntersectionPoint.Dir.DOWN) {
                  dir.y = -dir.y;
                  collisionPoint.y -= 0.1;
               } else if (collisionPoint.dir == IntersectionPoint.Dir.LEFT) {
                  dir.x = -dir.x;
                  collisionPoint.x += 0.1;
               } else if (collisionPoint.dir == IntersectionPoint.Dir.RIGHT) {
                  dir.x = -dir.x;
                  collisionPoint.x -= 0.1;
               }

               Ray<T> ray = new Ray<>(currentPos.x, currentPos.y, collisionPoint.x, collisionPoint.y, collisionPoint.target, collisionPoint.dir);
               out.addLast(ray);
               out.totalDist = out.totalDist + ray.dist;
               distance -= ray.dist;
               currentLine = new java.awt.geom.Line2D.Double(
                  collisionPoint.x,
                  collisionPoint.y,
                  collisionPoint.x + dir.x * Math.min(checkDistance, distance),
                  collisionPoint.y + dir.y * Math.min(checkDistance, distance)
               );
               lastLine = false;
               currentDistance = 0.0;
               currentPos = new Double(collisionPoint.x, collisionPoint.y);
            }
         }

         return out;
      } else {
         return new RayLinkedList<>();
      }
   }

   public static <T> RayLinkedList<T> castRay(Line2D line, double checkDistance, int maxBounces, Function<Line2D, IntersectionPoint<T>> rayHandler) {
      return castRay(
         line.getX1(),
         line.getY1(),
         line.getX2() - line.getX1(),
         line.getY2() - line.getY1(),
         line.getP1().distance(line.getP2()),
         checkDistance,
         maxBounces,
         rayHandler
      );
   }

   public static <T> T castRayFirstHit(double x, double y, double dx, double dy, double distance, double checkDistance, Function<Line2D, T> rayHandler) {
      RayLinkedList<T> result = castRay(x, y, dx, dy, distance, checkDistance, 0, line -> {
         T hit = rayHandler.apply(line);
         return hit == null ? null : new IntersectionPoint<>(line.getX2(), line.getY2(), hit, IntersectionPoint.Dir.DOWN);
      });
      Ray<T> lastHit = result.getLast();
      return lastHit != null && lastHit.targetHit != null ? lastHit.targetHit : null;
   }

   public static <T> T castRayFirstHit(Line2D line, double checkDistance, Function<Line2D, T> rayHandler) {
      return castRayFirstHit(
         line.getX1(), line.getY1(), line.getX2() - line.getX1(), line.getY2() - line.getY1(), line.getP1().distance(line.getP2()), checkDistance, rayHandler
      );
   }

   public static RayLinkedList<LevelObjectHit> castRay(
      Level level,
      double x,
      double y,
      double dx,
      double dy,
      double distance,
      double checkDistance,
      int maxBounces,
      CollisionFilter collisionFilter,
      boolean checkInsideRect
   ) {
      return castRay(
         x, y, dx, dy, distance, checkDistance, maxBounces, line -> level.getCollisionPoint(level.getCollisions(line, collisionFilter), line, checkInsideRect)
      );
   }

   public static RayLinkedList<LevelObjectHit> castRay(
      Level level, double x, double y, double dx, double dy, double distance, int maxBounces, CollisionFilter collisionFilter
   ) {
      return castRay(level, x, y, dx, dy, distance, 100.0, maxBounces, collisionFilter, false);
   }

   public static Ray<LevelObjectHit> castRayFirstHit(Level level, double x, double y, double dx, double dy, double distance, CollisionFilter collisionFilter) {
      RayLinkedList<LevelObjectHit> rays = castRay(level, x, y, dx, dy, distance, 100.0, 0, collisionFilter, false);
      return rays.isEmpty() ? null : rays.getFirst();
   }

   public static Ray<LevelObjectHit> castRayFirstHit(Level level, Line2D line, CollisionFilter collisionFilter) {
      return castRayFirstHit(
         level, line.getX1(), line.getY1(), line.getX2() - line.getX1(), line.getY2() - line.getY1(), line.getP1().distance(line.getP2()), collisionFilter
      );
   }

   public static LevelObject getInteractObjectHit(Level level, int hitX, int hitY, int layerID, Predicate<LevelObject> filter) {
      return getInteractObjectHit(
         level, hitX, hitY, layerID, filter, new LevelObject(level, GameMath.getTileCoordinate(hitX), GameMath.getTileCoordinate(hitY))
      );
   }

   public static LevelObject getInteractObjectHit(Level level, int hitX, int hitY, int layerID, Predicate<LevelObject> filter, LevelObject defaultReturn) {
      if (Settings.useTileObjectHitboxes) {
         LevelObject lo = null;
         if (layerID == -1) {
            int tileX = GameMath.getTileCoordinate(hitX);
            int tileY = GameMath.getTileCoordinate(hitY);
            ArrayList<LevelObject> list = level.objectLayer.getHitboxPriorityList(tileX, tileY, true);
            if (list.isEmpty()) {
               lo = new LevelObject(level, 0, tileX, tileY);
            } else {
               lo = list.get(0);
            }
         } else {
            lo = new LevelObject(level, layerID, GameMath.getTileCoordinate(hitX), GameMath.getTileCoordinate(hitY));
         }

         return lo == null || filter != null && !filter.test(lo) ? defaultReturn : lo;
      } else {
         int startTileX = GameMath.getTileCoordinate(hitX - 100);
         int startTileY = GameMath.getTileCoordinate(hitY - 50);
         int endTileX = GameMath.getTileCoordinate(hitX + 100);
         int endTileY = GameMath.getTileCoordinate(hitY + 200);
         ObjectHoverHitbox best = null;

         for (int currentTileY = endTileY; currentTileY >= startTileY; currentTileY--) {
            for (int currentTileX = startTileX; currentTileX <= endTileX; currentTileX++) {
               if (layerID == -1) {
                  for (LevelObject lo : level.objectLayer.getHitboxPriorityList(currentTileX, currentTileY, true)) {
                     if (filter == null || filter.test(lo)) {
                        for (ObjectHoverHitbox box : lo.getHoverHitboxes()) {
                           if (box.contains(hitX, hitY) && (best == null || best.sortY < box.sortY)) {
                              best = box;
                           }
                        }
                     }
                  }
               } else {
                  LevelObject lo = new LevelObject(level, layerID, currentTileX, currentTileY);
                  if (filter == null || filter.test(lo)) {
                     for (ObjectHoverHitbox box : lo.getHoverHitboxes()) {
                        if (box.contains(hitX, hitY) && (best == null || best.sortY < box.sortY)) {
                           best = box;
                        }
                     }
                  }
               }
            }

            if (best != null) {
               break;
            }
         }

         return best != null ? new LevelObject(level, best.layerID, best.tileX, best.tileY) : defaultReturn;
      }
   }

   public static <T, R> Iterable<R> mapIterable(Iterator<T> iterator, Function<T, R> mapper) {
      return () -> mapIterator(iterator, mapper);
   }

   public static <T, R> Iterator<R> mapIterator(final Iterator<T> iterator, final Function<T, R> mapper) {
      return new Iterator<R>() {
         @Override
         public boolean hasNext() {
            return iterator.hasNext();
         }

         @Override
         public R next() {
            return mapper.apply(iterator.next());
         }
      };
   }

   public static <T> Stream<? extends T> concat(Stream<? extends T>... streams) {
      if (streams.length == 0) {
         return Stream.empty();
      }

      Stream<? extends T> current = streams[0];

      for (int i = 1; i < streams.length; i++) {
         current = Stream.concat(current, streams[i]);
      }

      return current;
   }

   public static <T> T[] concat(T[] a, T[] b) {
      T[] out = (T[])Arrays.copyOf(a, a.length + b.length);
      System.arraycopy(b, 0, out, a.length, b.length);
      return out;
   }

   public static <T, R> R[] mapArray(T[] array, R[] c, Function<T, R> mapper) {
      R[] out = Arrays.copyOf(c, array.length);

      for (int i = 0; i < out.length; i++) {
         out[i] = mapper.apply(array[i]);
      }

      return out;
   }

   public static <T> Iterator<T> concatIterators(Collection<Iterator<? extends T>> iterators) {
      return new GameUtils.ConcatIterator<>(iterators);
   }

   @SafeVarargs
   public static <T> Iterator<T> concatIterators(Iterator<? extends T>... iterators) {
      return new GameUtils.ConcatIterator<>(iterators);
   }

   public static <T> Iterator<T> array2DIterator(T[][] array) {
      LinkedList<Iterator<? extends T>> iterators = new LinkedList<>();

      for (T[] ts : array) {
         iterators.add(arrayIterator(ts));
      }

      return concatIterators(iterators);
   }

   public static <T> Iterator<T> arrayIterator(T[] array) {
      return new GameUtils.ArrayIterator<>(array);
   }

   public static long longHash(Object... objects) {
      if (objects == null) {
         return 0L;
      }

      long hash = 1L;

      for (Object element : objects) {
         hash = 31L * hash + (element == null ? 0 : element.hashCode());
      }

      return hash;
   }

   public static String toValidFileName(String name) {
      for (String invalidSequence : invalidFileNameSequences) {
         name = name.replace(invalidSequence, "");
      }

      return name;
   }

   public static String formatFileExtension(String file, String defaultExtension) {
      String fileName;
      try {
         fileName = new File(file).getName();
      } catch (Exception e) {
         fileName = file;
      }

      if (!fileName.contains(".")) {
         file = file + "." + defaultExtension;
      }

      return file;
   }

   public static String getFileExtension(String file) {
      if (file == null) {
         return null;
      }

      String fileName;
      try {
         fileName = new File(file).getName();
      } catch (Exception e) {
         fileName = file;
      }

      int i = fileName.lastIndexOf(".");
      return i != -1 ? fileName.substring(i + 1) : null;
   }

   public static String removeFileExtension(String file) {
      String extension = getFileExtension(file);
      return extension != null ? file.substring(0, file.length() - extension.length() - 1) : file;
   }

   public static void openURL(String url) {
      try {
         openURL(new URI(url));
      } catch (URISyntaxException e) {
         System.err.println("Could not open invalid url :" + url);
      }
   }

   public static void openURL(URI uri) {
      System.out.println("Opening URL " + uri.toString());
      Thread thread = new Thread(() -> {
         try {
            if (Desktop.isDesktopSupported()) {
               Desktop.getDesktop().browse(uri);
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      });
      thread.start();
   }

   public static boolean openExplorerAtFile(File file) {
      if (file.exists()) {
         Thread thread = new Thread(() -> {
            try {
               if (Platform.get() == Platform.WINDOWS) {
                  Runtime.getRuntime().exec("explorer.exe /select," + file.getAbsolutePath());
               } else {
                  File dir = file.getParentFile();
                  if (dir.exists() && dir.isDirectory() && Desktop.isDesktopSupported()) {
                     Desktop.getDesktop().open(dir);
                  }
               }
            } catch (Exception e) {
               e.printStackTrace();
            }
         });
         thread.start();
         return true;
      } else {
         GameLog.warn.println("Tried to open non existing file: " + file.getAbsolutePath());
         return false;
      }
   }

   public static GameTooltips getDevQuoteTooltip(GameMessage message, String name, int maxWidth) {
      String str = message.translate();
      if (!str.startsWith("\"")) {
         str = "\"" + str + "\"";
      }

      if (name != null) {
         str = str + " - " + name;
      }

      return new StringTooltips(str, GameColor.CYAN, maxWidth);
   }

   public static GameTooltips getDevQuoteTooltip(GameMessage message, String name) {
      return getDevQuoteTooltip(message, name, 350);
   }

   public static GameTooltips getDevQuoteTooltip(String category, String key, String name) {
      return getDevQuoteTooltip(new LocalMessage(category, key), name);
   }

   public static GameTooltips getDevQuoteTooltip(String message, String name) {
      return getDevQuoteTooltip(new StaticMessage(message), name);
   }

   private static class ArrayIterator<T> implements Iterator<T> {
      public final T[] array;
      public int i;

      public ArrayIterator(T[] array) {
         this.array = array;
      }

      @Override
      public boolean hasNext() {
         return this.i < this.array.length;
      }

      @Override
      public T next() {
         return this.array[this.i++];
      }
   }

   private static class ConcatIterator<T> implements Iterator<T> {
      public final LinkedList<Iterator<? extends T>> iterators = new LinkedList<>();

      public ConcatIterator(Collection<Iterator<? extends T>> iterators) {
         this.iterators.addAll(iterators);
      }

      @SafeVarargs
      public ConcatIterator(Iterator<? extends T>... iterators) {
         this(Arrays.asList(iterators));
      }

      private Iterator<? extends T> nextIterator() {
         while (!this.iterators.isEmpty()) {
            if (this.iterators.getFirst().hasNext()) {
               return this.iterators.getFirst();
            }

            this.iterators.removeFirst();
         }

         return null;
      }

      @Override
      public boolean hasNext() {
         return this.nextIterator() != null;
      }

      @Override
      public T next() {
         Iterator<? extends T> iterator = this.nextIterator();
         if (iterator != null) {
            return (T)iterator.next();
         } else {
            throw new NoSuchElementException();
         }
      }
   }

   public static class MetricValue {
      public final String suffix;
      public final BigDecimal divider;

      public MetricValue(String suffix, BigDecimal divider) {
         this.suffix = suffix;
         this.divider = divider;
      }

      public MetricValue(String suffix, int exponent) {
         this(suffix, GameUtils.exponent(BigDecimal.ONE, exponent));
      }
   }
}
