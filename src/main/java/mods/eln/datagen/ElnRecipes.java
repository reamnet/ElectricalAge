package mods.eln.datagen;

import mods.eln.Eln;
import mods.eln.item.BrushDescriptor;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.node.voxel.LightingCableIngredient;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.minecraft.world.level.block.Blocks;
import mods.eln.misc.OreDictTags;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;

import static mods.eln.Eln.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * Electrical Age crafting and smelting recipes (P5), made by datagen (ElnRecipeProvider): the 1.12 ElnRecipes recipe
 * methods (1.7.10 Eln.java recipes), unchanged apart from generated edits (tools/port26-recipes.py): 1.12 meta stacks
 * through wool()/dye()/planks(), renamed vanilla items, the armour/tool/converter statics. The framework (addRecipe,
 * ore names -> tags, 1.7.10 pattern fixes, recipe ids) is ElnRecipesBase. The machine lists (macerator, compressor,
 * plate machine, magnetizer) are runtime code, ElnMachineRecipes.
 */
@SuppressWarnings({"SameParameterValue", "PointlessArithmeticExpression", "unused"})
public final class ElnRecipes extends ElnRecipesBase {
    private ElnRecipes() {
    }

    /** The 1.7.10 Eln.load() recipe sequence (1.12 registerAll), minus the machine lists. */
    static void registerAll() {
        run("recipeBrush", ElnRecipes::recipeBrush);
        run("recipeEnergyConverter", ElnRecipes::recipeEnergyConverter);
        run("recipeArmor", ElnRecipes::recipeArmor);
        run("recipeTool", ElnRecipes::recipeTool);
        run("recipeGround", ElnRecipes::recipeGround);
        run("recipeElectricalSource", ElnRecipes::recipeElectricalSource);
        run("recipeElectricalCable", ElnRecipes::recipeElectricalCable);
        run("recipeOldArcFurnace", () -> addRecipe(findItemStack("Old 480V Arc Furnace"), "ICI", "DMD", "IcI",
            'I', "ingotAlloy", 'C', findItemStack("3x Graphite Rods"), 'D', "plateGold",
            'M', findItemStack("Advanced Machine Block"), 'c', findItemStack("Synthetic Diamond")));
        run("recipeThermalCable", ElnRecipes::recipeThermalCable);
        run("recipeDecorativeLamps", ElnRecipes::recipeDecorativeLamps);
        run("recipeLampSocket", ElnRecipes::recipeLampSocket);
        run("recipeRemainingLights", ElnRecipes::recipeRemainingLights);
        run("recipeLampSupply", ElnRecipes::recipeLampSupply);
        run("recipePowerSocket", ElnRecipes::recipePowerSocket);
        run("recipePassiveComponent", ElnRecipes::recipePassiveComponent);
        run("recipeSwitch", ElnRecipes::recipeSwitch);
        run("recipeWirelessSignal", ElnRecipes::recipeWirelessSignal);
        run("recipeElectricalRelay", ElnRecipes::recipeElectricalRelay);
        run("recipeElectricalDataLogger", ElnRecipes::recipeElectricalDataLogger);
        run("recipeElectricalGateSource", ElnRecipes::recipeElectricalGateSource);
        run("recipeElectricalBreaker", ElnRecipes::recipeElectricalBreaker);
        run("recipeFuses", ElnRecipes::recipeFuses);
        run("recipeElectricalVuMeter", ElnRecipes::recipeElectricalVuMeter);
        run("recipeElectricalEnvironmentalSensor", ElnRecipes::recipeElectricalEnvironmentalSensor);
        run("recipeElectricalRedstone", ElnRecipes::recipeElectricalRedstone);
        run("recipeElectricalGate", ElnRecipes::recipeElectricalGate);
        run("recipeElectricalAlarm", ElnRecipes::recipeElectricalAlarm);
        run("recipeSixNodeCache", ElnRecipes::recipeSixNodeCache);
        run("recipeElectricalSensor", ElnRecipes::recipeElectricalSensor);
        run("recipeThermalSensor", ElnRecipes::recipeThermalSensor);
        run("recipeSixNodeMisc", ElnRecipes::recipeSixNodeMisc);
        run("recipeTurret", ElnRecipes::recipeTurret);
        run("recipeMachine", ElnRecipes::recipeMachine);
        run("recipeChips", ElnRecipes::recipeChips);
        run("recipeTransformer", ElnRecipes::recipeTransformer);
        run("recipeHeatFurnace", ElnRecipes::recipeHeatFurnace);
        run("recipeTurbine", ElnRecipes::recipeTurbine);
        run("recipeBattery", ElnRecipes::recipeBattery);
        run("recipeElectricalFurnace", ElnRecipes::recipeElectricalFurnace);
        run("recipeAutoMiner", ElnRecipes::recipeAutoMiner);
        run("recipeSolarPanel", ElnRecipes::recipeSolarPanel);
        run("recipeForkPatchSolar", ElnRecipes::recipeForkPatchSolar);
        run("recipeForkPatchOffgrid", ElnRecipes::recipeForkPatchOffgrid);
        run("recipeForkPatchStandby", ElnRecipes::recipeForkPatchStandby);
        run("recipeForkPatchUps", ElnRecipes::recipeForkPatchUps);
        run("recipeForkPatchGraphite", ElnRecipes::recipeForkPatchGraphite);
        run("recipeForkPatchArcClay", ElnRecipes::recipeForkPatchArcClay);
        run("recipeForkPatchArcMetal", ElnRecipes::recipeForkPatchArcMetal);
        run("recipeThermalDissipatorPassiveAndActive", ElnRecipes::recipeThermalDissipatorPassiveAndActive);
        run("recipeElectricalAntenna", ElnRecipes::recipeElectricalAntenna);
        run("recipeEggIncubator", ElnRecipes::recipeEggIncubator);
        run("recipeBatteryCharger", ElnRecipes::recipeBatteryCharger);
        run("recipeTransporter", ElnRecipes::recipeTransporter);
        run("recipeWindTurbine", ElnRecipes::recipeWindTurbine);
        run("recipeFuelGenerator", ElnRecipes::recipeFuelGenerator);
        run("recipeGeneral", ElnRecipes::recipeGeneral);
        run("recipeHeatingCorp", ElnRecipes::recipeHeatingCorp);
        run("recipeRegulatorItem", ElnRecipes::recipeRegulatorItem);
        run("recipeLampItem", ElnRecipes::recipeLampItem);
        run("recipeProtection", ElnRecipes::recipeProtection);
        run("recipeCombustionChamber", ElnRecipes::recipeCombustionChamber);
        run("recipeFerromagneticCore", ElnRecipes::recipeFerromagneticCore);
        run("recipeIngot", ElnRecipes::recipeIngot);
        run("recipeDust", ElnRecipes::recipeDust);
        run("recipeElectricalMotor", ElnRecipes::recipeElectricalMotor);
        run("recipeSolarTracker", ElnRecipes::recipeSolarTracker);
        run("recipeDynamo", ElnRecipes::recipeDynamo);
        run("recipeWindRotor", ElnRecipes::recipeWindRotor);
        run("recipeMeter", ElnRecipes::recipeMeter);
        run("recipeElectricalDrill", ElnRecipes::recipeElectricalDrill);
        run("recipeOreScanner", ElnRecipes::recipeOreScanner);
        run("recipeMiningPipe", ElnRecipes::recipeMiningPipe);
        run("recipeTreeResinAndRubber", ElnRecipes::recipeTreeResinAndRubber);
        run("recipeRawCable", ElnRecipes::recipeRawCable);
        run("recipeMiscItem", ElnRecipes::recipeMiscItem);
        run("recipeBatteryItem", ElnRecipes::recipeBatteryItem);
        run("recipeElectricalTool", ElnRecipes::recipeElectricalTool);
        run("recipePortableCapacitor", ElnRecipes::recipePortableCapacitor);
        run("recipeFurnace", ElnRecipes::recipeFurnace);
        run("recipeFuelBurnerItem", ElnRecipes::recipeFuelBurnerItem);
        run("recipeECoal", ElnRecipes::recipeECoal);
        run("recipeGridDevices", ElnRecipes::recipeGridDevices);
    }
    private static void recipeRemainingLights() {
        addShapelessRecipe(findItemStack("Christmas Tree"),findItemStack("String Lights"),Items.SPRUCE_SAPLING,findItemStack("String Lights"));
        addRecipe(findItemStack("Holiday Candle")," g ","gbg"," i ",'g',Items.GLASS_PANE,'b',findItemStack("120V LED Light Bulb"),'i',Items.IRON_INGOT);
        addShapelessRecipe(findItemStack("White Fairy Lights on Green Wire"),findItemStack("String Lights"),findItemStack("Flexible Appliance Cord"),Items.DYE.white(),Items.DYE.green());
        addShapelessRecipe(findItemStack("Multicolor Fairy Lights on Green Wire"),findItemStack("String Lights"),findItemStack("Flexible Appliance Cord"),Items.DYE.red(),Items.DYE.green());
        addShapelessRecipe(findItemStack("Warm White Deck Globe String Lights"),findItemStack("White Fairy Lights on Green Wire"),Items.GLASS_BOTTLE,Items.DYE.black());
        addShapelessRecipe(findItemStack("Multicolor Deck Globe String Lights"),findItemStack("Multicolor Fairy Lights on Green Wire"),Items.GLASS_BOTTLE,Items.DYE.black());
        addRecipe(findItemStack("120V Basic Floodlight")," PG","MBP","ICI",'P',findItemStack("Iron Plate"),'G',Items.GLASS_PANE,'M',findItemStack("Electrical Motor"),'B',findItemStack("Machine Block"),'I',Items.IRON_INGOT,'C',lightingWire(1));
        addRecipe(findItemStack("240V Motorized Floodlight")," PG","MBP","ICI",'P',findItemStack("Iron Plate"),'G',Items.GLASS_PANE,'M',findItemStack("Advanced Electrical Motor"),'B',findItemStack("Advanced Machine Block"),'I',Items.IRON_INGOT,'C',lightingWire(2));
        // Native200V generator is the existing engine assembly; the fixture regulates to its bulbs' source voltage.
        addRecipe(findItemStack("Gasoline Construction Work Light"),"L L"," G ","IPI",'L',findItemStack("Warehouse Wall Floodlight"),'G',findItemStack("230V Fuel Generator"),'I',Items.IRON_INGOT,'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("120V Emergency Lamp"),"cbc"," l "," g ",'c',lightingWire(1),'b',findItemStack("Portable Battery Pack"),'l',findItemStack("120V LED Light Bulb"),'g',Items.GLASS_PANE);
        addRecipe(findItemStack("240V Emergency Lamp"),"cbc"," l "," g ",'c',lightingWire(2),'b',findItemStack("Portable Battery Pack"),'l',findItemStack("240V LED Light Bulb"),'g',Items.GLASS_PANE);
    }


    // ------------------------------------------------------------------ recipes (1.7.10)

    /** 1.7.10 registerBrush: empty white brush from wool + iron, coloured brushes from dye + empty white brush. */
    public static void recipeBrush() {
        String[] subNames = {"Black Brush", "Red Brush", "Green Brush", "Brown Brush", "Blue Brush", "Purple Brush",
            "Cyan Brush", "Silver Brush", "Gray Brush", "Pink Brush", "Lime Brush", "Yellow Brush", "Light Blue Brush",
            "Magenta Brush", "Orange Brush", "White Brush"};
        RStack emptyStack = findItemStack("White Brush");
        if (!emptyStack.isEmpty()) emptyStack = withLife(emptyStack, 0); // 1.12 BrushDescriptor.setLife(stack, 0)

        for (int idx = 0; idx < 16; idx++) {
            addShapelessRecipe(emptyStack.copy(),
                wool(idx),
                stack(Items.IRON_INGOT));
        }

        for (int idx = 0; idx < 16; idx++) {
            addShapelessRecipe(findItemStack(subNames[idx], 1),
                dye(idx),
                emptyStack.copy());
        }
    }



    public static void recipeGround() {
        addRecipe(findItemStack("Ground Cable"),
            " C ",
            " C ",
            "CCC",
            'C', findItemStack("Copper Cable"));
    }

    public static void recipeElectricalSource() {
        // Trololol
    }

    public static void recipeElectricalCable() {
        addRecipe(stack(signalCableDescriptor, 1),
            "R",
            "C",
            'C', findItemStack("Iron Cable"),
            'R', "itemRubber");

        // the power cables (Baughn 2026-11-07): recipes take bare Copper Cable now, so the hand recipes make cable kits
        // of the tier's preset (16 voxels a former cable item), each layer of insulation a rubber (230 V two, 400 V
        // three), and the 1.12 cable items are retired
        addRecipe(kit(1, 1),
            "R",
            "C",
            'C', findItemStack("Copper Cable"),
            'R', "itemRubber");

        addRecipe(kit(2, 1),
            "R",
            "C",
            "R",
            'C', findItemStack("Copper Cable"),
            'R', "itemRubber");

        addRecipe(kit(3, 1),
            " R ",
            "RCR",
            'C', findItemStack("Copper Cable"),
            'R', "itemRubber");

        addRecipe(stack(signalCableDescriptor, 6),
            "RRR",
            "CCC",
            "RRR",
            'C', stack(Items.IRON_INGOT),
            'R', "itemRubber");

        addRecipe(kit(1, 6),
            "RRR",
            "CCC",
            "RRR",
            'C', "ingotCopper",
            'R', "itemRubber");
        // a low voltage kit insulated with leather instead, one for one (Baughn 2026-11-08: before there's rubber)
        addRecipe(kit(1, 1),
            "L",
            "C",
            'C', findItemStack("Copper Cable"),
            'L', stack(Items.LEATHER));

        addRecipe(kit(4, 6),
            "RRR",
            "CCC",
            "RRR",
            'C', "ingotAlloy",
            'R', "itemRubber");

    }

    /** A cable kit of a power tier's preset, as long as n of the 1.12 cable items (16 voxels each). */
    static RStack kit(int tier, int n) {
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.putInt(mods.eln.node.voxel.KitItem.LEN_KEY, 16 * n);
        return new RStack(mods.eln.registry.ElnRegistry.CABLE_KITS.get(tier).get(), 1, t);
    }

    /** One former cable item or its current one-block preset kit. Longer and custom kits keep their material. */
    private static RecipeIngredient lightingWire(int tier) {
        var kit = mods.eln.registry.ElnRegistry.CABLE_KITS.get(tier).get();
        var legacy = findItemStack(tier == 1 ? "Low Voltage Cable" : "Medium Voltage Cable");
        var preset = new LightingCableIngredient(tier).toVanilla();
        return new RecipeIngredient(CompoundIngredient.of(preset, Ingredient.of(legacy.item())), kit);
    }

    public static void recipeThermalCable() {
        addRecipe(findItemStack("Copper Thermal Cable", 6),
            "SSS",
            "CCC",
            "SSS",
            'S', stack(Blocks.COBBLESTONE),
            'C', "ingotCopper");

        addRecipe(findItemStack("Copper Thermal Cable", 1),
            "S",
            "C",
            'S', stack(Blocks.COBBLESTONE),
            'C', findItemStack("Copper Cable"));
    }

    public static void recipeDecorativeLamps() {
        for(String metal:new String[]{"Steel","Aluminum","Aluminium"})
            addRecipe(findItemStack("Grid DC-DC Converter"),"i i","mtm","imi",'i',"ingot"+metal,'t',findItemStack("Winding DC-DC Converter"),'m',findItemStack("Advanced Machine Block"));
        for(var r:mods.eln.sixnode.lampsocket.RatedLampBulbs.INSTANCE.getRatings()) {
            if(r.getId()==302||r.getId()==303)continue;
            var wire = r.getVolts() == 12 ? findItemStack("Copper Cable") : lightingWire(r.getVolts() == 120 ? 1 : 2);
            String title=r.getTitle();
            if(title.contains("Halogen"))addRecipe(findItemStack(title,2),"GGG","CTC","GGG",'G',Blocks.GLASS_PANE,'C',wire,'T',dictTungstenIngot);
            else if(title.contains("LED"))addRecipe(findItemStack(title,2),"GGG","SSS","PCP",'G',Blocks.GLASS_PANE,'S',findItemStack("Silicon Ingot"),'P',findItemStack("Iron Plate"),'C',wire);
            else if(title.contains("Farming"))addRecipe(findItemStack(title,2),"GGG","FFF","PSP",'G',Blocks.GLASS_PANE,'F',dictTungstenIngot,'P',findItemStack("Iron Plate"),'S',wire);
            else {
                Object filament=title.contains("Carbon")?Items.COAL:title.contains("Fluorescent")?Items.GLOWSTONE_DUST:dictTungstenIngot;
                addRecipe(findItemStack(title,4)," G ","GFG","PSP",'G',Blocks.GLASS_PANE,'F',filament,'P',findItemStack("Iron Plate"),'S',wire);
                if(title.contains("Carbon"))addRecipe(findItemStack(title,4)," G ","GFG","PSP",'G',Blocks.GLASS_PANE,'F',Items.CHARCOAL,'P',findItemStack("Iron Plate"),'S',wire);
            }
        }
        addRecipe(findItemStack("Electric Microwave"), "PPP", "GRC", "PPP",
            'P',findItemStack("Iron Plate"),'G',Blocks.GLASS_PANE,'R',Items.REDSTONE,'C',findItemStack("Copper Cable"));
        addRecipe(findItemStack("Electric Toaster"), "I I", "PRP", "CCC",
            'I',Items.IRON_INGOT,'P',findItemStack("Iron Plate"),'R',Items.REDSTONE,'C',findItemStack("Copper Cable"));
        addRecipe(findItemStack("Flat Electric Cooktop"), "GGG", "RRR", "PCP",
            'G',Blocks.STAINED_GLASS.black(),'R',Items.REDSTONE,'P',findItemStack("Iron Plate"),'C',findItemStack("Copper Cable"));
        addRecipe(findItemStack("Patio Electric Grill"),"III","RCR","P P",
            'I',Items.IRON_BARS,'R',Items.REDSTONE,'C',findItemStack("Copper Cable"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Electric Mini Fridge"),"PP","CR","PP",
            'P',findItemStack("Iron Plate"),'C',Blocks.CHEST,'R',findItemStack("Copper Cable"));
        addRecipe(findItemStack("Electric Washing Machine"),"PBP","MCM","PPP",
            'P',findItemStack("Iron Plate"),'B',Items.BUCKET,'M',findItemStack("Electrical Motor"),'C',findItemStack("Copper Cable"));
        addRecipe(findItemStack("Electric Dryer"),"PPP","MHM","PCP",
            'P',findItemStack("Iron Plate"),'M',findItemStack("Electrical Motor"),'H',Items.BLAZE_ROD,'C',findItemStack("Copper Cable"));
        addRecipe(findItemStack("Fusion Matter Reactor"),"NDN","MCM","NDN",
            'N',Items.NETHERITE_INGOT,'D',Blocks.DIAMOND_BLOCK,'M',findItemStack("Electrical Motor"),
            'C',Items.NETHER_STAR);
        addRecipe(findItemStack("Electric Refrigerator"), "PPP", "GCG", "PRP",
            'P',findItemStack("Iron Plate"),'G',Blocks.GLASS_PANE,'C',Blocks.CHEST,'R',findItemStack("Copper Cable"));
        addRecipe(findItemStack("Electric Coffee Maker"), " I ", "GBG", "PCP",
            'I',Items.IRON_INGOT,'G',Blocks.GLASS_PANE,'B',Items.BUCKET,'P',findItemStack("Iron Plate"),'C',findItemStack("Copper Cable"));
        addRecipe(findItemStack("Electric Mini Oven"),"PPP","GCG","PRP",
            'P',findItemStack("Iron Plate"),'G',Blocks.GLASS_PANE,'C',findItemStack("Copper Cable"),'R',Items.REDSTONE);
        addRecipe(findItemStack("Electric Kettle")," P ","BCP"," R ",
            'P',findItemStack("Iron Plate"),'B',Items.BUCKET,'C',findItemStack("Copper Cable"),'R',Items.REDSTONE);
        addRecipe(findItemStack("Countertop Blender"),"G G","GGG","PMC",
            'G',Blocks.GLASS_PANE,'P',findItemStack("Iron Plate"),'M',findItemStack("Electrical Motor"),'C',findItemStack("Copper Cable"));
        addRecipe(findItemStack("Electric Rice Cooker"),"PPP","BCB","PRP",
            'P',findItemStack("Iron Plate"),'B',Items.BUCKET,'C',findItemStack("Copper Cable"),'R',Items.REDSTONE);
        addRecipe(findItemStack("Industrial Five Block Fan"),"PMP","MCM","PMP",'P',findItemStack("Iron Plate"),'M',findItemStack("Electrical Motor"),'C',findItemStack("Copper Cable"));
        for(var t:mods.eln.transparentnode.fan.FanType.values())if(!t.getIndustrial())
            addRecipe(findItemStack(t.getTitle()),"PPP","MCM","PRP",'P',t==mods.eln.transparentnode.fan.FanType.SHOP?Items.IRON_INGOT:t==mods.eln.transparentnode.fan.FanType.COMPACT?findItemStack("Rubber"):findItemStack("Iron Plate"),'M',findItemStack("Electrical Motor"),'C',findItemStack("Copper Cable"),'R',Items.REDSTONE);
        for(var k:mods.eln.utility.SubstationKind.values()) {
            var pole=findItemStack(k.getInputVolts()>100000?"230 kV Steel Transmission Tower":"69 kV Steel Transmission Pole");
            if(k.getTransformer())addRecipe(findItemStack(k.getTitle()),"III","CTC","PBP",'I',Blocks.IRON_BLOCK,'C',findItemStack("Copper Cable"),'T',findItemStack("Grid DC-DC Converter"),'P',findItemStack("Iron Plate"),'B',pole);
            else addRecipe(findItemStack(k.getTitle()),"CCC","IBI","PPP",'C',findItemStack("Copper Cable"),'I',Blocks.IRON_BARS,'B',pole,'P',findItemStack("Iron Plate"));
        }

        // Original LED patterns still produce legacy bulbs. Distinct paid patterns add both source ratings.
        addRecipe(findItemStack("120V LED Light Bulb",2),"GGG","SSS","PCP",
            'G',Blocks.GLASS_PANE,'S',findItemStack("Silicon Ingot"),'P',findItemStack("Iron Plate"),'C',lightingWire(1));
        addRecipe(findItemStack("240V LED Light Bulb",2),"GGG","SSS","PCP",
            'G',Blocks.GLASS_PANE,'S',findItemStack("Silicon Ingot"),'P',findItemStack("Iron Plate"),'C',lightingWire(2));
        // Source materialString is vanilla string; retain the two-fixture bootstrap yield.
        addShapelessRecipe(findItemStack("String Lights",2),findItemStack("120V LED Light Bulb"),Items.STRING);
        // Exact pinned ForkPatch b7444a7 source fixture recipes; native socket names are stable aliases.
        addRecipe(findItemStack("Twin Parking Lot Light",1),"L L"," I "," P ",'L',findItemStack("Robust Lamp Socket"),'I',Items.IRON_INGOT,'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Warehouse Wall Floodlight",1),"IPI","GLG"," I ",'I',Items.IRON_INGOT,'P',findItemStack("Iron Plate"),'G',Blocks.GLASS_PANE,'L',findItemStack("Lamp Socket B Projector"));
        addRecipe(findItemStack("Household Desk Lamp",1),"WWW"," L "," P ",'W',wool(0),'L',findItemStack("Lamp Socket A"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Round Recessed Ceiling Light",2)," P ","GLG",'P',findItemStack("Iron Plate"),'G',Blocks.GLASS_PANE,'L',findItemStack("Flat Lamp Socket"));
        addRecipe(findItemStack("Basic Chandelier",1)," I ","LIL","LLL",'I',Items.IRON_INGOT,'L',findItemStack("Lamp Socket A"));
        addRecipe(findItemStack("Mini Dot Spotlight",2)," I ","GLG",'I',Items.IRON_INGOT,'G',Blocks.GLASS_PANE,'L',findItemStack("Lamp Socket B Projector"));
        addRecipe(findItemStack("Porch Lantern Sconce",1)," I ","GLG"," P ",'I',Items.IRON_INGOT,'G',Blocks.GLASS_PANE,'L',findItemStack("Lamp Socket A"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Basic Lamp Holder",2)," C "," P ",'C',Items.COPPER_INGOT,'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Thin Fluorescent Shop Light",1),"PPP","LLL",'P',findItemStack("Iron Plate"),'L',findItemStack("Flat Lamp Socket"));
        addRecipe(findItemStack("Suspended Factory Fluorescent Light",1),"C C","PPP","LLL",'C',Items.IRON_CHAIN,'P',findItemStack("Iron Plate"),'L',findItemStack("Thin Fluorescent Shop Light"));
        addRecipe(findItemStack("Outdoor Motion Floodlight",1),"L L"," S "," P ",'L',findItemStack("Lamp Socket B Projector"),'S',findItemStack("Electrical Entity Sensor"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Candle Arm Foyer Chandelier",1)," C ","GLG","LLL",'C',Items.IRON_CHAIN,'G',Items.GOLD_INGOT,'L',findItemStack("Lamp Socket A"));
        addRecipe(findItemStack("Crystal Tier Foyer Chandelier",1)," C ","GLG","GGG",'C',Items.IRON_CHAIN,'G',Blocks.GLASS_PANE,'L',findItemStack("Candle Arm Foyer Chandelier"));
        addRecipe(findItemStack("Wagon Wheel Foyer Chandelier",1),"CCC","LPL","LLL",'C',Items.IRON_CHAIN,'P',Blocks.DARK_OAK_PLANKS,'L',findItemStack("Lamp Socket A"));
        addRecipe(findItemStack("Lantern Cage Foyer Chandelier",1)," C ","ILI","ILI",'C',Items.IRON_CHAIN,'I',Blocks.IRON_BARS,'L',findItemStack("Lamp Socket A"));
        addRecipe(findItemStack("Modern Sputnik Foyer Chandelier",1)," L ","LGL"," L ",'G',Items.GOLD_INGOT,'L',findItemStack("Lamp Socket A"));
        addRecipe(findItemStack("Up Down Cylinder Wall Sconce",1)," L ","IPI"," L ",'L',findItemStack("Lamp Socket B Projector"),'I',Items.IRON_INGOT,'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Fabric Shade Wall Sconce",1),"WWW"," L "," P ",'W',wool(0),'L',findItemStack("Basic Lamp Holder"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Glass Globe Wall Sconce",1)," G ","GLG"," P ",'G',Blocks.GLASS,'L',findItemStack("Basic Lamp Holder"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Industrial Cage Wall Sconce",1),"III","ILI"," P ",'I',Blocks.IRON_BARS,'L',findItemStack("Basic Lamp Holder"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Double Candle Wall Sconce",1),"L L"," G "," P ",'L',findItemStack("Basic Lamp Holder"),'G',Items.GOLD_INGOT,'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Ceramic Drum Table Lamp",1),"WWW"," L "," B ",'W',wool(0),'L',findItemStack("Basic Lamp Holder"),'B',Blocks.TERRACOTTA);
        addRecipe(findItemStack("Green Banker Table Lamp",1),"GGG"," L "," P ",'G',vanilla("green_stained_glass"),'L',findItemStack("Basic Lamp Holder"),'P',Items.COPPER_INGOT);
        addRecipe(findItemStack("Articulated Task Table Lamp",1)," IL"," I "," P ",'I',Items.IRON_INGOT,'L',findItemStack("Lamp Socket B Projector"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Deep Baffle Recessed Light",2)," I ","BLB",'I',findItemStack("Iron Plate"),'B',Items.DYE.black(),'L',findItemStack("Flat Lamp Socket"));
        addRecipe(findItemStack("Trimless Recessed Downlight",2)," W ","GLG",'W',Items.DYE.white(),'G',Blocks.GLASS_PANE,'L',findItemStack("Flat Lamp Socket"));
        addRecipe(findItemStack("Adjustable Gimbal Recessed Light",2)," I ","ILI",'I',Items.IRON_INGOT,'L',findItemStack("Lamp Socket B Projector"));
        addRecipe(findItemStack("Garden Bollard Light",1)," G ","ILI"," P ",'G',Blocks.GLASS,'I',Items.IRON_INGOT,'L',findItemStack("Basic Lamp Holder"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Gooseneck Barn Light",1),"IIL","P  ",'I',Items.IRON_INGOT,'L',findItemStack("Robust Lamp Socket"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Traditional Lantern Post Light",1)," L "," I "," P ",'L',findItemStack("Porch Lantern Sconce"),'I',Items.IRON_INGOT,'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Window Candle Light",1)," D ","DLD"," P ",'D',Items.HONEYCOMB,'L',findItemStack("Basic Lamp Holder"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Standing Drum Floor Lamp",1)," D ","DLD"," P ",'D',Items.IRON_INGOT,'L',findItemStack("Ceramic Drum Table Lamp"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Under Cabinet LED Strip",1)," D ","DLD"," P ",'D',Items.IRON_NUGGET,'L',findItemStack("Flat Lamp Socket"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Recessed LED Channel",1)," D ","DLD"," P ",'D',Items.QUARTZ,'L',findItemStack("Flat Lamp Socket"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Indirect Cove LED Strip",1)," D ","DLD"," P ",'D',Items.DYE.white(),'L',findItemStack("Flat Lamp Socket"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Surface Linear LED Strip",1)," D ","DLD"," P ",'D',Items.IRON_INGOT,'L',findItemStack("Thin Fluorescent Shop Light"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Flexible Neon LED Strip",1)," D ","DLD"," P ",'D',Items.SLIME_BALL,'L',findItemStack("Flat Lamp Socket"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Grand Halo Ring Chandelier",1)," D ","DLD"," P ",'D',Items.GOLD_INGOT,'L',findItemStack("Modern Sputnik Foyer Chandelier"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Grand Crystal Cascade Chandelier",1)," D ","DLD"," P ",'D',Items.DIAMOND,'L',findItemStack("Crystal Tier Foyer Chandelier"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Sculptural Branch Chandelier",1)," D ","DLD"," P ",'D',Items.COPPER_INGOT,'L',findItemStack("Modern Sputnik Foyer Chandelier"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Spiral Rain Chandelier",1)," D ","DLD"," P ",'D',Items.IRON_CHAIN,'L',findItemStack("Crystal Tier Foyer Chandelier"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Grand Empire Chandelier",1)," D ","DLD"," P ",'D',Items.GOLD_INGOT,'L',findItemStack("Candle Arm Foyer Chandelier"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Heavy Duty Stadium Wall Floodlight",1)," D ","DLD"," P ",'D',Items.IRON_BLOCK,'L',findItemStack("Warehouse Wall Floodlight"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Armored Oval Bulkhead Light",1)," D ","DLD"," P ",'D',Items.IRON_BARS,'L',findItemStack("Robust Lamp Socket"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Industrial Wall Pack Light",1)," D ","DLD"," P ",'D',Items.IRON_INGOT,'L',findItemStack("Warehouse Wall Floodlight"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Rugged Outdoor Canopy Light",1)," D ","DLD"," P ",'D',Items.IRON_BLOCK,'L',findItemStack("Robust Lamp Socket"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Industrial Yard Area Light",1)," D ","DLD"," P ",'D',Items.IRON_INGOT,'L',findItemStack("Gooseneck Barn Light"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Offshore Caged Wellglass Light",1)," D ","DLD"," P ",'D',Items.DYE.yellow(),'L',findItemStack("Robust Lamp Socket"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Offshore Sealed Linear Light",1)," D ","DLD"," P ",'D',Items.DYE.yellow(),'L',findItemStack("Thin Fluorescent Shop Light"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Offshore Platform Floodlight",1)," D ","DLD"," P ",'D',Items.DYE.yellow(),'L',findItemStack("Warehouse Wall Floodlight"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Modern LED Street Light",1)," D ","DLD"," P ",'D',Items.IRON_INGOT,'L',findItemStack("Street Light"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Boulevard Lantern Street Light",1)," D ","DLD"," P ",'D',Items.IRON_CHAIN,'L',findItemStack("Traditional Lantern Post Light"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Warm White Christmas Fairy Lights",1)," D ","DLD"," P ",'D',Items.DYE.yellow(),'L',findItemStack("String Lights"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Multicolor Christmas String Lights",1)," D ","DLD"," P ",'D',Items.DYE.red(),'L',findItemStack("String Lights"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Christmas Icicle Lights",1)," D ","DLD"," P ",'D',Items.DYE.lightBlue(),'L',findItemStack("String Lights"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Christmas Star Wall Light",1)," D ","DLD"," P ",'D',Items.GOLD_NUGGET,'L',findItemStack("Basic Lamp Holder"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Halloween Pumpkin String Lights",1)," D ","DLD"," P ",'D',Items.PUMPKIN,'L',findItemStack("String Lights"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Halloween Ghost String Lights",1)," D ","DLD"," P ",'D',Items.DYE.white(),'L',findItemStack("String Lights"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Halloween Bat Wall Light",1)," D ","DLD"," P ",'D',Items.DYE.black(),'L',findItemStack("Basic Lamp Holder"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Jack O Lantern Table Lamp",1)," D ","DLD"," P ",'D',Items.CARVED_PUMPKIN,'L',findItemStack("Basic Lamp Holder"),'P',findItemStack("Iron Plate"));
        addShapelessRecipe(findItemStack("Mini Pinhole Dot Light",1),findItemStack("Mini Dot Spotlight"),findItemStack("Iron Plate"),Items.IRON_NUGGET);
        addShapelessRecipe(findItemStack("Mini Bezel Dot Spotlight",1),findItemStack("Mini Dot Spotlight"),findItemStack("Iron Plate"),Items.GOLD_NUGGET);
        addShapelessRecipe(findItemStack("Mini Eyeball Dot Spotlight",1),findItemStack("Mini Dot Spotlight"),findItemStack("Iron Plate"),Blocks.LEVER);
        addShapelessRecipe(findItemStack("Mini Deep Baffle Dot Light",1),findItemStack("Mini Dot Spotlight"),findItemStack("Iron Plate"),Items.DYE.black());
        addShapelessRecipe(findItemStack("Mini Twin Dot Light",1),findItemStack("Mini Dot Spotlight"),findItemStack("Iron Plate"),Blocks.GLASS_PANE);
        addRecipe(findItemStack("Twin Adjustable Eave Floodlight",1),"L L"," P ",'L',findItemStack("Mini Bezel Dot Spotlight"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Classic Four Blade Ceiling Fan",1)," M ","PLP"," P ",'M',findItemStack("Electrical Motor"),'P',Blocks.OAK_PLANKS,'L',findItemStack("Basic Lamp Holder"));
        addRecipe(findItemStack("Modern Three Blade Ceiling Fan",1)," M ","PLP"," P ",'M',findItemStack("Electrical Motor"),'P',findItemStack("Iron Plate"),'L',findItemStack("Basic Lamp Holder"));
        addRecipe(findItemStack("Industrial Five Block Ceiling Fan",1),"PMP","MLM","PPP",'P',findItemStack("Iron Plate"),'M',findItemStack("Electrical Motor"),'L',findItemStack("Basic Lamp Holder"));
        addRecipe(findItemStack("Aura Five Blade Ceiling Fan",1),"PMP","LLL","PPP",'P',findItemStack("Iron Plate"),'M',findItemStack("Electrical Motor"),'L',findItemStack("Basic Lamp Holder"));
        addRecipe(findItemStack("Large Area Floodlight",1)," D ","DLD"," P ",'D',Items.DIAMOND,'L',findItemStack("Warehouse Wall Floodlight"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Quad Head Parking Lot Light",1)," D ","DLD"," P ",'D',Items.IRON_BLOCK,'L',findItemStack("Twin Parking Lot Light"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Round Canopy Parking Lot Light",1)," D ","DLD"," P ",'D',Items.QUARTZ,'L',findItemStack("Twin Parking Lot Light"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Offset Twin Arm Parking Lot Light",1)," D ","DLD"," P ",'D',Items.COPPER_INGOT,'L',findItemStack("Twin Parking Lot Light"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Round Niche Pool Light",1)," D ","DLD"," P ",'D',Items.PRISMARINE_CRYSTALS,'L',findItemStack("Robust Lamp Socket"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Linear Pool Wall Light",1)," D ","DLD"," P ",'D',Items.PRISMARINE_CRYSTALS,'L',findItemStack("Thin Fluorescent Shop Light"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Square Recessed Pool Light",1)," D ","DLD"," P ",'D',Items.PRISMARINE_CRYSTALS,'L',findItemStack("Flat Lamp Socket"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Pool Corner Wash Light",1)," D ","DLD"," P ",'D',Items.PRISMARINE_CRYSTALS,'L',findItemStack("Lamp Socket B Projector"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Pool Step Marker Light",1)," D ","DLD"," P ",'D',Items.PRISMARINE_CRYSTALS,'L',findItemStack("Mini Dot Spotlight"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Patio Festoon Party Lights",1)," D ","DLD"," P ",'D',Items.DYE.orange(),'L',findItemStack("String Lights"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Electric Poolside Tiki Light",1)," D ","DLD"," P ",'D',Items.BAMBOO,'L',findItemStack("Traditional Lantern Post Light"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Multicolor Party Wash Light",1)," D ","DLD"," P ",'D',Items.DYE.magenta(),'L',findItemStack("Warehouse Wall Floodlight"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Poolside Globe String Lights",1)," D ","DLD"," P ",'D',Items.GLASS_BOTTLE,'L',findItemStack("String Lights"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Poolside Party Lantern",1)," D ","DLD"," P ",'D',Items.DYE.lightBlue(),'L',findItemStack("Porch Lantern Sconce"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Deck Post Cap Light",1)," D ","DLD"," P ",'D',Items.OAK_SLAB,'L',findItemStack("Basic Lamp Holder"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Deck Step Riser Light",1)," D ","DLD"," P ",'D',Items.SPRUCE_SLAB,'L',findItemStack("Flat Lamp Socket"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Tropical Patio Ceiling Fan",1)," D ","DLD"," P ",'D',Items.JUNGLE_PLANKS,'L',findItemStack("Classic Four Blade Ceiling Fan"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Adjustable Suspended Warehouse Light",1),"CCC","PFP"," C ",'C',Items.IRON_CHAIN,'P',findItemStack("Iron Plate"),'F',findItemStack("Suspended Factory Fluorescent Light"));
        addRecipe(findItemStack("Nautical Pier Bollard Light",1)," D ","GLG"," P ",'D',Items.DYE.white(),'G',Blocks.GLASS_PANE,'L',findItemStack("Basic Lamp Holder"),'P',Blocks.IRON_BLOCK);
        addRecipe(findItemStack("Marine Pier Bulkhead Light",1)," D ","GLG"," P ",'D',Items.DYE.orange(),'G',Blocks.GLASS_PANE,'L',findItemStack("Basic Lamp Holder"),'P',Blocks.IRON_BLOCK);
        addRecipe(findItemStack("Harbor Gooseneck Pier Light",1)," D ","GLG"," P ",'D',Items.DYE.magenta(),'G',Blocks.GLASS_PANE,'L',findItemStack("Basic Lamp Holder"),'P',Blocks.IRON_BLOCK);
        addRecipe(findItemStack("Pier Lantern Post Light",1)," D ","GLG"," P ",'D',Items.DYE.lightBlue(),'G',Blocks.GLASS_PANE,'L',findItemStack("Basic Lamp Holder"),'P',Blocks.IRON_BLOCK);
        addRecipe(findItemStack("Low Profile Dock Light",1)," D ","GLG"," P ",'D',Items.DYE.yellow(),'G',Blocks.GLASS_PANE,'L',findItemStack("Basic Lamp Holder"),'P',Blocks.IRON_BLOCK);
        addRecipe(findItemStack("Louvered Walkway Bollard",1)," D ","GLG"," P ",'D',Items.DYE.lime(),'G',Blocks.GLASS_PANE,'L',findItemStack("Basic Lamp Holder"),'P',Blocks.IRON_BLOCK);
        addRecipe(findItemStack("Shielded Walkway Light",1)," D ","GLG"," P ",'D',Items.DYE.pink(),'G',Blocks.GLASS_PANE,'L',findItemStack("Basic Lamp Holder"),'P',Blocks.IRON_BLOCK);
        addRecipe(findItemStack("Modern Blade Walkway Light",1)," D ","GLG"," P ",'D',Items.DYE.gray(),'G',Blocks.GLASS_PANE,'L',findItemStack("Basic Lamp Holder"),'P',Blocks.IRON_BLOCK);
        addRecipe(findItemStack("Mushroom Walkway Light",1)," D ","GLG"," P ",'D',Items.DYE.lightGray(),'G',Blocks.GLASS_PANE,'L',findItemStack("Basic Lamp Holder"),'P',Blocks.IRON_BLOCK);
        addRecipe(findItemStack("Arched Walkway Light",1)," D ","GLG"," P ",'D',Items.DYE.cyan(),'G',Blocks.GLASS_PANE,'L',findItemStack("Basic Lamp Holder"),'P',Blocks.IRON_BLOCK);
        addRecipe(findItemStack("Recessed Brick Patio Wall Light",1)," D ","GLG"," P ",'D',Items.DYE.purple(),'G',Blocks.GLASS_PANE,'L',findItemStack("Basic Lamp Holder"),'P',Blocks.STONE_BRICKS);
        addRecipe(findItemStack("Round Stone Wall Light",1)," D ","GLG"," P ",'D',Items.DYE.blue(),'G',Blocks.GLASS_PANE,'L',findItemStack("Basic Lamp Holder"),'P',Blocks.STONE_BRICKS);
        addRecipe(findItemStack("Slotted Patio Wall Light",1)," D ","GLG"," P ",'D',Items.DYE.brown(),'G',Blocks.GLASS_PANE,'L',findItemStack("Basic Lamp Holder"),'P',Blocks.STONE_BRICKS);
        addRecipe(findItemStack("Recessed Square Patio Wall Light",1)," D ","GLG"," P ",'D',Items.DYE.green(),'G',Blocks.GLASS_PANE,'L',findItemStack("Basic Lamp Holder"),'P',Blocks.STONE_BRICKS);
        addRecipe(findItemStack("Stone Wall Wash Light",1)," D ","GLG"," P ",'D',Items.DYE.red(),'G',Blocks.GLASS_PANE,'L',findItemStack("Basic Lamp Holder"),'P',Blocks.STONE_BRICKS);
        addRecipe(findItemStack("Shoebox Parking Lot Light",1)," D ","GLG"," P ",'D',Items.DYE.white(),'G',Blocks.GLASS,'L',findItemStack("Basic Lamp Holder"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Twin Wide Area Parking Lot Light",1)," D ","GLG"," P ",'D',Items.DYE.blue(),'G',Blocks.GLASS,'L',findItemStack("Basic Lamp Holder"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Three Arm Parking Lot Light",1)," D ","GLG"," P ",'D',Items.DYE.black(),'G',Blocks.GLASS,'L',findItemStack("Basic Lamp Holder"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Gumdrop Sidewalk Light",1)," D ","GLG"," P ",'D',Items.DYE.green(),'G',Blocks.GLASS,'L',findItemStack("Basic Lamp Holder"),'P',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Dual Gumdrop Sidewalk Light",1)," D ","GLG"," P ",'D',Items.DYE.red(),'G',Blocks.GLASS,'L',findItemStack("Basic Lamp Holder"),'P',findItemStack("Iron Plate"));
    }

    public static void recipeLampSocket() {
        addRecipe(findItemStack("Lamp Socket A", 3),
            "G ",
            "IG",
            "G ",
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Lamp Socket B Projector", 3),
            " I",
            "IG",
            " I",
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Street Light", 1),
            "G",
            "I",
            "I",
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Robust Lamp Socket", 3),
            "GIG",
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("Flat Lamp Socket", 3),
            "IGI",
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("Simple Lamp Socket", 3),
            " I ",
            "GGG",
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Fluorescent Lamp Socket", 3),
            " I ",
            "I I",
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));


        addRecipe(findItemStack("Suspended Lamp Socket", 2),
            "I",
            "G",
            'G', findItemStack("Robust Lamp Socket"),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Long Suspended Lamp Socket", 2),
            "I",
            "I",
            "G",
            'G', findItemStack("Robust Lamp Socket"),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Sconce Lamp Socket", 2),
            "GCG",
            "GIG",
            'G', stack(Blocks.GLASS_PANE),
            'C', "dustCoal",
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("48V Emergency Lamp"),
            "cbc",
            " l ",
            " g ",
            'c', findItemStack("Copper Cable"),
            'b', findItemStack("Portable Battery Pack"),
            'l', findItemStack("48V LED Bulb"),
            'g', stack(Blocks.GLASS_PANE));

        addRecipe(findItemStack("230V Emergency Lamp"),
            "cbc",
            " l ",
            " g ",
            'c', findItemStack("Copper Cable"),
            'b', findItemStack("Portable Battery Pack"),
            'l', findItemStack("230V LED Bulb"),
            'g', stack(Blocks.GLASS_PANE));
    }

    public static void recipeLampSupply() {
        addRecipe(findItemStack("Lamp Supply", 1),
            " I ",
            "ICI",
            " I ",
            'C', "ingotCopper",
            'I', stack(Items.IRON_INGOT));

        // Preserve the native generic recipe: a separate conversion avoids two outputs
        // competing for the source's otherwise identical 120 V crafting pattern.
        addShapelessRecipe(findItemStack("120V Lamp Supply"), findItemStack("Lamp Supply"));
        addRecipe(findItemStack("240V Lamp Supply"),
            " I ", "ACA", " I ",
            'C', "ingotCopper", 'I', stack(Items.IRON_INGOT),
            'A', findItemStack("Advanced Machine Block"));

    }

    public static void recipePowerSocket() {
        addRecipe(findItemStack("48V Power Socket", 16),
            "RUR",
            "ACA",
            'R', "itemRubber",
            'U', "plateCopper",
            'A', "plateAlloy",
            'C', findItemStack("Copper Cable"));
        addRecipe(findItemStack("230V Power Socket", 16),
            "RUR",
            "ACA",
            "R R",
            'R', "itemRubber",
            'U', "plateCopper",
            'A', "plateAlloy",
            'C', findItemStack("Copper Cable"));
    }

    public static void recipePassiveComponent() {

        addRecipe(findItemStack("Experimental Wall Outlet")," P ","CRC"," P ",'P',findItemStack("Iron Plate"),'C',findItemStack("Copper Cable"),'R',Items.REDSTONE);
        addShapelessRecipe(findItemStack("Flexible Appliance Cord"),findItemStack("Copper Cable"),findItemStack("Copper Cable"),findItemStack("Rubber"));
        addShapelessRecipe(findItemStack("240 V Wall Outlet"),findItemStack("Experimental Wall Outlet"),vanilla("orange_dye"));
        addShapelessRecipe(findItemStack("240 V Appliance Cord"),findItemStack("Flexible Appliance Cord"),vanilla("orange_dye"));
        addShapelessRecipe(findItemStack("240 V 30 A Dryer Cord"),findItemStack("240 V Appliance Cord"),findItemStack("Copper Cable"),findItemStack("Rubber"));
        addShapelessRecipe(findItemStack("240 V 30 A Dryer Outlet"),findItemStack("240 V Wall Outlet"),findItemStack("Copper Cable"),findItemStack("Iron Plate"));
        addShapelessRecipe(findItemStack("Six Socket Power Strip"),findItemStack("Experimental Wall Outlet"),findItemStack("Experimental Wall Outlet"),findItemStack("Experimental Wall Outlet"),findItemStack("Iron Plate"));
        var cordDyes = new Object[]{vanilla("purple_dye"),vanilla("light_gray_dye"),vanilla("green_dye"),vanilla("red_dye"),vanilla("black_dye"),vanilla("yellow_dye"),vanilla("cyan_dye")};
        int cordDye=0;
        for(var rating: mods.eln.transparentnode.cord.CordRating.values()) if(rating.getIndustrial()) {
            var dye=cordDyes[cordDye++];
            addShapelessRecipe(findItemStack(rating.getCordName()),findItemStack("Flexible Appliance Cord"),findItemStack("Rubber"),dye);
            addShapelessRecipe(findItemStack(rating.getOutletName()),findItemStack("Experimental Wall Outlet"),findItemStack("Iron Plate"),dye);
        }

        // Native bootstrap for source Fabricator; wafer production stays in the powered plate machine.
        addRecipe(findItemStack("Fabricator"),"ICI","PMP","IEI",'I',Items.IRON_INGOT,'C',dictCheapChip,'P',findItemStack("Copper Plate"),'M',findItemStack("Machine Block"),'E',findItemStack("Electrical Motor"));

        for (String[] wheel : new String[][]{{"Iron Roller Wheel","ingotIron"},{"Steel Roller Wheel","ingotSteel"},{"Aluminum Roller Wheel","ingotAluminum"}}) addRecipe(findItemStack(wheel[0],2)," M ","MIM"," M ",'M',wheel[1],'I',stack(Items.IRON_INGOT));
        addRecipe(findItemStack("Wire Roller"),"IRI","WMW","ICI",'I',stack(Items.IRON_INGOT),'R',stack(Items.REDSTONE),'W',findItemStack("Iron Roller Wheel"),'M',findItemStack("Machine Block"),'C',findItemStack("Electrical Motor"));
        addRecipe(findItemStack("Wire Insulator"),"IRI","BMB","ICI",'I',stack(Items.IRON_INGOT),'R',stack(Items.REDSTONE),'B',stack(Items.BUCKET),'M',findItemStack("Machine Block"),'C',findItemStack("Copper Cable"));
        addRecipe(findItemStack("Wire Combiner"),"IRI","CMC","IWI",'I',stack(Items.IRON_INGOT),'R',stack(Items.REDSTONE),'C',findItemStack("Electrical Motor"),'M',findItemStack("Machine Block"),'W',findItemStack("Iron Roller Wheel"));

        addRecipe(findItemStack("Winding DC-DC Converter"), "C C", "III", 'C', findItemStack("Copper Cable"), 'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("One-way DC-DC Converter"), "D", "R", 'D', findItemStack("Winding DC-DC Converter"), 'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("One-way Boost vDC/DC Converter"), "D", "R", 'D', findItemStack("Variable DC-DC Converter"), 'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("One-way Buck vDC/DC Converter"), "D", "r", 'D', findItemStack("Variable DC-DC Converter"), 'r', dictCheapChip);
        addRecipe(findItemStack("One-way Boost/Buck vDC/DC Converter"), " B ", "DVD", " B ", 'V', findItemStack("Variable DC-DC Converter"), 'D', findItemStack("Signal Diode"), 'B', dictCheapChip);
        addRecipe(findItemStack("Variable DC-DC Converter"), "CBC", "III", 'C', findItemStack("Copper Cable"), 'I', stack(Items.IRON_INGOT), 'B', dictCheapChip);
        addRecipe(findItemStack("Variable inductor"), " S ", "cIc", " c ", 'I', stack(Items.IRON_INGOT), 'c', findItemStack("Copper Cable"), 'S', findItemStack("Signal Cable"));

        addRecipe(findItemStack("Signal Diode", 4),
            " RB",
            "IIR",
            " RB",
            'R', stack(Items.REDSTONE),
            'I', findItemStack("Iron Cable"),
            'B', "itemRubber");

        addRecipe(findItemStack("10A Diode", 3),
            " RB",
            "IIR",
            " RB",
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT),
            'B', "itemRubber");

        addRecipe(findItemStack("25A Diode"),
            "D",
            "D",
            "D",
            'D', findItemStack("10A Diode"));


        addRecipe(findItemStack("Power Capacitor"),
            "cPc",
            "III",
            'I', stack(Items.IRON_INGOT),
            'c', findItemStack("Iron Cable"),
            'P', "plateIron");

        addRecipe(findItemStack("Power Inductor"),
            " P ",
            "cIc",
            "IPI",
            'I', stack(Items.IRON_INGOT),
            'c', findItemStack("Copper Coil"),
            'P', "plateIron");

        addRecipe(findItemStack("Power Resistor"),
            " P ",
            "c c",
            "IPI",
            'I', stack(Items.IRON_INGOT),
            'c', findItemStack("Copper Coil"),
            'P', "plateCopper");

        addRecipe(findItemStack("Rheostat"),
            " R ",
            " MS",
            "cmc",
            'R', findItemStack("Power Resistor"),
            'c', findItemStack("Copper Coil"),
            'm', findItemStack("Machine Block"),
            'M', findItemStack("Electrical Motor"),
            'S', findItemStack("Signal Cable")
        );

        addRecipe(findItemStack("Thermistor"),
            " P ",
            "csc",
            "IPI",
            's', "dustSilicon",
            'I', stack(Items.IRON_INGOT),
            'c', findItemStack("Copper Coil"),
            'P', "plateCopper");

        addRecipe(findItemStack("NTC Thermistor"), "CSC",
            'C', findItemStack("Copper Cable"), 'S', "dustSilicon");

        addRecipe(findItemStack("Large Rheostat"),
            "   ",
            " D ",
            "CRC",
            'R', findItemStack("Rheostat"),
            'C', findItemStack("Copper Thermal Cable"),
            'D', findItemStack("Small Passive Thermal Dissipator")
        );
    }

    public static void recipeSwitch() {
		/*
		 * addRecipe(findItemStack("Signal Switch"), "  I", " I ", "CAC", 'R', stack(Items.REDSTONE), 'A', "itemRubber", 'I', findItemStack("Copper Cable"), 'C', findItemStack("Signal Cable"));
		 *
		 * addRecipe(findItemStack("Signal Switch with LED"), " RI", " I ", "CAC", 'R', stack(Items.REDSTONE), 'A', "itemRubber", 'I', findItemStack("Copper Cable"), 'C', findItemStack("Signal Cable"));
		 */

        addRecipe(findItemStack("Low Voltage Switch"),
            "  I",
            " I ",
            "CAC",
            'R', stack(Items.REDSTONE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Medium Voltage Switch"),
            "  I",
            "AIA",
            "CAC",
            'R', stack(Items.REDSTONE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Copper Cable"));

        addRecipe(findItemStack("High Voltage Switch"),
            "AAI",
            "AIA",
            "CAC",
            'R', stack(Items.REDSTONE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Very High Voltage Switch"),
            "AAI",
            "AIA",
            "CAC",
            'R', stack(Items.REDSTONE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', "ingotAlloy");

    }

    public static void recipeElectricalRelay() {
        addRecipe(findItemStack("Low Voltage Relay"),
            "GAG",
            "OIO",
            "CRC",
            'R', stack(Items.REDSTONE),
            'O', stack(Items.IRON_INGOT),
            'G', stack(Blocks.GLASS_PANE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Medium Voltage Relay"),
            "AGA",
            "OIO",
            "CRC",
            'R', stack(Items.REDSTONE),
            'O', stack(Items.IRON_INGOT),
            'G', stack(Blocks.GLASS_PANE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Copper Cable"));

        addRecipe(findItemStack("High Voltage Relay"),
            "AAA",
            "OIO",
            "CRC",
            'R', stack(Items.REDSTONE),
            'O', stack(Items.IRON_INGOT),
            'G', stack(Blocks.GLASS_PANE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Very High Voltage Relay"),
            "AAA",
            "OIO",
            "CRC",
            'R', stack(Items.REDSTONE),
            'O', stack(Items.IRON_INGOT),
            'G', stack(Blocks.GLASS_PANE),
            'A', "itemRubber",
            'I', findItemStack("Copper Cable"),
            'C', "ingotAlloy");

        addRecipe(findItemStack("Signal Relay"),
            "GGG",
            "OIO",
            "CRC",
            'R', stack(Items.REDSTONE),
            'O', stack(Items.IRON_INGOT),
            'G', stack(Blocks.GLASS_PANE),
            'I', findItemStack("Copper Cable"),
            'C', findItemStack("Signal Cable"));
    }

    public static void recipeWirelessSignal() {
        addRecipe(findItemStack("Wireless Signal Transmitter"),
            " S ",
            " R ",
            "ICI",
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT),
            'C', dictCheapChip,
            'S', findItemStack("Signal Antenna"));

        addRecipe(findItemStack("Wireless Signal Repeater"),
            "S S",
            "R R",
            "ICI",
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT),
            'C', dictCheapChip,
            'S', findItemStack("Signal Antenna"));

        addRecipe(findItemStack("Wireless Signal Receiver"),
            " S ",
            "ICI",
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT),
            'C', dictCheapChip,
            'S', findItemStack("Signal Antenna"));
    }

    public static void recipeChips() {
        addRecipe(findItemStack("NOT Chip"),
            "   ",
            "cCr",
            "   ",
            'C', dictCheapChip,
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("AND Chip"),
            " c ",
            "cCc",
            " c ",
            'C', dictCheapChip,
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("NAND Chip"),
            " c ",
            "cCr",
            " c ",
            'C', dictCheapChip,
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("OR Chip"),
            " r ",
            "rCr",
            " r ",
            'C', dictCheapChip,
            'r', stack(Items.REDSTONE));

        addRecipe(findItemStack("NOR Chip"),
            " r ",
            "rCc",
            " r ",
            'C', dictCheapChip,
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("XOR Chip"),
            " rr",
            "rCr",
            " rr",
            'C', dictCheapChip,
            'r', stack(Items.REDSTONE));

        addRecipe(findItemStack("XNOR Chip"),
            " rr",
            "rCc",
            " rr",
            'C', dictCheapChip,
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("PAL Chip"),
            "rcr",
            "cCc",
            "rcr",
            'C', dictAdvancedChip,
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Schmitt Trigger Chip"),
            "   ",
            "cCc",
            "   ",
            'C', dictAdvancedChip,
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("D Flip Flop Chip"),
            "   ",
            "cCc",
            " p ",
            'C', dictAdvancedChip,
            'p', "plateCopper",
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Oscillator Chip"),
            "pdp",
            "cCc",
            "   ",
            'C', dictAdvancedChip,
            'p', "plateCopper",
            'c', findItemStack("Copper Cable"),
            'd', findItemStack("Dielectric"));

        addRecipe(findItemStack("JK Flip Flop Chip"),
            " p ",
            "cCc",
            " p ",
            'C', dictAdvancedChip,
            'p', "plateCopper",
            'c', findItemStack("Copper Cable"));


        addRecipe(findItemStack("Amplifier"),
            "  r",
            "cCc",
            "   ",
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("OpAmp"),
            "  r",
            "cCc",
            " c ",
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("Configurable summing unit"),
            " cr",
            "cCc",
            " c ",
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        // (1.12 registered the VCA without a recipe; AGE's: the amplifier's, plus a signal cable for the control input)
        addRecipe(findItemStack("Voltage controlled amplifier"),
            " sr",
            "cCc",
            "   ",
            'r', stack(Items.REDSTONE),
            's', findItemStack("Signal Cable"),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);
        addRecipe(findItemStack("Sample and hold"),
            " rr",
            "cCc",
            " c ",
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("Voltage controlled sine oscillator"),
            "rrr",
            "cCc",
            "   ",
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("Voltage controlled sawtooth oscillator"),
            "   ",
            "cCc",
            "rrr",
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("PID Regulator"),
            "rrr",
            "cCc",
            "rcr",
            'r', stack(Items.REDSTONE),
            'c', findItemStack("Copper Cable"),
            'C', dictAdvancedChip);

        addRecipe(findItemStack("Lowpass filter"),
            "CdC",
            "cDc",
            " s ",
            'd', findItemStack("Dielectric"),
            'c', findItemStack("Copper Cable"),
            'C', "plateCopper",
            'D', findItemStack("Coal Dust"),
            's', dictCheapChip);
    }

    public static void recipeTransformer() {
        addRecipe(findItemStack("DC-DC Converter"),
            "C C",
            "III",
            'C', findItemStack("Copper Coil"),
            'I', stack(Items.IRON_INGOT));
        // Baughn 2026-11-06: a dearer converter whose tap a signal sets
        addShapelessRecipe(findItemStack("Signal-Tapped DC-DC Converter", 1),
            findItemStack("DC-DC Converter"), dictAdvancedChip, findItemStack("Signal Cable"));
    }

    public static void recipeHeatFurnace() {
        addRecipe(findItemStack("Stone Heat Furnace"),
            "BBB",
            "BIB",
            "BiB",
            'B', stack(Blocks.STONE),
            'i', findItemStack("Copper Thermal Cable"),
            'I', findItemStack("Combustion Chamber"));

        addRecipe(findItemStack("Fuel Heat Furnace"),
            "IcI",
            "mCI",
            "IiI",
            'c', findItemStack("Cheap Chip"),
            'm', findItemStack("Electrical Motor"),
            'C', stack(Items.CAULDRON),
            'I', stack(Items.IRON_INGOT),
            'i', findItemStack("Copper Thermal Cable"));
    }

    public static void recipeTurbine() {
        // 26.x (Baughn): the 50V/200V heat turbines are replaced by the small steam turbines (boiler + 50V/200V Steam
        // Turbine); placed ones keep working, but they're no longer made
        addRecipe(findItemStack("Generator"),
            "mmm",
            "ama",
            " ME",
            'm', findItemStack("Advanced Electrical Motor"),
            'M', findItemStack("Advanced Machine Block"),
            'a', firstExistingOre("ingotAluminum", "ingotIron"),
            'E', findItemStack("Copper Cable")
        );
        addRecipe(findItemStack("Steam Turbine"),
            " a ",
            "aAa",
            " M ",
            'a', firstExistingOre("ingotAluminum", "ingotIron"),
            'A', firstExistingOre("blockAluminum", "blockIron"),
            'M', findItemStack("Advanced Machine Block")
        );
        // Baughn 2026-10-29: the 8 kW generator (two 4 kW ones' worth, a bigger winding) and the small shaft motor
        addRecipe(findItemStack("8kW Generator"),
            "mmm",
            "GaG",
            " ME",
            'm', findItemStack("Advanced Electrical Motor"),
            'G', findItemStack("Generator"),
            'M', findItemStack("Advanced Machine Block"),
            'a', firstExistingOre("ingotAluminum", "ingotIron"),
            'E', findItemStack("Copper Cable")
        );
        addRecipe(findItemStack("230V Shaft Motor"),
            " m ",
            "iMi",
            "AEA",
            'm', findItemStack("Electrical Motor"),
            'i', stack(Items.IRON_INGOT),
            'M', findItemStack("Machine Block"),
            'A', "itemRubber",
            'E', findItemStack("Copper Cable")
        );
        // the shaft redo's plumbing (Baughn 2026-10-29): tanks and the inline pump
        addRecipe(findItemStack("Iron Tank"),
            "iPi",
            "i i",
            "iPi",
            'i', stack(Items.IRON_INGOT),
            'P', stack(Items.GLASS_PANE)
        );
        addRecipe(findItemStack("Alloy Tank"),
            "aPa",
            "aTa",
            "aPa",
            'a', findItemStack("Alloy Ingot"),
            'P', stack(Items.GLASS_PANE),
            'T', findItemStack("Iron Tank")
        );
        // the pipe mixer: two check valves in a cast T, a signal to steer them
        addRecipe(findItemStack("Pipe Mixer"),
            " c ",
            "PIP",
            " P ",
            'c', findItemStack("Signal Cable"),
            'P', stack(Items.IRON_INGOT),
            'I', findItemStack("Machine Block"));

        addRecipe(findItemStack("230V Inline Pump"),
            "Bm ",
            "pMp",
            "AEA",
            'm', findItemStack("Electrical Motor"),
            'p', stack(Items.IRON_INGOT),
            'M', findItemStack("Machine Block"),
            'A', "itemRubber",
            'B', stack(Items.BUCKET),
            'E', findItemStack("Copper Cable")
        );
        // PLACEHOLDER (todo #471, prototype): the Inline Pump's with iron bars (an intake grille) for its bucket
        addRecipe(findItemStack("230V Air Blower"),
            "Bm ",
            "pMp",
            "AEA",
            'm', findItemStack("Electrical Motor"),
            'p', stack(Items.IRON_INGOT),
            'M', findItemStack("Machine Block"),
            'A', "itemRubber",
            'B', stack(Items.IRON_BARS),
            'E', findItemStack("Copper Cable")
        );
        addRecipe(findItemStack("LP Steam Turbine"),
            "aAa",
            "ASA",
            "aMa",
            'a', firstExistingOre("ingotAluminum", "ingotIron"),
            'A', firstExistingOre("blockAluminum", "blockIron"),
            'S', findItemStack("Steam Turbine"),
            'M', findItemStack("Advanced Machine Block")
        );
        addRecipe(findItemStack("Plant Steam Condenser"),
            "cTc",
            "cCc",
            "cMc",
            'c', findItemStack("Copper Thermal Cable"),
            'T', findItemStack("Iron Tank"),
            'C', findItemStack("Steam Condenser"),
            'M', findItemStack("Advanced Machine Block")
        );
        addRecipe(findItemStack("Counterflow Heat Exchanger"),
            "cIc",
            "ccc",
            "cIc",
            'c', findItemStack("Copper Thermal Cable"),
            'I', findItemStack("Iron Tank")
        );
        // cheap on purpose (Baughn 2026-10-29): what a water slug costs
        addRecipe(findItemStack("Turbine Blade Set"),
            "I I",
            " S ",
            "I I",
            'I', stack(Items.IRON_INGOT),
            'S', stack(Items.STICK)
        );
        addRecipe(findItemStack("Gas Turbine"),
            "msH",
            "sSs",
            " M ",
            'm', findItemStack("Advanced Electrical Motor"),
            'H', findItemStack("Copper Thermal Cable"),
            's', firstExistingOre("ingotSteel", "ingotIron"),
            'S', firstExistingOre("blockSteel", "blockIron"),
            'M', findItemStack("Advanced Machine Block")
        );

        addRecipe(findItemStack("Joint"),
            "   ",
            "iii",
            " m ",
            'i', "ingotIron",
            'm', findItemStack("Machine Block")
        );

        addRecipe(findItemStack("Joint hub"),
            " i ",
            "iii",
            " m ",
            'i', "ingotIron",
            'm', findItemStack("Machine Block")
        );

        addRecipe(findItemStack("Flywheel"),
            "iIi",
            "ImI",
            "iIi",
            'i', "ingotIron",
            'I', "blockIron",
            'm', findItemStack("Machine Block")
        );

        addRecipe(findItemStack("Tachometer"),
            "p  ",
            "iii",
            "cm ",
            'i', "ingotIron",
            'm', findItemStack("Machine Block"),
            'p', findItemStack("Electrical Probe Chip"),
            'c', findItemStack("Signal Cable")
        );
    }

    public static void recipeBattery() {
        addRecipe(findItemStack("Cost Oriented Battery"),
            "C C",
            "PPP",
            "PPP",
            'C', findItemStack("Copper Cable"),
            'P', "ingotLead",
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Capacity Oriented Battery"),
            "PPP",
            "PBP",
            "PPP",
            'B', findItemStack("Cost Oriented Battery"),
            'P', "ingotLead");

        addRecipe(findItemStack("Voltage Oriented Battery"),
            "PPP",
            "PBP",
            "PPP",
            'B', findItemStack("Cost Oriented Battery"),
            'P', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Current Oriented Battery"),
            "PPP",
            "PBP",
            "PPP",
            'B', findItemStack("Cost Oriented Battery"),
            'P', "ingotCopper");

        addRecipe(findItemStack("Life Oriented Battery"),
            "P P",
            " B ",
            "P P",
            'B', findItemStack("Cost Oriented Battery"),
            'P', stack(Items.GOLD_INGOT));

        addRecipe(findItemStack("Single-use Battery"),
            "ppp",
            "III",
            "ppp",
            'C', findItemStack("Copper Cable"),
            'p', stack(Items.COAL),
            'I', "ingotCopper");

        addRecipe(findItemStack("Single-use Battery"),
            "ppp",
            "III",
            "ppp",
            'C', findItemStack("Copper Cable"),
            'p', stack(Items.CHARCOAL),
            'I', "ingotCopper");
    }

    /**
     * 1.12 made one Utility Pole recipe per ore name present at startup (aluminium, steel), and an iron one when there
     * was none. 26.x: aluminium and steel as they load (NeoForge conditions on the tags), and EA's own alloy always.
     * The iron fallback is gone: it switched itself off whenever any mod made steel, so on Incognito poles needed
     * Energized Power's steel (nero 1572028077159223347). Baughn (msg 1572028948240404481): "make it accept alloy
     * ingots, and remove the iron fallback"; aluminium stays, for EA's own aluminium production later.
     */
    public static void recipeGridDevices() {
        for (var t : new String[]{"ingotAluminum", "ingotSteel"}) {
            addRecipeIf(findItemStack("Utility Pole"),
                new ICondition[]{new NotCondition(new TagEmptyCondition<>(OreDictTags.tag(t)))},
                "WWW",
                "IWI",
                " W ",
                'W', "logWood",
                'I', t
            );
        }
        addRecipe(findItemStack("Utility Pole"),
            "WWW",
            "IWI",
            " W ",
            'W', "logWood",
            'I', "ingotAlloy"
        );
        // (the Utility Pole w/DC-DC Converter is retired: a Pole Transformer from the wire machine fits any pole, Baughn 2026-11-08)
        // a pole streetlight: an LED bulb, a light sensor (its dark sensor), an iron bar arm and a glass pane lens (Baughn 2026-11-08)
        addRecipe(stack(mods.eln.registry.ElnRegistry.STREETLIGHT.get()),
            "BBI",
            " LS",
            "  G",
            'B', stack(Items.IRON_BARS),
            'I', "ingotIron",
            'L', findItemStack("230V LED Bulb"),
            'S', findItemStack("Electrical Light Sensor"),
            'G', stack(Items.GLASS_PANE));
//		if (oreNames.contains("sheetPlastic")) {
//			addRecipe(findItemStack("Downlink"),
//					"H H",
//					"PMP",
//					"PPP",
//					'P', "sheetPlastic",
//					'M', findItemStack("Machine Block"),
//					'H', findItemStack("Copper Cable")
//			);
//		} else {
//			addRecipe(findItemStack("Downlink"),
//					"H H",
//					"PMP",
//					"PPP",
//					'P', "itemRubber",
//					'M', findItemStack("Machine Block"),
//					'H', findItemStack("Copper Cable")
//			);
//		}
    }


    public static void recipeElectricalFurnace() {
        addRecipe(findItemStack("Electrical Furnace"),
            "III",
            "IFI",
            "ICI",
            'C', findItemStack("Copper Cable"),
            'F', stack(Blocks.FURNACE),
            'I', stack(Items.IRON_INGOT));
    }

    public static void recipeSixNodeMisc() {
        addRecipe(findItemStack("Analog Watch"),
            "crc",
            "III",
            'c', findItemStack("Iron Cable"),
            'r', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Digital Watch"),
            "rcr",
            "III",
            'c', findItemStack("Iron Cable"),
            'r', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Hub"),
            "I I",
            " c ",
            "I I",
            'c', findItemStack("Copper Cable"),
            'I', stack(Items.IRON_INGOT));


        addRecipe(findItemStack("Energy Meter"),
            "IcI",
            "IRI",
            "IcI",
            'c', findItemStack("Copper Cable"),
            'R', dictCheapChip,
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Advanced Energy Meter"),
            " c ",
            "PRP",
            " c ",
            'c', findItemStack("Copper Cable"),
            'R', dictAdvancedChip,
            'P', "plateIron");

    }

    public static void recipeAutoMiner() {
        addRecipe(findItemStack("Auto Miner"),
            "MCM",
            "BOB",
            " P ",
            'C', dictAdvancedChip,
            'O', findItemStack("Ore Scanner"),
            'B', findItemStack("Advanced Machine Block"),
            'M', findItemStack("Advanced Electrical Motor"),
            'P', findItemStack("Mining Pipe"));
    }

    public static void recipeWindTurbine() {
        addRecipe(findItemStack("Wind Turbine"),
            " I ",
            "IMI",
            " B ",
            'B', findItemStack("Machine Block"),
            'I', "plateIron",
            'M', findItemStack("Electrical Motor"));

        addRecipe(findItemStack("Water Turbine"),
            "  I",
            "BMI",
            "  I",
            'I', "plateIron",
            'B', findItemStack("Machine Block"),
            'M', findItemStack("Electrical Motor"));

    }

    public static void recipeFuelGenerator() {
        addRecipe(findItemStack("48V Fuel Generator"),
            "III",
            " BA",
            "CMC",
            'I', "plateIron",
            'B', findItemStack("Machine Block"),
            'A', findItemStack("Analogic Regulator"),
            'C', findItemStack("Copper Cable"),
            'M', findItemStack("Electrical Motor"));

        addRecipe(findItemStack("230V Fuel Generator"),
            "III",
            " BA",
            "CMC",
            'I', "plateIron",
            'B', findItemStack("Advanced Machine Block"),
            'A', findItemStack("Analogic Regulator"),
            'C', findItemStack("Copper Cable"),
            'M', findItemStack("Advanced Electrical Motor"));
    }

    public static void recipeForkPatchUps() {
        addRecipe(findItemStack("Utility Wind Turbine 3.2kV"),"PPP","MCM","IFI",'P',findItemStack("Iron Plate"),'M',findItemStack("Advanced Electrical Motor"),'C',findItemStack("Copper Cable"),'I',Items.IRON_BLOCK,'F',findItemStack("Machine Block"));
        addRecipe(findItemStack("Large Watermill 3.2kV"),"WWW","MCM","IFI",'W',Items.OAK_PLANKS,'M',findItemStack("Advanced Electrical Motor"),'C',findItemStack("Copper Cable"),'I',Items.IRON_BLOCK,'F',findItemStack("Machine Block"));
        addRecipe(findItemStack("Pole Streetlight Attachment")," II","CPG"," I ",'I',findItemStack("Iron Plate"),'C',findItemStack("Copper Cable"),'P',findItemStack("Simple Lamp Socket"),'G',Items.GLASS);
        addRecipe(findItemStack("Mains Step-down Unit"),"CFC","MVM","CCC",'C',findItemStack("Copper Cable"),'F',findItemStack("Optimal Ferromagnetic Core"),'M',findItemStack("Machine Block"),'V',findItemStack("Variable DC-DC Converter"));
        for(var kind: mods.eln.transparentnode.switchgear.SwitchgearKind.values()) {
            String previous=switch(kind) { case CONTACTOR_THREE -> "Single Pole Contactor";case RELAY_FOUR -> "Two Pole Changeover Relay";case SSR_THREE -> "Solid State Relay";default -> null; };
            if(previous!=null)addRecipe(findItemStack(kind.getTitle()),kind.getPoles()==4?"R R":"RRR","PWP",'R',findItemStack(previous),'P',findItemStack("Iron Plate"),'W',findItemStack("Copper Cable"));
            else addRecipe(findItemStack(kind.getTitle()),kind.getChangeover() ? " W " : " C ","PRP",kind.getChangeover() ? "ICI" : "IWI",'C',findItemStack(kind.getSolidState()?"Silicon Plate":"Cheap Ferromagnetic Core"),'P',Items.IRON_NUGGET,'R',findItemStack(kind.getSolidState()?"Signal Relay":"Low Voltage Relay"),'I',findItemStack("Iron Plate"),'W',findItemStack("Copper Cable"));
        }
        addRecipe(findItemStack("69 kV Steel Transmission Pole"),"I I","IPI","I I",'I',Blocks.IRON_BARS,'P',findItemStack("Three Phase Utility Pole"));
        addRecipe(findItemStack("230 kV Steel Transmission Tower"),"IPI","PBP","I I",'I',Blocks.IRON_BLOCK,'P',findItemStack("69 kV Steel Transmission Pole"),'B',Blocks.IRON_BARS);
        for (var kind : mods.eln.utility.TransmissionKind.values()) if (kind.getRiser())
            addShapelessRecipe(findItemStack(kind.getTitle()),findItemStack(kind.getVolts()>100000?"230 kV Steel Transmission Tower":"69 kV Steel Transmission Pole"),findItemStack("Copper Cable"),findItemStack("Copper Cable"),findItemStack("Copper Cable"));
        mods.eln.utility.DistributionPoles.recipes((out,args)->{addShapelessRecipe(out,args);return kotlin.Unit.INSTANCE;});
        addRecipe(findItemStack("120/240V Service Weatherhead"),"PPP"," C "," C ",'P',findItemStack("Iron Plate"),'C',findItemStack("Copper Cable"));
        int mastIndex=0;
        for(var mast: java.util.List.of(mods.eln.utility.ServiceMastStyle.COMPACT_SIDE,mods.eln.utility.ServiceMastStyle.THROUGH_ROOF,mods.eln.utility.ServiceMastStyle.GOOSENECK)) {
            java.util.List<Object> ingredients=new java.util.ArrayList<>();ingredients.add(findItemStack("120/240V Service Weatherhead"));
            for(int i=0;i<=mastIndex;i++)ingredients.add(Items.IRON_INGOT);
            addShapelessRecipe(findItemStack(mast.getTitle()),ingredients.toArray());mastIndex++;
        }
        addRecipe(findItemStack("North American Meter Base"),"P P","PCP","P P",'P',findItemStack("Iron Plate"),'C',findItemStack("Copper Cable"));
        addRecipe(findItemStack("Analog Glass Service Meter")," G ","CMC"," P ",'G',Items.GLASS,'C',findItemStack("Copper Cable"),'M',findItemStack("Electrical Motor"),'P',findItemStack("Iron Plate"));
        addShapelessRecipe(findItemStack("Smart Digital Service Meter"),findItemStack("Analog Glass Service Meter"),Items.COMPARATOR,Items.REDSTONE);
        addRecipe(findItemStack("Wide Underground Meter Socket"),"PPP","MCC","P P",'P',findItemStack("Iron Plate"),'M',findItemStack("North American Meter Base"),'C',findItemStack("Copper Cable"));
        addRecipe(findItemStack("Underground Service Entrance Riser")," P "," C ","ICI",'P',findItemStack("Iron Plate"),'I',Items.IRON_INGOT,'C',findItemStack("Copper Cable"));
        addRecipe(findItemStack("120/240V Underground Service Handhole"),"IPI","CBC","III",'I',Items.STONE_BRICKS,'P',findItemStack("Iron Plate"),'C',findItemStack("Copper Cable"),'B',Items.IRON_BARS);
        addRecipe(findItemStack("Red Handle Service Disconnect"),"PPP","CLC","PPP",'P',findItemStack("Iron Plate"),'C',findItemStack("Copper Cable"),'L',Items.LEVER);
        addRecipe(findItemStack("Pad Mounted Neighbourhood Transformer"),"IPI","WCW","IMI",'I',findItemStack("Iron Plate"),'P',Items.REDSTONE,'W',findItemStack("Copper Cable"),'C',Items.IRON_BLOCK,'M',findItemStack("Machine Block"));
        for(int i=0;i<5;i++)addRecipe(findItemStack(mods.eln.utility.PoleHardware.INSTANCE.getTransformerNames().get(i)),"ICI","CTC","ICI",'I',findItemStack("Iron Plate"),'C',findItemStack("Copper Cable"),'T',findItemStack(i==0?"Mains Step-down Unit":mods.eln.utility.PoleHardware.INSTANCE.getTransformerNames().get(i-1)));
        addRecipe(findItemStack("Variable Frequency Motor Drive"),"IPI","WCW","IMI",'I',findItemStack("Iron Plate"),'P',Items.REDSTONE,'W',findItemStack("Copper Cable"),'C',Items.REDSTONE_BLOCK,'M',findItemStack("Machine Block"));
        addRecipe(findItemStack("Grid Control Console"),"IPI","WCW","IMI",'I',findItemStack("Iron Plate"),'P',Items.REDSTONE,'W',findItemStack("Copper Cable"),'C',Items.COMPARATOR,'M',findItemStack("Machine Block"));
        addRecipe(findItemStack("Pole Surge Arrester"),"IPI","WCW","IMI",'I',findItemStack("Iron Plate"),'P',Items.REDSTONE,'W',findItemStack("Copper Cable"),'C',Items.LIGHTNING_ROD.weathering().unaffected(),'M',findItemStack("Machine Block"));
        addRecipe(findItemStack("Electric Forklift"),"FFF","CMC","III",'F',findItemStack("Welded Machine Frame"),'C',findItemStack("Copper Cable"),'M',findItemStack("Electrical Motor"),'I',Items.IRON_BLOCK);
        for(var kind:mods.eln.vehicle.RoadVehicleKind.values())addRecipe(findItemStack(kind.getTitle()),"F F",kind==mods.eln.vehicle.RoadVehicleKind.CART?"CMC":"MCM","III",'F',findItemStack("Welded Machine Frame"),'C',findItemStack("Copper Cable"),'M',findItemStack("Electrical Motor"),'I',Items.IRON_BLOCK);
        addRecipe(findItemStack("240 V 100 A Vehicle Charger"),"IPI","WCW","IMI",'I',findItemStack("Iron Plate"),'P',Items.REDSTONE,'W',findItemStack("Copper Cable"),'C',findItemStack("Electrical Motor"),'M',findItemStack("Machine Block"));
        addRecipe(findItemStack("100 A Vehicle Charging Lead"),"CCC","  C","  I",'C',findItemStack("Copper Cable"),'I',findItemStack("Iron Plate"));
        addRecipe(findItemStack("Warehouse Equipment Pallet"),"PPP","S S","PPP",'P',Items.OAK_PLANKS,'S',Items.STICK);
        addRecipe(findItemStack("Electric Welding Station"),"IPI","WCW","IMI",'I',findItemStack("Iron Plate"),'P',Items.REDSTONE,'W',findItemStack("Copper Cable"),'C',Items.FLINT_AND_STEEL,'M',findItemStack("Machine Block"));
        addRecipe(findItemStack("Programmable EV Pack Charger"),"IPI","WCW","IMI",'I',findItemStack("Iron Plate"),'P',Items.REDSTONE,'W',findItemStack("Copper Cable"),'C',Items.QUARTZ,'M',findItemStack("Machine Block"));
        addRecipe(findItemStack("48 V 20 kWh Traction Battery"),"IBI","BBB","CBC",'I',findItemStack("Iron Plate"),'B',findItemStack("UPS Industrial Battery Module"),'C',findItemStack("Copper Cable"));
        addRecipe(findItemStack("Programmable Load Bank"),"IPI","WCW","IMI",'I',findItemStack("Iron Plate"),'P',Items.REDSTONE,'W',findItemStack("Copper Cable"),'C',Items.BLAZE_ROD,'M',findItemStack("Machine Block"));
        addRecipe(findItemStack("Protective Relay Cabinet"),"IPI","WCW","IMI",'I',findItemStack("Iron Plate"),'P',Items.REDSTONE,'W',findItemStack("Copper Cable"),'C',Items.REPEATER,'M',findItemStack("Machine Block"));
        addRecipe(findItemStack("Automatic Load Shedding Controller"),"IPI","WCW","IMI",'I',findItemStack("Iron Plate"),'P',Items.REDSTONE,'W',findItemStack("Copper Cable"),'C',Items.OBSERVER,'M',findItemStack("Machine Block"));
        addRecipe(findItemStack("Programmable UPS"),"CVC","MFM","CVC",'C',findItemStack("Copper Cable"),'V',findItemStack("Variable DC-DC Converter"),'M',findItemStack("Machine Block"),'F',findItemStack("Optimal Ferromagnetic Core"));
        addRecipe(findItemStack("Four Cell Plug-in UPS"),"POP","CDC","PPP",'P',findItemStack("Iron Plate"),'O',findItemStack("Six Socket Power Strip"),'C',findItemStack("Copper Cable"),'D',findItemStack("DC-DC Converter"));
        addShapelessRecipe(findItemStack("Wall-mounted Programmable UPS"),findItemStack("Programmable UPS"),findItemStack("Iron Plate"));
    }

    public static void recipeForkPatchStandby() {
        addRecipe(findItemStack("Automatic Transfer Switch"),"IPI","RCR","IPI",'I',findItemStack("Iron Plate"),'P',Items.COMPARATOR,'R',Items.REDSTONE,'C',findItemStack("Copper Cable"));
        addShapelessRecipe(findItemStack("Wall-mounted Automatic Transfer Switch"),findItemStack("Automatic Transfer Switch"),findItemStack("Iron Plate"));
        addRecipe(findItemStack("Standby Gasoline Generator"),"PPP","BGB","CRC",'P',findItemStack("Iron Plate"),'B',Items.BUCKET,'G',findItemStack("230V Fuel Generator"),'C',findItemStack("Copper Cable"),'R',Items.REDSTONE);
        // Native survival upgrade paths conserve the source tiers' installed engine capacity.
        var tiers=mods.eln.transparentnode.standby.GeneratorTier.values();
        for(int i=1;i<tiers.length;i++) {
            if(i==tiers.length-1)addRecipe(findItemStack(tiers[i].getTitle()),"PPP","GGG","CRC",'P',findItemStack("Iron Plate"),'G',findItemStack(tiers[i-1].getTitle()),'C',findItemStack("Copper Cable"),'R',Items.REDSTONE);
            else addRecipe(findItemStack(tiers[i].getTitle()),"PPP","GCG","PRP",'P',findItemStack("Iron Plate"),'G',findItemStack(tiers[i-1].getTitle()),'C',findItemStack("Copper Cable"),'R',Items.REDSTONE);
        }
    }

    public static void recipeForkPatchOffgrid() {
        addRecipe(findItemStack("UPS AGM Battery Cell"),"PPP","LBL","PPP",'P',findItemStack("Iron Plate"),'L',"ingotLead",'B',findItemStack("Portable Battery"));
        addRecipe(findItemStack("UPS LiFePO4 Battery Cell"),"CCC","GBG","CCC",'C',Items.COPPER_INGOT,'G',Items.GOLD_INGOT,'B',findItemStack("UPS AGM Battery Cell"));
        addRecipe(findItemStack("UPS Industrial Battery Module"),"PPP","BBB","PPP",'P',findItemStack("Iron Plate"),'B',findItemStack("UPS LiFePO4 Battery Cell"));
        addRecipe(findItemStack("16 kWh LiFePO4 Cell Stack"),"IBI","BCB","IBI",'I',findItemStack("Iron Plate"),'B',findItemStack("UPS Industrial Battery Module"),'C',findItemStack("Copper Cable"));
        for(var t:mods.eln.transparentnode.offgrid.OffGridType.values()) {
            if(t.getBank()) {
                String[] pattern=switch(t.getKwh()) { case 16 -> new String[]{" I ","CSC","IMI"}; case 32 -> new String[]{"SIS","CMC","III"}; case 48 -> new String[]{"SIS","CMC","ISI"}; default -> new String[]{"SIS","CMC","SIS"}; };
                addRecipe(findItemStack(t.getTitle()),pattern[0],pattern[1],pattern[2],'I',t.getWall()?Items.IRON_CHAIN:findItemStack("Iron Plate"),'S',findItemStack("16 kWh LiFePO4 Cell Stack"),'C',findItemStack("Copper Cable"),'M',findItemStack("Machine Block"));
            } else {
                Object core=switch(t) { case MPPT -> findItemStack("MPPT Solar Controller"); case INVERTER -> findItemStack("Hybrid Solar Inverter"); case COMBINER -> findItemStack("30 A Pole Fuse"); default -> Items.REPEATER; };
                addRecipe(findItemStack(t.getTitle()),t == mods.eln.transparentnode.offgrid.OffGridType.AC_CHARGER ? "IMI" : "IPI","WCW",t == mods.eln.transparentnode.offgrid.OffGridType.AC_CHARGER ? "IPI" : "IMI",'I',findItemStack("Iron Plate"),'P',Items.COMPARATOR,'W',findItemStack("Copper Cable"),'C',core,'M',findItemStack("Machine Block"));
            }
        }
        addRecipe(findItemStack("Pole Fuse Link",3)," C "," P "," C ",'C',findItemStack("Copper Cable"),'P',findItemStack("Iron Plate"));
        var names=mods.eln.utility.PoleHardware.INSTANCE.getFuseNames();
        for(int i=0;i<names.size();i++) {
            var parts=new java.util.ArrayList<Object>();parts.add(findItemStack("Pole Fuse Link"));
            for(int j=0;j<1+i/3;j++)parts.add(Items.COPPER_INGOT);
            for(int j=0;j<i%3;j++)parts.add(Items.IRON_NUGGET);
            addShapelessRecipe(findItemStack(names.get(i)),parts.toArray());
        }
    }

    public static void recipeForkPatchSolar() {
        addRecipe(findItemStack("MPPT Solar Controller"), "IPI", "RCR", "IPI",
            'I', findItemStack("Iron Plate"), 'P', Items.COMPARATOR, 'R', Items.REDSTONE,
            'C', findItemStack("Small Solar Panel"));
        addRecipe(findItemStack("Hybrid Solar Inverter"), "PPP", "MCM", "CRC",
            'P', findItemStack("Iron Plate"), 'M', findItemStack("MPPT Solar Controller"),
            'C', findItemStack("Copper Cable"), 'R', Items.REDSTONE);
        addShapelessRecipe(findItemStack("Wall-mounted MPPT Solar Controller"), findItemStack("MPPT Solar Controller"), findItemStack("Iron Plate"));
        addShapelessRecipe(findItemStack("Wall-mounted Hybrid Solar Inverter"), findItemStack("Hybrid Solar Inverter"), findItemStack("Iron Plate"));
    }

    /** ForkPatch 1.21.1: Graphite Rod (shaped, from 2x/3x/4x Graphite Rods) and 2x/3x/4x Graphite Rods
     * (shapeless from rods; splits: 2x -> 2 rod, 3x -> 3 rod, 4x -> 4 rod, 2x + rod -> 3x,
     * 2x + 2 rod / 2x + 2x / 3x + rod -> 4x). All ported together. Source CraftingRecipes.kt:1719-1763. */
    public static void recipeForkPatchGraphite() {
        addRecipe(findItemStack("Graphite Rod", 2), "I", 'I', findItemStack("2x Graphite Rods"));
        addShapelessRecipe(findItemStack("2x Graphite Rods"), findItemStack("Graphite Rod"), findItemStack("Graphite Rod"));
        addRecipe(findItemStack("Graphite Rod", 3), "I", 'I', findItemStack("3x Graphite Rods"));
        addShapelessRecipe(findItemStack("3x Graphite Rods"), findItemStack("Graphite Rod"), findItemStack("Graphite Rod"), findItemStack("Graphite Rod"));
        addShapelessRecipe(findItemStack("3x Graphite Rods"), findItemStack("2x Graphite Rods"), findItemStack("Graphite Rod"));
        addRecipe(findItemStack("Graphite Rod", 4), "I", 'I', findItemStack("4x Graphite Rods"));
        addShapelessRecipe(findItemStack("4x Graphite Rods"), findItemStack("Graphite Rod"), findItemStack("Graphite Rod"), findItemStack("Graphite Rod"), findItemStack("Graphite Rod"));
        addShapelessRecipe(findItemStack("4x Graphite Rods"), findItemStack("2x Graphite Rods"), findItemStack("Graphite Rod"), findItemStack("Graphite Rod"));
        addShapelessRecipe(findItemStack("4x Graphite Rods"), findItemStack("2x Graphite Rods"), findItemStack("2x Graphite Rods"));
        addShapelessRecipe(findItemStack("4x Graphite Rods"), findItemStack("3x Graphite Rods"), findItemStack("Graphite Rod"));
        // ForkPatch 1.21.1 CraftingRecipes.kt:1762: one Synthetic Diamond shapeless-crafts into two Diamonds.
        addShapelessRecipe(stack(Items.DIAMOND, 2), findItemStack("Synthetic Diamond"));
    }

    /** ForkPatch 1.21.1 CraftingRecipes.kt:1715-1716 (misc): 3x3 Arc Clay Ingot -> Arc Clay Block; block -> 9 ingots.
     * No machine processing of arc clay in source (grep-verified). */
    public static void recipeForkPatchArcClay() {
        addRecipe(stack(mods.eln.registry.ElnRegistry.ARC_CLAY_BLOCK.get()), "III", "III", "III", 'I', findItemStack("Arc Clay Ingot"));
        addShapelessRecipe(findItemStack("Arc Clay Ingot", 9), stack(mods.eln.registry.ElnRegistry.ARC_CLAY_BLOCK.get()));
    }

    /** ForkPatch 1.21.1 CraftingRecipes.kt:1717-1718 (misc): 3x3 Arc Metal Ingot -> Arc Metal Block; block -> 9 ingots.
     * No machine processing of arc metal in source (grep-verified); the T1/T2 transmission cable recipes using
     * ingotSteel are commented out in source. */
    public static void recipeForkPatchArcMetal() {
        addRecipe(stack(mods.eln.registry.ElnRegistry.ARC_METAL_BLOCK.get()), "III", "III", "III", 'I', findItemStack("Arc Metal Ingot"));
        addShapelessRecipe(findItemStack("Arc Metal Ingot", 9), stack(mods.eln.registry.ElnRegistry.ARC_METAL_BLOCK.get()));
    }

    public static void recipeSolarPanel() {
        addRecipe(findItemStack("Small Solar Panel"),
            "III",
            "CSC",
            "III",
            'S', "plateSilicon",
            'I', stack(Items.IRON_INGOT),
            'C', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Small Rotating Solar Panel"),
            "ISI",
            "I I",
            'S', findItemStack("Small Solar Panel"),
            'M', findItemStack("Electrical Motor"),
            'I', stack(Items.IRON_INGOT));

        for (String metal : new String[] { "blockSteel", "blockAluminum", "blockAluminium", "casingMachineAdvanced" }) {
            for (String panel : new String[] {"Small Solar Panel", "Small Rotating Solar Panel"}) {
                addRecipe(findItemStack("2x3 Solar Panel"),
                    "PPP",
                    "PPP",
                    "I I",
                    'P', findItemStack(panel),
                    'I', metal);
            }
        }
        addRecipe(findItemStack("2x3 Rotating Solar Panel"),
            "ISI",
            "IMI",
            "I I",
            'S', findItemStack("2x3 Solar Panel"),
            'M', findItemStack("Electrical Motor"),
            'I', stack(Items.IRON_INGOT));
    }

    public static void recipeThermalDissipatorPassiveAndActive() {
        addRecipe(
            findItemStack("Small Passive Thermal Dissipator"),
            "I I",
            "III",
            "CIC",
            'I', "ingotCopper",
            'C', findItemStack("Copper Thermal Cable"));

	/*	addRecipe(
				findItemStack("Small Active Thermal Dissipator"),
				"RMR",
				"I I",
				"III",
				'I', "ingotCopper",
				'M', findItemStack("Electrical Motor"),
				'R', "itemRubber",
				'C', findItemStack("Copper Thermal Cable"));*/

        addRecipe(
            findItemStack("Small Active Thermal Dissipator"),
            "RMR",
            " D ",
            'D', findItemStack("Small Passive Thermal Dissipator"),
            'M', findItemStack("Electrical Motor"),
            'R', "itemRubber");

	/*	addRecipe(
				findItemStack("230V Active Thermal Dissipator"),
				"RMR",
				"I I",
				"III",
				'I', "ingotCopper",
				'M', findItemStack("Advanced Electrical Motor"),
				'R', "itemRubber",
				'C', findItemStack("Copper Thermal Cable"));*/

        addRecipe(
            findItemStack("230V Active Thermal Dissipator"),
            "RMR",
            " D ",
            'D', findItemStack("Small Passive Thermal Dissipator"),
            'M', findItemStack("Advanced Electrical Motor"),
            'R', "itemRubber");

    }

    public static void recipeGeneral() {
        // the rubber chain (Baughn 2026-11-07): resin smelts to raw rubber, which smelts to 1 Rubber as resin did; mixed
        // with sulfur and the soul dust activator into a compound, it vulcanises to twice that
        addSmelting(treeResin.parentItem,
            treeResin.parentItemDamage, stack(mods.eln.registry.ElnRegistry.RAW_RUBBER.get()), 0f);
        addSmelting(mods.eln.registry.ElnRegistry.RAW_RUBBER.get(), 0, findItemStack("Rubber", 1), 0.1f);
        addShapelessRecipe(stack(mods.eln.registry.ElnRegistry.RUBBER_COMPOUND.get()),
            stack(mods.eln.registry.ElnRegistry.RAW_RUBBER.get()), stack(mods.eln.registry.ElnRegistry.RAW_RUBBER.get()),
            stack(mods.eln.registry.ElnRegistry.RAW_RUBBER.get()), stack(mods.eln.registry.ElnRegistry.RAW_RUBBER.get()),
            stack(mods.eln.registry.ElnRegistry.SULFUR_DUST.get()), stack(mods.eln.registry.ElnRegistry.SOUL_DUST.get()));
        // (the compound vulcanises only in the Electrical Furnace, held at its temperature: ElectricalFurnaceProcess.TREATMENTS)
        // carbon black as a filler (Baughn 2026-11-07, option A): the same compound with a Carbon Black vulcanises to 11;
        // and it is a black dye
        addShapelessRecipe(stack(mods.eln.registry.ElnRegistry.CARBON_BLACK_COMPOUND.get()),
            stack(mods.eln.registry.ElnRegistry.RAW_RUBBER.get()), stack(mods.eln.registry.ElnRegistry.RAW_RUBBER.get()),
            stack(mods.eln.registry.ElnRegistry.RAW_RUBBER.get()), stack(mods.eln.registry.ElnRegistry.RAW_RUBBER.get()),
            stack(mods.eln.registry.ElnRegistry.SULFUR_DUST.get()), stack(mods.eln.registry.ElnRegistry.SOUL_DUST.get()),
            stack(mods.eln.registry.ElnRegistry.CARBON_BLACK.get()));
        addShapelessRecipe(stack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("black_dye"))),
            stack(mods.eln.registry.ElnRegistry.CARBON_BLACK.get()));
        // the Desulfuriser: a heated vessel (an iron tank's worth of iron) on a machine block, a heating element
        addRecipe(findItemStack("Desulfuriser", 1),
            "PCP",
            "PMP",
            " H ",
            'P', findItemStack("Iron Plate"),
            'C', stack(Items.CAULDRON),
            'M', findItemStack("Machine Block"),
            'H', findItemStack("230V Copper Heating Corp"));
        // the Steam Cracker: a firebox (an advanced machine block, alloy) with tungsten heaters (it runs hot), the drum an
        // iron tank
        addRecipe(findItemStack("Steam Cracker", 1),
            "PTP",
            "AMA",
            "HPH",
            'P', findItemStack("Iron Plate"),
            'T', findItemStack("Iron Tank"),
            'A', findItemStack("Alloy Ingot"),
            'M', findItemStack("Advanced Machine Block"),
            'H', findItemStack("230V Tungsten Heating Corp"));
        // the Polymeriser: a stirred vessel (a cauldron) on a machine block, an electrical motor driving its agitator
        addRecipe(findItemStack("Polymeriser", 1),
            "PCP",
            "PMP",
            " E ",
            'P', findItemStack("Iron Plate"),
            'C', stack(Items.CAULDRON),
            'M', findItemStack("Machine Block"),
            'E', findItemStack("Electrical Motor"));
        addRecipe(findItemStack("Carbon Black Burner", 1),
            " P ",
            "BFB",
            "BBB",
            'P', findItemStack("Iron Plate"),
            'B', stack(Items.BRICKS),
            'F', stack(Items.FURNACE));

    }

    public static void recipeHeatingCorp() {
        addRecipe(findItemStack("Small 48V Copper Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', findItemStack("Copper Coil"));

        addRecipe(findItemStack("48V Copper Heating Corp"),
            "CAC",
            "CCC",
            "CAC",
            'A', "itemRubber",
            'C', findItemStack("Copper Coil"));

        addRecipe(findItemStack("Small 230V Copper Heating Corp"),
            "CC",
            'C', findItemStack("48V Copper Heating Corp"));

        addRecipe(findItemStack("230V Copper Heating Corp"),
            "CC",
            'C', findItemStack("Small 230V Copper Heating Corp"));

        addRecipe(findItemStack("Small 48V Iron Heating Corp"),
            "C C",
            "CCC",
            "C C", 'C', findItemStack("Iron Cable"));

        addRecipe(findItemStack("48V Iron Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Small 230V Iron Heating Corp"),
            "CC",
            'C', findItemStack("48V Iron Heating Corp"));

        addRecipe(findItemStack("230V Iron Heating Corp"),
            "CC",
            'C', findItemStack("Small 230V Iron Heating Corp"));

        addRecipe(findItemStack("Small 48V Tungsten Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', findItemStack("Tungsten Cable"));

        addRecipe(findItemStack("48V Tungsten Heating Corp"),
            "C C",
            "CCC",
            "C C",
            'C', findItemStack("Tungsten Ingot"));

        addRecipe(findItemStack("Small 230V Tungsten Heating Corp"),
            "CC",
            'C', findItemStack("48V Tungsten Heating Corp"));
        addRecipe(findItemStack("230V Tungsten Heating Corp"),
            "CC",
            'C', findItemStack("Small 230V Tungsten Heating Corp"));
    }

    public static void recipeRegulatorItem() {
        addRecipe(findItemStack("On/OFF Regulator 10 Percent", 1),
            "R R",
            " R ",
            " I ",
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("On/OFF Regulator 1 Percent", 1),
            "RRR",
            " I ",
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Analogic Regulator", 1),
            "R R",
            " C ",
            " I ",
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT),
            'C', dictCheapChip);
    }

    public static void recipeLampItem() {
        // Tungsten
        addRecipe(
            findItemStack("Small 48V Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', stack(Blocks.GLASS_PANE),
            'F', dictTungstenIngot,
            'S', findItemStack("Copper Cable"));

        addRecipe(findItemStack("48V Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            "AS ",
            'G', stack(Blocks.GLASS_PANE),
            'F', dictTungstenIngot,
            'A', "itemRubber",
            'S', findItemStack("Copper Cable"));

        addRecipe(findItemStack("230V Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            "ASA",
            'G', stack(Blocks.GLASS_PANE),
            'F', dictTungstenIngot,
            'A', "itemRubber",
            'S', findItemStack("Copper Cable"));

        // CARBON
        addRecipe(findItemStack("Small 48V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', stack(Blocks.GLASS_PANE),
            'F', stack(Items.COAL),
            'S', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Small 48V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', stack(Blocks.GLASS_PANE),
            'F', stack(Items.CHARCOAL),
            'S', findItemStack("Copper Cable"));

        addRecipe(
            findItemStack("48V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            "AS ",
            'G', stack(Blocks.GLASS_PANE),
            'F', stack(Items.COAL),
            'A', "itemRubber",
            'S', findItemStack("Copper Cable"));

        addRecipe(findItemStack("48V Carbon Incandescent Light Bulb", 4),
            " G ",
            "GFG",
            "AS ",
            'G', stack(Blocks.GLASS_PANE),
            'F', stack(Items.CHARCOAL),
            'A', "itemRubber",
            'S', findItemStack("Copper Cable"));

        addRecipe(
            findItemStack("Small 48V Economic Light Bulb", 4),
            " G ",
            "GFG",
            " S ",
            'G', stack(Blocks.GLASS_PANE),
            'F', stack(Items.GLOWSTONE_DUST),
            'S', findItemStack("Copper Cable"));

        addRecipe(findItemStack("48V Economic Light Bulb", 4),
            " G ",
            "GFG",
            "AS ",
            'G', stack(Blocks.GLASS_PANE),
            'F', stack(Items.GLOWSTONE_DUST),
            'A', "itemRubber",
            'S', findItemStack("Copper Cable"));

        addRecipe(findItemStack("230V Economic Light Bulb", 4),
            " G ",
            "GFG",
            "ASA",
            'G', stack(Blocks.GLASS_PANE),
            'F', stack(Items.GLOWSTONE_DUST),
            'A', "itemRubber",
            'S', findItemStack("Copper Cable"));

        addRecipe(findItemStack("48V Farming Lamp", 2),
            "GGG",
            "FFF",
            "GSG",
            'G', stack(Blocks.GLASS_PANE),
            'F', dictTungstenIngot,
            'S', findItemStack("Copper Cable"));

        addRecipe(findItemStack("230V Farming Lamp", 2),
            "GGG",
            "FFF",
            "ASA",
            'G', stack(Blocks.GLASS_PANE),
            'F', dictTungstenIngot,
            'A', "itemRubber",
            'S', findItemStack("Copper Cable"));

        addRecipe(findItemStack("48V LED Bulb", 2),
            "GGG",
            "SSS",
            "AC ",
            'G', stack(Blocks.GLASS_PANE),
            'S', findItemStack("Silicon Ingot"),
            'A', "itemRubber",
            'C', findItemStack("Copper Cable"));

        addRecipe(findItemStack("230V LED Bulb", 2),
            "GGG",
            "SSS",
            "ACA",
            'G', stack(Blocks.GLASS_PANE),
            'S', findItemStack("Silicon Ingot"),
            'A', "itemRubber",
            'C', findItemStack("Copper Cable"));

    }

    public static void recipeProtection() {
        addRecipe(findItemStack("Overvoltage Protection", 4),
            "SCD",
            'S', findItemStack("Electrical Probe Chip"),
            'C', dictCheapChip,
            'D', stack(Items.REDSTONE));

        addRecipe(findItemStack("Overheating Protection", 4),
            "SCD",
            'S', findItemStack("Thermal Probe Chip"),
            'C', dictCheapChip,
            'D', stack(Items.REDSTONE));

    }

    public static void recipeCombustionChamber() {
        addRecipe(findItemStack("Combustion Chamber"),
            " L ",
            "L L",
            " L ",
            'L', stack(Blocks.STONE));
        addRecipe(findItemStack("Thermal Insulation", 4),
            "WSW",
            "SWS",
            "WSW",
            'S', stack(Blocks.STONE),
            'W', net.minecraft.tags.ItemTags.WOOL);   // any colour (Baughn 2026-10-26; 1.12: white)
    }

    public static void recipeFerromagneticCore() {
        addRecipe(findItemStack("Cheap Ferromagnetic Core"),
            "LLL",
            "L  ",
            "LLL",
            'L', Items.IRON_INGOT);

        // Baughn 2026-11-06 (after Argon): three separate recipes, the better cores costly. Average: laminated silicon steel,
        // the sheets coated with Dielectric (inorganic: it survives the anneal), annealed in the Electrical Furnace.
        addRecipe(findItemStack("Silicon Steel Laminations"),
            "PSP",
            "PDP",
            "PSP",
            'P', "plateIron",
            'S', findItemStack("Silicon Plate"),
            'D', findItemStack("Dielectric"));
        // Optimal: a nanocrystalline core (iron, silicon, a little copper and tungsten for niobium, an alloy former),
        // field-annealed in the Magnetizer (ElnMachineRecipes.recipeMagnetizer)
        addShapelessRecipe(findItemStack("Nanocrystalline Core Blank"),
            findItemStack("Iron Dust"), findItemStack("Iron Dust"), findItemStack("Iron Dust"), findItemStack("Iron Dust"),
            findItemStack("Silicon Dust"), findItemStack("Silicon Dust"), findItemStack("Copper Dust"),
            findItemStack("Tungsten Dust"), findItemStack("Alloy Plate"));
    }

    public static void recipeIngot() {
        // Done
    }

    public static void recipeDust() {
        addShapelessRecipe(findItemStack("Alloy Dust", 2),
            "dustIron",
            "dustIron",
            "dustCoal",
            dictTungstenDust);

    }

    public static void recipeElectricalMotor() {
        addRecipe(findItemStack("Electrical Motor"),
            " C ",
            "III",
            "C C",
            'I', stack(Items.IRON_INGOT),
            'C', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Advanced Electrical Motor"),
            "RCR",
            "MIM",
            "CRC",
            'M', findItemStack("Advanced Magnet"),
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE),
            'C', findItemStack("Copper Cable"));

        // TODO

    }

    public static void recipeSolarTracker() {
        addRecipe(findItemStack("Solar Tracker", 4),
            "VVV",
            "RQR",
            "III",
            'Q', stack(Items.QUARTZ),
            'V', stack(Blocks.GLASS_PANE),
            'R', stack(Items.REDSTONE),
            'G', stack(Items.GOLD_INGOT),
            'I', stack(Items.IRON_INGOT));

    }

    public static void recipeDynamo() {

    }

    public static void recipeWindRotor() {

    }

    public static void recipeMeter() {
        addRecipe(findItemStack("MultiMeter"),
            "RGR",
            "RER",
            "RCR",
            'G', stack(Blocks.GLASS_PANE),
            'C', findItemStack("Electrical Probe Chip"),
            'E', stack(Items.REDSTONE),
            'R', "itemRubber");

        addRecipe(findItemStack("Thermometer"),
            "RGR",
            "RER",
            "RCR",
            'G', stack(Blocks.GLASS_PANE),
            'C', findItemStack("Thermal Probe Chip"),
            'E', stack(Items.REDSTONE),
            'R', "itemRubber");

        addShapelessRecipe(findItemStack("AllMeter"),
            findItemStack("MultiMeter"),
            findItemStack("Thermometer"));

        addRecipe(findItemStack("Wireless Analyser"),
            " S ",
            "RGR",
            "RER",
            'G', stack(Blocks.GLASS_PANE),
            'S', findItemStack("Signal Antenna"),
            'E', stack(Items.REDSTONE),
            'R', "itemRubber");

    }

    public static void recipeElectricalDrill() {
        addRecipe(findItemStack("Cheap Electrical Drill"),
            "CMC",
            " T ",
            " P ",
            'T', findItemStack("Mining Pipe"),
            'C', dictCheapChip,
            'M', findItemStack("Electrical Motor"),
            'P', stack(Items.IRON_PICKAXE));

        addRecipe(findItemStack("Average Electrical Drill"),
            "RCR",
            " D ",
            " d ", 'R', Items.REDSTONE,
            'C', dictCheapChip,
            'D', findItemStack("Cheap Electrical Drill"),
            'd', stack(Items.DIAMOND));

        addRecipe(findItemStack("Fast Electrical Drill"),
            "MCM",
            " T ",
            " P ",
            'T', findItemStack("Mining Pipe"),
            'C', dictAdvancedChip,
            'M', findItemStack("Advanced Electrical Motor"),
            'P', stack(Items.DIAMOND_PICKAXE));

    }

    public static void recipeOreScanner() {
        addRecipe(findItemStack("Ore Scanner"),
            "IGI",
            "RCR",
            "IGI",
            'C', dictCheapChip,
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT),
            'G', stack(Items.GOLD_INGOT));

    }

    public static void recipeMiningPipe() {
        addRecipe(findItemStack("Mining Pipe", 4),
            "A",
            "A",
            "A",
            'A', "ingotAlloy");
    }

    public static void recipeTreeResinAndRubber() {
        // rubber trees (Baughn 2026-11-07): a sapling from four others, one of them jungle; the pale wood planks as birch
        addShapelessRecipe(stack(mods.eln.rubber.RubberTrees.SAPLING_ITEM.get()),
            stack(Items.JUNGLE_SAPLING), stack(Items.OAK_SAPLING), stack(Items.BIRCH_SAPLING), stack(Items.ACACIA_SAPLING));
        addShapelessRecipe(stack(Items.BIRCH_PLANKS, 4), stack(mods.eln.rubber.RubberTrees.LOG_ITEM.get()));
        // the Industrial Resin Collector (2026-11-07): a hand collector on a machine block, a motor to tap, iron plates
        addRecipe(findItemStack("Industrial Resin Collector", 1),
            " T ",
            "PMP",
            " E ",
            'T', findItemStack("Tree Resin Collector"),
            'P', findItemStack("Iron Plate"),
            'M', findItemStack("Machine Block"),
            'E', findItemStack("Electrical Motor"));
        addShapelessRecipe(stack(Items.BIRCH_PLANKS, 4), stack(mods.eln.rubber.RubberTrees.WOOD_ITEM.get()));
        addRecipe(stack(mods.eln.rubber.RubberTrees.WOOD_ITEM.get(), 3), "LL", "LL", 'L', stack(mods.eln.rubber.RubberTrees.LOG_ITEM.get()));

        addRecipe(findItemStack("Tree Resin Collector"),
            "W W",
            "WW ", 'W', "plankWood");

        addRecipe(findItemStack("Tree Resin Collector"),
            "W W",
            " WW", 'W', "plankWood");

    }

    public static void recipeRawCable() {
        // 26.x (Baughn): copper cable is 1 per ingot by hand; the thin Copper Coil too, wound on a stick
        addRecipe(findItemStack("Copper Cable", 3),
            "III",
            'I', "ingotCopper");

        addRecipe(findItemStack("Copper Coil", 4),
            " I ",
            "ISI",
            " I ",
            'I', "ingotCopper",
            'S', stack(Items.STICK));

        addRecipe(findItemStack("Iron Cable", 6),
            "III",
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Tungsten Cable", 6),
            "III",
            'I', dictTungstenIngot);

    }

    public static void recipeBatteryItem() {
        addRecipe(findItemStack("Portable Battery"),
            " I ",
            "IPI",
            "IPI",
            'P', "ingotLead",
            'I', stack(Items.IRON_INGOT));
        // (the Portable Battery Pack: BatteryPackRecipe, which keeps the batteries' charge; its JSON is in main resources)
    }

    public static void recipeElectricalTool() {
        addRecipe(findItemStack("Small Flashlight"),
            "GLG",
            "IBI",
            " I ",
            'L', findItemStack("48V Incandescent Light Bulb"),
            'B', findItemStack("Portable Battery"),
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("Improved Flashlight"),
            "GLG",
            "IBI",
            " I ",
            'L', findItemStack("48V LED Bulb"),
            'B', findItemStack("Portable Battery Pack"),
            'G', stack(Blocks.GLASS_PANE),
            'I', stack(Items.IRON_INGOT));

        // the headlamp (Baughn 2026-10-30): a small flashlight on a leather band
        addShapelessRecipe(stack(mods.eln.registry.ElnRegistry.HEADLAMP.get()),
            findItemStack("Small Flashlight"), stack(Items.LEATHER), stack(Items.LEATHER), stack(Items.STRING));

        addRecipe(findItemStack("Portable Electrical Mining Drill"),
            " T ",
            "IBI",
            " I ",
            'T', findItemStack("Average Electrical Drill"),
            'B', findItemStack("Portable Battery"),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Portable Electrical Axe"),
            " T ",
            "IMI",
            "IBI",
            'T', stack(Items.IRON_AXE),
            'B', findItemStack("Portable Battery"),
            'M', findItemStack("Electrical Motor"),
            'I', stack(Items.IRON_INGOT));

        if (xRayScannerCanBeCrafted) {
            addRecipe(findItemStack("X-Ray Scanner"),
                "PGP",
                "PCP",
                "PBP",
                'C', dictAdvancedChip,
                'B', findItemStack("Portable Battery"),
                'P', stack(Items.IRON_INGOT),
                'G', findItemStack("Ore Scanner"));
        }

    }

    public static void recipeECoal() {
        addRecipe(findItemStack("E-Coal Helmet"),
            "PPP",
            "PCP",
            'P', "plateCoal",
            'C', dictAdvancedChip);
        addRecipe(findItemStack("E-Coal Boots"),
            " C ",
            "P P",
            "P P",
            'P', "plateCoal",
            'C', dictAdvancedChip);

        addRecipe(findItemStack("E-Coal Chestplate"),
            "P P",
            "PCP",
            "PPP",
            'P', "plateCoal",
            'C', dictAdvancedChip);

        addRecipe(findItemStack("E-Coal Leggings"),
            "PPP",
            "PCP",
            "P P",
            'P', "plateCoal",
            'C', dictAdvancedChip);

    }

    public static void recipePortableCapacitor() {
        addRecipe(findItemStack("Portable Condensator"),
            "RcR",
            "wCw",
            "RcR",
            'C', stack(Items.REDSTONE),
            'R', "itemRubber",
            'w', findItemStack("Copper Cable"),
            'c', "plateCopper");

        addShapelessRecipe(findItemStack("Portable Condensator Pack"),
            findItemStack("Portable Condensator"),
            findItemStack("Portable Condensator"),
            findItemStack("Portable Condensator"));
    }

    public static void recipeMiscItem() {
        addRecipe(findItemStack("Cheap Chip"),
            " R ",
            "RSR",
            " R ",
            'S', "ingotSilicon",
            'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("Advanced Chip"),
            "LRL",
            "RCR",
            "LRL",
            'C', dictCheapChip,
            'L', "ingotSilicon",
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("Machine Block"),
            "LLL",
            "LcL",
            "LLL",
            'L', stack(Items.IRON_INGOT),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Advanced Machine Block"),
            " C ",
            "CcC",
            " C ",
            'C', "plateAlloy",
            'L', "ingotAlloy",
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("Electrical Probe Chip"),
            " R ",
            "RCR",
            " R ",
            'C', findItemStack("Copper Cable"),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("Thermal Probe Chip"),
            " C ",
            "RIR",
            " C ",
            'G', stack(Items.GOLD_INGOT),
            'I', stack(Items.IRON_INGOT),
            'C', "ingotCopper",
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("Signal Antenna"),
            "c",
            "c",
            'c', findItemStack("Iron Cable"));

        addRecipe(findItemStack("Machine Booster"),
            "m",
            "c",
            "m",
            'm', findItemStack("Electrical Motor"),
            'c', dictAdvancedChip);

        addRecipe(findItemStack("Wrench"),
            " c ",
            "cc ",
            "  c",
            'c', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Player Filter"),
            " g",
            "gc",
            " g",
            'g', stack(Blocks.GLASS_PANE),
            'c', dye(2));

        addRecipe(findItemStack("Monster Filter"),
            " g",
            "gc",
            " g",
            'g', stack(Blocks.GLASS_PANE),
            'c', dye(1));

        // (1.12 registered the Animal Filter without a recipe; AGE's: blue dye, as the others' colours)
        addRecipe(findItemStack("Animal Filter"),
            " g",
            "gc",
            " g",
            'g', stack(Blocks.GLASS_PANE),
            'c', dye(4));

        addRecipe(findItemStack("Casing", 8),
            "ppp",
            "p p",
            "ppp",
            'p', "plateIron");

        // ForkPatch 1.21.1: clutch pin (source data/eln/recipe/clutch_pin.json: 2x steel ingot -> 4)
        addRecipe(findItemStack("Clutch Pin", 4),
            "s",
            "s",
            's', "ingotSteel");

        // ForkPatch 1.21.1: iron/gold/copper/lead/coal clutch plates (tungsten dust ring + plate centre)
        addRecipe(findItemStack("Iron Clutch Plate"),
            " t ",
            "tIt",
            " t ",
            'I', "plateIron",
            't', "dustElnTungsten");

        addRecipe(findItemStack("Gold Clutch Plate"),
            " t ",
            "tGt",
            " t ",
            'G', "plateGold",
            't', "dustElnTungsten");

        addRecipe(findItemStack("Copper Clutch Plate"),
            " t ",
            "tCt",
            " t ",
            'C', "plateCopper",
            't', "dustElnTungsten");

        addRecipe(findItemStack("Lead Clutch Plate"),
            " t ",
            "tLt",
            " t ",
            'L', "plateLead",
            't', "dustElnTungsten");

        addRecipe(findItemStack("Coal Clutch Plate"),
            " t ",
            "tCt",
            " t ",
            'C', "plateCoal",
            't', "dustElnTungsten");

        // ForkPatch 1.21.1: motor cowling + heavy winding (source data/eln/recipe/{motor_cowling,heavy_winding}.json)
        // S -> "ingotSteel" resolves to tag c:ingots/steel and T -> "itemRubber" to tag c:rubber,
        // matching the source's generated recipe JSON exactly.
        addRecipe(findItemStack("Motor Cowling"),
            "ISI",
            "MAM",
            "ISI",
            'I', findItemStack("Iron Plate"),
            'S', "ingotSteel",
            'M', findItemStack("Machine Block"),
            'A', findItemStack("Advanced Machine Block"));
        addRecipe(findItemStack("Heavy Winding"),
            "cRc",
            "RTR",
            "cRc",
            'c', findItemStack("Copper Cable"),
            'R', findItemStack("Tree Resin"),
            'T', "itemRubber");

        // ForkPatch 1.21.1: turbine blades (source CraftingRecipes.kt recipeTurbineBlades): all four tiers ported
        // (iron + steel 2026-11-18, alloy + tungsten 2026-11-19); the turbine consumers are out of scope.
        // Tungsten uses the config dictionary default "ingotElnTungsten" (source
        // runtime.dictionary.tungstenIngot default) mapped to eln:ingots/tungsten by OreDictTags.
        addRecipe(findItemStack("Iron Turbine Blade"),
            " I ",
            "IMI",
            " I ",
            'I', "ingotIron",
            'M', findItemStack("Machine Block"));
        addRecipe(findItemStack("Steel Turbine Blade"),
            "SIS",
            "SMS",
            "SIS",
            'S', firstExistingOre("ingotSteel", "ingotIron"),
            'I', "ingotIron",
            'M', findItemStack("Machine Block"));
        addRecipe(findItemStack("Alloy Turbine Blade"),
            "AIA", "ACA", "AIA",
            'A', "ingotAlloy",
            'I', "ingotIron",
            'C', findItemStack("Advanced Machine Block"));
        addRecipe(findItemStack("Tungsten Turbine Blade"),
            "TAT", "ACA", "TAT",
            'T', "ingotElnTungsten",
            'A', "ingotAlloy",
            'C', findItemStack("Advanced Machine Block"));

    }













    /** Fuel burners from alloy plates, not stone combustion chambers (Baughn 2026-11-11). */
    public static void recipeFuelBurnerItem() {
        addRecipe(findItemStack("Small Fuel Burner"),
            "   ",
            " Pc",
            "   ",
            'P', findItemStack("Alloy Plate"),
            'c', findItemStack("Copper Thermal Cable"));

        addRecipe(findItemStack("Medium Fuel Burner"),
            "   ",
            " Pc",
            " P ",
            'P', findItemStack("Alloy Plate"),
            'c', findItemStack("Copper Thermal Cable"));

        addRecipe(findItemStack("Big Fuel Burner"),
            "   ",
            "PPc",
            "PP ",
            'P', findItemStack("Alloy Plate"),
            'c', findItemStack("Copper Thermal Cable"));
    }

    public static void recipeFurnace() {
        RStack in;

        in = findItemStack("dustCopper");
        addSmelting(in.getItem(), 0,
            stack(Items.COPPER_INGOT)); // 26.x: EA's copper ingot is gone
        in = findItemStack("Lead Ore");
        addSmelting(in.getItem(), 0,
            findItemStack("ingotLead"));
        in = findItemStack("Deepslate Lead Ore");
        addSmelting(in.getItem(), 0,
            findItemStack("ingotLead"));
        in = findItemStack("dustLead");
        addSmelting(in.getItem(), 0,
            findItemStack("ingotLead"));
        in = findItemStack("Tungsten Ore");
        addSmelting(in.getItem(), 0,
            findItemStack("Tungsten Ingot"));
        in = findItemStack("Tungsten Dust");
        addSmelting(in.getItem(), 0,
            findItemStack("Tungsten Ingot"));
        // raw lead and tungsten (their ores' drops, as raw iron), smelted and blasted
        // Field Bus Cable: signal cable in violet jackets, gold on the contacts (mods.eln.bus)
        addShapelessRecipe(stack(mods.eln.registry.ElnRegistry.FIELD_BUS_CABLE.get(), 4),
            findItemStack("Signal Cable"), findItemStack("Signal Cable"), findItemStack("Signal Cable"), findItemStack("Signal Cable"),
            stack(net.minecraft.world.item.Items.DYE.purple()), stack(net.minecraft.world.item.Items.GOLD_NUGGET));
        addSmelting(mods.eln.registry.ElnRegistry.RAW_LEAD.get(), 0, findItemStack("ingotLead"));
        addSmelting(mods.eln.registry.ElnRegistry.RAW_TUNGSTEN.get(), 0, findItemStack("Tungsten Ingot"));
        addBlasting(mods.eln.registry.ElnRegistry.RAW_LEAD.get(), findItemStack("ingotLead"), 0.7f);
        addBlasting(mods.eln.registry.ElnRegistry.RAW_TUNGSTEN.get(), findItemStack("Tungsten Ingot"), 0.7f);
        // charcoal nuggets (an electrical oven's burnt food): four to a charcoal, and back
        var nugget = mods.eln.registry.ElnRegistry.CHARCOAL_NUGGET.get();
        addShapelessRecipe(stack(net.minecraft.world.item.Items.CHARCOAL), stack(nugget), stack(nugget), stack(nugget), stack(nugget));
        addShapelessRecipe(stack(nugget, 4), stack(net.minecraft.world.item.Items.CHARCOAL));
        in = findItemStack("ingotAlloy");
        // addSmelting(in.getItem().itemID, 0,
        // findItemStack("Ferrite Ingot"));
        in = findItemStack("dustIron");
        addSmelting(in.getItem(), 0,
            stack(Items.IRON_INGOT));

        in = findItemStack("dustGold");
        addSmelting(in.getItem(), 0,
            stack(Items.GOLD_INGOT));

        in = findItemStack("Tree Resin");
        addSmelting(in.getItem(), 0,
            findItemStack("Rubber", 2));

        in = findItemStack("Alloy Dust");
        addSmelting(in.getItem(), 0,
            findItemStack("Alloy Ingot"));

        in = findItemStack("Silicon Dust");
        addSmelting(in.getItem(), 0,
            findItemStack("Silicon Ingot"));

        // in = findItemStack("Purified Cinnabar Dust");
        in = findItemStack("dustCinnabar");
        addSmelting(in.getItem(), 0,
            findItemStack("Mercury"));

    }

    public static void recipeElectricalSensor() {
        addRecipe(findItemStack("Voltage Probe", 1),
            "SC",
            'S', findItemStack("Electrical Probe Chip"),
            'C', findItemStack("Signal Cable"));

        addRecipe(findItemStack("Electrical Probe", 1),
            "SCS",
            'S', findItemStack("Electrical Probe Chip"),
            'C', findItemStack("Signal Cable"));

    }

    public static void recipeThermalSensor() {
        addRecipe(findItemStack("Thermal Probe", 1),
            "SCS",
            'S', findItemStack("Thermal Probe Chip"),
            'C', findItemStack("Signal Cable"));

        addRecipe(findItemStack("Temperature Probe", 1),
            "SC",
            'S', findItemStack("Thermal Probe Chip"),
            'C', findItemStack("Signal Cable"));

    }

    public static void recipeTransporter() {
        addRecipe(findItemStack("Experimental Transporter", 1),
            "RMR",
            "RMR",
            " R ",
            'M', findItemStack("Advanced Machine Block"),
            'C', findItemStack("Copper Cable"),
            'R', dictAdvancedChip);
    }


    public static void recipeTurret() {
        addRecipe(findItemStack("400V Defence Turret", 1),
            " R ",
            "CMC",
            " c ",
            'M', findItemStack("Advanced Machine Block"),
            'C', dictAdvancedChip,
            'c', findItemStack("Copper Cable"),
            'R', stack(Blocks.REDSTONE_BLOCK));

    }

    public static void recipeMachine() {
        addRecipe(findItemStack("48V Macerator", 1),
            "IRI",
            "FMF",
            "IcI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'F', stack(Items.FLINT),
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("230V Macerator", 1),
            "ICI",
            "DMD",
            "IcI",
            'M', findItemStack("Advanced Machine Block"),
            'C', dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'D', stack(Items.DIAMOND),
            'I', "ingotAlloy");

        // 26.x oil: tar sand separators (hot-water extraction: a heated, stirred tank)
        addRecipe(findItemStack("48V Tar Sand Separator", 1),
            "IBI",
            "HMH",
            "IcI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'H', findItemStack("48V Copper Heating Corp"),
            'B', stack(Items.BUCKET),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("230V Tar Sand Separator", 1),
            "IBI",
            "HMH",
            "IcI",
            'M', findItemStack("Advanced Machine Block"),
            'c', findItemStack("Advanced Electrical Motor"),
            'H', findItemStack("230V Copper Heating Corp"),
            'B', stack(Items.BUCKET),
            'I', "ingotAlloy");

        addRecipe(stack(mods.eln.oil.OilWorldgen.WELL_CASING_ITEM.get(), 4),
            "I I",
            "I I",
            "I I",
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("48V Oil Pump", 1),
            "IPI",
            "cMc",
            "IWI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'P', stack(Blocks.PISTON),
            'W', stack(mods.eln.oil.OilWorldgen.WELL_CASING_ITEM.get()),
            'I', stack(Items.IRON_INGOT));
        // the steam injection wellhead (todo #138): an alloy pipe in, a valve, well casing down
        addRecipe(findItemStack("Steam Injection Wellhead", 1),
            " p ",
            "IvI",
            "IWI",
            'p', stack(mods.eln.registry.ElnRegistry.PIPE_KIT_ALLOY.get()),
            'v', stack(mods.eln.registry.ElnRegistry.PIPE_VALVE.get()),
            'W', stack(mods.eln.oil.OilWorldgen.WELL_CASING_ITEM.get()),
            'I', "ingotAlloy");
        // the submersible pumps (Baughn 2026-10-25): a motor in a can, an intake screen, a pipe out of the top
        addRecipe(findItemStack("48V Submersible Pump", 1),
            " p ",
            "IcI",
            "IBI",
            'p', stack(mods.eln.registry.ElnRegistry.PIPE_KIT_IRON.get()),
            'c', findItemStack("Electrical Motor"),
            'B', stack(Items.IRON_BARS),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("230V Submersible Pump", 1),
            " p ",
            "IcI",
            "IBI",
            'p', stack(mods.eln.registry.ElnRegistry.PIPE_KIT_IRON.get()),
            'c', findItemStack("Advanced Electrical Motor"),
            'B', stack(Items.IRON_BARS),
            'I', "ingotAlloy");
        addRecipe(findItemStack("230V Oil Pump", 1),
            "IPI",
            "cMc",
            "IWI",
            'M', findItemStack("Advanced Machine Block"),
            'c', findItemStack("Advanced Electrical Motor"),
            'P', stack(Blocks.PISTON),
            'W', stack(mods.eln.oil.OilWorldgen.WELL_CASING_ITEM.get()),
            'I', "ingotAlloy");

        // electric heaters: heating corps around a thermal cable out of the back; the boiler: a kettle on a thermal face
        addRecipe(findItemStack("48V Electric Heater", 1),
            "IHI",
            "HCH",
            "ItI",
            'H', findItemStack("48V Copper Heating Corp"),
            'C', findItemStack("Machine Block"),
            't', findItemStack("Copper Thermal Cable"),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("230V Electric Heater", 1),
            "IHI",
            "HCH",
            "ItI",
            'H', findItemStack("230V Copper Heating Corp"),
            'C', findItemStack("Advanced Machine Block"),
            't', findItemStack("Copper Thermal Cable"),
            'I', "ingotAlloy");
        addRecipe(findItemStack("48V Steam Turbine", 1),
            " m ",
            "IMI",
            "AE ",
            'M', findItemStack("Machine Block"),
            'A', "itemRubber",
            'E', findItemStack("Copper Cable"),
            'I', stack(Items.IRON_INGOT),
            'm', findItemStack("Electrical Motor"));
        addRecipe(findItemStack("230V Steam Turbine", 1),
            "ImI",
            "IMI",
            "IEI",
            'I', "itemRubber",
            'M', findItemStack("Advanced Machine Block"),
            'E', findItemStack("Copper Cable"),
            'm', findItemStack("Advanced Electrical Motor"));
        // the bootstrap turbine (Baughn 2026-10-27): cheaper than the 50V Steam Turbine and no boiler to buy; a bucket for
        // its tank, copper plates for its condenser
        addRecipe(findItemStack("48V Compact Steam Turbine", 1),
            "CBC",
            "ImI",
            "IEI",
            'C', "plateCopper",
            'B', stack(Items.BUCKET),
            'I', stack(Items.IRON_INGOT),
            'm', findItemStack("Electrical Motor"),
            'E', findItemStack("Copper Cable"));
        // the electrical anvil (Baughn 2026-10-28: late-midgame gear): an anvil fed by 800 V cable on an advanced machine
        // block, a steel-plate top and two legs
        addRecipe(findItemStack("Electrical Anvil", 1),
            "HAH",
            "pMp",
            "i i",
            'H', findItemStack("Copper Cable"),
            'A', stack(Items.ANVIL),
            'p', "plateIron",
            'M', findItemStack("Advanced Machine Block"),
            'i', "ingotIron");
        // the capacitor bank (Baughn 2026-10-28: lots of dielectrics): six cells, each six Dielectrics round a block of
        // redstone between copper foils; the bank is six cells over an iron base with its 800 V lead
        addRecipe(stack(mods.eln.registry.ElnRegistry.CAPACITOR_CELL.get()),
            "dpd",
            "dRd",
            "dpd",
            'd', findItemStack("Dielectric"),
            'p', "plateCopper",
            'R', stack(Items.REDSTONE_BLOCK));
        addRecipe(findItemStack("Capacitor Bank", 1),
            "CCC",
            "CCC",
            "pHp",
            'C', stack(mods.eln.registry.ElnRegistry.CAPACITOR_CELL.get()),
            'p', "plateIron",
            'H', findItemStack("Copper Cable"));
        // the electric bike (Baughn 2026-10-28): a chest for its trunk and a leather saddle, an iron frame round an electrical
        // motor, two rubber wheels
        addRecipe(stack(mods.eln.registry.ElnRegistry.ELECTRIC_BIKE.get()),
            "CL ",
            "ImI",
            "R R",
            'C', stack(Items.CHEST),
            'L', stack(Items.LEATHER),
            'I', stack(Items.IRON_INGOT),
            'm', findItemStack("Electrical Motor"),
            'R', "itemRubber");
        // the servo layer (Baughn 2026-11-02: 200 V only): the Linear Servo Drive is an electrical motor in a machine block,
        // fed by a 200 V cable, with its signal wiring; rails are iron posts (station markers: no longer made, 2026-11-05)
        addRecipe(findItemStack("Linear Servo Drive", 1),
            "pmp",
            "SBS",
            "pVp",
            'p', "plateIron",
            'm', findItemStack("Electrical Motor"),
            'S', findItemStack("Signal Cable"),
            'B', findItemStack("Machine Block"),
            'V', findItemStack("Copper Cable"));
        addRecipe(stack(mods.eln.robotics.Robotics.SERVO_RAIL_ITEM.get(), 8),
            "IpI",
            "IpI",
            "IpI",
            'I', stack(Items.IRON_INGOT),
            'p', "plateIron");
        // the terrain bike (Baughn 2026-10-29: much dearer): an electric bike rebuilt round a 200 V motor in an advanced
        // machine block, alloy plates for its frame, more rubber for its tyres
        addRecipe(stack(mods.eln.registry.ElnRegistry.TERRAIN_BIKE.get()),
            "PmP",
            "RBR",
            " M ",
            'P', "plateAlloy",
            'm', findItemStack("Advanced Electrical Motor"),
            'R', "itemRubber",
            'B', stack(mods.eln.registry.ElnRegistry.ELECTRIC_BIKE.get()),
            'M', findItemStack("Advanced Machine Block"));
        // the turbines' condensers (Baughn 2026-10-26): a shell of copper tubes (the cooler) round a vessel, and a pump
        addRecipe(findItemStack("Small Steam Condenser", 1),
            "PCP",
            "CMC",
            "PmP",
            'P', "plateIron",
            'C', "plateCopper",
            'M', findItemStack("Machine Block"),
            'm', findItemStack("Electrical Motor"));
        addRecipe(findItemStack("Steam Condenser", 1),
            "PCP",
            "CMC",
            "PmP",
            'P', "plateAlloy",
            'C', "plateCopper",
            'M', findItemStack("Advanced Machine Block"),
            'm', findItemStack("Advanced Electrical Motor"));
        // the steam oven (Baughn 2026-10-23): an iron box with a glass door, a pipe in on top and a fan
        addRecipe(findItemStack("Steam Oven", 1),
            "IpI",
            "GMG",
            "IEI",
            'I', stack(Items.IRON_INGOT),
            'p', stack(mods.eln.registry.ElnRegistry.PIPE_KIT_IRON.get()),
            'G', stack(Items.GLASS_PANE),
            'M', findItemStack("Machine Block"),
            'E', findItemStack("Electrical Motor"));
        // the smelter (Baughn 2026-10-23): a furnace in a brick jacket on a thermal cable, with a hopper's funnel on top
        addRecipe(findItemStack("Smelter", 1),
            "BHB",
            "BFB",
            "BtB",
            'B', stack(Items.BRICKS),
            'H', stack(Items.HOPPER),
            'F', stack(Items.FURNACE),
            't', findItemStack("Copper Thermal Cable"));
        addRecipe(findItemStack("Fractionating Column Base", 1),
            "IBI",
            "ICI",
            "ItI",
            'B', stack(Items.BUCKET),
            'C', findItemStack("Machine Block"),
            't', findItemStack("Copper Thermal Cable"),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("Fractionating Column Segment", 2),
            "IGI",
            "B B",
            "IGI",
            'B', stack(Items.BUCKET),
            'G', stack(Blocks.GLASS),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("Flare Stack", 1),
            "IFI",
            "I I",
            "IBI",
            'F', stack(Items.FLINT_AND_STEEL),
            'B', stack(Items.BUCKET),
            'I', stack(Items.IRON_INGOT));
        addRecipe(findItemStack("Boiler", 1),
            "IBI",
            "ICI",
            "ItI",
            'B', stack(Items.BUCKET),
            'C', stack(Items.CAULDRON),
            't', findItemStack("Copper Thermal Cable"),
            'I', stack(Items.IRON_INGOT));
        // PLACEHOLDER (Baughn 2026-11-15: a somewhat expensive alloy-plate device): six Alloy Plates round two Boilers
        addRecipe(findItemStack("Large Boiler", 1),
            "PBP",
            "PtP",
            "PBP",
            'B', findItemStack("Boiler"),
            't', findItemStack("Copper Thermal Cable"),
            'P', findItemStack("Alloy Plate"));

        addRecipe(findItemStack("48V Compressor", 1),
            "IRI",
            "FMF",
            "IcI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'F', "plateIron",
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("230V Compressor", 1),
            "ICI",
            "DMD",
            "IcI",
            'M', findItemStack("Advanced Machine Block"),
            'C', dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'D', "plateAlloy",
            'I', "ingotAlloy");

        addRecipe(findItemStack("48V Plate Machine", 1),
            "IRI",
            "IMI",
            "IcI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("230V Plate Machine", 1),
            "DCD",
            "DMD",
            "DcD",
            'M', findItemStack("Advanced Machine Block"),
            'C', dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'D', "plateAlloy",
            'I', "ingotAlloy");

        // the wire-drawing machines (Baughn 2026-11-06, Wires stage 4): a diamond drawing die between two motors
        addRecipe(findItemStack("48V Wire Drawing Machine", 1),
            "IdI",
            "cMc",
            "IRI",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'd', stack(Items.DIAMOND),
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("230V Wire Drawing Machine", 1),
            "DdD",
            "cMc",
            "DCD",
            'M', findItemStack("Advanced Machine Block"),
            'C', dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'd', stack(Items.DIAMOND),
            'D', "plateAlloy");

        addRecipe(findItemStack("48V Magnetizer", 1),
            "IRI",
            "cMc",
            "III",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Electrical Motor"),
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("230V Magnetizer", 1),
            "ICI",
            "cMc",
            "III",
            'M', findItemStack("Advanced Machine Block"),
            'C', dictAdvancedChip,
            'c', findItemStack("Advanced Electrical Motor"),
            'I', "ingotAlloy");

    }

    public static void recipeElectricalGate() {
        addShapelessRecipe(findItemStack("Electrical Timer"),
            stack(Items.REPEATER),
            dictCheapChip);

        addRecipe(findItemStack("Signal Processor", 1),
            "IcI",
            "cCc",
            "IcI",
            'I', stack(Items.IRON_INGOT),
            'c', findItemStack("Signal Cable"),
            'C', dictCheapChip);

        // the field bus's bridge to signal wires: a signal processor's parts, the bus cable on top
        addRecipe(findItemStack("Bus I/O Module", 1),
            "IbI",
            "cCc",
            "IcI",
            'I', stack(Items.IRON_INGOT),
            'b', stack(mods.eln.registry.ElnRegistry.FIELD_BUS_CABLE.get()),
            'c', findItemStack("Signal Cable"),
            'C', dictCheapChip);

        // RS485 devices with an address: the plain part, a chip and the bus cable (Baughn 2026-11-05)
        addShapelessRecipe(findItemStack("Addressable Button", 1),
            findItemStack("Signal Button"), dictCheapChip, stack(mods.eln.registry.ElnRegistry.FIELD_BUS_CABLE.get()));
        addShapelessRecipe(findItemStack("Addressable Light", 1),
            findItemStack("LED vuMeter"), dictCheapChip, stack(mods.eln.registry.ElnRegistry.FIELD_BUS_CABLE.get()));
        addShapelessRecipe(findItemStack("Floor Sensor", 1),
            findItemStack("Electrical Entity Sensor"), dictCheapChip, stack(mods.eln.registry.ElnRegistry.FIELD_BUS_CABLE.get()));

        // the computer (robotics phase 3, Baughn 2026-11-05): two advanced chips in an advanced machine block, its bus
        // and 50 V; chips from silicon and EA's chips (Janet's ROM: a book's worth of language); floppies from paper
        // in an iron sleeve; the Elevator floppy: a floppy and a servo rail
        addRecipe(findItemStack("Computer", 1),
            "pAp",
            "bBL",
            "pAp",
            'p', "plateIron",
            'A', dictAdvancedChip,
            'b', stack(mods.eln.registry.ElnRegistry.FIELD_BUS_CABLE.get()),
            'B', findItemStack("Advanced Machine Block"),
            'L', findItemStack("Copper Cable"));
        addShapelessRecipe(stack(mods.eln.robotics.Robotics.RAM_CHIP.get(), 1),
            dictCheapChip, dictCheapChip, dictCheapChip, findItemStack("Silicon Ingot"), stack(Items.GOLD_NUGGET));
        addShapelessRecipe(stack(mods.eln.robotics.Robotics.JANET_ROM.get(), 1),
            dictAdvancedChip, findItemStack("Silicon Ingot"), stack(Items.BOOK));
        addShapelessRecipe(stack(mods.eln.robotics.Robotics.BOOT_ROM.get(), 1),
            dictCheapChip, findItemStack("Silicon Ingot"), stack(Items.PAPER));
        addShapelessRecipe(stack(mods.eln.robotics.Robotics.FLOPPY_DISK.get(), 1),
            stack(Items.PAPER), stack(Items.IRON_NUGGET), stack(Items.IRON_NUGGET), "itemRubber");
        addShapelessRecipe(stack(mods.eln.robotics.Robotics.ELEVATOR_DISK.get(), 1),
            stack(mods.eln.robotics.Robotics.FLOPPY_DISK.get()), stack(mods.eln.robotics.Robotics.SERVO_RAIL_ITEM.get()));
        addShapelessRecipe(stack(mods.eln.robotics.Robotics.APPLICATIONS_DISK.get(), 1),
            stack(mods.eln.robotics.Robotics.FLOPPY_DISK.get()), stack(Items.BOOK));
    }

    public static void recipeElectricalRedstone() {
        addRecipe(findItemStack("Redstone-to-Voltage Converter", 1),
            "TCS",
            'S', findItemStack("Signal Cable"),
            'C', dictCheapChip,
            'T', stack(Blocks.REDSTONE_TORCH));

        addRecipe(findItemStack("Voltage-to-Redstone Converter", 1),
            "CTR",
            'R', stack(Items.REDSTONE),
            'C', dictCheapChip,
            'T', stack(Blocks.REDSTONE_TORCH));

    }

    public static void recipeElectricalEnvironmentalSensor() {
        addShapelessRecipe(findItemStack("Electrical Daylight Sensor"),
            stack(Blocks.DAYLIGHT_DETECTOR),
            findItemStack("Redstone-to-Voltage Converter"));

        addShapelessRecipe(findItemStack("Electrical Light Sensor"),
            stack(Blocks.DAYLIGHT_DETECTOR),
            stack(Items.QUARTZ),
            findItemStack("Redstone-to-Voltage Converter"));

        addRecipe(findItemStack("Electrical Weather Sensor"),
            " r ",
            "rRr",
            " r ",
            'R', stack(Items.REDSTONE),
            'r', "itemRubber");

        addRecipe(findItemStack("Electrical Anemometer Sensor"),
            " I ",
            " R ",
            "I I",
            'R', stack(Items.REDSTONE),
            'I', stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Electrical Entity Sensor"),
            " G ",
            "GRG",
            " G ",
            'G', stack(Blocks.GLASS_PANE),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("Electrical Fire Detector"),
            "cbr",
            "p p",
            "r r",
            'c', findItemStack("Signal Cable"),
            'b', dictCheapChip,
            'r', "itemRubber",
            'p', "plateCopper");

        addRecipe(findItemStack("Electrical Fire Buzzer"),
            "rar",
            "p p",
            "r r",
            'a', dictAdvancedChip,
            'r', "itemRubber",
            'p', "plateCopper");

        addShapelessRecipe(findItemStack("Scanner"),
            stack(Items.COMPARATOR),
            dictAdvancedChip);

    }

    public static void recipeElectricalVuMeter() {
        for (int idx = 0; idx < 4; idx++) {
            addRecipe(findItemStack("Analog vuMeter", 1),
                "WWW",
                "RIr",
                "WSW",
                'W', planks(idx),
                'R', stack(Items.REDSTONE),
                'I', stack(Items.IRON_INGOT),
                'r', dye(1),
                'S', findItemStack("Signal Cable"));
        }
        for (int idx = 0; idx < 4; idx++) {
            addRecipe(findItemStack("LED vuMeter", 1),
                " W ",
                "WTW",
                " S ",
                'W', planks(idx),
                'T', stack(Blocks.REDSTONE_TORCH),
                'S', findItemStack("Signal Cable"));
        }
    }

    public static void recipeElectricalBreaker() {
        net.minecraft.world.item.Item[] alarmParts={Items.COMPARATOR,Items.LEVER,Items.NOTE_BLOCK,Items.BELL,Items.GLOWSTONE_DUST,Items.OBSERVER,Items.BLAZE_POWDER};
        for(var kind:mods.eln.transparentnode.firealarm.AlarmKind.values())addRecipe(findItemStack(kind.getTitle()),"IRI","RCR","IWI",
            'I',findItemStack("Iron Plate"),'R',stack(Items.REDSTONE),'C',stack(alarmParts[kind.ordinal()]),'W',findItemStack("Signal Cable"));

        addRecipe(findItemStack("Twelve Circuit American Breaker Panel"),"P P","CRC","PPP",
            'P',findItemStack("Iron Plate"),'C',findItemStack("Copper Cable"),'R',stack(Items.REDSTONE));
        addRecipe(findItemStack("5A Switch"),"  I"," I ","CAC",'A',"itemRubber",'I',findItemStack("Copper Cable"),'C',findItemStack("Copper 20 AWG Cable 300V"));
        addRecipe(findItemStack("20A Switch"),"  I"," I ","CAC",'A',"itemRubber",'I',findItemStack("Copper Cable"),'C',findItemStack("Copper 12 AWG Cable 600V"));
        addRecipe(findItemStack("50A Switch"),"AAI","AIA","CAC",'A',"itemRubber",'I',findItemStack("Copper Cable"),'C',findItemStack("Copper 6 AWG Cable 600V"));
        addRecipe(findItemStack("100A Switch"),"AAI","AIA","CAC",'A',"itemRubber",'I',findItemStack("Copper Cable"),'C',findItemStack("Copper 2 AWG Cable 600V"));
        String[] relayNames={"5V Control Relay 10A","12V Control Relay 20A","12V Control Relay 50A","12V Control Relay 100A"};
        String[] relayWire={"Copper 18 AWG Cable 300V","Copper 12 AWG Cable 600V","Copper 6 AWG Cable 600V","Copper 2 AWG Cable 600V"};
        for(int i=0;i<relayNames.length;i++)addRecipe(findItemStack(relayNames[i]),"GGG","OIO","CRC",
            'R',stack(Items.REDSTONE),'O',findItemStack("Iron Cable"),'G',stack(Blocks.GLASS_PANE),
            'I',findItemStack("Copper Cable"),'C',findItemStack(relayWire[i]));

        addRecipe(findItemStack("Electrical Breaker", 1),
            "crC",
            'c', findItemStack("Overvoltage Protection"),
            'C', findItemStack("Overheating Protection"),
            'r', findItemStack("High Voltage Relay"));

        // Preserve the source gauge/relay input for each exact current rating.
        String[] ratedParts={"5A Switch","5V Control Relay 10A","Copper 14 AWG Cable 600V","12V Control Relay 20A",
            "Copper 10 AWG Cable 600V","Copper 8 AWG Cable 600V","12V Control Relay 50A","Copper 6 AWG Cable 600V",
            "12V Control Relay 100A","Copper 1/0 AWG Cable 1000V","Copper 2/0 AWG Cable 1000V","Copper 4/0 AWG Cable 1000V"};
        int[] ratedAmps={5,10,15,20,30,40,50,60,100,125,200,400};
        for (int i=0;i<ratedAmps.length;i++) {
            int amps=ratedAmps[i];
            addRecipe(findItemStack(amps + "A Electrical Breaker",1),
                "crC", 'c', findItemStack("Overvoltage Protection"),
                'C', findItemStack("Overheating Protection"), 'r', findItemStack(ratedParts[i]));
        }

    }

    public static void recipeFuses() {

        addRecipe(findItemStack("Electrical Fuse Holder", 1),
            "i",
            " ",
            "i",
            'i', stack(Items.IRON_INGOT));

        // one fuse for every cable, its rating set in the holder (Baughn 2026-11-06); the 1.12 fuses for the other tiers
        // are no longer made (old ones still work, at their cable's rating until set)
        addRecipe(findItemStack("Lead Fuse", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("Copper Cable"));

        addRecipe(findItemStack("5A Lead Fuse", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("Copper 20 AWG Cable 300V"));

        addRecipe(findItemStack("10A Lead Fuse", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("Copper 16 AWG Cable 300V"));

        addRecipe(findItemStack("15A Lead Fuse", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("Copper 14 AWG Cable 600V"));

        addRecipe(findItemStack("20A Lead Fuse", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("Copper 12 AWG Cable 600V"));

        addRecipe(findItemStack("30A Lead Fuse", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("Copper 10 AWG Cable 600V"));

        addRecipe(findItemStack("50A Lead Fuse", 4),
            "rcr",
            'r', findItemStack("itemRubber"),
            'c', findItemStack("Copper 6 AWG Cable 600V"));

    }

    public static void recipeElectricalGateSource() {
        addRecipe(findItemStack("Signal Trimmer", 1),
            "RsR",
            "rRr",
            " c ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'r', "itemRubber",
            's', stack(Items.STICK),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("Signal Switch", 3),
            " r ",
            "rRr",
            " c ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'r', "itemRubber",
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("Signal Button", 3),
            " R ",
            "rRr",
            " c ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'r', "itemRubber",
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("Wireless Switch", 3),
            " a ",
            "rCr",
            " r ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'C', dictCheapChip,
            'a', findItemStack("Signal Antenna"),
            'r', "itemRubber",
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("Wireless Button", 3),
            " a ",
            "rCr",
            " R ",
            'M', findItemStack("Machine Block"),
            'c', findItemStack("Signal Cable"),
            'C', dictCheapChip,
            'a', findItemStack("Signal Antenna"),
            'r', "itemRubber",
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

        // Wireless Switch
        // Wireless Button
    }

    public static void recipeElectricalDataLogger() {
        addRecipe(findItemStack("Data Logger", 1),
            "RRR",
            "RGR",
            "RCR",
            'R', "itemRubber",
            'C', dictCheapChip,
            'G', stack(Blocks.GLASS_PANE));

        addRecipe(findItemStack("Modern Data Logger", 1),
            "RRR",
            "RGR",
            "RCR",
            'R', "itemRubber",
            'C', dictAdvancedChip,
            'G', stack(Blocks.GLASS_PANE));

        // three inputs: a data logger and two cheap chips (Baughn 2026-10-30, board #45)
        addShapelessRecipe(findItemStack("Three-Channel Data Logger", 1),
            findItemStack("Data Logger", 1), dictCheapChip, dictCheapChip);

        // its screen on a bracket: a three-channel logger, a glass pane and an iron ingot
        addShapelessRecipe(findItemStack("Tilted Terminal Logger", 1),
            findItemStack("Three-Channel Data Logger", 1), stack(Blocks.GLASS_PANE), stack(Items.IRON_INGOT));

        addRecipe(findItemStack("Industrial Data Logger", 1),
            "RRR",
            "GGG",
            "RCR",
            'R', "itemRubber",
            'C', dictAdvancedChip,
            'G', stack(Blocks.GLASS_PANE));
    }

    public static void recipeSixNodeCache() {

    }

    public static void recipeElectricalAlarm() {
        addRecipe(findItemStack("Nuclear Alarm", 1),
            "ITI",
            "IMI",
            "IcI",
            'c', findItemStack("Signal Cable"),
            'T', stack(Blocks.REDSTONE_TORCH),
            'I', stack(Items.IRON_INGOT),
            'M', stack(Blocks.NOTE_BLOCK));
        addRecipe(findItemStack("Standard Alarm", 1),
            "MTM",
            "IcI",
            "III",
            'c', findItemStack("Signal Cable"),
            'T', stack(Blocks.REDSTONE_TORCH),
            'I', stack(Items.IRON_INGOT),
            'M', stack(Blocks.NOTE_BLOCK));

    }

    public static void recipeElectricalAntenna() {
        addRecipe(findItemStack("Low Power Transmitter Antenna", 1),
            "R i",
            "CI ",
            "R i",
            'C', dictCheapChip,
            'i', stack(Items.IRON_INGOT),
            'I', "plateIron",
            'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("Low Power Receiver Antenna", 1),
            "i  ",
            " IC",
            "i  ",
            'C', dictCheapChip,
            'I', "plateIron",
            'i', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("Medium Power Transmitter Antenna", 1),
            "c I",
            "CI ",
            "c I",
            'C', dictAdvancedChip,
            'c', dictCheapChip,
            'I', "plateIron",
            'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("Medium Power Receiver Antenna", 1),
            "I  ",
            " IC",
            "I  ",
            'C', dictAdvancedChip,
            'I', "plateIron",
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("High Power Transmitter Antenna", 1),
            "C I",
            "CI ",
            "C I",
            'C', dictAdvancedChip,
            'c', dictCheapChip,
            'I', "plateIron",
            'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("High Power Receiver Antenna", 1),
            "I D",
            " IC",
            "I D",
            'C', dictAdvancedChip,
            'I', "plateIron",
            'R', stack(Items.REDSTONE),
            'D', stack(Items.DIAMOND));

    }

    public static void recipeBatteryCharger() {
        addRecipe(findItemStack("Weak 48V Battery Charger", 1),
            "RIR",
            "III",
            "RcR",
            'c', findItemStack("Copper Cable"),
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));
        addRecipe(findItemStack("48V Battery Charger", 1),
            "RIR",
            "ICI",
            "RcR",
            'C', dictCheapChip,
            'c', findItemStack("Copper Cable"),
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

        addRecipe(findItemStack("230V Battery Charger", 1),
            "RIR",
            "ICI",
            "RcR",
            'C', dictAdvancedChip,
            'c', findItemStack("Copper Cable"),
            'I', stack(Items.IRON_INGOT),
            'R', stack(Items.REDSTONE));

    }

    public static void recipeEggIncubator() {
        addRecipe(findItemStack("48V Egg Incubator", 1),
            "IGG",
            "E G",
            "CII",
            'C', dictCheapChip,
            'E', findItemStack("Small 48V Tungsten Heating Corp"),
            'I', stack(Items.IRON_INGOT),
            'G', stack(Blocks.GLASS_PANE));

    }

    public static void recipeEnergyConverter() {
        if (ElnToOtherEnergyConverterEnable) {
            addRecipe(findItemStack("48V Electrical Age to other energy exporter"),
                "III",
                "cCR",
                "III",
                'C', dictCheapChip,
                'c', findItemStack("Copper Cable"),
                'I', stack(Items.IRON_INGOT),
                'R', "ingotCopper");

            addRecipe(findItemStack("230V Electrical Age to other energy exporter"),
                "III",
                "cCR",
                "III",
                'C', dictCheapChip,
                'c', findItemStack("Copper Cable"),
                'I', stack(Items.IRON_INGOT),
                'R', dictTungstenIngot);

            addRecipe(findItemStack("400V Electrical Age to other energy exporter"),
                "III",
                "cCR",
                "III",
                'C', dictAdvancedChip,
                'c', findItemStack("Copper Cable"),
                'I', stack(Items.IRON_INGOT),
                'R', stack(Items.GOLD_INGOT));

        }
    }

    // recipeComputerProbe(): OpenComputers/ComputerCraft dropped (rule 8)


    // recipeArmor(), recipeTool(): copper armour and tools dropped, vanilla since 1.21.9 (Baughn, 2026-10-11)
    public static void recipeArmor() {
    }

    public static void recipeTool() {
        addRecipe(findItemStack("Wire Snips"), " I", "I ", "RR", 'I', "ingotIron", 'R', "itemRubber");
    }
}
