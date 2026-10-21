#!/usr/bin/env python3
"""Compile every recovered game source, without original game classes on classpath."""
from pathlib import Path
import argparse,json,time,subprocess,zipfile,hashlib,shutil
parser=argparse.ArgumentParser();parser.add_argument('--tag',default='latest');args=parser.parse_args()
root=Path(__file__).resolve().parents[1];source=root/'source-rebuild';work=root/'rebuild';out=work/'classes'
if out.exists():shutil.rmtree(out)
out.mkdir(exist_ok=True)
resources=root/'runtime/Necesse.app/Contents/Resources'
if not (resources/'lib').is_dir():raise SystemExit('Missing private runtime dependency directory: '+str(resources/'lib'))
javac='/Users/zhangjunhao/Documents/Codex/tools/jdk-21.0.12.1+1/Contents/Home/bin/javac'
source_files=sorted(source.rglob('*.java'))
argfile=work/'sources.args';argfile.write_text('\n'.join('"'+str(p)+'"' for p in source_files))
classpath=':'.join(str(p) for p in sorted((resources/'lib').glob('*.jar')))
cmd=[javac,'-J-Xmx3g','-J-Duser.language=en','-J-Duser.country=US','-XDcompilePolicy=simple','-XDshould-stop.ifError=FLOW','--release','17','-encoding','UTF-8','-Xmaxerrs','10000','-Xmaxwarns','100','-cp',classpath,'-d',str(out),'@'+str(argfile)]
start=time.monotonic()
with (work/f'javac-{args.tag}.log').open('w') as log:run=subprocess.run(cmd,stdout=log,stderr=log)
result={'tag':args.tag,'exitCode':run.returncode,'elapsedSeconds':time.monotonic()-start,'sourceFiles':len(source_files),'compiledClassFiles':sum(1 for p in out.rglob('*.class')),'originalGameJarOnClasspath':False,'command':cmd}
(work/f'build-{args.tag}.json').write_text(json.dumps(result,indent=2))
if run.returncode==0:
 jars=[]
 for original,destination in [('Necesse.jar','Necesse-source-built-1.3.3.jar'),('Server.jar','Server-source-built-1.3.3.jar')]:
  jar=work/destination
  manifest_path=work/'manifests'/(Path(original).stem+'.MF')
  manifest=manifest_path.read_bytes()
  with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
   z.writestr('META-INF/MANIFEST.MF',manifest)
   for p in sorted(out.rglob('*.class')):z.write(p,str(p.relative_to(out)))
  jars.append({'originalManifestFrom':str(manifest_path),'path':str(jar),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest()})
 result.update(jars=jars)
 (work/f'build-{args.tag}.json').write_text(json.dumps(result,indent=2))
print(json.dumps({k:v for k,v in result.items() if k!='command'}))
raise SystemExit(run.returncode)
