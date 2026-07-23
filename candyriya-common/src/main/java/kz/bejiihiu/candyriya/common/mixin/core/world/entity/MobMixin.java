package kz.bejiihiu.candyriya.common.mixin.core.world.entity;

import kz.bejiihiu.candyriya.common.bridge.core.world.entity.LivingEntityBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.entity.MobBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.level.WorldBridge;
import kz.bejiihiu.candyriya.common.mod.server.CandyriyaServer;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntityMixin implements MobBridge {

    // @formatter:off
    @Shadow public boolean persistenceRequired;
    @Shadow public abstract boolean removeWhenFarAway(double distanceToClosestPlayer);
    @Shadow @Nullable public abstract LivingEntity getTarget();
    @Shadow private LivingEntity target;
    @Shadow public abstract ItemStack getItemBySlot(EquipmentSlot slotIn);
    @Shadow public abstract boolean canHoldItem(ItemStack stack);
    @Shadow protected abstract float getEquipmentDropChance(EquipmentSlot slotIn);
    @Shadow public abstract void setItemSlot(EquipmentSlot slotIn, ItemStack stack);
    @Shadow @Final public float[] handDropChances;
    @Shadow @Final public float[] armorDropChances;
    @Shadow public abstract boolean isPersistenceRequired();
    @Shadow protected void customServerAiStep() { }
    @Shadow public abstract boolean isNoAi();
    @Shadow protected abstract boolean canReplaceCurrentItem(ItemStack candidate, ItemStack existing);
    @Shadow protected abstract void setItemSlotAndDropWhenKilled(EquipmentSlot p_233657_1_, ItemStack p_233657_2_);
    @Shadow @Nullable public abstract <T extends Mob> T convertTo(EntityType<T> p_233656_1_, boolean p_233656_2_);
    @Shadow @Nullable protected abstract SoundEvent getAmbientSound();
    @Shadow public abstract void setTarget(@org.jetbrains.annotations.Nullable LivingEntity livingEntity);
    // @formatter:on

    public boolean aware;

    @Override
    public void bridge$setAware(boolean aware) {
        this.aware = aware;
    }

    @Inject(method = "setCanPickUpLoot", at = @At("HEAD"))
    public void Candyriya$setPickupLoot(boolean canPickup, CallbackInfo ci) {
        super.bukkitPickUpLoot = canPickup;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean canPickUpLoot() {
        return super.bukkitPickUpLoot;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void Candyriya$init(EntityType<? extends Mob> type, net.minecraft.world.level.Level worldIn, CallbackInfo ci) {
        this.aware = true;
    }

    public SoundEvent getAmbientSound0() {
        return getAmbientSound();
    }

    protected transient boolean Candyriya$targetSuccess = false;
    private transient EntityTargetEvent.TargetReason Candyriya$reason;
    private transient boolean Candyriya$fireEvent;

    @Decorate(method = "setTarget", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/entity/Mob;target:Lnet/minecraft/world/entity/LivingEntity;"))
    private void Candyriya$setTargetEvent(Mob instance, LivingEntity livingEntity) throws Throwable {
        boolean fireEvent = Candyriya$fireEvent;
        Candyriya$fireEvent = false;
        EntityTargetEvent.TargetReason reason = Candyriya$reason == null ? EntityTargetEvent.TargetReason.UNKNOWN : Candyriya$reason;
        Candyriya$reason = null;
        if (getTarget() == livingEntity) {
            Candyriya$targetSuccess = false;
            return;
        }
        if (fireEvent) {
            if (reason == EntityTargetEvent.TargetReason.UNKNOWN && this.getTarget() != null && livingEntity == null) {
                reason = (this.getTarget().isAlive() ? EntityTargetEvent.TargetReason.FORGOT_TARGET : EntityTargetEvent.TargetReason.TARGET_DIED);
            }
            if (reason == EntityTargetEvent.TargetReason.UNKNOWN) {
                CandyriyaServer.LOGGER.debug("Unknown target reason setting {} target to {}", this, livingEntity);
            }
            CraftLivingEntity ctarget = null;
            if (livingEntity != null) {
                ctarget = ((LivingEntityBridge) livingEntity).bridge$getBukkitEntity();
            }
            final EntityTargetLivingEntityEvent event = new EntityTargetLivingEntityEvent(this.getBukkitEntity(), ctarget, reason);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                Candyriya$targetSuccess = false;
                DecorationOps.cancel().invoke();
                return;
            }
            if (event.getTarget() != null) {
                livingEntity = ((CraftLivingEntity) event.getTarget()).getHandle();
            } else {
                livingEntity = null;
            }
        }
        DecorationOps.callsite().invoke(instance, livingEntity);
        Candyriya$targetSuccess = true;
    }

    public boolean setTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        bridge$pushGoalTargetReason(reason, fireEvent);
        setTarget(livingEntity);
        return Candyriya$targetSuccess;
    }

    @Override
    public boolean bridge$lastGoalTargetResult() {
        return Candyriya$targetSuccess;
    }

    @Override
    public boolean bridge$setGoalTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        return setTarget(livingEntity, reason, fireEvent);
    }

    @Override
    public void bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        if (fireEvent) {
            this.Candyriya$reason = reason;
        } else {
            this.Candyriya$reason = null;
        }
        Candyriya$fireEvent = fireEvent;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void Candyriya$setAware(CompoundTag compound, CallbackInfo ci) {
        compound.putBoolean("Bukkit.Aware", this.aware);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void Candyriya$readAware(CompoundTag compound, CallbackInfo ci) {
        if (compound.contains("Bukkit.Aware")) {
            this.aware = compound.getBoolean("Bukkit.Aware");
        }
    }

    @Redirect(method = "readAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;setCanPickUpLoot(Z)V"))
    public void Candyriya$setIfTrue(Mob mobEntity, boolean canPickup) {
        if (canPickup) mobEntity.setCanPickUpLoot(true);
    }

    @Inject(method = "serverAiStep", cancellable = true, at = @At("HEAD"))
    private void Candyriya$unaware(CallbackInfo ci) {
        if (!this.aware) {
            ++this.noActionTime;
            ci.cancel();
        }
    }

    @Inject(method = "pickUpItem", at = @At("HEAD"))
    private void Candyriya$captureItemEntity(ItemEntity itemEntity, CallbackInfo ci) {
        Candyriya$item = itemEntity;
    }

    @Inject(method = "pickUpItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private void Candyriya$pickupCause(ItemEntity itemEntity, CallbackInfo ci) {
        itemEntity.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PICKUP);
    }

    @Override
    public void bridge$captureItemDrop(ItemEntity itemEntity) {
        this.Candyriya$item = itemEntity;
    }

    private transient ItemEntity Candyriya$item;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public ItemStack equipItemIfPossible(ItemStack stack) {
        ItemEntity itemEntity = Candyriya$item;
        Candyriya$item = null;
        EquipmentSlot equipmentslottype = getEquipmentSlotForItem(stack);
        ItemStack itemstack = this.getItemBySlot(equipmentslottype);
        boolean flag = this.canReplaceCurrentItem(stack, itemstack);

        if (equipmentslottype.isArmor() && !flag) {
            equipmentslottype = EquipmentSlot.MAINHAND;
            itemstack = this.getItemBySlot(equipmentslottype);
            flag = itemstack.isEmpty();
        }

        boolean canPickup = flag && this.canHoldItem(stack);
        if (itemEntity != null) {
            canPickup = !CraftEventFactory.callEntityPickupItemEvent((Mob) (Object) this, itemEntity, 0, !canPickup).isCancelled();
        }
        if (canPickup) {
            double d0 = this.getEquipmentDropChance(equipmentslottype);
            if (!itemstack.isEmpty() && (double) Math.max(this.random.nextFloat() - 0.1F, 0.0F) < d0) {
                forceDrops = true;
                this.spawnAtLocation(itemstack);
                forceDrops = false;
            }

            var itemstack2 = equipmentslottype.limit(stack);
            this.setItemSlotAndDropWhenKilled(equipmentslottype, itemstack2);
            return itemstack2;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Inject(method = "startRiding", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;dropLeash(ZZ)V"))
    private void Candyriya$unleashRide(Entity entityIn, boolean force, CallbackInfoReturnable<Boolean> cir) {
        Bukkit.getPluginManager().callEvent(new EntityUnleashEvent(this.getBukkitEntity(), EntityUnleashEvent.UnleashReason.UNKNOWN));
    }

    @Decorate(method = "convertTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean Candyriya$copySpawn(net.minecraft.world.level.Level world, Entity entityIn) throws Throwable {
        EntityTransformEvent.TransformReason transformReason = Candyriya$transform == null ? EntityTransformEvent.TransformReason.UNKNOWN : Candyriya$transform;
        Candyriya$transform = null;
        if (CraftEventFactory.callEntityTransformEvent((Mob) (Object) this, (LivingEntity) entityIn, transformReason).isCancelled()) {
            return (boolean) DecorationOps.cancel().invoke((Mob) null);
        } else {
            return (boolean) DecorationOps.callsite().invoke(world, entityIn);
        }
    }

    @Inject(method = "convertTo", at = @At("RETURN"))
    private <T extends Mob> void Candyriya$cleanReason(EntityType<T> p_233656_1_, boolean p_233656_2_, CallbackInfoReturnable<T> cir) {
        ((WorldBridge) this.level()).bridge$pushAddEntityReason(null);
        this.Candyriya$transform = null;
    }

    @Inject(method = "convertTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V"))
    private <T extends Mob> void Candyriya$transformCause(EntityType<T> entityType, boolean bl, CallbackInfoReturnable<T> cir) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.TRANSFORMATION);
    }

    public <T extends Mob> T convertTo(EntityType<T> entityType, boolean flag, EntityTransformEvent.TransformReason transformReason, CreatureSpawnEvent.SpawnReason spawnReason) {
        ((WorldBridge) this.level()).bridge$pushAddEntityReason(spawnReason);
        bridge$pushTransformReason(transformReason);
        return this.convertTo(entityType, flag);
    }

    private transient EntityTransformEvent.TransformReason Candyriya$transform;

    @Override
    public void bridge$pushTransformReason(EntityTransformEvent.TransformReason transformReason) {
        this.Candyriya$transform = transformReason;
    }

    @Inject(method = "doHurtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
    private void Candyriya$attackKnockback(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ((LivingEntityBridge) entity).bridge$pushKnockbackCause((Entity) (Object) this, EntityKnockbackEvent.KnockbackCause.ENTITY_ATTACK);
    }

    @Inject(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V"))
    private void Candyriya$naturalDespawn(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Override
    public boolean bridge$isPersistenceRequired() {
        return this.persistenceRequired;
    }

    public void setPersistenceRequired(boolean value) {
        this.persistenceRequired = value;
    }

    @Override
    public void bridge$setPersistenceRequired(boolean value) {
        this.setPersistenceRequired(value);
    }

    @Override
    public boolean bridge$common$animalTameEvent(Player player) {
        return !CraftEventFactory.callEntityTameEvent((Mob) (Object) this, player).isCancelled();
    }
}
