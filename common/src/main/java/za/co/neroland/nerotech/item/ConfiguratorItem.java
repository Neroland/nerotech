package za.co.neroland.nerotech.item;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.Configurator;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SideConfigured;
import za.co.neroland.nerolandcore.sideconfig.SideMode;

import za.co.neroland.nerotech.registry.ModDataComponents;

/**
 * The Configurator — NeroTech's wrench for Core's universal machine side-configuration. Per the
 * side-config design's resolved open question, Core ships the server-authoritative API
 * ({@link Configurator}) and the item lives here; every handler below just routes clicks into it,
 * so validation and the routing model stay in Core.
 *
 * <p>Controls (all server-side, feedback on the actionbar):
 * <ul>
 *   <li><b>Right-click a machine face</b> — cycle that face's mode ({@link Configurator#cycle}).</li>
 *   <li><b>Sneak + right-click a machine face</b> — read the mode without changing it
 *       ({@link Configurator#read}).</li>
 *   <li><b>Sneak + right-click anything else</b> — toggle between <em>configure</em> and
 *       <em>copy/paste</em> mode (stored in the stack's {@link ConfiguratorState} data component).</li>
 *   <li><b>Copy/paste mode:</b> right-click copies the machine's full side config into the stack's
 *       clipboard ({@link Configurator#snapshot}); sneak + right-click pastes it onto another
 *       machine of the same block-entity type ({@link Configurator#apply}).</li>
 * </ul>
 *
 * <p><b>Channel selection (v1):</b> cycle and read operate on the machine's {@link Channel#ITEM}
 * channel when its side config declares one, else on {@link Channel#ENERGY}, else on the machine's
 * first declared channel. Per-channel targeting stays in the in-GUI Side Config widget; copy/paste
 * always round-trips <em>all</em> channels.
 *
 * <p>Blocks that open a GUI on plain right-click must let the wrench through by returning
 * {@code PASS} from {@code useItemOn} when it is held (as {@code NeroTechMachineBlock} does) —
 * otherwise only the sneak interactions reach this item.
 *
 * <p>Touches world/block routing data only — no player identity is read, stored or logged
 * (POPIA/GDPR).
 */
public class ConfiguratorItem extends Item {

    public ConfiguratorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        SideConfigComponent comp = componentAt(level, pos);
        if (comp == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ConfiguratorState state = stateOf(context.getItemInHand());
        if (state.copyPaste()) {
            return context.isSecondaryUseActive()
                    ? paste(context, level, pos, state)
                    : copy(context, level, pos, state);
        }
        Direction side = context.getClickedFace();
        Channel channel = targetChannel(comp);
        if (context.isSecondaryUseActive()) {
            SideMode mode = Configurator.read(level, pos, side, channel);
            if (mode == null || channel == null) {
                tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.not_configurable"));
            } else {
                tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.read",
                        channel.lowerName(), side.getName(), mode.lowerName()));
            }
            return InteractionResult.SUCCESS;
        }
        SideMode next = Configurator.cycle(level, pos, side, channel);
        if (next == null || channel == null) {
            tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.not_configurable"));
        } else {
            tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.cycled",
                    channel.lowerName(), side.getName(), next.lowerName()));
        }
        return InteractionResult.SUCCESS;
    }

    /** Sneak-use away from a configurable machine: flip configure ↔ copy/paste mode. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);
            ConfiguratorState state = stateOf(stack).toggled();
            stack.set(ModDataComponents.CONFIGURATOR_STATE.get(), state);
            tell(player, Component.translatable(state.copyPaste()
                    ? "item.nerotech.configurator.mode.copy_paste"
                    : "item.nerotech.configurator.mode.configure"));
        }
        return InteractionResult.SUCCESS;
    }

    // --- copy / paste ---------------------------------------------------------

    private InteractionResult copy(UseOnContext context, Level level, BlockPos pos, ConfiguratorState state) {
        Map<Channel, Integer> packed = Configurator.snapshot(level, pos);
        if (packed == null) {
            tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.not_configurable"));
            return InteractionResult.SUCCESS;
        }
        String typeKey = typeKey(level, pos);
        context.getItemInHand().set(ModDataComponents.CONFIGURATOR_STATE.get(),
                state.withClipboard(ConfiguratorState.Snapshot.of(typeKey, packed)));
        tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.copied", typeKey));
        return InteractionResult.SUCCESS;
    }

    private InteractionResult paste(UseOnContext context, Level level, BlockPos pos, ConfiguratorState state) {
        if (state.clipboard().isEmpty()) {
            tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.paste_empty"));
            return InteractionResult.SUCCESS;
        }
        ConfiguratorState.Snapshot snapshot = state.clipboard().get();
        String typeKey = typeKey(level, pos);
        if (!snapshot.typeKey().equals(typeKey)) {
            tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.paste_mismatch",
                    snapshot.typeKey()));
            return InteractionResult.SUCCESS;
        }
        if (Configurator.apply(level, pos, snapshot.toPacked())) {
            tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.pasted", typeKey));
        }
        return InteractionResult.SUCCESS;
    }

    // --- helpers ---------------------------------------------------------------

    /** The stack's tool state, defaulting when the component is absent. */
    private static ConfiguratorState stateOf(ItemStack stack) {
        ConfiguratorState state = stack.get(ModDataComponents.CONFIGURATOR_STATE.get());
        return state == null ? ConfiguratorState.DEFAULT : state;
    }

    /** V1 channel choice: ITEM if the machine declares it, else ENERGY, else its first channel. */
    @Nullable
    private static Channel targetChannel(SideConfigComponent comp) {
        if (comp.config().has(Channel.ITEM)) {
            return Channel.ITEM;
        }
        if (comp.config().has(Channel.ENERGY)) {
            return Channel.ENERGY;
        }
        var it = comp.config().channels().keySet().iterator();
        return it.hasNext() ? it.next() : null;
    }

    @Nullable
    private static SideConfigComponent componentAt(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SideConfigured configured ? configured.sideConfig() : null;
    }

    /** Block-entity-type registry id — the same paste-compatibility key the Side Config widget uses. */
    private static String typeKey(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return "nerotech:machine";
        }
        Identifier id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
        return id == null ? "nerotech:machine" : id.toString();
    }

    /** Actionbar feedback — transient, so wrenching in bulk does not flood the chat. */
    private static void tell(@Nullable Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message, true);
        }
    }
}
