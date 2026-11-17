package mods.eln.utility

import mods.eln.i18n.I18N

enum class SubstationKind(val title: String,val model: String,val inputVolts: Double,val outputVolts: Double,
    val height: Int,val spread: Double,val subId: Int,val transformer: Boolean=false,val disconnect: Boolean=false,val riser: Boolean=false) {
    STEP_69(I18N.TR_NAME(I18N.Type.NONE,"69 kV to 12.5 kV Substation Transformer"),"substation69125",69_000.0,12_500.0,5,1.8,16,transformer=true),
    STEP_230(I18N.TR_NAME(I18N.Type.NONE,"230 kV to 69 kV Substation Transformer"),"substation23069",230_000.0,69_000.0,7,2.7,17,transformer=true),
    SWITCH_69(I18N.TR_NAME(I18N.Type.NONE,"69 kV Three Phase Substation Disconnect"),"substationdisconnect69",69_000.0,69_000.0,4,1.8,18,disconnect=true),
    SWITCH_230(I18N.TR_NAME(I18N.Type.NONE,"230 kV Three Phase Substation Disconnect"),"substationdisconnect230",230_000.0,230_000.0,6,2.7,19,disconnect=true);
    val id get()=8000+subId
    val range get()=if(inputVolts>100_000)192 else 128
    val terminals get()=listOf(52,53,32,33,34,36,37,38)
    val station get()=transformer || disconnect
    val wattsPerPhase get()=if(inputVolts>100_000)30_000_000.0 else 5_000_000.0
    val ratio get()=outputVolts/inputVolts
}
