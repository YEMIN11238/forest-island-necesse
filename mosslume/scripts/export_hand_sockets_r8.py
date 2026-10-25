#!/usr/bin/env python3
"""Export reviewed hand coordinates; never repaints or moves character art."""
from pathlib import Path
import hashlib,json
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[2]
ART=ROOT/'mosslume/art/hand-grips-r8'
def main():
 data=json.loads((ART/'hand-sockets.json').read_text())
 lines=['# Reviewed 64px source hand centres. UP RIGHT DOWN LEFT, six attack frames.', '# Mechanical export from mosslume/art/hand-grips-r8/hand-sockets.json']
 for actor,rows in data['actors'].items():
  contact=Image.new('RGB',(6*192,4*212),(39,42,45));draw=ImageDraw.Draw(contact)
  for row,entry in enumerate(rows):
   path=ROOT/entry['strip'];image=Image.open(path).convert('RGBA')
   digest=hashlib.sha256(path.read_bytes()).hexdigest()
   if digest!=entry['sha256']:raise ValueError(f'Art changed: re-review hand sockets: {path}')
   hands=entry['hands']
   lines.append(actor+'.'+entry['direction']+'='+','.join(str(v) for xy in hands for v in xy))
   for frame,(x,y) in enumerate(hands):
    if not 0<=x<64 or not 0<=y<64 or image.getpixel((frame*64+x,y))[3]<128:raise ValueError((actor,entry['direction'],frame,'hand outside alpha'))
    tile=image.crop((frame*64,0,frame*64+64,64)).resize((192,192),Image.Resampling.NEAREST)
    ox,oy=frame*192,row*212+20;contact.paste(tile,(ox,oy),tile)
    draw.text((ox+4,oy-16),f'{actor} {entry["direction"]} {frame}  hand({x},{y})',fill='white')
    xx,yy=ox+x*3+1,oy+y*3+1
    draw.ellipse((xx-5,yy-5,xx+5,yy+5),outline=(100,255,190),width=1)
    draw.line((xx-8,yy,xx+8,yy),fill=(100,255,190));draw.line((xx,yy-8,xx,yy+8),fill=(100,255,190))
  contact.save(ART/'qa'/f'{actor}-reviewed-hands.png')
 output=ROOT/'mod/resources/mosslume/characters/attack-hand-sockets.properties'
 output.write_text('\n'.join(lines)+'\n')
 print(json.dumps({'actors':list(data['actors']),'sockets':len(data['actors'])*24,'properties':str(output)}))
if __name__=='__main__':main()
