#!/usr/bin/env python3
"""Render the modular equipment profiles to transparent inventory sprites."""
from itertools import product
from pathlib import Path
from struct import pack
from zlib import compress, crc32

DEST = Path(__file__).resolve().parents[2] / "src/main/resources/assets/eln/textures/item"
RESOLUTION = 32


def write_sprite(name, solids, scale=24):
    faces = []
    for low, high, color in solids:
        corners = list(product(*zip(low, high)))
        for indices, shade in [((4, 6, 7, 5), .66), ((2, 3, 7, 6), 1), ((1, 5, 7, 3), .81)]:
            border = [corners[i] for i in indices]
            projected = [(16 + (x-z)*scale*.76, 17 + ((x+z)*.42-y)*scale) for x,y,z in border]
            rgb = tuple(round(c*shade) for c in color)
            faces.append((sum(sum(point) for point in border), projected, rgb))
    pixels = [[(0, 0, 0, 0)]*RESOLUTION for _ in range(RESOLUTION)]
    for _, polygon, rgb in sorted(faces, key=lambda f: f[0]):
        for y in range(RESOLUTION):
            for x in range(RESOLUTION):
                cross = [(q[0]-p[0])*(y+.5-p[1])-(q[1]-p[1])*(x+.5-p[0])
                         for p,q in zip(polygon, polygon[1:]+polygon[:1])]
                if min(cross) >= 0 or max(cross) <= 0:
                    pixels[y][x] = (*rgb,255)
    def chunk(label, data):
        return pack(">I", len(data))+label+data+pack(">I", crc32(label+data))
    scanlines = b"".join(b"\0"+bytes(c for pixel in row for c in pixel) for row in pixels)
    png = b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",pack(">IIBBBBB",RESOLUTION,RESOLUTION,8,6,0,0,0))
    (DEST / (name+".png")).write_bytes(png+chunk(b"IDAT",compress(scanlines,9))+chunk(b"IEND", b""))


def busbars():
    for name, width in [("small", .12), ("medium", .20), ("large", .29), ("transmission", .40)]:
        h, core = width/2, width*.59
        for metal, color in [("copper", (201,123,70)), ("aluminum", (180,195,207))]:
            solids = [
                ((-.54,-h,-h),(-core,h,h),color),
                ((core,-h,-h),(.54,h,h),color),
                ((-core,-core,-core),(core,core,core),color),
                ((-.8*h,core,-.8*h),(.8*h,core+.028,.8*h),(215,82,70)),
            ]
            for x,z in product((-.76*h,.76*h), repeat=2):
                solids.append(((x-.034,core+.028,z-.034),(x+.034,core+.070,z+.034),(90,96,104)))
            write_sprite(f"{name}_{metal}_busbar", solids)


if __name__ == "__main__":
    busbars()
