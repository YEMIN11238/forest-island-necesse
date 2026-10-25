#!/usr/bin/env python3
"""Pin a same-version native Apple Silicon JRE; original Intel runtime stays intact."""
from pathlib import Path
import argparse,hashlib,json,shutil,tarfile,tempfile,urllib.request

ROOT=Path(__file__).resolve().parents[1]
NAME='OpenJDK17U-jre_aarch64_mac_hotspot_17.0.19_10.tar.gz'
URL='https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.19%2B10/'+NAME
SHA='cef790b404cf168fd1a8a7abc5054fbb442c7d4bfe390cceccfe3f64b9b776a9'
TARGET=ROOT/'runtime/java/temurin-17.0.19+10-macos-aarch64'

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--archive',type=Path,help='Reuse an already downloaded official archive; checksum is always required.')
    args=p.parse_args()
    archive=args.archive or ROOT/'build/tools'/NAME
    archive.parent.mkdir(parents=True,exist_ok=True)
    if not archive.exists():
        urllib.request.urlretrieve(URL,archive)
    digest=hashlib.sha256(archive.read_bytes()).hexdigest()
    if digest!=SHA:raise SystemExit('JRE checksum mismatch; no runtime was installed')
    if not TARGET.exists():
        TARGET.parent.mkdir(parents=True,exist_ok=True)
        with tempfile.TemporaryDirectory(dir=TARGET.parent) as temporary:
            work=Path(temporary)
            with tarfile.open(archive,'r:gz') as source:
                for member in source.getmembers():
                    resolved=(work/member.name).resolve()
                    if work.resolve() not in resolved.parents:raise SystemExit('Unsafe archive member')
                    if member.issym() or member.islnk():
                        linked=(resolved.parent/member.linkname).resolve()
                        if work.resolve() not in linked.parents:raise SystemExit('Unsafe archive link')
                source.extractall(work)
            shutil.move(str(work/'jdk-17.0.19+10-jre'),TARGET)
    result={'vendor':'Eclipse Temurin','version':'17.0.19+10','architecture':'aarch64',
        'source':URL,'sha256':SHA,'home':str(TARGET/'Contents/Home'),
        'policy':'Original Intel runtime and recovered kernel preserved; package explicit runtime home.'}
    (TARGET/'mosslume-runtime-source.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result))

if __name__=='__main__':main()
