package mekanism.common.content.gear.mekasuit;

import java.util.function.Consumer;
import mekanism.api.annotations.ParametersAreNotNullByDefault;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IHUDElement;
import mekanism.api.gear.IModule;
import mekanism.api.gear.IModuleHelper;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.config.MekanismConfig;
import mekanism.common.item.gear.ItemMekaSuitArmor;
import mekanism.common.registries.MekanismFluids;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.common.util.StorageUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

@ParametersAreNotNullByDefault
public class ModuleNutritionalInjectionUnit implements ICustomModule<ModuleNutritionalInjectionUnit> {

    private static final ResourceLocation icon = MekanismUtils.getResource(ResourceType.GUI_HUD, "nutritional_injection_unit.png");

    @Override
    public void tickServer(IModule<ModuleNutritionalInjectionUnit> module, Player player) {
        FloatingLong usage = MekanismConfig.gear.mekaSuitEnergyUsageNutritionalInjection.get();
        if (MekanismUtils.isPlayingMode(player) && player.canEat(false)) {
            //Check if we can use a single iteration of it
            ItemStack container = module.getContainer();
            ItemMekaSuitArmor item = (ItemMekaSuitArmor) container.getItem();
            int needed = Math.min(20 - player.getFoodData().getFoodLevel(),
                  item.getContainedFluid(container, MekanismFluids.NUTRITIONAL_PASTE.getFluidStack(1)).getAmount() / MekanismConfig.general.nutritionalPasteMBPerFood.get());
            int toFeed = Math.min(module.getContainerEnergy().divideToInt(usage), needed);
            if (toFeed > 0) {
                module.useEnergy(player, usage.multiply(toFeed));
                IFluidHandlerItem handler = Capabilities.FLUID.getCapability(container);
                if (handler != null) {
                    handler.drain(MekanismFluids.NUTRITIONAL_PASTE.getFluidStack(toFeed * MekanismConfig.general.nutritionalPasteMBPerFood.get()), FluidAction.EXECUTE);
                }
                player.getFoodData().eat(needed, MekanismConfig.general.nutritionalPasteSaturation.get());
            }
        }
    }

    @Override
    public void addHUDElements(IModule<ModuleNutritionalInjectionUnit> module, Player player, Consumer<IHUDElement> hudElementAdder) {
        if (module.isEnabled()) {
            ItemStack container = module.getContainer();
            IFluidHandlerItem handler = Capabilities.FLUID.getCapability(container);
            if (handler != null) {
                int max = MekanismConfig.gear.mekaSuitNutritionalMaxStorage.getAsInt();
                handler.drain(MekanismFluids.NUTRITIONAL_PASTE.getFluidStack(max), FluidAction.SIMULATE);
            }
            FluidStack stored = ((ItemMekaSuitArmor) container.getItem()).getContainedFluid(container, MekanismFluids.NUTRITIONAL_PASTE.getFluidStack(1));
            double ratio = StorageUtils.getRatio(stored.getAmount(), MekanismConfig.gear.mekaSuitNutritionalMaxStorage.get());
            hudElementAdder.accept(IModuleHelper.INSTANCE.hudElementPercent(icon, ratio));
        }
    }
}
