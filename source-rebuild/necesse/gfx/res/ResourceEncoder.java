package necesse.gfx.res;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import necesse.engine.GameLaunch;
import necesse.engine.GlobalData;
import necesse.engine.modLoader.GameFileEntry;
import necesse.engine.modLoader.LoadedMod;

public class ResourceEncoder {
   public static final String resDataFile = "res.data";
   public static final String jarResourcePath = "resources/";
   public static final String previewImagePath = "resources/preview.png";
   public static final HashSet<String> fileExtensions = new HashSet<>();
   public static final HashSet<String> ignoreFiles = new HashSet<>();
   private static ResourceFolder resources;

   public static boolean isLoaded() {
      return resources != null;
   }

   public static void loadResourceFile() {
      try {
         resources = new ResourceFolder(getFileResources());
      } catch (IOException e) {
         throw new RuntimeException("Failed to load game resources", e);
      }
   }

   public static void loadEmptyResourceFile() {
      resources = new ResourceFolder();
   }

   private static List<GameFileEntry> getFileResources() {
      List<GameFileEntry> out = new ArrayList<>();
      boolean ignoreResData = GameLaunch.launchOptions != null && GameLaunch.launchOptions.containsKey("ignoreresdata");
      if (!ignoreResData) {
         File file = new File(GlobalData.rootPath() + "res.data");
         if (file.isFile()) {
            out.add(new GameFileEntry("resources/res.data", file));
         }
      }

      return out;
   }

   public static void addModResources(LoadedMod mod) {
      if (resources == null) {
         throw new IllegalStateException("Resources not loaded yet.");
      }

      try {
         Iterator<GameFileEntry> iterator = mod.jarFile.stream().map(ex -> new GameFileEntry(mod.jarFile, ex)).iterator();
         resources.addModResources(() -> iterator, mod);
      } catch (IOException e) {
         System.err.println("Could not load mod " + mod.id + " resources");
         e.printStackTrace();
      }
   }

   public static Set<Entry<String, ResourceFile>> getAllFiles() {
      return resources.files.entrySet();
   }

   public static byte[] getResourceBytes(String path) throws IOException {
      if (resources == null) {
         throw new IllegalStateException("Resources not loaded yet.");
      } else {
         ResourceFile file = resources.files.get(path);
         if (file != null) {
            return file.loadBytes(true);
         } else {
            throw new FileNotFoundException("Could not find resource file \"" + path + "\"");
         }
      }
   }

   public static InputStream getResourceInputStream(String path) throws IOException {
      return new ByteArrayInputStream(getResourceBytes(path));
   }

   static {
      Collections.addAll(fileExtensions, "png", "ogg", "glsl", "ttf");

      for (int i = 1; i <= 5; i++) {
         ignoreFiles.add("sound/temp/temp" + i + ".ogg");
      }

      resources = null;
   }
}
