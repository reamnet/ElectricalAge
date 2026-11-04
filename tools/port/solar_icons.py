#!/usr/bin/env python3
"""Render incoming cabinet geometry into reproducible native inventory icons."""
from pathlib import Path
import sys
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools/icons/shaft"))
from render3d import render

COLORS = {
    "Pad": (.42,.44,.41), "Door": (.17,.35,.22), "DoorHardware": (.12,.14,.16),
    "Nameplate": (.75,.78,.71), "Warning": (.95,.65,.08), "WarningMark": (.05,.06,.04),
    "Lugs": (.65,.48,.25), "Elbows": (.1,.12,.13), "Ground": (.35,.49,.32),
    "Vents": (.035,.045,.05), "Positive": (.85,.15,.08), "Negative": (.08,.09,.1),
    "StatusLed": (.02,.14,.04), "ChargeLed": (.02,.06,.18), "FaultLed": (.15,.02,.01),
    "Voltage": (.02,.04,.06), "Charge": (.02,.04,.06), "Power": (.02,.04,.06), "Capacity": (.02,.04,.06),
    "Mains": (.1,.35,.85), "Backup": (1,.65,.06), "Indicator": (.02,.2,.04), "Display": (.015,.045,.055),
    "MainsLed": (.02,.2,.04), "BackupLed": (.2,.1,.02), "BypassLed": (.15,.03,.2),
    "Input": (.18,.36,.65),
    "Case": (.77, .80, .82), "Trim": (.17, .20, .23),
    "Solar": (.12, .55, .92), "Grid": (.08, .24, .55),
    "Output": (.97, .37, .05), "Return": (.1, .12, .14),
    "PanelFrame": (.10, .13, .16), "PanelFace": (.035, .065, .085),
    "PanelAccent": (.12, .58, .65), "PanelInk": (.75, .85, .83),
    "Readout0": (.012, .025, .03), "Readout1": (.012, .025, .03),
    "Readout2": (.012, .025, .03), "LinkTrack": (.08, .16, .19),
    "StatusLed0": (.02, .12, .14), "StatusLed1": (.05, .14, .05),
    "StatusLed2": (.02, .14, .09), "StatusLed3": (.15, .03, .01),
}

def cabinet(model, item):
    vertices, faces, part = [], [], None
    colors = list(COLORS)
    atlas = np.array([[(.18,.37,.23) if model == "industrial_pad" and name == "Case" else COLORS[name] for name in colors]], dtype=np.float32)
    for line in (ROOT / f"src/main/resources/assets/eln/model/{model}/{model}.obj").read_text().splitlines():
        words = line.split()
        if not words:
            continue
        if words[0] == "o":
            part = words[1]
        elif words[0] == "v":
            vertices.append(tuple(map(float, words[1:4])))
        elif words[0] == "f" and part in COLORS:
            points = [vertices[int(w.split("/")[0]) - 1] for w in words[1:]]
            uv = ((colors.index(part) + .5) / len(colors), .5)
            for i in range(1, len(points) - 1):
                faces.append((part, [points[0], points[i], points[i + 1]], [uv] * 3))
    result = render(faces, atlas, view=(1, -.65, -1.6), size=128, margin=.08)
    rgba = np.zeros((128, 128, 4), dtype=np.uint8)
    rgba[:, :, :3] = np.clip(result["rgb"] * 255, 0, 255).astype(np.uint8)
    rgba[:, :, 3] = result["mask"].astype(np.uint8) * 255
    output = ROOT / f"src/main/resources/assets/eln/textures/item/{item}.png"
    output.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(rgba).resize((64, 64), Image.Resampling.LANCZOS).save(output)

if __name__ == "__main__":
    for model, item in [
        ("industrial_pad", "pad_mounted_neighbourhood_transformer"),
        ("industrial_relay", "protective_relay_cabinet"),
        ("industrial_shedder", "automatic_load_shedding_controller"),
        ("solarmppt", "mppt_solar_controller"),
        ("solarhybrid", "hybrid_solar_inverter"),
        ("wallmppt", "wall_mounted_mppt_solar_controller"),
        ("wallhybrid", "wall_mounted_hybrid_solar_inverter"),
        ('lifepo_cabinet', '16_kwh_lifepo4_battery_cabinet'),
        ('lifepo_cabinet', '32_kwh_lifepo4_battery_cabinet'),
        ('lifepo_cabinet', '48_kwh_lifepo4_battery_cabinet'),
        ('lifepo_cabinet', '64_kwh_lifepo4_battery_cabinet'),
        ('lifepo_wall', '16_kwh_wall_mounted_lifepo4_battery'),
        ('lifepo_wall', '32_kwh_wall_mounted_lifepo4_battery'),
        ('lifepo_wall', '48_kwh_wall_mounted_lifepo4_battery'),
        ('lifepo_wall', '64_kwh_wall_mounted_lifepo4_battery'),
        ('offgrid_inverter', 'off_grid_inverter_charger'),
        ('offgrid_mppt', 'lifepo4_mppt_charge_controller'),
        ('offgrid_charger', 'ac_lifepo4_battery_charger'),
        ('pv_combiner', 'fused_pv_combiner_and_disconnect'),
        ('battery_disconnect', '400_a_dc_battery_disconnect'),
        ('automatictransferswitch', 'automatic_transfer_switch'),
        ('wallats', 'wall_mounted_automatic_transfer_switch'),
        ('standbygenerator', 'standby_gasoline_generator'),
        ('standbygenerator_medium', 'medium_standby_gasoline_generator'),
        ('standbygenerator_large', 'large_standby_gasoline_generator'),
        ('standbygenerator_extra_large', 'extra_large_standby_gasoline_generator'),
        ('standbygenerator_extreme', 'extreme_standby_gasoline_generator'),
        ('programmableups', 'programmable_ups'),
        ('wallups', 'wall_mounted_programmable_ups'),
    ]:
        cabinet(model, item)
