package necesse.reports;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import necesse.engine.GameAuth;
import necesse.engine.Settings;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.util.GameUtils;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.presets.Preset;

public class ReportUtils {
   private static SessionStats sessionStats;
   private static long lastSessionStatsTime;

   private static String getLocale(String category, String key, String errorDefault) {
      try {
         return Localization.translate(category, key);
      } catch (Exception e) {
         return errorDefault;
      }
   }

   private static String getString(Supplier<Object> supplier) {
      return getString(supplier, e -> "ERR: " + e.getClass().getSimpleName());
   }

   private static String getString(Supplier<Object> supplier, Function<Exception, Object> errReturn) {
      try {
         return String.valueOf(supplier.get());
      } catch (Exception var5) {
         Exception e1 = var5;

         try {
            return String.valueOf(errReturn.apply(e1));
         } catch (Exception e2) {
            return "ERR_RETURN";
         }
      }
   }

   public static String sendCrashReport(CrashReportData data, String userDetails) {
      try {
         HashMap<String, String> body = new HashMap<>(data.data);
         body.replaceAll((k, v) -> v == null ? "null" : v);
         body.put("user_details", userDetails);
         body.put("report", getString(() -> data.getFullReport(null)));
         body.put("details", userDetails);
         sendBody(body, "");
         return null;
      } catch (MalformedURLException | UnsupportedEncodingException | ProtocolException e) {
         e.printStackTrace();
         return getLocale("ui", "sendreporterr", "Unknown error sending report");
      } catch (IOException e) {
         e.printStackTrace();
         return getLocale("ui", "sendreportfail", "Could not send report, please try again!");
      }
   }

   public static String sendFeedback(FeedbackData data, String message) {
      try {
         Map<String, String> body = new HashMap<>(data.data);
         body.put("user_message", message);
         body.put("feedback", data.generateFullFeedback(message));
         sendBody(body, "");
         return null;
      } catch (MalformedURLException | UnsupportedEncodingException | ProtocolException e) {
         e.printStackTrace();
         return getLocale("ui", "sendfeedbackerr", "Unknown error sending feedback");
      } catch (IOException e) {
         e.printStackTrace();
         return getLocale("ui", "sendfeedbackfail", "Could not send feedback, please try again!");
      }
   }

   public static String submitPreset(
      BasicsData basicsData, Preset preset, GameTexture previewTexture, String title, String details, boolean includeCredits, String creditsName
   ) {
      try {
         String script = preset.getCompressedBase64Script();
         byte[] pngBytes = previewTexture.encodeTextureToPNGBytes();
         Map<String, String> body = new HashMap<>(basicsData.data);
         body.put("preset_script", script);
         body.put("preset_title", title);
         body.put("preset_details", details);
         body.put("preset_include_credits", Boolean.toString(includeCredits));
         body.put("preset_credits_name", creditsName);
         LinkedList<ReportUtils.HTTPContent> content = createBodyContent(body);
         content.add(new ReportUtils.HTTPPNGContent("preset_preview", pngBytes));
         int responseCode = sendBody(content, "/preset", 30000);
         return responseCode != 200 ? new LocalMessage("ui", "presetsubmiterrcode", "error", responseCode).translate() : null;
      } catch (MalformedURLException | UnsupportedEncodingException | ProtocolException e) {
         e.printStackTrace();
         return getLocale("ui", "presetsubmiterr", "Unknown error submitting preset");
      } catch (IOException e) {
         e.printStackTrace();
         return getLocale("ui", "presetsubmitfail", "Could not submit preset, please try again!");
      }
   }

   public static long getSessionID() {
      if (sessionStats == null) {
         long auth = GameAuth.getAuthentication();
         Random random = new Random();
         if (auth != 0L) {
            random.nextInt((int)auth);
         }

         sessionStats = new SessionStats(random.nextLong());
         lastSessionStatsTime = System.currentTimeMillis();
      }

      return sessionStats.sessionID;
   }

   public static void updateSessionSeconds(Client client) {
      if (sessionStats == null) {
         getSessionID();
      }

      sessionStats.update(client);
      long timeSinceLastSessionStats = System.currentTimeMillis() - lastSessionStatsTime;
      int cooldown = 3600000;
      if (timeSinceLastSessionStats > cooldown) {
         new Thread(() -> {
            try {
               sendBody(sessionStats.getBodyContent(), "/session/stats", 5000);
            } catch (IOException var1x) {
            }
         }, "session-stats").start();
         lastSessionStatsTime = System.currentTimeMillis();
      }
   }

   public static void sendSessionStart(long loadingTime) {
      new Thread(() -> {
         try {
            sendBody((new SessionData("start", loadingTime)).data, "/session", 10000);
         } catch (IOException var3) {
         }
      }, "session-start").start();
   }

   public static void sendSessionEnd() {
      new Thread(() -> {
         try {
            LinkedList<ReportUtils.HTTPContent> bodyContent = createBodyContent((new SessionData("end", 0L)).data);
            if (sessionStats != null) {
               bodyContent.add(new ReportUtils.HTTPStringContent("session_stats", "stats_"));
               sessionStats.addBodyContent("stats_", bodyContent);
            }

            sendBody(bodyContent, "/session", 5000);
         } catch (IOException var1) {
         }
      }, "session-end").start();
   }

   public static String submitNewsletterSignup(String email) {
      Map<String, String> body = new HashMap<>();
      body.put("user_email", email);
      body.put("user_language", Settings.language);
      String error = null;

      try {
         int result = sendBody(body, "/newsletter/signup");
         if (result != 200) {
            error = Localization.translate("misc", "newslettercodeerror", "code", result);
         }
      } catch (MalformedURLException | UnsupportedEncodingException | ProtocolException e) {
         error = Localization.translate("misc", "newsletterunknownerror");
         e.printStackTrace();
      } catch (IOException e) {
         error = Localization.translate("misc", "newslettersignupfail");
         e.printStackTrace();
      }

      return error;
   }

   private static int sendBody(Map<String, String> body, String path) throws IOException {
      return sendBody(body, path, 30000);
   }

   private static int sendBody(Map<String, String> body, String path, int timeoutMillis) throws IOException {
      return sendBody(createBodyContent(body), path, timeoutMillis);
   }

   private static LinkedList<ReportUtils.HTTPContent> createBodyContent(Map<String, String> body) {
      LinkedList<ReportUtils.HTTPContent> content = new LinkedList<>();

      for (Entry<String, String> entry : body.entrySet()) {
         content.add(new ReportUtils.HTTPStringContent(entry.getKey(), entry.getValue()));
      }

      return content;
   }

   private static int sendBody(Iterable<ReportUtils.HTTPContent> content, String path, int timeoutMillis) throws IOException {
      URL url = new URL(
         GameUtils.getStringFromBytes(new BigInteger("202921288190179107278106653765607798678877561825639252414760966464065470894813073191989").toByteArray())
            + path
      );
      URLConnection conn = url.openConnection();
      HttpURLConnection http = (HttpURLConnection)conn;
      http.setRequestMethod("POST");
      http.setDoOutput(true);
      String boundary = UUID.randomUUID().toString();
      String lineSeparator = "\r\n";
      http.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
      ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "report-utils"));
      Future<Integer> future = executor.submit(() -> {
         http.connect();
         http.setConnectTimeout(timeoutMillis);
         http.setReadTimeout(timeoutMillis);
         OutputStream os = http.getOutputStream();

         try {
            DataOutputStream writer = new DataOutputStream(os);

            try {
               for (ReportUtils.HTTPContent httpContent : content) {
                  ReportUtils.HTTPContent.writeString(writer, "--" + boundary + lineSeparator);
                  httpContent.write(writer, lineSeparator);
               }

               ReportUtils.HTTPContent.writeString(writer, "--" + boundary + "--" + lineSeparator);
            } catch (Throwable var11) {
               try {
                  writer.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }

               throw var11;
            }

            writer.close();
         } catch (Throwable var12) {
            if (os != null) {
               try {
                  os.close();
               } catch (Throwable var9x) {
                  var12.addSuppressed(var9x);
               }
            }

            throw var12;
         }

         if (os != null) {
            os.close();
         }

         return http.getResponseCode();
      });

      try {
         return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
         future.cancel(true);
         http.disconnect();
      } catch (Exception e) {
         if (e.getCause() instanceof IOException) {
            throw (IOException)e.getCause();
         }

         throw new IOException(e);
      } finally {
         executor.shutdownNow();
      }

      return 408;
   }

   public abstract static class HTTPContent {
      public abstract void write(DataOutputStream var1, String var2) throws IOException;

      public static void writeString(DataOutputStream writer, String string) throws IOException {
         writer.write(GameUtils.getStringBytes(string));
      }
   }

   public static class HTTPPNGContent extends ReportUtils.HTTPContent {
      private final String name;
      private final byte[] pngBytes;

      public HTTPPNGContent(String name, byte[] pngBytes) {
         this.name = name;
         this.pngBytes = pngBytes;
      }

      @Override
      public void write(DataOutputStream writer, String lineSeparator) throws IOException {
         writeString(writer, "Content-Disposition: form-data; name=\"" + this.name + "\"; filename=\"" + this.name + ".png\"" + lineSeparator);
         writeString(writer, "Content-Type: image/png" + lineSeparator);
         writeString(writer, lineSeparator);
         writer.write(this.pngBytes);
         writeString(writer, lineSeparator);
      }
   }

   public static class HTTPStringContent extends ReportUtils.HTTPContent {
      private final String name;
      private final String value;

      public HTTPStringContent(String name, String value) {
         this.name = name;
         this.value = value;
      }

      @Override
      public void write(DataOutputStream writer, String lineSeparator) throws IOException {
         writeString(writer, "Content-Disposition: form-data; name=\"" + this.name + "\"" + lineSeparator);
         writeString(writer, "Content-Type: text/plain; charset=UTF-8" + lineSeparator);
         writeString(writer, lineSeparator);
         writeString(writer, this.value + lineSeparator);
      }
   }
}
