package com.mrfuzzihead.vinery.core.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

import com.mrfuzzihead.vinery.core.registry.EntityTypeRegistry;

public class DarkCherryChestBoatEntity extends ChestBoat {

    private static final EntityDataAccessor<Integer> WOOD_TYPE = SynchedEntityData
        .defineId(DarkCherryChestBoatEntity.class, EntityDataSerializers.INT);

    public DarkCherryChestBoatEntity(EntityType<? extends Boat> entityType, Level level) {
        super(entityType, level);
    }

    public DarkCherryChestBoatEntity(Level level, double x, double y, double z) {
        this(EntityTypeRegistry.DARK_CHERRY_CHEST_BOAT.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WOOD_TYPE, DarkCherryBoatEntity.Type.DARK_CHERRY.ordinal());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        if (pCompound.contains("Type", 8)) {
            this.setWoodType(DarkCherryBoatEntity.Type.byName(pCompound.getString("Type")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putString(
            "Type",
            this.getWoodType()
                .getName());
    }

    public DarkCherryBoatEntity.Type getWoodType() {
        return DarkCherryBoatEntity.Type.byId(this.entityData.get(WOOD_TYPE));
    }

    public void setWoodType(DarkCherryBoatEntity.Type type) {
        this.entityData.set(WOOD_TYPE, type.ordinal());
    }

    public DarkCherryBoatEntity.Type getModVariant() {
        return DarkCherryBoatEntity.Type.byId(this.entityData.get(WOOD_TYPE));
    }

    @Override
    public @NotNull Item getDropItem() {
        return this.getWoodType()
            .getItem()
            .get();
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
        return new ClientboundAddEntityPacket(this, entity);
    }
}
