#!/usr/bin/env python3
"""Fetch only pinned AgentScope model-path jars; hashes are verified before use."""
from pathlib import Path
import hashlib
import json
import tempfile
import urllib.request
import os

ROOT=Path(__file__).resolve().parents[1]
LOCK=ROOT/'mod/dependencies/agentscope-lock.json'
CACHE=ROOT/'mod/build/agentscope-libs'

def dependencies(fetch=True):
    entries=json.loads(LOCK.read_text())['artifacts']
    CACHE.mkdir(parents=True,exist_ok=True)
    result=[]
    for entry in entries:
        target=CACHE/(entry['artifact']+'-'+entry['version']+'.jar')
        if not target.is_file() or hashlib.sha256(target.read_bytes()).hexdigest()!=entry['sha256']:
            if not fetch:raise RuntimeError('Missing or invalid pinned dependency: '+target.name)
            if not entry['url'].startswith('https://repo.maven.apache.org/maven2/'):
                raise RuntimeError('Only Maven Central is permitted')
            data=urllib.request.urlopen(entry['url'],timeout=45).read(15_000_000)
            if hashlib.sha256(data).hexdigest()!=entry['sha256'] or len(data)!=entry['bytes']:
                raise RuntimeError('Dependency checksum mismatch: '+target.name)
            fd,temporary=tempfile.mkstemp(prefix='.'+target.name,dir=CACHE)
            try:
                with os.fdopen(fd,'wb') as out:out.write(data)
                os.replace(temporary,target)
            finally:
                if os.path.exists(temporary):os.unlink(temporary)
        result.append(target)
    return result

if __name__=='__main__':
    for path in dependencies():print(path)
