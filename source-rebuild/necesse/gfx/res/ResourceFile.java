package necesse.gfx.res;

import java.io.IOException;
import java.io.InputStream;
import necesse.engine.modLoader.InputStreamSupplier;
import necesse.engine.util.GameUtils;

public class ResourceFile {
   public final String path;
   private final int length;
   private final InputStreamSupplier inputStreamSupplier;

   public ResourceFile(String path, int length, InputStreamSupplier inputStreamSupplier) {
      this.path = path;
      this.length = length;
      this.inputStreamSupplier = inputStreamSupplier;
   }

   public byte[] loadBytes(boolean throwOnUnexpectedLength) throws IOException {
      InputStream inputStream = this.inputStreamSupplier.get();

      byte[] data;
      label49: {
         byte[] var5;
         try {
            if (this.length == -1) {
               data = GameUtils.loadInputStream(inputStream);
               break label49;
            }

            data = new byte[this.length];
            int read = inputStream.read(data, 0, this.length);
            if (throwOnUnexpectedLength && read != this.length) {
               throw new IOException("Read unexpected size of resource file " + this.path + ": " + read + " bytes read out of " + this.length);
            }

            var5 = data;
         } catch (Throwable var7) {
            if (inputStream != null) {
               try {
                  inputStream.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (inputStream != null) {
            inputStream.close();
         }

         return var5;
      }

      if (inputStream != null) {
         inputStream.close();
      }

      return data;
   }
}
