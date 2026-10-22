#!/usr/bin/env python3
"""Prepare an ffmpeg concat list using the native probe's real frame timestamps.

This does not interpolate frames or claim the capture rate is the game render rate.
Pass --ffmpeg /absolute/path/to/ffmpeg to also create a viewable H.264 preview.
"""
from __future__ import annotations
import argparse
import csv
from pathlib import Path
import subprocess


def concat_quote(path: Path) -> str:
    return "'" + str(path.resolve()).replace("'", "'\\''") + "'"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('probe', type=Path)
    parser.add_argument('--ffmpeg', type=Path)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    rows = list(csv.DictReader((args.probe / 'frames.csv').open()))
    if len(rows) < 2:
        raise SystemExit('At least two captured frames are needed')
    times = [float(row['capture_wall_seconds']) for row in rows]
    deltas = [b-a for a, b in zip(times, times[1:])]
    if any(delta <= 0 for delta in deltas):
        raise SystemExit('Capture timestamps must be strictly increasing')
    frames = [args.probe / 'frames' / row['file'] for row in rows]
    if not all(frame.is_file() for frame in frames):
        raise SystemExit('A frame listed in frames.csv is missing')
    durations = deltas + [sorted(deltas)[len(deltas)//2]]
    lines = ['ffconcat version 1.0']
    for frame, duration in zip(frames, durations):
        # image2 otherwise supplies a 1/25 timebase and can drop distinct PNGs
        # before VFR encoding. Millisecond input precision preserves all samples.
        lines.extend(['file ' + concat_quote(frame), 'option framerate 1000', f'duration {duration:.9f}'])
    lines.extend(['file ' + concat_quote(frames[-1]), 'option framerate 1000'])
    listing = args.probe / 'preview.ffconcat'
    listing.write_text('\n'.join(lines) + '\n')
    print(listing.resolve())
    if args.ffmpeg:
        output = args.output or args.probe / 'preview.mp4'
        subprocess.run([str(args.ffmpeg), '-y', '-safe', '0', '-f', 'concat',
                        '-i', str(listing), '-fps_mode', 'vfr', '-c:v', 'libx264',
                        '-crf', '18', '-pix_fmt', 'yuv420p', '-movflags', '+faststart',
                        str(output)], check=True)
        print(output.resolve())


if __name__ == '__main__':
    main()
