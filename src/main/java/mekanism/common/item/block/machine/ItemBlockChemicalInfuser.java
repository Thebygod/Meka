package mekanism.common.item.block.machine;

import java.util.List;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.capabilities.chemical.variable.RateLimitGasTank;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.tile.machine.TileEntityChemicalInfuser;
import net.neoforged.bus.api.IEventBus;

public class ItemBlockChemicalInfuser extends ItemBlockMachine {

    public ItemBlockChemicalInfuser(BlockTile<?, ?> block) {
        super(block);
    }

    @Override
    public void attachAttachments(IEventBus eventBus) {
        super.attachAttachments(eventBus);
        //Note: We pass null for the event bus to not expose this attachment as a capability
        ContainerType.GAS.addDefaultContainers(null, this, stack -> List.of(
              RateLimitGasTank.createBasicItem(TileEntityChemicalInfuser.MAX_GAS,
                    ChemicalTankBuilder.GAS.manualOnly, ChemicalTankBuilder.GAS.alwaysTrueBi,
                    gas -> MekanismRecipeType.CHEMICAL_INFUSING.getInputCache().containsInput(null, gas.getStack(1))
              ),
              RateLimitGasTank.createBasicItem(TileEntityChemicalInfuser.MAX_GAS,
                    ChemicalTankBuilder.GAS.manualOnly, ChemicalTankBuilder.GAS.alwaysTrueBi,
                    gas -> MekanismRecipeType.CHEMICAL_INFUSING.getInputCache().containsInput(null, gas.getStack(1))
              ),
              RateLimitGasTank.createBasicItem(TileEntityChemicalInfuser.MAX_GAS,
                    ChemicalTankBuilder.GAS.manualOnly, ChemicalTankBuilder.GAS.alwaysTrueBi, ChemicalTankBuilder.GAS.alwaysTrue
              )
        ));
    }
}