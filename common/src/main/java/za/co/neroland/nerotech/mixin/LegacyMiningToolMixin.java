package za.co.neroland.nerotech.mixin;

//? if <1.20.5 {
/*import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import za.co.neroland.nerotech.item.tools.CuttingLanceItem;
import za.co.neroland.nerotech.item.tools.PowerDrillItem;

@Mixin(ItemStack.class)
abstract class LegacyMiningToolMixin {
    @Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
    private void nerotech$checkPoweredTool(BlockState state, CallbackInfoReturnable<Boolean> result) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() instanceof PowerDrillItem drill) {
            result.setReturnValue(drill.isCorrectToolForDrops(stack, state));
        } else if (stack.getItem() instanceof CuttingLanceItem lance) {
            result.setReturnValue(lance.isCorrectToolForDrops(stack, state));
        }
    }
}
*///?}
