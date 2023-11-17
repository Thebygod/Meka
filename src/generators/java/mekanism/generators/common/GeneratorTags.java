package mekanism.generators.common;

import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.common.Mekanism;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public class GeneratorTags {

    private GeneratorTags() {
    }

    public static class Fluids {

        private Fluids() {
        }

        public static final TagKey<Fluid> BIOETHANOL = forgeTag("bioethanol");
        public static final TagKey<Fluid> DEUTERIUM = forgeTag("deuterium");
        public static final TagKey<Fluid> FUSION_FUEL = forgeTag("fusion_fuel");
        public static final TagKey<Fluid> TRITIUM = forgeTag("tritium");

        private static TagKey<Fluid> forgeTag(String name) {
            return FluidTags.create(new ResourceLocation("forge", name));
        }
    }

    public static class Gases {

        private Gases() {
        }

        public static final TagKey<Gas> DEUTERIUM = tag("deuterium");
        public static final TagKey<Gas> TRITIUM = tag("tritium");
        public static final TagKey<Gas> FUSION_FUEL = tag("fusion_fuel");

        private static TagKey<Gas> tag(String name) {
            return TagKey.create(MekanismAPI.GAS_REGISTRY_NAME, Mekanism.rl(name));
        }
    }
}