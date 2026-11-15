package mods.eln.sixnode.lampsocket

import mods.eln.i18n.I18N
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import net.minecraft.world.phys.Vec3
import kotlin.math.*

/** First fixed-fixture batch. Explicit source ordinals keep unported styles' IDs reserved. */
enum class DecorativeLampStyle(val sourceOrdinal: Int, val model: String, val itemName: String,
    val bulbs: Int, val depth: Double, val range: Int, val heads: List<Vec3>, val beam: Vec3,
    val wirelessOnly: Boolean = false) {
    RECESSED(3, "roundrecessedlight", I18N.TR_NAME(I18N.Type.NONE, "Round Recessed Ceiling Light"), 1, .6, 3,
        listOf(Vec3(.05,0.0,0.0)),Vec3(1.0,0.0,0.0),true),
    CHANDELIER(4, "basicchandelier", I18N.TR_NAME(I18N.Type.NONE, "Basic Chandelier"), 5, 1.0, 0,
        (0..4).map { Vec3(.68,.34*cos(it*2*PI/5),.34*sin(it*2*PI/5)) },Vec3(1.0,0.0,0.0)),
    PORCH(6, "porchlanternsconce", I18N.TR_NAME(I18N.Type.NONE, "Porch Lantern Sconce"), 1, .8, 0,
        listOf(Vec3(.31,-.06,0.0)),Vec3(1.0,0.0,0.0)),
    HOLDER(7, "basiclampholder", I18N.TR_NAME(I18N.Type.NONE, "Basic Lamp Holder"), 1, .34, 0,
        listOf(Vec3(.20,0.0,0.0)),Vec3(1.0,0.0,0.0)),
    SHOP(8, "thinshoplight", I18N.TR_NAME(I18N.Type.NONE, "Thin Fluorescent Shop Light"), 2, 2.5, 4,
        listOf(Vec3(.12,-.07,0.0),Vec3(.12,.07,0.0)),Vec3(1.0,0.0,0.0)),
    FACTORY(9, "hangingfactorylight", I18N.TR_NAME(I18N.Type.NONE, "Suspended Factory Fluorescent Light"), 4, 2.5, 4,
        (0..3).map { Vec3(1.24,-.21+it*.14,0.0) },Vec3(1.0,0.0,0.0)),
    CANDLE(11, "candlefoyerchandelier", I18N.TR_NAME(I18N.Type.NONE, "Candle Arm Foyer Chandelier"), 6, 1.7, 1,
        (0..5).map { Vec3(1.02,.60*cos(it*PI/3),.60*sin(it*PI/3)) },Vec3(1.0,0.0,0.0)),
    CRYSTAL(12, "crystaltierchandelier", I18N.TR_NAME(I18N.Type.NONE, "Crystal Tier Foyer Chandelier"), 6, 1.9, 1,
        (0..5).map { Vec3(1.04,.40*cos(it*PI/3),.40*sin(it*PI/3)) },Vec3(1.0,0.0,0.0)),
    WAGON(13, "wagonwheelchandelier", I18N.TR_NAME(I18N.Type.NONE, "Wagon Wheel Foyer Chandelier"), 8, 1.9, 1,
        (0..7).map { Vec3(.99,.77*cos(it*PI/4),.77*sin(it*PI/4)) },Vec3(1.0,0.0,0.0)),
    LANTERN(14, "lanternfoyerchandelier", I18N.TR_NAME(I18N.Type.NONE, "Lantern Cage Foyer Chandelier"), 4, 1.8, 1,
        (0..3).map { Vec3(1.05,.23*cos(it*PI/2),.23*sin(it*PI/2)) },Vec3(1.0,0.0,0.0)),
    SPUTNIK(15, "sputnikchandelier", I18N.TR_NAME(I18N.Type.NONE, "Modern Sputnik Foyer Chandelier"), 8, 1.9, 1,
        (0..7).map { Vec3(1.05+if(it%2==0) -.25 else .25,.70*cos(it*PI/4),.70*sin(it*PI/4)) },Vec3(1.0,0.0,0.0)),
    SCONCE_CYLINDER(16, "cylinderwallsconce", I18N.TR_NAME(I18N.Type.NONE, "Up Down Cylinder Wall Sconce"), 2, .65, 4,
        listOf(Vec3(.21,.28,0.0),Vec3(.21,-.28,0.0)),Vec3(.15,1.0,0.0)),
    SCONCE_FABRIC(17, "fabricwallsconce", I18N.TR_NAME(I18N.Type.NONE, "Fabric Shade Wall Sconce"), 1, .8, 0,
        listOf(Vec3(.28,.05,0.0)),Vec3(1.0,0.0,0.0)),
    SCONCE_GLOBE(18, "globewallsconce", I18N.TR_NAME(I18N.Type.NONE, "Glass Globe Wall Sconce"), 1, .65, 0,
        listOf(Vec3(.32,.03,0.0)),Vec3(1.0,0.0,0.0)),
    SCONCE_CAGE(19, "cagewallsconce", I18N.TR_NAME(I18N.Type.NONE, "Industrial Cage Wall Sconce"), 1, .8, 0,
        listOf(Vec3(.24,0.0,0.0)),Vec3(1.0,0.0,0.0)),
    SCONCE_CANDLE(20, "candlewallsconce", I18N.TR_NAME(I18N.Type.NONE, "Double Candle Wall Sconce"), 2, .9, 0,
        listOf(Vec3(.30,.04,-.23),Vec3(.30,.04,.23)),Vec3(1.0,0.0,0.0)),
    POT_BAFFLE(24, "deepbafflepotlight", I18N.TR_NAME(I18N.Type.NONE, "Deep Baffle Recessed Light"), 1, .40, 5,
        listOf(Vec3(.06,0.0,0.0)),Vec3(1.0,0.0,0.0),true),
    POT_TRIMLESS(25, "trimlesspotlight", I18N.TR_NAME(I18N.Type.NONE, "Trimless Recessed Downlight"), 1, .38, 4,
        listOf(Vec3(.025,0.0,0.0)),Vec3(1.0,0.0,0.0),true),
    OUTDOOR_BOLLARD(27, "gardenbollardlight", I18N.TR_NAME(I18N.Type.NONE, "Garden Bollard Light"), 1, 1.1, 0,
        listOf(Vec3(.83,0.0,0.0)),Vec3(1.0,0.0,0.0)),
    OUTDOOR_BARN(28, "gooseneckbarnlight", I18N.TR_NAME(I18N.Type.NONE, "Gooseneck Barn Light"), 1, .96, 7,
        listOf(Vec3(.63,-.04,0.0)),Vec3(.3,-1.0,0.0)),
    OUTDOOR_POST(29, "traditionallanternpost", I18N.TR_NAME(I18N.Type.NONE, "Traditional Lantern Post Light"), 1, 2.6, 0,
        listOf(Vec3(2.06,0.0,0.0)),Vec3(1.0,0.0,0.0)),
    FLOOR_DRUM(31, "standingdrumfloorlamp", I18N.TR_NAME(I18N.Type.NONE, "Standing Drum Floor Lamp"), 1, 2.0, 0,
        listOf(Vec3(1.66,0.0,0.0)),Vec3(1.0,0.0,0.0)),
    STRIP_CABINET(32, "undercabinetledstrip", I18N.TR_NAME(I18N.Type.NONE, "Under Cabinet LED Strip"), 2, 1.0, 2,
        listOf(Vec3(.045,0.0,-.23),Vec3(.045,0.0,.23)),Vec3(1.0,0.0,0.0)),
    STRIP_RECESSED(33, "recessedledchannel", I18N.TR_NAME(I18N.Type.NONE, "Recessed LED Channel"), 2, 1.0, 3,
        listOf(Vec3(.025,0.0,-.23),Vec3(.025,0.0,.23)),Vec3(1.0,0.0,0.0)),
    STRIP_COVE(34, "coveledstrip", I18N.TR_NAME(I18N.Type.NONE, "Indirect Cove LED Strip"), 2, 1.0, 3,
        listOf(Vec3(.08,.14,-.23),Vec3(.08,.14,.23)),Vec3(.4,1.0,0.0)),
    STRIP_SURFACE(35, "surfacelinearledstrip", I18N.TR_NAME(I18N.Type.NONE, "Surface Linear LED Strip"), 4, 2.0, 4,
        (0..3).map { Vec3(.075,0.0,-.72+it*.48) },Vec3(1.0,0.0,0.0)),
    STRIP_NEON(36, "flexneonledstrip", I18N.TR_NAME(I18N.Type.NONE, "Flexible Neon LED Strip"), 3, 1.0, 1,
        (0..2).map { Vec3(.06,.12*cos(it*PI/2),-.32+it*.32) },Vec3(1.0,0.0,0.0)),
    LUXURY_RINGS(37, "grandhalochandelier", I18N.TR_NAME(I18N.Type.NONE, "Grand Halo Ring Chandelier"), 16, 4.2, 2,
        (0..15).map { Vec3(1.8+(it/8)*1.25,(if(it<8)1.9 else 1.2)*cos(it*PI/4),(if(it<8)1.9 else 1.2)*sin(it*PI/4)) },Vec3(1.0,0.0,0.0)),
    LUXURY_CASCADE(38, "crystalcascadechandelier", I18N.TR_NAME(I18N.Type.NONE, "Grand Crystal Cascade Chandelier"), 20, 5.0, 2,
        (0..19).map { Vec3(1.6+(it%5)*.65,(1.8-(it%5)*.22)*cos(it*PI/2),(1.8-(it%5)*.22)*sin(it*PI/2)) },Vec3(1.0,0.0,0.0)),
    LUXURY_BRANCH(39, "sculpturalbranchchandelier", I18N.TR_NAME(I18N.Type.NONE, "Sculptural Branch Chandelier"), 18, 3.8, 2,
        (0..17).map { Vec3(2.0+(it%3)*.45,(.75+(it%3)*.65)*cos(it*PI/3),(.75+(it%3)*.65)*sin(it*PI/3)) },Vec3(1.0,0.0,0.0)),
    LUXURY_SPIRAL(40, "spiralrainchandelier", I18N.TR_NAME(I18N.Type.NONE, "Spiral Rain Chandelier"), 20, 5.5, 2,
        (0..19).map { Vec3(1.6+it*.18,1.7*cos(it*PI/5),1.7*sin(it*PI/5)) },Vec3(1.0,0.0,0.0)),
    LUXURY_EMPIRE(41, "grandempirechandelier", I18N.TR_NAME(I18N.Type.NONE, "Grand Empire Chandelier"), 16, 4.8, 2,
        (0..15).map { Vec3(if(it<8)1.8 else 3.0,(if(it<8)1.9 else 1.2)*cos(it*PI/4),(if(it<8)1.9 else 1.2)*sin(it*PI/4)) },Vec3(1.0,0.0,0.0));
    val lookup get() = when {
        sourceOrdinal in 0..29 -> (64 shl 6) + 20 + sourceOrdinal
        sourceOrdinal in 30..87 -> (68 shl 6) + sourceOrdinal - 30
        else -> (70 shl 6) + sourceOrdinal - 88
    }
    val foyer get() = this in CANDLE..SPUTNIK
    val luxury get() = this in LUXURY_RINGS..LUXURY_EMPIRE
    val ceiling get() = foyer || luxury || this in listOf(RECESSED, CHANDELIER, SHOP, FACTORY, POT_BAFFLE, POT_TRIMLESS, STRIP_RECESSED)
    val floor get() = this == OUTDOOR_BOLLARD || this == OUTDOOR_POST || this == FLOOR_DRUM
    val exposedBulbs get() = foyer || luxury || this in listOf(CHANDELIER, PORCH, HOLDER, SCONCE_CANDLE, SCONCE_CAGE, SCONCE_GLOBE, OUTDOOR_POST, FLOOR_DRUM)
    /** Under-cabinet, cove, surface and neon strips mount on any supporting face. */
    val anyFace get() = this in listOf(STRIP_CABINET, STRIP_COVE, STRIP_SURFACE, STRIP_NEON)
    fun localFacing(v: Vec3, front: LRDU) = front.rotateOnXnLeft(v.xRot((-PI/2).toFloat()))
    fun worldPoint(origin: Vec3, side: Direction, front: LRDU, local: Vec3): Vec3 =
        origin.add(side.rotateFromXN(localFacing(local,front).add(-.5,0.0,0.0)))
    fun worldDirection(side: Direction, front: LRDU, local: Vec3) = side.rotateFromXN(localFacing(local,front)).normalize()
}
