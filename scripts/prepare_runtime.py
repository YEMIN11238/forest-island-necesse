from pathlib import Path
import json,hashlib,shutil,struct,zipfile
root=Path(__file__).resolve().parents[1]
app=Path('/Users/zhangjunhao/Library/Application Support/Steam/steamapps/common/Necesse/Necesse.app')
dest=root/'runtime/Necesse.app'
if not dest.exists():
 dest.parent.mkdir(parents=True,exist_ok=True)
 shutil.copytree(app,dest,symlinks=True)
r=dest/'Contents/Resources'
resources=root/'resources-original'
entries=[]
data=(r/'res.data').read_bytes();offset=0
def read_int():
 global offset
 value=struct.unpack_from('<I',data,offset)[0];offset+=4;return value
count=read_int()
assert 0<count<100000,count
for i in range(count):
 length=read_int();assert length<4096
 path=data[offset:offset+length].decode('utf-8');offset+=length
 size=read_int();assert offset+size<=len(data)
 rel=Path(path);assert not rel.is_absolute() and '..' not in rel.parts
 target=resources/rel
 payload=data[offset:offset+size]
 if not target.exists():
  target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(payload)
 entries.append({'path':path,'offset':offset,'bytes':size,'sha256':hashlib.sha256(payload).hexdigest()})
 offset+=size
assert offset==len(data),(offset,len(data))
(root/'evidence/resource-inventory.json').write_text(json.dumps({'source':str(app/'Contents/Resources/res.data'),'sha256':hashlib.sha256(data).hexdigest(),'format':'ResourceFolder.read: little-endian count, UTF8 path length/path, payload length/payload','count':count,'parsedEnd':offset,'totalBytes':len(data),'entries':entries},indent=2))
main=zipfile.ZipFile(r/'Necesse.jar');server=zipfile.ZipFile(r/'Server.jar')
mc={n:hashlib.sha256(main.read(n)).hexdigest() for n in main.namelist() if n.endswith('.class')}
sc={n:hashlib.sha256(server.read(n)).hexdigest() for n in server.namelist() if n.endswith('.class')}
comparison={'mainClasses':len(mc),'serverClasses':len(sc),'onlyServer':sorted(sc.keys()-mc.keys()),'onlyClientCount':len(mc.keys()-sc.keys()),'sharedClassesChanged':[n for n in mc.keys()&sc.keys() if mc[n]!=sc[n]]}
(root/'evidence/server-client-comparison.json').write_text(json.dumps(comparison,indent=2))
print(json.dumps({'runtime':str(dest),'resources':count,'resourceBytes':len(data),'serverComparison':comparison},ensure_ascii=False))
