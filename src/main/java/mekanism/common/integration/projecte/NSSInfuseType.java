package mekanism.common.integration.projecte;

import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.providers.IInfuseTypeProvider;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of {@link NormalizedSimpleStack} and {@link moze_intel.projecte.api.nss.NSSTag} for representing {@link InfuseType}s.
 */
public final class NSSInfuseType /*TODO - 1.20.2: ProjectE extends AbstractNSSTag<InfuseType>*/ {

    private NSSInfuseType(@NotNull ResourceLocation resourceLocation, boolean isTag) {
        //super(resourceLocation, isTag);
    }

    /**
     * Helper method to create an {@link NSSInfuseType} representing an infuse type from a {@link InfusionStack}
     */
    @NotNull
    public static NSSInfuseType createInfuseType(@NotNull InfusionStack stack) {
        //Don't bother checking if it is empty as getType returns EMPTY which will then fail anyways for being empty
        return createInfuseType(stack.getType());
    }

    /**
     * Helper method to create an {@link NSSInfuseType} representing an infuse type from an {@link IInfuseTypeProvider}
     */
    @NotNull
    public static NSSInfuseType createInfuseType(@NotNull IInfuseTypeProvider infuseTypeProvider) {
        return createInfuseType(infuseTypeProvider.getChemical());
    }

    /**
     * Helper method to create an {@link NSSInfuseType} representing an infuse type from a {@link InfuseType}
     */
    @NotNull
    public static NSSInfuseType createInfuseType(@NotNull InfuseType infuseType) {
        if (infuseType.isEmptyType()) {
            throw new IllegalArgumentException("Can't make NSSInfuseType with an empty infuse type");
        }
        //This should never be null, or it would have crashed on being registered
        return createInfuseType(infuseType.getRegistryName());
    }

    /**
     * Helper method to create an {@link NSSInfuseType} representing an infuse type from a {@link ResourceLocation}
     */
    @NotNull
    public static NSSInfuseType createInfuseType(@NotNull ResourceLocation infuseTypeID) {
        return new NSSInfuseType(infuseTypeID, false);
    }

    /**
     * Helper method to create an {@link NSSInfuseType} representing a tag from a {@link ResourceLocation}
     */
    @NotNull
    public static NSSInfuseType createTag(@NotNull ResourceLocation tagId) {
        return new NSSInfuseType(tagId, true);
    }

    /**
     * Helper method to create an {@link NSSInfuseType} representing a tag from a {@link TagKey<InfuseType>}
     */
    @NotNull
    public static NSSInfuseType createTag(@NotNull TagKey<InfuseType> tag) {
        return createTag(tag.location());
    }

    /*@Override//TODO - 1.20.2: ProjectE Update
    protected boolean isInstance(AbstractNSSTag o) {
        return o instanceof NSSInfuseType;
    }

    @NotNull
    @Override
    public String getJsonPrefix() {
        return "INFUSE_TYPE|";
    }

    @NotNull
    @Override
    public String getType() {
        return "Infuse Type";
    }

    @NotNull
    @Override
    protected Optional<Either<Named<InfuseType>, HolderSet.Named<InfuseType>>> getTag() {
        return getTag(MekanismAPI.INFUSE_TYPE_REGISTRY);
    }

    @Override
    protected Function<InfuseType, NormalizedSimpleStack> createNew() {
        return NSSInfuseType::createInfuseType;
    }*/
}