package mekanism.common.advancements;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import mekanism.api.datagen.recipe.RecipeCriterion;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ExtendedAdvancementBuilder {

    private final Advancement.Builder internal = Advancement.Builder.advancement();
    private final MekanismAdvancement advancement;

    private ExtendedAdvancementBuilder(MekanismAdvancement advancement) {
        this.advancement = advancement;
    }

    public static ExtendedAdvancementBuilder advancement(MekanismAdvancement advancement) {
        return new ExtendedAdvancementBuilder(advancement);
    }

    public ExtendedAdvancementBuilder parent(Advancement parent) {
        internal.parent(parent);
        return this;
    }

    public ExtendedAdvancementBuilder parent(ResourceLocation parentId) {
        internal.parent(parentId);
        return this;
    }

    public ExtendedAdvancementBuilder display(ItemStack stack, @Nullable ResourceLocation background, FrameType frame, boolean showToast, boolean announceToChat,
          boolean hidden) {
        internal.display(stack, advancement.translateTitle(), advancement.translateDescription(), background, frame, showToast, announceToChat, hidden);
        return this;
    }

    public ExtendedAdvancementBuilder display(ItemLike item, @Nullable ResourceLocation background, FrameType frame, boolean showToast, boolean announceToChat,
          boolean hidden) {
        return display(new ItemStack(item), background, frame, showToast, announceToChat, hidden);
    }

    public ExtendedAdvancementBuilder display(ItemLike item, FrameType frame) {
        return display(item, null, frame, true, true, false);
    }

    public ExtendedAdvancementBuilder display(DisplayInfo display) {
        return runInternal(builder -> builder.display(display));
    }

    public ExtendedAdvancementBuilder rewards(AdvancementRewards.Builder rewardsBuilder) {
        return runInternal(builder -> builder.rewards(rewardsBuilder));
    }

    public ExtendedAdvancementBuilder rewards(AdvancementRewards rewards) {
        return runInternal(builder -> builder.rewards(rewards));
    }

    public ExtendedAdvancementBuilder addCriterion(RecipeCriterion criterion) {
        return addCriterion(criterion.name(), criterion.criterion());
    }

    public ExtendedAdvancementBuilder addCriterion(String key, CriterionTriggerInstance criterion) {
        return runInternal(builder -> builder.addCriterion(key, criterion));
    }

    public ExtendedAdvancementBuilder addCriterion(String key, Criterion criterion) {
        return runInternal(builder -> builder.addCriterion(key, criterion));
    }

    public ExtendedAdvancementBuilder orRequirements() {
        return requirements(RequirementsStrategy.OR);
    }

    public ExtendedAdvancementBuilder requirements(RequirementsStrategy strategy) {
        return runInternal(builder -> builder.requirements(strategy));
    }

    public ExtendedAdvancementBuilder requirements(String[][] requirements) {
        return runInternal(builder -> builder.requirements(requirements));
    }

    private ExtendedAdvancementBuilder runInternal(Consumer<Advancement.Builder> consumer) {
        consumer.accept(internal);
        return this;
    }

    public Advancement save(Consumer<Advancement> consumer, ExistingFileHelper existingFileHelper) {
        return internal.save(consumer, advancement.path(), existingFileHelper);
    }
}