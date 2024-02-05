package mekanism.common.recipe.upgrade;

import java.util.ArrayList;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IMekanismStrictEnergyHandler;
import mekanism.api.math.FloatingLong;
import mekanism.api.math.FloatingLongTransferUtils;
import mekanism.common.attachments.containers.ContainerType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class EnergyRecipeData implements RecipeUpgradeData<EnergyRecipeData> {

    private final List<IEnergyContainer> energyContainers;

    EnergyRecipeData(List<IEnergyContainer> energyContainers) {
        this.energyContainers = energyContainers;
    }

    @Nullable
    @Override
    public EnergyRecipeData merge(EnergyRecipeData other) {
        List<IEnergyContainer> allContainers = new ArrayList<>(energyContainers);
        allContainers.addAll(other.energyContainers);
        return new EnergyRecipeData(allContainers);
    }

    @Override
    public boolean applyToStack(ItemStack stack) {
        if (energyContainers.isEmpty()) {
            return true;
        }
        IMekanismStrictEnergyHandler outputHandler = ContainerType.ENERGY.getAttachment(stack);
        if (outputHandler == null) {
            //Something went wrong, fail
            return false;
        }
        for (IEnergyContainer energyContainer : this.energyContainers) {
            if (!energyContainer.isEmpty() && !insertManualIntoOutputContainer(outputHandler, energyContainer.getEnergy()).isZero()) {
                //If we have a remainder, stop trying to insert as our upgraded item's buffer is just full
                break;
            }
        }
        return true;
    }

    private FloatingLong insertManualIntoOutputContainer(IMekanismStrictEnergyHandler outputHandler, FloatingLong energy) {
        //Insert into the output using manual as the automation type
        List<IEnergyContainer> energyContainers = outputHandler.getEnergyContainers(null);
        return FloatingLongTransferUtils.insert(energy, Action.EXECUTE, energyContainers::size, container -> energyContainers.get(container).getEnergy(),
              (container, amount, action) -> energyContainers.get(container).insert(amount, action, AutomationType.MANUAL));
    }
}