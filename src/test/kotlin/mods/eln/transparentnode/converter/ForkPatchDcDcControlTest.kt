package mods.eln.transparentnode.converter

import mods.eln.sim.ThermalLoad
import mods.eln.sim.power.ConverterKind
import mods.eln.sim.process.heater.ElectricalHeatAccumulator
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test
import java.io.*

class ForkPatchDcDcControlTest {
    private fun packet(write:(DataOutputStream)->Unit):DataInputStream {
        val bytes=ByteArrayOutputStream();write(DataOutputStream(bytes));return DataInputStream(ByteArrayInputStream(bytes.toByteArray()))
    }
    @Test fun historicalSaveKeepsLinearMappingWhileNewInstallsUseLogarithmicMapping() {
        val fresh=DcDcControl();assertEquals(1.0,fresh.ratio(ConverterKind.VARIABLE,.5),0.0)
        val old=DcDcControl();old.load(CompoundTag())
        assertEquals(1,old.version);assertEquals(128.001953125,old.ratio(ConverterKind.VARIABLE,.5),0.0)
        assertTrue(old.handle(43,packet{}));assertEquals(2,old.version);assertFalse(old.enabled);assertEquals("RATIO",old.mode)
    }
    @Test fun targetZeroAndConstantRatioNeverEvaluateInactiveSignal() {
        val control=DcDcControl();control.mode="VOLTAGE";control.value=0.0
        assertTrue(control.validSettings());assertEquals(1.0,control.ratio(ConverterKind.VARIABLE,Double.NaN),0.0)
        control.mode="RATIO";control.value=.5;assertEquals(.5,control.ratio(ConverterKind.VARIABLE,Double.NaN),0.0)
    }
    @Test fun malformedAndOutOfRangePacketsCannotPartiallyChangeSettings() {
        val c=DcDcControl();c.mode="RATIO";c.value=2.0
        for(value in listOf(Double.NaN,Double.POSITIVE_INFINITY,-1.0,0.0,257.0))c.handle(41,packet{it.writeDouble(value)})
        c.handle(40,packet{it.writeShort(2)});c.handle(41,packet{it.writeInt(5)});c.handle(42,packet{})
        assertEquals("RATIO",c.mode);assertEquals(2.0,c.value,0.0);assertTrue(c.enabled)
        assertFalse(c.handle(39,packet{}))
    }
    @Test fun changingUnitsResetsValueAndLegacyCannotSelectVoltageMode() {
        val c=DcDcControl();c.mode="RATIO";c.value=256.0
        c.handle(40,packet{it.writeInt(2)});assertEquals("VOLTAGE",c.mode);assertEquals(800.0,c.value,0.0)
        c.handle(40,packet{it.writeInt(1)});assertEquals(1.0,c.value,0.0)
        c.version=1;c.handle(40,packet{it.writeInt(2)});assertEquals("RATIO",c.mode)
    }
    @Test fun invalidSavedSettingsCannotBeEnabledByClient() {
        for(version in listOf(0,3)) {
            val tag=CompoundTag();tag.putInt("converterControlVersion",version);tag.putBoolean("converterEnabled",true)
            val c=DcDcControl();c.load(tag);assertFalse(c.enabled);c.handle(42,packet{it.writeBoolean(true)});assertFalse(c.enabled)
        }
        val tag=CompoundTag();tag.putDouble("converterControlValue",Double.NaN)
        val c=DcDcControl();c.load(tag);assertFalse(c.enabled)
    }
    @Test fun completeSaveAndNetworkRoundTripsAreExact() {
        val c=DcDcControl();c.mode="VOLTAGE";c.value=317.5;c.enabled=false
        val tag=CompoundTag();c.save(tag);val saved=DcDcControl();saved.load(tag)
        val client=DcDcControl();client.read(packet(c::write))
        for(copy in listOf(saved,client)) { assertEquals(c.version,copy.version);assertEquals(c.mode,copy.mode);assertEquals(c.value,copy.value,0.0);assertEquals(c.enabled,copy.enabled) }
    }
    @Test fun truncatedPublicationLeavesWholePreviousSnapshotIntact() {
        val c=DcDcControl();c.mode="RATIO";c.value=2.0
        try { c.read(packet { it.writeInt(1);it.writeUTF("SIGNAL");it.writeDouble(30.0) });fail("Expected truncated packet") }catch(_:EOFException){}
        assertEquals(2,c.version);assertEquals("RATIO",c.mode);assertEquals(2.0,c.value,0.0);assertTrue(c.enabled)
    }
    @Test fun shortPowerPulsesAndAlternatingCurrentHeatExactlyOncePerThermalStep() {
        val load=ThermalLoad();load.C=2.0;var power=0.0;val heat=ElectricalHeatAccumulator({power},load)
        repeat(5) { power=if(it==1 || it==3)100.0 else 0.0;heat.sample.process(.01) }
        assertEquals(2.0,heat.pendingJoules,1e-12);heat.deliver.process(.05)
        assertEquals(40.0,load.PcTemp,1e-12);assertEquals(0.0,heat.pendingJoules,0.0)
        load.integrateEnergy(load.PcTemp*.05);load.PcTemp=0.0;heat.deliver.process(.05)
        assertEquals(1.0,load.Tc,1e-12);assertEquals(0.0,load.PcTemp,0.0)
        assertEquals(heat.sampledJoules,heat.deliveredJoules,1e-12)
    }
    @Test fun disconnectFlushDeliversRemainingHeatOnceWithoutWaitingForThermalTick() {
        val load=ThermalLoad();load.C=4.0;val heat=ElectricalHeatAccumulator({200.0},load)
        heat.sample.process(.01);heat.flushIntoLoad();heat.flushIntoLoad()
        assertEquals(.5,load.Tc,1e-12);assertEquals(2.0,heat.deliveredJoules,1e-12);assertEquals(0.0,heat.pendingJoules,0.0)
    }
    @Test fun installedWindingPrecisionMatchesItemTemperatureAndReadsHistoricalFloats() {
        val load=WindingThermalLoad("wire");load.Tc=.000122453107128
        val tag=CompoundTag();load.writeToNBT(tag,"");val restored=WindingThermalLoad("wire");restored.readFromNBT(tag,"")
        assertEquals(load.Tc,restored.Tc,0.0)
        tag.putFloat("wireTc",75.25f);restored.readFromNBT(tag,"");assertEquals(75.25,restored.Tc,0.0)
        tag.putDouble("wireTc",Double.NaN);restored.readFromNBT(tag,"");assertEquals(0.0,restored.Tc,0.0)
    }

}
