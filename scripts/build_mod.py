from pathlib import Path
import subprocess,zipfile,json,hashlib,shutil
root=Path(__file__).resolve().parents[1]
jdk=Path('/Users/zhangjunhao/Documents/Codex/tools/jdk-21.0.12.1+1/Contents/Home/bin')
r=root/'runtime/Necesse.app/Contents/Resources'
classes=root/'mod/build/classes'
if classes.exists():shutil.rmtree(classes)
classes.mkdir(parents=True,exist_ok=True)
sources=sorted((root/'mod/src').rglob('*.java'))
kernel=r/'Necesse-source-built-1.3.3.jar'
if not kernel.exists():kernel=r/'Necesse.jar'
cp=':'.join(str(p) for p in [kernel,*sorted((r/'lib').glob('*.jar'))])
args=[str(jdk/'javac'),'--release','17','-encoding','UTF-8','-classpath',cp,'-d',str(classes),*[str(s) for s in sources]]
run=subprocess.run(args,text=True,capture_output=True)
(root/'evidence/mod-build.log').write_text(run.stdout+run.stderr)
if run.returncode:
 print(run.stdout+run.stderr);raise SystemExit(run.returncode)
out=root/'mod/dist';out.mkdir(exist_ok=True)
jar=out/'localforest-island.jar'
with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
 z.write(root/'mod/mod.info','mod.info')
 for p in sorted(classes.rglob('*.class')):z.write(p,str(p.relative_to(classes)))
 for p in sorted((root/'mod/resources').rglob('*')):
  if p.is_file():z.write(p,'resources/'+str(p.relative_to(root/'mod/resources')))
result={'javaSources':len(sources),'classes':len(list(classes.rglob('*.class'))),'jar':str(jar),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'classpathKernelSha256':hashlib.sha256(kernel.read_bytes()).hexdigest(),'compilation':'javac --release 17','scope':'Mod compilation; does not assert full recovered kernel recompilation or runtime correctness.'}
(root/'evidence/mod-build.json').write_text(json.dumps(result,indent=2));print(json.dumps(result))
