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

def steelwork():
    steel, cap, ceramic = (106,125,138), (92,108,114), (131,165,168)
    def solid(low,high,color=steel):
        return (low,high,color)
    for count, radius, name in [(4,.18,"busbar_support_insulator"),
                                (6,.27,"substation_vertical_insulator_stack")]:
        parts=[solid((-.1,-.40,-.1),(.10,.43,.10),ceramic),
               solid((-radius,-.5,-radius),(radius,-.42,radius),cap),
               solid((-.13,.42,-.13),(.13,.51,.13),cap)]
        for i in range(count):
            y=-.34+.68*i/(count-1)
            parts.append(solid((-radius,y,-radius),(radius,y+.06,radius),ceramic))
        write_sprite(name,parts)
    for name, width, thick in [("steel_equipment_support_frame",.38,.09),
                                ("steel_substation_gantry_section",.49,.08)]:
        parts=[solid((x,-.46,z),(x+thick,.45,z+thick))
               for x,z in product((-width,width-thick),repeat=2)]
        parts.extend(solid((-width,y,z),(width,y+.075,z+thick))
                     for y in [-.27,.06,.37] for z in [-width,width-thick])
        write_sprite(name,parts)
    write_sprite("substation_horizontal_bracket",[
        solid((-.5,-.31,-.26),(-.38,.30,.26),cap),
        solid((-.38,.02,-.23),(.5,.10,-.12)),
        solid((-.38,.02,.12),(.5,.10,.23)),
        solid((.21,.10,-.24),(.45,.16,.24),cap),
        solid((-.4,-.23,-.10),(-.20,.02,.10))])
    write_sprite("substation_transformer_foundation",[
        solid((-.52,-.37,-.52),(.52,-.11,.52),(139,148,146)),
        solid((-.47,-.11,-.47),(.47,.11,.47),(150,158,154)),
        solid((-.39,.11,-.39),(.39,.24,-.24),cap),
        solid((-.39,.11,.24),(.39,.24,.39),cap)], 20)
    cases=[
        ("small_single_phase_substation_disconnect",1,.43,.35,.20,3),
        ("large_single_phase_substation_disconnect",1,.64,.64,.26,5),
        ("three_phase_gang_substation_disconnect",3,.66,.68,.37,5),
        ("transmission_gang_substation_disconnect",3,.73,.86,.49,8),
    ]
    for name,n,x,h,spacing,fins in cases:
        parts=[solid((-x-.11,-.44,-spacing-.12),(x+.11,-.30,spacing+.12),cap)]
        for j in range(n):
            z=(j-(n-1)/2)*spacing
            for terminal in [-x,x]:
                parts.append(solid((terminal-.066,-.29,z-.066),
                                   (terminal+.066,h-.31,z+.066),ceramic))
                for i in range(fins):
                    y=-.23+i*(h-.12)/(fins-1)-.16
                    parts.append(solid((terminal-.105,y,z-.105),
                                       (terminal+.105,y+.033,z+.105),ceramic))
                parts.append(solid((terminal-.11,h-.34,z-.09),
                                   (terminal+.11,h-.261,z+.09),(164,129,76)))
            parts.append(solid((-x,h-.28,z-.05),(x,h-.234,z+.05),(196,124,63)))
        write_sprite(name,parts,16)


if __name__ == "__main__":
    busbars()
    steelwork()
