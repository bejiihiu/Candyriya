package kz.bejiihiu.candyriya.common.mixin.core.server.level;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import kz.bejiihiu.candyriya.common.bridge.bukkit.CraftServerBridge;
import kz.bejiihiu.candyriya.common.bridge.core.entity.EntityBridge;
import kz.bejiihiu.candyriya.common.bridge.core.server.level.ServerPlayerBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.IInventoryBridge;
import kz.bejiihiu.candyriya.common.bridge.core.server.MinecraftServerBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.level.ExplosionBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.level.levelgen.flat.FlatLevelGeneratorSettingsBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.server.ServerChunkProviderBridge;
import kz.bejiihiu.candyriya.common.bridge.core.server.level.ServerLevelBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.level.storage.DerivedLevelDataBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.level.storage.LevelStorageSourceBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.level.saveddata.maps.MapItemSavedDataBridge;
import kz.bejiihiu.candyriya.common.bridge.core.world.level.storage.PrimaryLevelDataBridge;
import kz.bejiihiu.candyriya.common.mixin.core.world.level.LevelMixin;
import kz.bejiihiu.candyriya.common.mod.mixins.annotation.CreateConstructor;
import kz.bejiihiu.candyriya.common.mod.mixins.annotation.ShadowConstructor;
import kz.bejiihiu.candyriya.common.mod.server.ArclightServer;
import kz.bejiihiu.candyriya.common.mod.server.entity.CandyriyaSpawnReason;
import kz.bejiihiu.candyriya.common.mod.server.event.CandyriyaEventFactory;
import kz.bejiihiu.candyriya.common.mod.server.world.LevelPersistentData;
import kz.bejiihiu.candyriya.common.mod.server.world.WorldSymlink;
import kz.bejiihiu.candyriya.common.mod.util.CandyriyaCaptures;
import kz.bejiihiu.candyriya.common.mod.util.DelegateWorldInfo;
import kz.bejiihiu.candyriya.common.mod.util.DistValidate;
import kz.bejiihiu.candyriya.i18n.CandyriyaConfig;
import kz.bejiihiu.candyriya.mixin.Decorate;
import kz.bejiihiu.candyriya.mixin.DecorationOps;
import kz.bejiihiu.candyriya.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.generator.CustomChunkGenerator;
import org.bukkit.craftbukkit.v.generator.CustomWorldChunkManager;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.craftbukkit.v.util.WorldUUID;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spigotmc.SpigotWorldConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends LevelMixin implements ServerLevelBridge {

    // @formatter:off
    @Shadow public abstract boolean addFreshEntity(Entity entityIn);
    @Shadow public abstract boolean addWithUUID(Entity entityIn);
    @Shadow public abstract <T extends ParticleOptions> int sendParticles(T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed);
    @Shadow protected abstract boolean sendParticles(ServerPlayer player, boolean longDistance, double posX, double posY, double posZ, Packet<?> packet);
    @Shadow @Nonnull public abstract MinecraftServer getServer();
    @Shadow @Final private List<ServerPlayer> players;
    @Shadow public abstract ServerChunkCache getChunkSource();
    @Shadow protected abstract void wakeUpAllPlayers();
    @Shadow @Final private ServerChunkCache chunkSource;
    @Shadow @Final public ServerLevelData serverLevelData;
    @Shadow @Final private PersistentEntitySectionManager<Entity> entityManager;
    @Shadow public abstract DimensionDataStorage getDataStorage();
    @Shadow protected abstract void addPlayer(ServerPlayer serverPlayer);
    @Shadow @Nullable public abstract Entity getEntity(int i);
    @Shadow public abstract void sendBlockUpdated(BlockPos blockPos, BlockState blockState, BlockState blockState2, int i);
    @Shadow @javax.annotation.Nullable private EndDragonFight dragonFight;
    // @formatter:on

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    public PrimaryLevelData K; // Stupid CraftBukkit patch.
    public LevelStorageSource.LevelStorageAccess convertable;
    public UUID uuid;
    public ResourceKey<LevelStem> typeKey;

    @Override
    public ResourceKey<LevelStem> getTypeKey() {
        return this.typeKey;
    }

    @ShadowConstructor
    public void Candyriya$constructor(MinecraftServer minecraftServer, Executor backgroundExecutor, LevelStorageSource.LevelStorageAccess levelSave, ServerLevelData worldInfo, ResourceKey<Level> dimension, LevelStem levelStem, ChunkProgressListener statusListener, boolean isDebug, long seed, List<CustomSpawner> specialSpawners, boolean shouldBeTicking, RandomSequences seq) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void Candyriya$constructor(MinecraftServer server, Executor backgroundExecutor, LevelStorageSource.LevelStorageAccess levelSave, PrimaryLevelData worldInfo, ResourceKey<Level> dimension, LevelStem levelStem, ChunkProgressListener statusListener, boolean isDebug, long seed, List<CustomSpawner> specialSpawners, boolean shouldBeTicking, RandomSequences seq, org.bukkit.World.Environment env, org.bukkit.generator.ChunkGenerator gen, org.bukkit.generator.BiomeProvider biomeProvider) {
        var craftBridge = (CraftServerBridge)(Object) ((MinecraftServerBridge) server).bridge$getServer();
        assert craftBridge != null; // Already checked in bridge
        // We have no way but store it somewhere and use a default value
        // in order to avoid having to pass them as arguments.
        craftBridge.bridge$offerEnvironmentCache(worldInfo.getLevelName(), env);
        craftBridge.bridge$offerGeneratorCache(worldInfo.getLevelName(), gen);
        craftBridge.bridge$offerBiomeProviderCache(worldInfo.getLevelName(), biomeProvider);
        Candyriya$constructor(server, backgroundExecutor, levelSave, worldInfo, dimension, levelStem, statusListener, isDebug, seed, specialSpawners, shouldBeTicking, seq);
    }

    // Support custom chunk generator; in consistency with CraftBukkit
    // The real part is inside ServerChunkCache, when initializing ChunkMap (in ctor).
    // A generator state is created, which is later used for chunk generation.
    // Previously we didn't modify it before ChunkMap is created,
    // which in turn cause custom world generation from Bukkit failing to work.
    @Decorate(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/LevelStem;generator()Lnet/minecraft/world/level/chunk/ChunkGenerator;"))
    private ChunkGenerator Candyriya$initChunkGenerator(LevelStem instance, @Local(ordinal = -1) MinecraftServer server, @Local(ordinal = -1) ServerLevelData worldInfo) throws Throwable {
        // Pulling up world info init since level info is used when selecting ChunkGenerator.
        if (Candyriya$isActual() && worldInfo instanceof PrimaryLevelData primary) {
            this.K = primary;
        } else {
            // damn spigot again
            this.K = DelegateWorldInfo.wrap(worldInfo);
        }

        if (Candyriya$isActual()) {
            var craftBridge = (CraftServerBridge) (Object) ((MinecraftServerBridge) server).bridge$getServer();

            this.biomeProvider = craftBridge.bridge$consumeBiomeProviderCache(worldInfo.getLevelName());
            this.generator = craftBridge.bridge$consumeGeneratorCache(worldInfo.getLevelName());
            this.environment = craftBridge.bridge$consumeEnvironmentCache(worldInfo.getLevelName());

            if (this.environment == null) {
                // Select world environment for vanilla/mod world creation
                if (instance.type().is(LevelStem.OVERWORLD.location())) {
                    this.environment = World.Environment.NORMAL;
                } else if (instance.type().is(LevelStem.NETHER.location())) {
                    this.environment = World.Environment.NETHER;
                } else if (instance.type().is(LevelStem.END.location())) {
                    this.environment = World.Environment.THE_END;
                } else {
                    // Don't use CUSTOM; it's not even supported in Multiverse
                    // this.environment = World.Environment.CUSTOM;
                    this.environment = World.Environment.NORMAL;
                }
            }

            // Now we create the CraftWorld
            this.world = new CraftWorld((ServerLevel) (Object) this, generator, biomeProvider, environment);
        }

        ChunkGenerator raw = (ChunkGenerator) DecorationOps.callsite().invoke(instance);
        if (Candyriya$isActual()) {
            // Data needed by getWorld() are all initialized for possible creating CraftWorld.
            // CraftBukkit start: select custom chunk generator
            if (biomeProvider != null) {
                BiomeSource biomeSource = new CustomWorldChunkManager(getWorld(), biomeProvider, getServer().registryAccess().registryOrThrow(Registries.BIOME));
                if (raw instanceof NoiseBasedChunkGenerator noise) {
                    raw = new NoiseBasedChunkGenerator(biomeSource, noise.settings);
                } else if (raw instanceof FlatLevelSource flat) {
                    raw = new FlatLevelSource(((FlatLevelGeneratorSettingsBridge) flat.settings()).bridge$withBiomeSource(biomeSource));
                } else {
                    ArclightServer.LOGGER.warn("Level {} has unknown customized generator -- requested biome provider won't be satisfied.", this.serverLevelData.getLevelName());
                }
            }
            if (generator != null) {
                raw = new CustomChunkGenerator((ServerLevel) (Object) this, raw, generator);
            }
            // CraftBukkit end
        }
        return raw;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorldData()Lnet/minecraft/world/level/storage/WorldData;"))
    private WorldData Candyriya$useRespective(MinecraftServer server) {
        return K;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void Candyriya$init(MinecraftServer minecraftServer, Executor backgroundExecutor, LevelStorageSource.LevelStorageAccess levelSave, ServerLevelData worldInfo, ResourceKey<Level> dimension, LevelStem levelStem, ChunkProgressListener statusListener, boolean isDebug, long seed, List<CustomSpawner> specialSpawners, boolean shouldBeTicking, RandomSequences seq, CallbackInfo ci) {
        this.pvpMode = minecraftServer.isPvpAllowed();
        this.convertable = levelSave;
        if (Candyriya$isActual() && this.dragonFight == null && this.environment == World.Environment.THE_END) {
            this.dragonFight = new EndDragonFight((ServerLevel)(Object) this, K.worldGenOptions().seed(), K.endDragonFightData());
        }
        var typeKey = ((LevelStorageSourceBridge.LevelStorageAccessBridge) levelSave).bridge$getTypeKey();
        if (typeKey != null) {
            this.typeKey = typeKey;
        } else {
            var dimensions = getServer().registryAccess().registryOrThrow(Registries.LEVEL_STEM);
            var key = dimensions.getResourceKey(levelStem);
            if (key.isPresent()) {
                this.typeKey = key.get();
            } else {
                ArclightServer.LOGGER.warn("Assign {} to unknown level stem {}", dimension.location(), levelStem);
                this.typeKey = ResourceKey.create(Registries.LEVEL_STEM, dimension.location());
            }
            if (worldInfo instanceof DerivedLevelData data) {
                ((DerivedLevelDataBridge) worldInfo).bridge$setDimType(this.getTypeKey());
                if (CandyriyaConfig.spec().getCompat().isSymlinkWorld()) {
                    WorldSymlink.create(data, levelSave.getDimensionPath(this.dimension()).toFile());
                }
            }
        }
        this.spigotConfig = new SpigotWorldConfig(worldInfo.getLevelName());
        this.uuid = WorldUUID.getUUID(levelSave.getDimensionPath(this.dimension()).toFile());
        ((ServerChunkProviderBridge) this.chunkSource).bridge$setViewDistance(spigotConfig.viewDistance);
        ((ServerChunkProviderBridge) this.chunkSource).bridge$setSimulationDistance(spigotConfig.simulationDistance);
        if (Candyriya$isActual()) {
            ((PrimaryLevelDataBridge) this.K).bridge$setWorld((ServerLevel) (Object) this);
            var data = this.getDataStorage().computeIfAbsent(LevelPersistentData.factory(), "bukkit_pdc");
            this.getWorld().readBukkitValues(data.getTag());
            this.getCraftServer().addWorld(this.getWorld());
        }
    }

    @Inject(method = "saveLevelData", at = @At("RETURN"))
    private void Candyriya$savePdc(CallbackInfo ci) {
        var data = this.getDataStorage().computeIfAbsent(LevelPersistentData.factory(), "bukkit_pdc");
        data.save(this.world);
    }

    @Inject(method = "gameEvent", cancellable = true, at = @At("HEAD"))
    private void Candyriya$gameEventEvent(Holder<GameEvent> holder, Vec3 pos, GameEvent.Context context, CallbackInfo ci) {
        var entity = context.sourceEntity();
        var i = holder.value().notificationRadius();
        GenericGameEvent event = new GenericGameEvent(org.bukkit.GameEvent.getByKey(CraftNamespacedKey.fromMinecraft(BuiltInRegistries.GAME_EVENT.getKey(holder.value()))), new Location(this.getWorld(), pos.x(), pos.y(), pos.z()), (entity == null) ? null : entity.bridge$getBukkitEntity(), i, !Bukkit.isPrimaryThread());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    public LevelChunk getChunkIfLoaded(int x, int z) {
        return this.chunkSource.getChunk(x, z, false);
    }

    public <T extends ParticleOptions> int sendParticles(final ServerPlayer sender, final T t0, final double d0, final double d1, final double d2, final int i, final double d3, final double d4, final double d5, final double d6, final boolean force) {
        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(t0, force, d0, d1, d2, (float) d3, (float) d4, (float) d5, (float) d6, i);
        int j = 0;
        for (ServerPlayer entity : this.players) {
            if (sender == null || ((ServerPlayerBridge) entity).bridge$getBukkitEntity().canSee(((ServerPlayerBridge) sender).bridge$getBukkitEntity())) {
                if (this.sendParticles(entity, force, d0, d1, d2, packet)) {
                    ++j;
                }
            }
        }
        return j;
    }

    @Override
    public LevelStorageSource.LevelStorageAccess bridge$getConvertable() {
        return this.convertable;
    }

    @Inject(method = "tickNonPassenger", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void Candyriya$tickPortal(Entity entityIn, CallbackInfo ci) {
        ((EntityBridge) entityIn).bridge$postTick();
    }

    @Inject(method = "tickPassenger", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private void Candyriya$tickPortalPassenger(Entity ridingEntity, Entity passengerEntity, CallbackInfo ci) {
        ((EntityBridge) passengerEntity).bridge$postTick();
    }

    @Decorate(method = "tickChunk", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int Candyriya$modifyThunderChance(RandomSource instance, int i) throws Throwable {
        return (int) DecorationOps.callsite().invoke(instance, i == 100000 ? spigotConfig.thunderChance : i);
    }

    @Inject(method = "tickChunk", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void Candyriya$spawnReasonForSkeletonHorse(LevelChunk levelChunk, int i, CallbackInfo ci) {
        bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.LIGHTNING);
    }

    @Decorate(method = "tickChunk", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean Candyriya$spawnReasonForLightning(ServerLevel instance, Entity entity) throws Throwable {
        if (DistValidate.isValid(this)) {
            LightningStrikeEvent lightning = CraftEventFactory.callLightningStrikeEvent((LightningStrike) entity.bridge$getBukkitEntity(), LightningStrikeEvent.Cause.WEATHER);
            if (lightning.isCancelled()) {
                return false;
            }
        }
        return (boolean) DecorationOps.callsite().invoke(instance, entity);
    }

    public boolean strikeLightning(Entity entity) {
        return this.strikeLightning(entity, LightningStrikeEvent.Cause.UNKNOWN);
    }

    public boolean strikeLightning(Entity entity, LightningStrikeEvent.Cause cause) {
        if (Candyriya$cause != null) {
            cause = Candyriya$cause;
            Candyriya$cause = null;
        }
        if (DistValidate.isValid(this)) {
            LightningStrikeEvent lightning = CraftEventFactory.callLightningStrikeEvent((LightningStrike) entity.bridge$getBukkitEntity(), cause);
            if (lightning.isCancelled()) {
                return false;
            }
        }
        return this.addFreshEntity(entity);
    }

    @Decorate(method = "tickPrecipitation", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    public boolean Candyriya$snowForm(ServerLevel level, BlockPos pos, BlockState newState) throws Throwable {
        final var event = CandyriyaEventFactory.callBlockFormEvent(level, pos, newState, 3, null);
        if (event != null) {
            if (event.isCancelled()) {
                return false;
            }
            newState = ((CraftBlockState) event.getNewState()).getHandle();
        }
        return (boolean) DecorationOps.callsite().invoke(level, pos, newState);
    }

    //TODO: weather cycle for every player

    @Inject(method = "save", at = @At(value = "JUMP", ordinal = 0, opcode = Opcodes.IFNULL))
    private void Candyriya$worldSaveEvent(ProgressListener progress, boolean flush, boolean skipSave, CallbackInfo ci) {
        if (DistValidate.isValid(this)) {
            Bukkit.getPluginManager().callEvent(new WorldSaveEvent(getWorld()));
        }
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void Candyriya$saveLevelDat(ProgressListener progress, boolean flush, boolean skipSave, CallbackInfo ci) {
        if (this.serverLevelData instanceof PrimaryLevelData worldInfo) {
            worldInfo.setWorldBorder(this.getWorldBorder().createSettings());
            worldInfo.setCustomBossEvents(this.getServer().getCustomBossEvents().save(this.registryAccess()));
            this.convertable.saveDataTag(this.getServer().registryAccess(), worldInfo, this.getServer().getPlayerList().getSingleplayerData());
        }
    }

    // Multiworld support: use respective world data
    @Decorate(method = {"saveLevelData", "findNearestMapStructure", "isFlat", "getSeed"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorldData()Lnet/minecraft/world/level/storage/WorldData;"))
    private WorldData Candyriya$findNearestMapStructure(MinecraftServer instance) throws Throwable {
        return serverLevelData instanceof PrimaryLevelData primary ? primary : (WorldData) DecorationOps.callsite().invoke(instance);
    }

    @Inject(method = "unload", at = @At("HEAD"))
    public void Candyriya$closeOnChunkUnloading(LevelChunk chunkIn, CallbackInfo ci) {
        for (BlockEntity tileentity : chunkIn.getBlockEntities().values()) {
            if (tileentity instanceof Container) {
                for (HumanEntity h : Lists.newArrayList(((IInventoryBridge) tileentity).getViewers())) {
                    if (h instanceof CraftHumanEntity) {
                        ((CraftHumanEntity) h).getHandle().closeContainer();
                    }
                }
            }
        }
    }

    private transient boolean Candyriya$force = false;

    @Decorate(method = "sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z"))
    public boolean Candyriya$particleVisible(ServerLevel serverWorld, ServerPlayer player, boolean longDistance, double posX, double posY, double posZ, Packet<?> packet) throws Throwable {
        try {
            return (boolean) DecorationOps.callsite().invoke(serverWorld, player, Candyriya$force, posX, posY, posZ, packet);
        } finally {
            Candyriya$force = false;
        }
    }

    public <T extends ParticleOptions> int sendParticles(T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed, boolean force) {
        Candyriya$force = force;
        return this.sendParticles(type, posX, posY, posZ, particleCount, xOffset, yOffset, zOffset, speed);
    }

    @Override
    public <T extends ParticleOptions> int bridge$sendParticles(T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed, boolean force) {
        return this.sendParticles(type, posX, posY, posZ, particleCount, xOffset, yOffset, zOffset, speed, force);
    }

    private transient LightningStrikeEvent.Cause Candyriya$cause;

    @Override
    public void bridge$pushStrikeLightningCause(LightningStrikeEvent.Cause cause) {
        this.Candyriya$cause = cause;
    }

    @Override
    public void bridge$strikeLightning(LightningBolt entity, LightningStrikeEvent.Cause cause) {
        strikeLightning(entity, cause);
    }

    private transient CreatureSpawnEvent.SpawnReason Candyriya$reason;
    // Fix for SPIGOT-6415
    private transient CandyriyaSpawnReason Candyriya$extendedReason;

    @Inject(method = "addEntity", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;addNewEntity(Lnet/minecraft/world/level/entity/EntityAccess;)Z"))
    private void Candyriya$addEntityEvent(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        // SPIGOT-6415: Don't call spawn event when reason is null.
        final var reason = Candyriya$reason;
        Candyriya$reason = null;
        if (Candyriya$extendedReason == CandyriyaSpawnReason.TELEPORT && reason == null) {
            return;
        }
        CreatureSpawnEvent.SpawnReason spawnReason = reason == null ? CreatureSpawnEvent.SpawnReason.DEFAULT : reason;
        if (!Candyriya$isActual()) {
            return;
        }
        ((EntityBridge) entityIn).Candyriya$onAddedToLevel();
        if (!CraftEventFactory.doEntityAddEventCalling((ServerLevel) (Object) this, entityIn, spawnReason)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "addEntity", at = @At("RETURN"))
    public void Candyriya$resetReason(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        Candyriya$reason = null;
    }

    @Override
    public void bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason) {
        this.Candyriya$reason = reason;
    }

    @Override
    public CreatureSpawnEvent.SpawnReason bridge$getAddEntityReason() {
        return this.Candyriya$reason;
    }

    public boolean addFreshEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        bridge$pushAddEntityReason(reason);
        return addFreshEntity(entity);
    }

    @Override
    public boolean bridge$addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return addFreshEntity(entity, reason);
    }

    public boolean addWithUUID(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        bridge$pushAddEntityReason(reason);
        return addWithUUID(entity);
    }

    @Inject(method = "addDuringTeleport", at = @At("HEAD"))
    private void Candyriya$ignoreSpawnOnTeleport(Entity entity, CallbackInfo ci) {
        Candyriya$extendedReason = CandyriyaSpawnReason.TELEPORT;
    }

    @Inject(method = "addDuringTeleport", at = @At("RETURN"))
    private void Candyriya$unsetIgnoreSpawnOnTeleport(Entity entity, CallbackInfo ci) {
        Candyriya$extendedReason = null;
    }

    public void addDuringTeleport(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        if (entity instanceof ServerPlayer player) {
            this.addPlayer(player);
        } else if (reason != null) {
            this.addFreshEntity(entity, reason);
        } else {
            Candyriya$extendedReason = CandyriyaSpawnReason.TELEPORT;
            addFreshEntity(entity);
            Candyriya$extendedReason = null;
        }
    }

    @Override
    public boolean bridge$addEntitySerialized(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return addWithUUID(entity, reason);
    }

    public boolean tryAddFreshEntityWithPassengers(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        if (entity.getSelfAndPassengers().map(Entity::getUUID).anyMatch(this.entityManager::isLoaded)) {
            return false;
        }
        return this.bridge$addAllEntities(entity, reason);
    }

    @Override
    public boolean bridge$addAllEntitiesSafely(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return tryAddFreshEntityWithPassengers(entity, reason);
    }

    @Decorate(method = "destroyBlockProgress", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private Iterator<ServerPlayer> Candyriya$limitBreakVisibility(List<ServerPlayer> instance, @Local(ordinal = 0) int id) throws Throwable {
        final var raw = (Iterator<ServerPlayer>) DecorationOps.callsite().invoke(instance);
        final var actor = getEntity(id);
        if (!(actor instanceof ServerPlayerBridge player)) {
            return raw;
        }
        return Iterators.filter(raw, it -> it != null && ((ServerPlayerBridge)it).bridge$getBukkitEntity().canSee(player.bridge$getBukkitEntity()));
    }

    @Decorate(method = "sendBlockUpdated", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;"))
    private Object Candyriya$guardConcurrencyOnNavigation(Iterator<PathNavigation> instance, @Local(ordinal = 0) BlockPos pos, @Local(ordinal = 0) BlockState before, @Local(ordinal = 1) BlockState after, @Local(ordinal = 0) int i) throws Throwable {
        try {
            return DecorationOps.callsite().invoke(instance);
        } catch (ConcurrentModificationException ignored) {
            sendBlockUpdated(pos,  before, after, i);
            return DecorationOps.cancel().invoke();
        }
    }

    @Decorate(method = "explode", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Explosion;interactsWithBlocks()Z"))
    private void Candyriya$doExplosion(@Local(ordinal = -1) Explosion explosion) throws Throwable {
        if (((ExplosionBridge) explosion).bridge$wasCancelled()) {
            DecorationOps.cancel().invoke(explosion);
            return;
        }
        DecorationOps.blackhole().invoke();
    }

    @Inject(method = "getMapData", at = @At("RETURN"))
    private void Candyriya$mapSetId(MapId id, CallbackInfoReturnable<MapItemSavedData> cir) {
        var data = cir.getReturnValue();
        if (data != null) {
            ((MapItemSavedDataBridge) data).bridge$setId(id);
        }
    }

    @Inject(method = "setMapData", at = @At("HEAD"))
    private void Candyriya$mapSetId(MapId id, MapItemSavedData data, CallbackInfo ci) {
        ((MapItemSavedDataBridge) data).bridge$setId(id);
        MapInitializeEvent event = new MapInitializeEvent(((MapItemSavedDataBridge) data).bridge$getMapView());
        Bukkit.getServer().getPluginManager().callEvent(event);
    }

    @Inject(method = "blockUpdated", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V"))
    private void Candyriya$returnIfPopulate(BlockPos pos, Block block, CallbackInfo ci) {
        if (populating) {
            ci.cancel();
        }
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos, boolean validate) {
        return this.getBlockEntity(pos);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"))
    private void Candyriya$timeSkip(ServerLevel world, long time) {
        TimeSkipEvent event = new TimeSkipEvent(this.getWorld(), TimeSkipEvent.SkipReason.NIGHT_SKIP, (time - time % 24000L) - this.getDayTime());
        Bukkit.getPluginManager().callEvent(event);
        Candyriya$timeSkipCancelled = event.isCancelled();
        if (!event.isCancelled()) {
            world.setDayTime(this.getDayTime() + event.getSkipAmount());
        }
    }

    private transient boolean Candyriya$timeSkipCancelled;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;wakeUpAllPlayers()V"))
    private void Candyriya$notWakeIfCancelled(ServerLevel world) {
        if (!Candyriya$timeSkipCancelled) {
            this.wakeUpAllPlayers();
        }
        Candyriya$timeSkipCancelled = false;
    }

    @Override
    public ServerLevel bridge$getMinecraftWorld() {
        return (ServerLevel) (Object) this;
    }

    @ModifyVariable(method = "tickBlock", ordinal = 0, argsOnly = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private BlockPos Candyriya$captureTickingBlock(BlockPos pos) {
        CandyriyaCaptures.captureTickingBlock((ServerLevel) (Object) this, pos);
        return pos;
    }

    @ModifyVariable(method = "tickBlock", ordinal = 0, argsOnly = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/block/state/BlockState;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private BlockPos Candyriya$resetTickingBlock(BlockPos pos) {
        CandyriyaCaptures.resetTickingBlock();
        return pos;
    }

    @ModifyVariable(method = "tickChunk", ordinal = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private BlockPos Candyriya$captureRandomTick(BlockPos pos) {
        CandyriyaCaptures.captureTickingBlock((ServerLevel) (Object) this, pos);
        return pos;
    }

    @ModifyVariable(method = "tickChunk", ordinal = 0, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private BlockPos Candyriya$resetRandomTick(BlockPos pos) {
        CandyriyaCaptures.resetTickingBlock();
        return pos;
    }

    @ModifyVariable(method = "tickNonPassenger", argsOnly = true, ordinal = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private Entity Candyriya$captureTickingEntity(Entity entity) {
        CandyriyaCaptures.captureTickingEntity(entity);
        return entity;
    }

    @ModifyVariable(method = "tickNonPassenger", argsOnly = true, ordinal = 0, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private Entity Candyriya$resetTickingEntity(Entity entity) {
        CandyriyaCaptures.resetTickingEntity();
        return entity;
    }

    @ModifyVariable(method = "tickPassenger", argsOnly = true, ordinal = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private Entity Candyriya$captureTickingPassenger(Entity entity) {
        CandyriyaCaptures.captureTickingEntity(entity);
        return entity;
    }

    @ModifyVariable(method = "tickPassenger", argsOnly = true, ordinal = 1, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private Entity Candyriya$resetTickingPassenger(Entity entity) {
        CandyriyaCaptures.resetTickingEntity();
        return entity;
    }
}
