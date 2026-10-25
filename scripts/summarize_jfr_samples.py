#!/usr/bin/env python3
"""Bounded-memory JFR JSON analysis, restricted to the opt-in render event."""
import argparse,collections,datetime,json,re
from pathlib import Path

def events(path):
    decoder=json.JSONDecoder()
    with path.open() as stream:
        buf=''
        while '"events": [' not in buf:
            chunk=stream.read(65536)
            if not chunk:raise ValueError('Missing JFR event list')
            buf+=chunk
        buf=buf.split('"events": [',1)[1]
        while True:
            buf=buf.lstrip(' \n\r\t,')
            if buf.startswith(']'):return
            try:
                event,end=decoder.raw_decode(buf)
            except json.JSONDecodeError:
                chunk=stream.read(65536)
                if not chunk:raise ValueError('Truncated JFR JSON')
                buf+=chunk
                if len(buf)>16*1024*1024:raise ValueError('Unexpected event size')
                continue
            yield event
            buf=buf[end:]

def instant(s):
    return datetime.datetime.fromisoformat(re.sub(r'(\.\d{6})\d+',r'\1',s).replace('Z','+00:00')).timestamp()

def seconds(s):
    m=re.fullmatch(r'PT(?:(\d+)H)?(?:(\d+)M)?(?:([\d.]+)S)?',s)
    if not m:raise ValueError(s)
    return int(m[1] or 0)*3600+int(m[2] or 0)*60+float(m[3] or 0)

def method(frame):
    m=frame['method'];return m['type']['name'].replace('/','.')+'.'+m['name']

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('samples',type=Path);p.add_argument('measurement',type=Path)
    p.add_argument('--output',type=Path);a=p.parse_args()
    mark=next(events(a.measurement))['values'];start=instant(mark['startTime']);end=start+seconds(mark['duration'])
    leaves=collections.Counter();own=collections.Counter();threads=collections.Counter();gc=[];allocated=0
    for e in events(a.samples):
        v=e['values'];t=instant(v['startTime'])
        if not start<=t<=end:continue
        if e['type']=='jdk.GarbageCollection':gc.append(seconds(v['duration'])*1000)
        frames=(v.get('stackTrace') or {}).get('frames',[])
        if e['type'] in ('jdk.ExecutionSample','jdk.NativeMethodSample'):
            name=(v.get('sampledThread') or {}).get('javaName','?');threads[name]+=1
            if name=='main' and frames:leaves[method(frames[0])]+=1
        elif e['type']=='jdk.ObjectAllocationSample':
            weight=v['weight'];allocated+=weight
            for f in frames:
                name=method(f)
                if name.startswith('localforest.'):
                    own[name]+=weight;break
    report={'scope':'JFR samples inside mosslume.RenderMeasurement only; allocation weights are estimates. Native waiting samples are not GPU timing.',
        'measurementSeconds':end-start,'gcCycles':len(gc),'gcCycleMaxMs':max(gc,default=0),'gcCycleTotalMs':sum(gc),
        'sampledThreadCounts':threads.most_common(12),'mainLeafSamples':leaves.most_common(25),
        'allocationWeightedBytes':allocated,'ownAllocationSitesWeightedBytes':own.most_common(25)}
    encoded=json.dumps(report,indent=2)+'\n'
    if a.output:a.output.write_text(encoded)
    print(encoded,end='')

if __name__=='__main__':main()
