package com.warmthdawn.appliedpackaging.diagnostic;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import com.warmthdawn.appliedpackaging.AppliedPackaging;
import com.warmthdawn.appliedpackaging.core.package_data.PackageData;
import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import java.util.List;
import java.util.StringJoiner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;

/**
 * Temporary always-on diagnostics for the package routing investigation. Every line is intentionally prefixed with
 * {@value #PREFIX} so a client log can be filtered without including unrelated mod output.
 */
public final class RoutingTrace {
    public static final String PREFIX = "[AP_ROUTE]";

    private RoutingTrace() {
    }

    public static void log(Level level, BlockPos pos, String component, String event, String details) {
        long tick = level == null ? -1 : level.getGameTime();
        AppliedPackaging.LOGGER.info(
                "{} tick={} component={} event={} pos={} {}",
                PREFIX,
                tick,
                component,
                event,
                pos == null ? "unknown" : pos.toShortString(),
                details == null ? "" : details);
    }

    public static String stack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        String base = BuiltInRegistries.ITEM.getKey(stack.getItem()) + "x" + stack.getCount();
        return PackageDataStorage.read(stack)
                .map(data -> base + "{" + packageData(data) + "}")
                .orElse(base);
    }

    public static String stacks(List<ItemStack> stacks) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (int index = 0; index < stacks.size(); index++) {
            joiner.add(index + "=" + stack(stacks.get(index)));
        }
        return joiner.toString();
    }

    public static String packageData(PackageData data) {
        if (data == null) {
            return "package=null";
        }
        String layout = data.layout()
                .map(value -> value.slotCount() + ":" + value.contentSlots())
                .orElse("dense");
        return "hash=" + data.canonicalHash()
                + ",layout=" + layout
                + ",contents=" + genericStacks(data.contents());
    }

    public static String genericStack(GenericStack stack) {
        if (stack == null || stack.what() == null) {
            return "null";
        }
        return key(stack.what()) + "x" + stack.amount();
    }

    public static String genericStacks(List<GenericStack> stacks) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (int index = 0; index < stacks.size(); index++) {
            joiner.add(index + "=" + genericStack(stacks.get(index)));
        }
        return joiner.toString();
    }

    public static String key(AEKey key) {
        if (key == null) {
            return "null";
        }
        return key.getType().getId() + "/" + key.getId();
    }

    public static String counters(KeyCounter[] counters) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        if (counters == null) {
            return joiner.toString();
        }
        for (int counterIndex = 0; counterIndex < counters.length; counterIndex++) {
            KeyCounter counter = counters[counterIndex];
            if (counter == null) {
                continue;
            }
            for (var entry : counter) {
                if (entry.getLongValue() > 0) {
                    joiner.add(counterIndex + ":" + key(entry.getKey()) + "x" + entry.getLongValue());
                }
            }
        }
        return joiner.toString();
    }

    public static String itemHandler(IItemHandler handler) {
        if (handler == null) {
            return "handler=null";
        }
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                joiner.add(slot + "=" + stack(stack));
            }
        }
        return joiner.toString();
    }
}
