#!/usr/bin/env python3
"""Inspect source reconstruction and shipped/rebuilt structure; never launch the game."""
from pathlib import Path
import hashlib,json,re,subprocess,sys,zipfile
root=Path(__file__).resolve().parents[1];work=root/'rebuild';resources=root/'runtime/Necesse.app/Contents/Resources'
jdk=Path('/Users/zhangjunhao/Documents/Codex/tools/jdk-21.0.12.1+1/Contents/Home/bin')
def sha(data):return hashlib.sha256(data).hexdigest()
subprocess.run([sys.executable,str(work/'compare_api.py')],check=True)
asm=resources/'lib/byte-buddy-1.12.8.jar';helper=work/'helpers'
subprocess.run([str(jdk/'javac'),'-cp',str(asm),str(helper/'StaticInitAudit.java')],check=True)
static=subprocess.check_output([str(jdk/'java'),'-cp',str(helper)+':'+str(asm),'StaticInitAudit',str(resources/'Necesse.jar'),str(resources/'Server.jar'),str(work/'Necesse-source-built-1.3.3.jar')])
(work/'static-init-order.json').write_bytes(static)
static_report=json.loads(static)
source_files=sorted((root/'source-rebuild').rglob('*.java'));markers=[];inventory=[]
for p in source_files:
 data=p.read_bytes();text=data.decode();name=str(p.relative_to(root/'source-rebuild'))
 inventory.append({'source':name,'sha256':sha(data),'bytes':len(data)})
 for m in re.finditer(r"couldn.t be decompiled|Could not decompile|Method not decompiled|\$VF:|<unrepresentable>",text):markers.append({'source':name,'line':text.count('\n',0,m.start())+1,'marker':m.group()})
(work/'source-inventory.json').write_text(json.dumps(inventory,indent=2))
original_names=set()
for name in ['Necesse.jar','Server.jar']:
 with zipfile.ZipFile(resources/name) as z:original_names.update(n for n in z.namelist() if n.endswith('.class'))
libs=sorted((resources/'lib').glob('*.jar'));leaked=[]
for p in libs:
 with zipfile.ZipFile(p) as z:
  for n in z.namelist():
   if n in original_names:leaked.append({'library':p.name,'entry':n})
jars=[]
for original,rebuilt in [('Necesse.jar','Necesse-source-built-1.3.3.jar'),('Server.jar','Server-source-built-1.3.3.jar')]:
 with zipfile.ZipFile(resources/original) as a,zipfile.ZipFile(work/rebuilt) as b:
  om=a.read('META-INF/MANIFEST.MF');bm=b.read('META-INF/MANIFEST.MF')
  jars.append({'path':str(work/rebuilt),'sha256':sha((work/rebuilt).read_bytes()),'classFiles':sum(n.endswith('.class') for n in b.namelist()),'originalManifestByteIdentical':om==bm,'manifestMaxLineBytes':max(map(len,bm.splitlines()))})
api=json.loads((work/'api-comparison.json').read_text())
report={'sourceFiles':len(source_files),'sourceInventory':'source-inventory.json','sourceDecompilerFailureMarkers':markers,'compilerDependencyJars':len(libs),'compilerDependencyGameClassEntries':leaked,'originalGameJarRequiredToCompile':False,'jars':jars,'namedClassAPI':{'compared':api['checkedClasses'],'missing':len(api['missingNamedClasses']),'different':len(api['apiDifferences'])},'staticFieldWriteSequence':static_report,'limits':['Anonymous and synthetic-numbered classes are excluded from named API comparison; compiler may regenerate different anonymous class numbering.','Static check compares same-owner PUTSTATIC sequences excluding $-prefixed helpers, not arbitrary instruction equivalence.','No whole-game behavioral equivalence claim. Runtime tests belong to parent task evidence.']}
report['passed']=len(source_files)==6504 and not markers and not leaked and all(j['originalManifestByteIdentical'] for j in jars) and not api['missingNamedClasses'] and not api['apiDifferences'] and not static_report['differences'] and not static_report['missingOriginalClasses']
(work/'verification.json').write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2));raise SystemExit(0 if report['passed'] else 1)
