package mekanism.common.capabilities.chemical.dynamic;

import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.IChemicalTank;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

@MethodsReturnNonnullByDefault
public interface IInfusionTracker extends IContentsListener {

    List<IChemicalTank> getInfusionTanks(@Nullable Direction side);
}