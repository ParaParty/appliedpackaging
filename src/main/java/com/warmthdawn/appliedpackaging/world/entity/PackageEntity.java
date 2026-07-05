package com.warmthdawn.appliedpackaging.world.entity;

import com.warmthdawn.appliedpackaging.item.PackageItem;
import com.warmthdawn.appliedpackaging.core.package_data.PackageUnpacker;
import com.warmthdawn.appliedpackaging.registry.APEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

public class PackageEntity extends LivingEntity implements IEntityAdditionalSpawnData {
    public static final float WIDTH = 10.0F / 16.0F;
    public static final float HEIGHT = 8.0F / 16.0F;
    private static final String PACKAGE_ITEM_TAG = "PackageItem";
    private static final String LEGACY_PACKAGE_TAG = "Package";

    private Entity originalEntity;
    private ItemStack packageStack = ItemStack.EMPTY;

    public PackageEntity(EntityType<? extends PackageEntity> type, Level level) {
        super(type, level);
        setYRot(random.nextFloat() * 360.0F);
        setYHeadRot(getYRot());
        yRotO = getYRot();
    }

    public PackageEntity(Level level, double x, double y, double z, ItemStack packageStack) {
        this(APEntityTypes.PACKAGE.get(), level);
        setPos(x, y, z);
        setPackageStack(packageStack);
    }

    public static PackageEntity fromDroppedItem(Level level, Entity originalEntity, ItemStack packageStack) {
        PackageEntity entity = new PackageEntity(
                level,
                originalEntity.getX(),
                originalEntity.getY(),
                originalEntity.getZ(),
                packageStack);
        entity.setDeltaMovement(originalEntity.getDeltaMovement().scale(1.5D));
        entity.originalEntity = originalEntity;
        return entity;
    }

    public static PackageEntity spawn(PlayMessages.SpawnEntity spawnEntity, Level level) {
        PackageEntity entity = new PackageEntity(
                APEntityTypes.PACKAGE.get(),
                level);
        entity.setPos(spawnEntity.getPosX(), spawnEntity.getPosY(), spawnEntity.getPosZ());
        entity.setDeltaMovement(spawnEntity.getVelX(), spawnEntity.getVelY(), spawnEntity.getVelZ());
        return entity;
    }

    public static AttributeSupplier.Builder createPackageAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D);
    }

    public ItemStack getPackageStack() {
        return packageStack;
    }

    public void setPackageStack(ItemStack stack) {
        packageStack = stack.copy();
        refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return new EntityDimensions(WIDTH, HEIGHT, true);
    }

    @Override
    public ItemStack getPickedResult(HitResult target) {
        return packageStack.copy();
    }

    @Override
    public void tick() {
        if (firstTick) {
            verifyInitialEntity();
            originalEntity = null;
        }
        super.tick();
        if (!packageStack.isEmpty() && !(packageStack.getItem() instanceof PackageItem)) {
            discard();
            return;
        }
        if (onGround()) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x, 0.0D, motion.z);
        }
    }

    private void verifyInitialEntity() {
        if (!(originalEntity instanceof ItemEntity itemEntity)) {
            return;
        }
        CompoundTag itemData = new CompoundTag();
        itemEntity.addAdditionalSaveData(itemData);
        if (itemData.getInt("PickupDelay") == 32767) {
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!packageStack.isEmpty()) {
            tag.put(PACKAGE_ITEM_TAG, packageStack.save(new CompoundTag()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(PACKAGE_ITEM_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            setPackageStack(ItemStack.of(tag.getCompound(PACKAGE_ITEM_TAG)));
            return;
        }
        if (tag.contains(LEGACY_PACKAGE_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            setPackageStack(ItemStack.of(tag.getCompound(LEGACY_PACKAGE_TAG)));
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (packageStack.isEmpty()) {
            return InteractionResult.PASS;
        }
        player.setItemInHand(hand, packageStack.copy());
        playPickupSound();
        discard();
        return InteractionResult.CONSUME;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return java.util.Collections.emptyList();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        setDeltaMovement(getDeltaMovement().add(x, y, z).scale(0.5D));
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return entity instanceof PackageEntity && entity.getBoundingBox().maxY < getBoundingBox().minY + 0.125F;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void push(Entity entity) {
        if (entity instanceof PackageEntity) {
            if (entity.getBoundingBox().minY < getBoundingBox().maxY) {
                super.push(entity);
            }
            return;
        }
        if (entity.getBoundingBox().minY <= getBoundingBox().minY) {
            super.push(entity);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || amount <= 0.0F || packageStack.isEmpty()) {
            return false;
        }
        if (!PackageUnpacker.unpackStackToWorld(level(), position(), packageStack)) {
            return false;
        }
        discard();
        return true;
    }

    @Override
    public double getPassengersRidingOffset() {
        return getDimensions(getPose()).height;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeItem(packageStack);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        setPackageStack(buffer.readItem());
    }

    private void playPickupSound() {
        level().playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                0.75F + level().random.nextFloat());
    }
}
