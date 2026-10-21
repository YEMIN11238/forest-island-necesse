#!/usr/bin/env python3
"""Compare shipped and source-built named class APIs without executing target code."""
from pathlib import Path
import struct,zipfile,json,re
root=Path(__file__).resolve().parents[1];resources=root/'runtime/Necesse.app/Contents/Resources'
class Reader:
 def __init__(self,data):self.data=data;self.i=0
 def u1(self):v=self.data[self.i];self.i+=1;return v
 def u2(self):v=struct.unpack_from('>H',self.data,self.i)[0];self.i+=2;return v
 def u4(self):v=struct.unpack_from('>I',self.data,self.i)[0];self.i+=4;return v
 def read(self,n):v=self.data[self.i:self.i+n];self.i+=n;return v
 def attrs(self):
  for _ in range(self.u2()):self.u2();self.read(self.u4())
def parse(data):
 r=Reader(data);assert r.u4()==0xcafebabe;r.u2();major=r.u2();n=r.u2();cp=[None]*n;i=1
 while i<n:
  tag=r.u1()
  if tag==1:cp[i]=r.read(r.u2()).decode('utf-8',errors='replace')
  elif tag in [3,4]:r.read(4)
  elif tag in [5,6]:r.read(8);i+=1
  elif tag in [7,8,16,19,20]:cp[i]=r.u2()
  elif tag in [9,10,11,12,17,18]:r.read(4)
  elif tag==15:r.read(3)
  else:raise ValueError(tag)
  i+=1
 access=r.u2();name=cp[cp[r.u2()]];r.u2()
 for _ in range(r.u2()):r.u2()
 fields=[];methods=[]
 for group,kind in [(fields,'field'),(methods,'method')]:
  for _ in range(r.u2()):
   flags=r.u2();name_=cp[r.u2()];desc=cp[r.u2()];r.attrs()
   if flags&5 and not flags&0x1000 and not(kind=='method' and flags&0x40):group.append((name_,desc,flags&0x0dff))
 return {'name':name,'major':major,'publicOrProtected':bool(access&5),'fields':fields,'methods':methods}
original={}
for jar in [resources/'Necesse.jar',resources/'Server.jar']:
 with zipfile.ZipFile(jar) as z:
  for name in z.namelist():
   if name.endswith('.class'):original[name]=z.read(name)
rebuilt=zipfile.ZipFile(root/'rebuild/Necesse-source-built-1.3.3.jar');rebuilt_names=set(rebuilt.namelist())
missing=[];diff=[];checked=0
for path,data in original.items():
 if re.search(r'\$\d',path):continue
 a=parse(data)
 if path not in rebuilt_names:missing.append(path);continue
 b=parse(rebuilt.read(path));checked+=1
 delta={}
 for group in ['fields','methods']:
  before={tuple(i) for i in a[group]};after={tuple(i) for i in b[group]}
  if before!=after:delta[group]={'removed':sorted(before-after),'added':sorted(after-before)}
 if delta:diff.append({'class':path,'changes':delta})
report={'scope':'Named classes (anonymous/synthetic-numbered classes excluded); public/protected non-synthetic fields and non-bridge methods, including descriptors and access flags. No behavioral equivalence claim.','checkedClasses':checked,'missingNamedClasses':missing,'apiDifferences':diff,'originalAllClassFiles':len(original),'rebuiltAllClassFiles':len([n for n in rebuilt.namelist() if n.endswith('.class')])}
(root/'rebuild/api-comparison.json').write_text(json.dumps(report,indent=2));print({k:v for k,v in report.items() if k not in ['apiDifferences','scope']});print('classes with API differences',len(diff))
