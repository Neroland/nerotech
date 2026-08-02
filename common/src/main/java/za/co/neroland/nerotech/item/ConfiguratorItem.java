package za.co.neroland.nerotech.item;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

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

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.MachinePreset;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.WirelessNodeBlockEntity;
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
 *   <li><b>Copy/paste mode:</b> right-click copies the machine's full side config <em>and</em> its
 *       overclock preset into the stack's clipboard ({@link Configurator#snapshot}); sneak +
 *       right-click pastes onto <b>any</b> machine (Stage E) — each copied channel lands only where
 *       the target declares that channel ({@link Configurator#apply}), and the preset always lands.
 *       A paste with nothing compatible reports the mismatch rather than silently doing nothing.</li>
 *   <li><b>Sneak + right-click a Wireless Power Node</b> (Stage D, configure mode) — pair it: the
 *       first node is remembered in the stack, the second completes the link. Sneak-using an
 *       already-linked node unlinks it. See {@link #link}.</li>
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
        // Stage D: on a Wireless Power Node the sneak interaction is pairing, not mode-read — the
        // node's whole purpose is the link, and its faces are all I/O anyway. Plain right-click
        // still cycles a face, so nothing about side config is lost.
        if (context.isSecondaryUseActive()
                && level.getBlockEntity(pos) instanceof WirelessNodeBlockEntity node) {
            return link(context, level, pos, node, state);
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
        // Stage E: the overclock preset rides along with the side config, so one copy carries the
        // whole tuning of a machine, not just its faces.
        int preset = level.getBlockEntity(pos) instanceof NeroTechMachineBlockEntity machine
                ? machine.preset().ordinal() : ConfiguratorState.Snapshot.NO_PRESET;
        context.getItemInHand().set(ModDataComponents.CONFIGURATOR_STATE.get(),
                state.withClipboard(ConfiguratorState.Snapshot.of(typeKey, packed, preset)));
        tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.copied", typeKey));
        return InteractionResult.SUCCESS;
    }

    /**
     * Paste onto a machine of <b>any</b> type (Stage E): each copied channel is applied only where
     * the target declares that same channel, and the preset is applied whenever the target is a
     * NeroTech machine. Nothing compatible at all still reports the mismatch, so a paste that does
     * nothing never looks like a paste that worked. Server-side mutation only; the clipboard lives
     * on the stack and holds routing modes plus a preset ordinal — no player data (POPIA/GDPR).
     */
    private InteractionResult paste(UseOnContext context, Level level, BlockPos pos, ConfiguratorState state) {
        if (state.clipboard().isEmpty()) {
            tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.paste_empty"));
            return InteractionResult.SUCCESS;
        }
        ConfiguratorState.Snapshot snapshot = state.clipboard().get();
        SideConfigComponent comp = componentAt(level, pos);
        Map<Channel, Integer> compatible = new EnumMap<>(Channel.class);
        if (comp != null) {
            for (Map.Entry<Channel, Integer> entry : snapshot.toPacked().entrySet()) {
                if (comp.config().has(entry.getKey())) {
                    compatible.put(entry.getKey(), entry.getValue());
                }
            }
        }
        boolean applied = !compatible.isEmpty() && Configurator.apply(level, pos, compatible);
        if (snapshot.preset() >= 0
                && level.getBlockEntity(pos) instanceof NeroTechMachineBlockEntity machine) {
            machine.setPreset(MachinePreset.byOrdinal(snapshot.preset()));
            applied = true;
        }
        tell(context.getPlayer(), applied
                ? Component.translatable("item.nerotech.configurator.pasted", typeKey(level, pos))
                : Component.translatable("item.nerotech.configurator.paste_mismatch", snapshot.typeKey()));
        return InteractionResult.SUCCESS;
    }

    // --- Wireless Power Node pairing (Stage D) ---------------------------------

    /**
     * Sneak-use on a node, in configure mode. Three outcomes, in the order a player discovers them:
     *
     * <ul>
     *   <li>the node is already linked → <b>unlink</b> it (and its partner);</li>
     *   <li>no pairing is pending → <b>remember</b> this node as the first end;</li>
     *   <li>a pairing is pending → <b>pair</b> the two, provided the stored source still exists, is
     *       in this dimension and is within {@code wirelessNodeRange}.</li>
     * </ul>
     *
     * <p>The pending end lives in the stack's {@code configurator_state} data component: a block
     * position and a dimension id, never a player identity (POPIA/GDPR).
     */
    private InteractionResult link(UseOnContext context, Level level, BlockPos pos,
            WirelessNodeBlockEntity node, ConfiguratorState state) {
        ItemStack stack = context.getItemInHand();
        String dimension = level.dimension().identifier().toString();

        if (node.partner() != null) {
            node.unlink();
            stack.set(ModDataComponents.CONFIGURATOR_STATE.get(), state.withoutLinkSource());
            tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.link.cleared"));
            return InteractionResult.SUCCESS;
        }

        Optional<ConfiguratorState.Link> pending = state.linkSource();
        if (pending.isEmpty() || !pending.get().dimension().equals(dimension)) {
            stack.set(ModDataComponents.CONFIGURATOR_STATE.get(),
                    state.withLinkSource(ConfiguratorState.Link.of(pos, dimension)));
            tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.link.stored"));
            return InteractionResult.SUCCESS;
        }

        BlockPos source = pending.get().toPos();
        if (source.equals(pos)) {
            // Same node twice: keep the pending end so the player can simply click the other one.
            tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.link.same_node"));
            return InteractionResult.SUCCESS;
        }
        if (!level.hasChunkAt(source)
                || !(level.getBlockEntity(source) instanceof WirelessNodeBlockEntity first)) {
            // Source gone (or asleep) — drop the stale pending end rather than pair blind.
            stack.set(ModDataComponents.CONFIGURATOR_STATE.get(), state.withoutLinkSource());
            tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.link.cleared"));
            return InteractionResult.SUCCESS;
        }
        if (!first.canPairWith(node)) {
            tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.link.too_far",
                    NeroTechConfig.wirelessNodeRange()));
            return InteractionResult.SUCCESS;
        }

        first.pairWith(node, dimension);
        stack.set(ModDataComponents.CONFIGURATOR_STATE.get(), state.withoutLinkSource());
        tell(context.getPlayer(), Component.translatable("item.nerotech.configurator.link.paired"));
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
