package localforest.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Explicit local opt-in; credentials never come from the mod, saves or bundled resources. */
public final class DeepSeekConfig {
    public final Path directory;
    public final long dailyBudgetMicros;
    public final int dailyRequests;
    public final int maxOutputTokens;
    private final String key;

    DeepSeekConfig(Path directory,String key,long dailyBudgetMicros,int dailyRequests,int maxOutputTokens) {
        this.directory=directory;this.key=key;this.dailyBudgetMicros=dailyBudgetMicros;
        this.dailyRequests=dailyRequests;this.maxOutputTokens=maxOutputTokens;
    }
    public static DeepSeekConfig load() throws IOException {
        Path folder=Path.of(System.getProperty("mosslume.ai.directory",
            Path.of(System.getProperty("user.home"),"Library","Application Support","Mosslume-0.4","ai").toString()));
        Path file=folder.resolve("deepseek.properties");
        if(!Files.isRegularFile(file))return null;
        Properties settings=new Properties();try(var reader=Files.newBufferedReader(file)){settings.load(reader);}
        if(!Boolean.parseBoolean(settings.getProperty("enabled","false")))return null;
        if(!settings.getProperty("model","deepseek-flash").equals("deepseek-flash"))
            throw new IOException("当前预算配置仅支持 deepseek-flash");
        String key=Files.readString(folder.resolve("deepseek.key")).strip();
        if(!key.startsWith("sk-")||key.length()>256||key.chars().anyMatch(Character::isWhitespace))
            throw new IOException("DeepSeek Key 格式无效");
        try {
            long budget=Math.round(Double.parseDouble(settings.getProperty("dailyBudgetRmb","5"))*1_000_000);
            int requests=Integer.parseInt(settings.getProperty("dailyRequests","200"));
            int output=Integer.parseInt(settings.getProperty("maxOutputTokens","600"));
            if(budget<=0||budget>50_000_000||requests<1||requests>1000||output<64||output>1000)
                throw new IllegalArgumentException();
            return new DeepSeekConfig(folder,key,budget,requests,output);
        }catch(IllegalArgumentException error){throw new IOException("DeepSeek 用量限制配置无效");}
    }
    String authorization(){return "Bearer "+key;}
    @Override public String toString(){return "DeepSeekConfig[credential=redacted, model=deepseek-flash]";}
}
