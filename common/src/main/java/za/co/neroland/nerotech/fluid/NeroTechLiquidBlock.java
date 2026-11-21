package za.co.neroland.nerotech.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
//? if >=1.21.5 {
import net.minecraft.world.entity.InsideBlockEffectApplier;
//?}
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * The world block of a {@link NeroTechLiquid}. Vanilla's {@link LiquidBlock} constructor is protected,
 * so common code needs this public subclass to register one (the Nerospace rocket-fuel recipe).
 *
 * <p>A corrosive liquid (sulfuric acid) deals {@link #CORROSION_DAMAGE} to a living entity standing in
 * it every {@link #CORROSION_INTERVAL} ticks. The values are constants for now.
 */
public class NeroTechLiquidBlock extends LiquidBlock {

    /** Damage per corrosion hit (half a heart per point). */
    public static final float CORROSION_DAMAGE = 2.0F;
    /** Ticks between corrosion hits on one entity. */
    public static final int CORROSION_INTERVAL = 10;

    private final NeroTechLiquid liquid;

    public NeroTechLiquidBlock(NeroTechLiquid liquid, FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
        this.liquid = liquid;
    }

    public NeroTechLiquid liquid() {
        return this.liquid;
    }

    //? if >=1.21.9 {
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
    //?}
    //? if >=1.21.5 && <1.21.9 {
    /*@Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier) {
        super.entityInside(state, level, pos, entity, effectApplier);
    *///?}
    //? if <1.21.5 {
    /*@Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
    *///?}
        if (!this.liquid.corrosive() || !(level instanceof ServerLevel server)
                || !(entity instanceof LivingEntity living) || !pos.equals(entity.blockPosition())
                || (server.getGameTime() + entity.getId()) % CORROSION_INTERVAL != 0) {
            return;
        }
        //? if >=1.21.2 {
        living.hurtServer(server, server.damageSources().generic(), CORROSION_DAMAGE);
        //?} else {
        /*living.hurt(server.damageSources().generic(), CORROSION_DAMAGE);
        *///?}
    }
}
