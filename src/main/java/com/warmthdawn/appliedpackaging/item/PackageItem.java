package com.warmthdawn.appliedpackaging.item;

import com.warmthdawn.appliedpackaging.core.package_data.PackageDataStorage;
import com.warmthdawn.appliedpackaging.core.package_data.PackageTooltipBuilder;
import com.warmthdawn.appliedpackaging.core.package_data.PackageUnpacker;
import com.warmthdawn.appliedpackaging.client.renderer.PackageItemRenderer;
import com.warmthdawn.appliedpackaging.world.entity.PackageEntity;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class PackageItem extends Item {
    private final PackageColor color;

    public PackageItem(PackageColor color, Properties properties) {
        super(properties);
        this.color = color;
    }

    public PackageColor color() {
        return color;
    }

    public static boolean hasPackageData(ItemStack stack) {
        return PackageDataStorage.hasPackageData(stack);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public Entity createEntity(Level level, Entity originalEntity, ItemStack stack) {
        return PackageEntity.fromDroppedItem(level, originalEntity, stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !PackageUnpacker.canUnpackAsItems(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        return PackageUnpacker.unpackStackToPlayer(player, stack)
                ? InteractionResultHolder.consume(player.getItemInHand(hand))
                : InteractionResultHolder.pass(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return PackageItemRenderer.instance();
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        PackageDataStorage.read(stack).ifPresentOrElse(data -> {
            PackageTooltipBuilder.append(stack, color, data, tooltip, flag);
        }, () -> {
            tooltip.add(Component.translatable("tooltip.appliedpackaging.package.invalid")
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.appliedpackaging.package.invalid_hint")
                    .withStyle(ChatFormatting.GRAY));
        });
    }
}
