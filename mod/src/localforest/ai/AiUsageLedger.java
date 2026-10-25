package localforest.ai;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Local, durable, text-free usage accounting. Reservations survive errors and app restarts. */
final class AiUsageLedger {
    record Reservation(String id,String day,long micros,boolean background) {}
    record Totals(int requests,long micros) {}
    private final DeepSeekConfig config;
    private volatile Totals last=new Totals(0,0);
    private volatile String lastDay="";
    AiUsageLedger(DeepSeekConfig config){this.config=config;}
    private static String day(){return LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();}
    static long conservativeMicros(long inputTokens,long outputTokens){
        // 2026-10-23 published peak Flash rates: RMB 2/M input, RMB 8/M output.
        // Cache hits/off-peak savings are intentionally excluded from this estimate.
        return Math.addExact(Math.multiplyExact(Math.max(0,inputTokens),2),Math.multiplyExact(Math.max(0,outputTokens),8));
    }
    synchronized Reservation reserve(int requestUtf8Bytes) throws IOException {
        return reserve(requestUtf8Bytes,false);
    }
    synchronized Reservation reserve(int requestUtf8Bytes,boolean background) throws IOException {
        long amount=conservativeMicros(requestUtf8Bytes+1024L,config.maxOutputTokens);
        Reservation r=new Reservation(UUID.randomUUID().toString(),day(),amount,background);
        write(r,"reserve",amount,-1,-1);return r;
    }
    synchronized void settle(Reservation r,long input,long output) throws IOException {
        if(input<0||output<0)return; // Missing usage retains the conservative reservation.
        write(r,"settle",conservativeMicros(input,output),input,output);
    }
    private void write(Reservation r,String event,long amount,long input,long output) throws IOException {
        Files.createDirectories(config.directory);
        Path path=config.directory.resolve("usage-"+r.day()+".jsonl");
        try(FileChannel channel=FileChannel.open(path,Set.of(StandardOpenOption.CREATE,StandardOpenOption.READ,StandardOpenOption.WRITE),
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            var lock=channel.lock()) {
            if(channel.size()>1_000_000)throw new IOException("AI 用量记录过大，已停止调用");
            ByteBuffer bytes=ByteBuffer.allocate((int)channel.size());while(bytes.hasRemaining()&&channel.read(bytes)>=0){}
            Map<String,Long> charges=new HashMap<>();int requests=0,backgroundRequests=0;
            try {
                for(String line:new String(bytes.array(),StandardCharsets.UTF_8).split("\n"))if(!line.isBlank()){
                    JsonObject row=Json.parse(line).asObject();String id=row.getString("id","");
                    if(row.getString("event","").equals("reserve")){requests++;if(row.getString("category","player").equals("ambient"))backgroundRequests++;}
                    charges.put(id,row.getLong("costMicros",0));
                }
            }catch(RuntimeException invalid){throw new IOException("AI 用量记录无法读取，已停止调用");}
            long total=charges.values().stream().mapToLong(Long::longValue).sum();
            if(event.equals("reserve")){
                if(requests>=config.dailyRequests||total+amount>config.dailyBudgetMicros)
                    throw new IOException("今天的 AI 用量额度已到上限；离线任务仍可用");
                if(r.background()&&backgroundRequests>=Math.min(40,Math.max(1,config.dailyRequests/3)))
                    throw new IOException("今天的背景交谈额度已用完；玩家交谈和离线活动仍可用");
                requests++;
            }else if(!charges.containsKey(r.id()))throw new IOException("AI 用量预约不存在");
            total-=charges.getOrDefault(r.id(),0L);total+=amount;
            JsonObject row=new JsonObject().add("event",event).add("id",r.id()).add("costMicros",amount)
                .add("inputTokens",input).add("outputTokens",output).add("model","deepseek-flash").add("category",r.background()?"ambient":"player");
            channel.position(channel.size());ByteBuffer line=StandardCharsets.UTF_8.encode(row+"\n");while(line.hasRemaining())channel.write(line);channel.force(false);
            last=new Totals(requests,total);lastDay=r.day();
        }
    }
    Totals latest(){return day().equals(lastDay)?last:new Totals(0,0);}
}
