package mekanism.common.advancements.triggers;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Arrays;
import java.util.Locale;
import javax.annotation.Nonnull;
import mekanism.api.JsonConstants;
import mekanism.common.advancements.MekanismCriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringRepresentable;

public class UseGaugeDropperTrigger extends SimpleCriterionTrigger<UseGaugeDropperTrigger.TriggerInstance> {

    private final ResourceLocation id;

    public UseGaugeDropperTrigger(ResourceLocation id) {
        this.id = id;
    }

    @Nonnull
    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Nonnull
    @Override
    protected TriggerInstance createInstance(@Nonnull JsonObject json, @Nonnull EntityPredicate.Composite playerPredicate, @Nonnull DeserializationContext context) {
        String actionName = GsonHelper.getAsString(json, JsonConstants.ACTION);
        UseDropperAction action = Arrays.stream(UseDropperAction.ACTIONS)
              .filter(a -> a.getSerializedName().equals(actionName))
              .findFirst()
              .orElseThrow(() -> new JsonSyntaxException("Unknown dropper use action: " + actionName));
        return new TriggerInstance(playerPredicate, action);
    }

    public void trigger(ServerPlayer player, UseDropperAction action) {
        this.trigger(player, instance -> instance.action == UseDropperAction.ANY || instance.action == action);
    }

    public enum UseDropperAction implements StringRepresentable {
        ANY,
        FILL,
        DRAIN,
        DUMP;

        //Do not modify
        private static final UseDropperAction[] ACTIONS = values();

        @Nonnull
        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {

        private final UseDropperAction action;

        public TriggerInstance(EntityPredicate.Composite playerPredicate, UseDropperAction action) {
            super(MekanismCriteriaTriggers.USE_GAUGE_DROPPER.getId(), playerPredicate);
            this.action = action;
        }

        @Nonnull
        @Override
        public JsonObject serializeToJson(@Nonnull SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.addProperty(JsonConstants.ACTION, action.getSerializedName());
            return json;
        }

        public static UseGaugeDropperTrigger.TriggerInstance any() {
            return new UseGaugeDropperTrigger.TriggerInstance(EntityPredicate.Composite.ANY, UseDropperAction.ANY);
        }
    }
}