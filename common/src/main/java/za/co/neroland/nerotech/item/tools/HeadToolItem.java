package za.co.neroland.nerotech.item.tools;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.platform.EnergyLookup;

/**
 * A charged tool worn in the head slot (Thermal Goggles, Filter Mask). From 1.21.2 the item carries the
 * vanilla {@code equippable} component (set at registration, see {@link ToolsContent}), so using it
 * equips it; on 1.21.1 it implements the old {@code Equipable} interface instead. Sneak-use on a powered
 * block charges it. Worn, it is serviced by {@link WornTools} on the server's pollution clock.
 */
//? if >=1.21.2 {
public abstract class HeadToolItem extends ChargedToolItem {
//?} else {
/*public abstract class HeadToolItem extends ChargedToolItem implements net.minecraft.world.item.Equipable {
*///?}

    protected HeadToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        NeroEnergyStorage source = EnergyLookup.INSTANCE.find(context.getLevel(), context.getClickedPos(),
                context.getClickedFace());
        if (source == null) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            chargeFrom(serverPlayer, context.getItemInHand(), source);
        }
        return InteractionResult.SUCCESS;
    }

    /** A head-tool's own right-click in the air, before equipping; true when it handled the click. */
    protected boolean useInAir(Level level, Player player, InteractionHand hand) {
        return false;
    }

    //? if >=1.21.2 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (useInAir(level, player, hand)) {
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }
    //?} else {
    /*@Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (useInAir(level, player, hand)) {
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),
                    level.isClientSide());
        }
        return this.swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public net.minecraft.world.entity.EquipmentSlot getEquipmentSlot() {
        return net.minecraft.world.entity.EquipmentSlot.HEAD;
    }
    *///?}

    /** Whether {@code stack} is charged enough to work (a worn tool with no charge does nothing). */
    public static boolean powered(ItemStack stack) {
        return charge(stack).energy() > 0;
    }
}
