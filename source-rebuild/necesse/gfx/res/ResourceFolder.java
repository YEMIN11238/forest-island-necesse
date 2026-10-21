package necesse.gfx.res;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import necesse.engine.GameLog;
import necesse.engine.modLoader.GameFileEntry;
import necesse.engine.modLoader.InputStreamSupplier;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.util.GameUtils;

public class ResourceFolder {
   public final HashMap<String, ResourceFile> files;

   public ResourceFolder(List<GameFileEntry> files) throws IOException {
      this.files = new HashMap<>();
      this.addModResources(files, null);
   }

   public ResourceFolder(File folder) throws IOException {
      Objects.requireNonNull(folder);
      if (!folder.isDirectory()) {
         throw new IllegalStateException("Folder doesn't exist or is not directory");
      }

      HashMap<String, ResourceFile> filesList = new HashMap<>();
      this.addResourceFiles("", folder, filesList);
      this.files = filesList;
   }

   public ResourceFolder() {
      this.files = new HashMap<>();
   }

   private void addResourceFiles(String parentDir, File folder, HashMap<String, ResourceFile> map) throws IOException {
      File[] files = folder.listFiles();
      if (files == null) {
         throw new NullPointerException("Could not read files inside folder " + folder.getPath());
      }

      for (File file : files) {
         if (file.isDirectory()) {
            this.addResourceFiles(parentDir + file.getName() + "/", file, map);
         } else {
            String filePath = parentDir + file.getName();
            if (!ResourceEncoder.ignoreFiles.contains(filePath)) {
               String fileExtension = GameUtils.getFileExtension(filePath);
               if (fileExtension != null && ResourceEncoder.fileExtensions.contains(fileExtension)) {
                  map.put(filePath, new ResourceFile(filePath, -1, () -> Files.newInputStream(file.toPath())));
               }
            }
         }
      }
   }

   public void addResourceFile(String resourcePath, File file) {
      this.addResourceFile(resourcePath, () -> Files.newInputStream(file.toPath()));
   }

   public void addResourceFile(String resourcePath, InputStreamSupplier inputStreamSupplier) {
      this.files.put(resourcePath, new ResourceFile(resourcePath, -1, inputStreamSupplier));
   }

   public void addModResources(Iterable<GameFileEntry> files, LoadedMod mod) throws IOException {
      LinkedList<String> overrides = new LinkedList<>();
      int adds = 0;

      for (GameFileEntry file : files) {
         if (file.getPath().startsWith("resources/") && !file.getPath().equals("resources/preview.png")) {
            String pathStripped = file.getPath().substring("resources/".length());
            if (pathStripped.equals("res.data")) {
               GameStreamReader reader = new GameStreamReader(file::getFileInputStream);

               try {
                  this.read(reader);
               } catch (Throwable var12) {
                  try {
                     reader.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }

                  throw var12;
               }

               reader.close();
            } else {
               for (String fileExtension : ResourceEncoder.fileExtensions) {
                  if (pathStripped.endsWith(fileExtension)) {
                     if (this.files.containsKey(pathStripped)) {
                        overrides.add(pathStripped);
                     } else {
                        adds++;
                     }

                     this.files.put(pathStripped, new ResourceFile(pathStripped, -1, file::getFileInputStream));
                     break;
                  }
               }
            }
         }
      }

      if (mod != null && (adds > 0 || overrides.size() > 0)) {
         if (overrides.size() > 10) {
            System.out
               .println("Mod \"" + mod.id + "\" added " + adds + " resources and overwrote " + overrides.size() + " existing. Check out log file for full list");

            for (String override : overrides) {
               GameLog.file.println("Mod \"" + mod.id + "\" overwrote resource: " + override);
            }
         } else {
            System.out.println("Mod \"" + mod.id + "\" added " + adds + " resources and overwrote " + overrides.size() + " existing:");

            for (String override : overrides) {
               System.out.println("Mod \"" + mod.id + "\" overwrote resource: " + override);
            }
         }
      }
   }

   public void read(GameStreamReader reader) throws IOException {
      int filesCount = reader.readInt();

      for (int i = 0; i < filesCount; i++) {
         int pathBytesSize = reader.readInt();
         byte[] pathBytes = reader.readBytes(pathBytesSize);
         String path = GameUtils.getStringFromBytes(pathBytes);
         int bytesLength = reader.readInt();
         this.files.put(path, new ResourceFile(path, bytesLength, reader.getSupplierAtCurrent()));
         reader.skipBytes(bytesLength);
      }
   }

   public long write(GameStreamWriter writer) throws IOException {
      long size = 0L;
      writer.writeInt(this.files.size());

      for (ResourceFile file : this.files.values()) {
         byte[] pathBytes = GameUtils.getStringBytes(file.path);
         writer.writeInt(pathBytes.length);
         writer.writeBytes(pathBytes);
         byte[] bytes = file.loadBytes(true);
         size += bytes.length;
         writer.writeInt(bytes.length);
         writer.writeBytes(bytes);
      }

      return size;
   }

   public void printTree() {
      List<ResourceFile> files = new ArrayList<>(this.files.values());
      files.sort(Comparator.comparing(a -> a.path));

      for (ResourceFile file : files) {
         System.out.println(file.path);
      }
   }
}
