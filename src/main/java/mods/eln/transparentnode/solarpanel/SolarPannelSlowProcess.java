package mods.eln.transparentnode.solarpanel;


import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

public class SolarPannelSlowProcess implements IProcess {
    SolarPanelElement solarPannel;

    public SolarPannelSlowProcess(SolarPanelElement solarPannel) {
        this.solarPannel = solarPannel;
    }

    double timeCounter = 0;
    final double timeCounterRefreshMax = 0.2;

    @Override
    public void process(double time) {
        timeCounter -= time;
        if (timeCounter < 0) {
            //Utils.println("Solar Light : " + getSolarLight());
            /*if(solarPannel.descriptor.basicModel == false)
			{
				solarPannel.currentSource.setI(solarPannel.descriptor.solarIfS.getValue(getSolarLight()));
			}
			else*/
            {
                solarPannel.powerSource.setLightFactor(getSolarLight());
            }
            timeCounter += Math.random() * timeCounterRefreshMax / 2 + timeCounterRefreshMax / 2;
        }

    }

    public SolarRay.Result lastTrace = new SolarRay.Result(0, 0, 0, false);

    /** The source's sampling point follows the array front and its saved pitch. */
    public Vec3 sampleOrigin() {
        double[] offset = {solarPannel.descriptor.solarOffsetX, solarPannel.descriptor.solarOffsetY, solarPannel.descriptor.solarOffsetZ};
        solarPannel.front.rotateFromXN(offset);
        return Utils.getVec05(solarPannel.node.coordonate).add(solarPannel.roofMount.point(new Vec3(offset[0], offset[1], offset[2])));
    }

    public static double weatherMultiplier(double rain, double thunder) {
        if (!Double.isFinite(rain) || !Double.isFinite(thunder)) return 0;
        return (1 - .5 * Math.clamp(rain, 0, 1)) * (1 - .5 * Math.clamp(thunder, 0, 1));
    }

    public double getSolarLight() {
        lastTrace = new SolarRay.Result(0, 0, 0, false);
        Coordonate coordonate = solarPannel.node.coordonate;
        if (!coordonate.getWorldExist()) return 0;
        double solarAlpha = getSolarAlpha();
        if (!Double.isFinite(solarAlpha) || solarAlpha < 0 || solarAlpha >= Math.PI) return 0;
        if (!Utils.isEmpty(solarPannel.getInventory().getItem(SolarPanelElement.trackerSlotId)))
            solarPannel.panelAlpha = solarPannel.descriptor.alphaTrunk(solarPannel.roofMount.targetAlpha(solarAlpha));
        double light = solarPannel.roofMount.incidence(solarPannel.panelAlpha, solarAlpha);
        if (light <= 0) return 0;
        Level world = coordonate.world();
        // Preserve destination local/graded weather integration, with bounded finite provider values.
        double[] weather = Utils.weatherAt(world, coordonate.pos());
        if (weather == null || weather.length < 2) return 0;
        light *= weatherMultiplier(weather[0], weather[1]);
        if (light <= 0) return 0;
        lastTrace = SolarWorldRay.trace(world, sampleOrigin(), solarAlpha);
        return light * lastTrace.transmission();
    }

    public static double getSolarAlpha(Level world) {
        double alpha = mods.eln.misc.Utils.sunAngleRadians(world); // 1.12 getCelestialAngleRadians(0)
        if (alpha < Math.PI / 2 * 3) {
            alpha += Math.PI / 2;
        } else {
            alpha -= Math.PI / 2 * 3;
        }
        //return ((((world.getWorldTime()%24000)*12.0/13.0 +500 ) / 24000.0) )% 1.0 * Math.PI*2;
        return alpha;
    }

    public double getSolarAlpha() {
        return getSolarAlpha(solarPannel.node.coordonate.world());
    }
}
