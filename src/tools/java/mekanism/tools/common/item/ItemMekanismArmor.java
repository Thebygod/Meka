package mekanism.tools.common.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import mekanism.common.capabilities.ICapabilityAware;
import mekanism.common.config.value.CachedIntValue;
import mekanism.common.lib.attribute.AttributeCache;
import mekanism.common.lib.attribute.IAttributeRefresher;
import mekanism.tools.common.IHasRepairType;
import mekanism.tools.common.integration.gender.ToolsGenderCapabilityHelper;
import mekanism.tools.common.material.MaterialCreator;
import mekanism.tools.common.util.ToolsUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemMekanismArmor extends ArmorItem implements IHasRepairType, IAttributeRefresher, ICapabilityAware {

    private final MaterialCreator material;
    private final AttributeCache attributeCache;

    public ItemMekanismArmor(MaterialCreator material, ArmorItem.Type armorType, Item.Properties properties) {
        super(material, armorType, properties);
        this.material = material;
        CachedIntValue armorConfig = switch (armorType) {
            case BOOTS -> material.bootArmor;
            case LEGGINGS -> material.leggingArmor;
            case CHESTPLATE -> material.chestplateArmor;
            case HELMET -> material.helmetArmor;
        };
        this.attributeCache = new AttributeCache(this, material.toughness, material.knockbackResistance, armorConfig);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level world, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        ToolsUtils.addDurability(tooltip, stack);
    }

    @NotNull
    @Override
    public Ingredient getRepairMaterial() {
        return getMaterial().getRepairIngredient();
    }

    @Override
    public int getDefense() {
        return getMaterial().getDefenseForType(getType());
    }

    @Override
    public float getToughness() {
        return getMaterial().getToughness();
    }

    public float getKnockbackResistance() {
        return getMaterial().getKnockbackResistance();
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return material.getDurabilityForType(getType());
    }

    @Override
    public boolean canBeDepleted() {
        return material.getDurabilityForType(getType()) > 0;
    }

    @NotNull
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(@NotNull EquipmentSlot slot, @NotNull ItemStack stack) {
        return slot == getEquipmentSlot() ? attributeCache.get() : ImmutableMultimap.of();
    }

    @Override
    public void addToBuilder(ImmutableMultimap.Builder<Attribute, AttributeModifier> builder) {
        UUID modifier = ARMOR_MODIFIER_UUID_PER_TYPE.get(getType());
        builder.put(Attributes.ARMOR, new AttributeModifier(modifier, "Armor modifier", getDefense(), Operation.ADDITION));
        builder.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(modifier, "Armor toughness", getToughness(), Operation.ADDITION));
        builder.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(modifier, "Armor knockback resistance", getKnockbackResistance(), Operation.ADDITION));
    }

    @Override
    public void attachCapabilities(RegisterCapabilitiesEvent event) {
        ToolsGenderCapabilityHelper.addGenderCapability(this, event);
    }
}