package mekanism.api;

import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.Chemical;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;

/**
 * Provides access to pre-existing tag keys for various functionality that we use tags for.
 *
 * @since 10.6.2
 */
@NothingNullByDefault
public class MekanismAPITags {
    private static final ResourceLocation HIDDEN_RL = ResourceLocation.fromNamespaceAndPath("c", "hidden_from_recipe_viewers");

    /**
     * Tag that holds all chemicals that recipe viewers should not show to users.
     *
     * @since 10.6.8
     */
    public static final TagKey<Chemical> CHEMICAL_HIDDEN_FROM_RECIPE_VIEWERS = TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, HIDDEN_RL);

    /**
     * Chemicals in this tag that are radioactive will not decay inside a Radioactive Waste Barrel.
     *
     * @since 10.6.8
     */
    public static final TagKey<Chemical> WASTE_BARREL_DECAY_BLACKLIST = TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, rl("waste_barrel_decay_blacklist"));

    private MekanismAPITags() {
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MekanismAPI.MEKANISM_MODID, path);
    }

    public static class InfuseTypes {

        private InfuseTypes() {
        }

        /**
         * Represents an infuse type that is equivalent to carbon.
         */
        public static final TagKey<Chemical> CARBON = TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, rl("carbon"));
        /**
         * Represents an infuse type that is equivalent to redstone.
         */
        public static final TagKey<Chemical> REDSTONE = TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, rl("redstone"));
        /**
         * Represents an infuse type that is equivalent to diamond.
         */
        public static final TagKey<Chemical> DIAMOND = TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, rl("diamond"));
        /**
         * Represents an infuse type that is equivalent to refined obsidian.
         */
        public static final TagKey<Chemical> REFINED_OBSIDIAN = TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, rl("refined_obsidian"));
        /**
         * Represents an infuse type that is equivalent to bio.
         */
        public static final TagKey<Chemical> BIO = TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, rl("bio"));
        /**
         * Represents an infuse type that is equivalent to fungi.
         */
        public static final TagKey<Chemical> FUNGI = TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, rl("fungi"));
        /**
         * Represents an infuse type that is equivalent to gold.
         */
        public static final TagKey<Chemical> GOLD = TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, rl("gold"));
        /**
         * Represents an infuse type that is equivalent to tin.
         */
        public static final TagKey<Chemical> TIN = TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, rl("tin"));

    }

    public static class Slurries {

        private Slurries() {
        }

        /**
         * Represents all dirty slurries.
         */
        public static final TagKey<Chemical> DIRTY = tag("dirty");
        /**
         * Represents all clean slurries.
         */
        public static final TagKey<Chemical> CLEAN = tag("clean");

        private static TagKey<Chemical> tag(String name) {
            return TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, rl(name));
        }
    }

    public static class MobEffects {

        private MobEffects() {
        }

        /**
         * Mob effects in this tag, will be skipped when trying to speed up potion effects with a Scuba Mask or the Inhalation Purification Unit.
         */
        public static final TagKey<MobEffect> SPEED_UP_BLACKLIST = tag("speed_up_blacklist");

        private static TagKey<MobEffect> tag(String name) {
            return TagKey.create(Registries.MOB_EFFECT, rl(name));
        }
    }

    public static class DamageTypes {

        private DamageTypes() {
        }

        /**
         * Represents any damage type that is always supported by the MekaSuit.
         */
        public static final TagKey<DamageType> MEKASUIT_ALWAYS_SUPPORTED = tag("mekasuit_always_supported");
        /**
         * Represents any type of damage that can be prevented by the Scuba Mask or the Inhalation Purification Unit.
         */
        public static final TagKey<DamageType> IS_PREVENTABLE_MAGIC = tag("is_preventable_magic");

        private static TagKey<DamageType> tag(String name) {
            return TagKey.create(Registries.DAMAGE_TYPE, rl(name));
        }
    }

    /**
     * @since 10.6.3
     */
    public static class Entities {

        private Entities() {
        }

        /**
         * Represents any entity type that is immune to all Radiation.
         */
        public static final TagKey<EntityType<?>> RADIATION_IMMUNE = commonTag("radiation_immune");
        /**
         * Represents any entity type that is immune to Mekanism Radiation.
         */
        public static final TagKey<EntityType<?>> MEK_RADIATION_IMMUNE = tag("radiation_immune");

        private static TagKey<EntityType<?>> commonTag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", name));
        }

        private static TagKey<EntityType<?>> tag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, rl(name));
        }
    }
}