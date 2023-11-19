package mekanism.additions.common.config;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mekanism.additions.common.entity.baby.BabyType;
import mekanism.additions.common.registries.AdditionsEntityTypes;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.config.BaseMekanismConfig;
import mekanism.common.config.IMekanismConfig;
import mekanism.common.config.value.CachedBooleanValue;
import mekanism.common.config.value.CachedDoubleValue;
import mekanism.common.config.value.CachedFloatValue;
import mekanism.common.config.value.CachedIntValue;
import mekanism.common.config.value.CachedResourceLocationListValue;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec;

public class AdditionsConfig extends BaseMekanismConfig {

    private final ModConfigSpec configSpec;

    public final CachedIntValue obsidianTNTDelay;
    public final CachedFloatValue obsidianTNTBlastRadius;
    public final CachedBooleanValue voiceServerEnabled;
    public final CachedIntValue voicePort;
    private final Map<BabyType, SpawnConfig> spawnConfigs = new EnumMap<>(BabyType.class);

    AdditionsConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Mekanism Additions Config. This config is synced between server and client.").push("additions");

        obsidianTNTDelay = CachedIntValue.wrap(this, builder.comment("Fuse time for Obsidian TNT.")
              .defineInRange("obsidianTNTDelay", 100, 0, Integer.MAX_VALUE));
        obsidianTNTBlastRadius = CachedFloatValue.wrap(this, builder.comment("Radius of the explosion of Obsidian TNT.")
              .defineInRange("obsidianTNTBlastRadius", 12, 0.1, 1_000));

        voiceServerEnabled = CachedBooleanValue.wrap(this, builder.comment("Enables the voice server for Walkie Talkies.").worldRestart()
              .define("voiceServerEnabled", false));
        voicePort = CachedIntValue.wrap(this, builder.comment("TCP port for the Voice server to listen on.")
              .defineInRange("VoicePort", 36_123, 1, 65_535));

        builder.comment("Config options regarding spawning of entities.").push("spawning");
        addBabyTypeConfig(BabyType.CREEPER, builder, AdditionsEntityTypes.BABY_CREEPER, EntityType.CREEPER);
        addBabyTypeConfig(BabyType.ENDERMAN, builder, AdditionsEntityTypes.BABY_ENDERMAN, EntityType.ENDERMAN);
        addBabyTypeConfig(BabyType.SKELETON, builder, AdditionsEntityTypes.BABY_SKELETON, EntityType.SKELETON);
        addBabyTypeConfig(BabyType.STRAY, builder, AdditionsEntityTypes.BABY_STRAY, EntityType.STRAY);
        addBabyTypeConfig(BabyType.WITHER_SKELETON, builder, AdditionsEntityTypes.BABY_WITHER_SKELETON, EntityType.WITHER_SKELETON);
        builder.pop(2);
        configSpec = builder.build();
    }

    private void addBabyTypeConfig(BabyType type, ModConfigSpec.Builder builder, Holder<EntityType<?>> entityTypeProvider, EntityType<?> parentType) {
        spawnConfigs.put(type, new SpawnConfig(this, builder, "baby " + type.getSerializedName().replace('_', ' '),
              entityTypeProvider, parentType));
    }

    @Override
    public String getFileName() {
        return "additions";
    }

    @Override
    public ModConfigSpec getConfigSpec() {
        return configSpec;
    }

    @Override
    public Type getConfigType() {
        return Type.SERVER;
    }

    public SpawnConfig getConfig(BabyType babyType) {
        return spawnConfigs.get(babyType);
    }

    public static class SpawnConfig {

        public final CachedBooleanValue shouldSpawn;
        public final CachedDoubleValue weightPercentage;
        public final CachedDoubleValue minSizePercentage;
        public final CachedDoubleValue maxSizePercentage;
        public final CachedDoubleValue spawnCostPerEntityPercentage;
        public final CachedDoubleValue maxSpawnCostPercentage;
        public final CachedResourceLocationListValue biomeBlackList;
        public final CachedResourceLocationListValue structureBlackList;
        public final Holder<EntityType<?>> entityType;
        public final EntityType<?> parentType;

        private SpawnConfig(IMekanismConfig config, ModConfigSpec.Builder builder, String name, Holder<EntityType<?>> entityType, EntityType<?> parentType) {
            this.entityType = entityType;
            this.parentType = parentType;
            builder.comment("Config options regarding " + name + ".").push(name.replace(" ", "-"));
            this.shouldSpawn = CachedBooleanValue.wrap(config, builder.comment("Enable the spawning of " + name + ". Think baby zombies.")
                  .worldRestart()
                  .define("shouldSpawn", true));
            this.weightPercentage = CachedDoubleValue.wrap(config, builder.comment("The multiplier for weight of " + name + " spawns, compared to the adult mob.")
                  .worldRestart()
                  .defineInRange("weightPercentage", 0.5, 0, 100));
            this.minSizePercentage = CachedDoubleValue.wrap(config, builder.comment("The multiplier for minimum group size of " + name + " spawns, compared to the adult mob.")
                  .worldRestart()
                  .defineInRange("minSizePercentage", 0.5, 0, 100));
            this.maxSizePercentage = CachedDoubleValue.wrap(config, builder.comment("The multiplier for maximum group size of " + name + " spawns, compared to the adult mob.")
                  .worldRestart()
                  .defineInRange("maxSizePercentage", 0.5, 0, 100));
            this.spawnCostPerEntityPercentage = CachedDoubleValue.wrap(config, builder.comment("The multiplier for spawn cost per entity of " + name + " spawns, compared to the adult mob.")
                  .worldRestart()
                  .defineInRange("spawnCostPerEntityPercentage", 1D, 0, 100));
            this.maxSpawnCostPercentage = CachedDoubleValue.wrap(config, builder.comment("The multiplier for max spawn cost of " + name + " spawns, compared to the adult mob.")
                  .worldRestart()
                  .defineInRange("maxSpawnCostPercentage", 1D, 0, 100));
            this.biomeBlackList = CachedResourceLocationListValue.define(config, builder.comment("The list of biome ids that " + name + " will not spawn in even if the normal mob variant can spawn.")
                  .worldRestart(), "biomeBlackList", ConstantPredicates.alwaysTrue());
            this.structureBlackList = CachedResourceLocationListValue.define(config, builder.comment("The list of structure ids that " + name + " will not spawn in even if the normal mob variant can spawn.")
                  .worldRestart(), "structureBlackList", BuiltInRegistries.STRUCTURE_TYPE::containsKey);
            builder.pop();
        }

        public MobSpawnSettings.SpawnerData getSpawner(MobSpawnSettings.SpawnerData parentEntry) {
            int weight = (int) Math.ceil(parentEntry.getWeight().asInt() * weightPercentage.get());
            int minSize = (int) Math.ceil(parentEntry.minCount * minSizePercentage.get());
            int maxSize = (int) Math.ceil(parentEntry.maxCount * maxSizePercentage.get());
            return new MobSpawnSettings.SpawnerData(entityType.value(), weight, minSize, Math.max(minSize, maxSize));
        }

        public List<MobSpawnSettings.SpawnerData> getSpawnersToAdd(List<MobSpawnSettings.SpawnerData> monsterSpawns) {
            //If the adult mob can spawn let the baby mob spawn as well
            //Note: We adjust the mob's spawning based on the adult's spawn rates
            return monsterSpawns.stream()
                  .filter(monsterSpawn -> monsterSpawn.type == parentType)
                  .map(this::getSpawner)
                  .toList();
        }
    }
}