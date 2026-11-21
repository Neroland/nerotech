package za.co.neroland.nerotech.item.tools;

import java.util.List;
import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import za.co.neroland.nerotech.api.WirelessCharging;
import za.co.neroland.nerotech.machine.extraction.ExtractionContent;
import za.co.neroland.nerotech.machine.extraction.ExtractionSettings;
import za.co.neroland.nerotech.machine.extraction.ScannerRules;
import za.co.neroland.nerotech.machine.extraction.ScannerState;
import za.co.neroland.nerotech.machine.extraction.SurveyScannerItem;
import za.co.neroland.nerotech.registry.ModDataComponents;
import za.co.neroland.nerotech.registry.ModItems;
import za.co.neroland.nerotech.registry.RegistrationProvider.RegistryEntry;

/**
 * The 0.8.0 tool set, in the same seam as the machine families' {@code <Family>Content} classes: its data
 * components register into {@link ModDataComponents#COMPONENTS} and its items into {@link ModItems#ITEMS}
 * when this class loads ({@code ModRegistries.init()} calls {@link #init()} after the families), and its
 * creative entries follow the families in {@code ModItems.creativeContents()}. The Survey Scanner stays in
 * the extraction family; it is charged by the same Induction Charger adapter registered here.
 *
 * <p>Every component is item data only: charge, cooldown time, mode, a fitted lens id, cartridge uses
 * left, and a carried machine's state with its owner stripped. No player data.
 */
public final class ToolsContent {

    // --- data components -----------------------------------------------------------------------------

    public static final RegistryEntry<DataComponentType<ToolCharge>> TOOL_CHARGE =
            ModDataComponents.COMPONENTS.register("tool_charge",
                    key -> DataComponentType.<ToolCharge>builder().persistent(ToolCharge.CODEC).build());
    public static final RegistryEntry<DataComponentType<String>> FITTED_LENS =
            ModDataComponents.COMPONENTS.register("fitted_lens",
                    key -> DataComponentType.<String>builder().persistent(Codec.STRING).build());
    public static final RegistryEntry<DataComponentType<Integer>> FITTED_FILTER =
            ModDataComponents.COMPONENTS.register("fitted_filter",
                    key -> DataComponentType.<Integer>builder().persistent(Codec.INT).build());
    /**
     * Client-visible look flags, derived from {@link #TOOL_CHARGE} every time it is set (item models read
     * them with {@code has_component} / {@code select}): the Power Drill's mode when it is not single
     * ("area" or "vein"), and whether a charged tool holds any charge (the Arc Wrench's idle model).
     */
    public static final RegistryEntry<DataComponentType<String>> DRILL_MODE =
            ModDataComponents.COMPONENTS.register("drill_mode",
                    key -> DataComponentType.<String>builder().persistent(Codec.STRING).build());
    public static final RegistryEntry<DataComponentType<Boolean>> TOOL_CHARGED =
            ModDataComponents.COMPONENTS.register("tool_charged",
                    key -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build());
    public static final RegistryEntry<DataComponentType<CarriedMachine>> CARRIED_MACHINE =
            ModDataComponents.COMPONENTS.register("carried_machine",
                    key -> DataComponentType.<CarriedMachine>builder().persistent(CarriedMachine.CODEC).build());

    // --- items ---------------------------------------------------------------------------------------

    public static final RegistryEntry<Item> POWER_DRILL = tool("power_drill", PowerDrillItem::new);
    public static final RegistryEntry<Item> CUTTING_LANCE = tool("cutting_lance", CuttingLanceItem::new);
    public static final RegistryEntry<Item> ARC_WRENCH = tool("arc_wrench", ArcWrenchItem::new);
    public static final RegistryEntry<Item> MULTIMETER = tool("multimeter", MultimeterItem::new);
    public static final RegistryEntry<Item> THERMAL_GOGGLES = head("thermal_goggles", ThermalGogglesItem::new);
    public static final RegistryEntry<Item> FILTER_MASK = head("filter_mask", FilterMaskItem::new);

    private static boolean initialised;

    private ToolsContent() {
    }

    private static RegistryEntry<Item> tool(String name, Function<Item.Properties, Item> factory) {
        return ModItems.ITEMS.register(name, key -> factory.apply(new Item.Properties().setId(key).stacksTo(1)));
    }

    private static RegistryEntry<Item> head(String name, Function<Item.Properties, Item> factory) {
        return ModItems.ITEMS.register(name, key -> factory.apply(new Item.Properties().setId(key).stacksTo(1)
                //? if >=1.21.2 {
                .equippable(net.minecraft.world.entity.EquipmentSlot.HEAD)
                //?}
        ));
    }

    /**
     * Force class-load so the registrations run, and register the Induction Charger adapter for every
     * NeroTech tool and the Survey Scanner (once).
     */
    public static void init() {
        if (initialised) {
            return;
        }
        initialised = true;
        WirelessCharging.registerItemCharger(ToolsContent::charge);
    }

    /** The item adapter: fills a NeroTech tool or Survey Scanner; 0 for anything else. */
    static long charge(ItemStack stack, long maxFe, boolean simulate) {
        if (stack.getItem() instanceof ChargedToolItem tool) {
            return tool.acceptCharge(stack, maxFe, simulate);
        }
        if (stack.getItem() instanceof SurveyScannerItem) {
            ScannerState state = SurveyScannerItem.state(stack);
            int accepted = ScannerRules.chargeAmount(state.energy(), ExtractionSettings.surveyScannerCapacity(), maxFe);
            if (accepted > 0 && !simulate) {
                stack.set(ExtractionContent.SCANNER_STATE.get(), state.withEnergy(state.energy() + accepted));
            }
            return accepted;
        }
        return 0L;
    }

    /** The tools' creative-tab entries, in display order. */
    public static List<ItemLike> creativeContents() {
        return List.of(POWER_DRILL.get(), CUTTING_LANCE.get(), ARC_WRENCH.get(), MULTIMETER.get(),
                THERMAL_GOGGLES.get(), FILTER_MASK.get());
    }
}
