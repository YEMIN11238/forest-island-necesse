import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import net.bytebuddy.jar.asm.*;
public class StaticInitAudit {
 static Map<String,List<String>> read(String path)throws Exception{
  Map<String,List<String>> out=new TreeMap<>();
  try(ZipFile z=new ZipFile(path)){
   Enumeration<? extends ZipEntry> es=z.entries();
   while(es.hasMoreElements()){
    ZipEntry e=es.nextElement();if(!e.getName().endsWith(".class"))continue;
    byte[] bytes=z.getInputStream(e).readAllBytes();final List<String> fields=new ArrayList<>();
    ClassReader cr=new ClassReader(bytes);String owner=cr.getClassName();
    cr.accept(new ClassVisitor(Opcodes.ASM9){
     @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] x){
      if(!n.equals("<clinit>"))return null;
      return new MethodVisitor(Opcodes.ASM9){@Override public void visitFieldInsn(int opcode,String o,String n,String d){if(opcode==Opcodes.PUTSTATIC&&o.equals(owner)&&!n.startsWith("$"))fields.add(n+":"+d);}};
     }
    },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
    if(!fields.isEmpty())out.put(owner,fields);
   }
  }return out;
 }
 static String quote(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
 static String arr(List<String> xs){return "["+String.join(",",xs.stream().map(StaticInitAudit::quote).toList())+"]";}
 public static void main(String[] args)throws Exception{
  Map<String,List<String>> a=read(args[0]);a.putAll(read(args[1]));Map<String,List<String>> b=read(args[2]);List<String> diffs=new ArrayList<>();List<String> missing=new ArrayList<>();int checked=0;
  for(String k:a.keySet()){if(!b.containsKey(k)){missing.add(k);continue;}checked++;if(!a.get(k).equals(b.get(k)))diffs.add("{\"class\":"+quote(k)+",\"original\":"+arr(a.get(k))+",\"rebuilt\":"+arr(b.get(k))+"}");}
  System.out.println("{\"originalClassesWithStaticWrites\":"+a.size()+",\"comparedClasses\":"+checked+",\"missingOriginalClasses\":"+arr(missing)+",\"differences\":["+String.join(",",diffs)+"]}");
 }
}
