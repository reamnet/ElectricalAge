#!/usr/bin/env python3
"""Render the source porcelain arrester without changing older cabinet icon palettes."""
from solar_icons import COLORS, cabinet

if __name__ == '__main__':
    COLORS.update({'Base': (.18,.20,.22), 'Terminal': (.66,.46,.20),
                   'Ceramic': (.67,.53,.39), 'Red': (.74,.06,.04)})
    cabinet('industrial_arrester', 'pole_surge_arrester')
