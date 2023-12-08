package mekanism.common.integration.lookingat.theoneprobe;

import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import mcjty.theoneprobe.api.CompoundText;
import mcjty.theoneprobe.api.IProbeConfig;
import mcjty.theoneprobe.api.IProbeConfig.ConfigMode;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.math.FloatingLong;
import mekanism.common.Mekanism;
import mekanism.common.integration.lookingat.LookingAtHelper;
import mekanism.common.integration.lookingat.LookingAtUtils;
import mekanism.common.integration.lookingat.theoneprobe.TOPChemicalElement.GasElementFactory;
import mekanism.common.integration.lookingat.theoneprobe.TOPChemicalElement.InfuseTypeElementFactory;
import mekanism.common.integration.lookingat.theoneprobe.TOPChemicalElement.PigmentElementFactory;
import mekanism.common.integration.lookingat.theoneprobe.TOPChemicalElement.SlurryElementFactory;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

//Registered via IMC
public class TOPProvider implements IProbeInfoProvider, Function<ITheOneProbe, Void> {

    private BooleanSupplier displayFluidTanks;
    private Supplier<ConfigMode> tankMode = () -> ConfigMode.EXTENDED;

    @Override
    public Void apply(ITheOneProbe probe) {
        probe.registerProvider(this);
        probe.registerEntityProvider(TOPEntityProvider.INSTANCE);
        probe.registerProbeConfigProvider(ProbeConfigProvider.INSTANCE);
        probe.registerElementFactory(new TOPEnergyElement.Factory());
        probe.registerElementFactory(new TOPFluidElement.Factory());
        probe.registerElementFactory(new GasElementFactory());
        probe.registerElementFactory(new InfuseTypeElementFactory());
        probe.registerElementFactory(new PigmentElementFactory());
        probe.registerElementFactory(new SlurryElementFactory());
        //Grab the default view settings
        IProbeConfig probeConfig = probe.createProbeConfig();
        displayFluidTanks = () -> probeConfig.getTankMode() > 0;
        tankMode = probeConfig::getShowTankSetting;
        return null;
    }

    @Override
    public ResourceLocation getID() {
        return Mekanism.rl("data");
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo info, Player player, Level world, BlockState blockState, IProbeHitData data) {
        BlockPos pos = data.getPos();
        BlockEntity tile = WorldUtils.getTileEntity(world, pos);
        LookingAtUtils.addInfoOrRedirect(new TOPLookingAtHelper(info), world, pos, blockState, tile, displayTanks(mode), displayFluidTanks.getAsBoolean());
    }

    private boolean displayTanks(ProbeMode mode) {
        return switch (tankMode.get()) {
            case NOT -> false;//Don't display tanks
            case NORMAL -> mode == ProbeMode.NORMAL;
            case EXTENDED -> mode == ProbeMode.EXTENDED;
        };
    }

    static class TOPLookingAtHelper implements LookingAtHelper {

        private final IProbeInfo info;

        public TOPLookingAtHelper(IProbeInfo info) {
            this.info = info;
        }

        @Override
        public void addText(Component text) {
            info.text(CompoundText.create().name(text).get());
        }

        @Override
        public void addEnergyElement(FloatingLong energy, FloatingLong maxEnergy) {
            info.element(new TOPEnergyElement(energy, maxEnergy));
        }

        @Override
        public void addFluidElement(FluidStack stored, int capacity) {
            info.element(new TOPFluidElement(stored, capacity));
        }

        @Override
        public void addChemicalElement(ChemicalStack<?> stored, long capacity) {
            TOPChemicalElement element = TOPChemicalElement.create(stored, capacity);
            if (element != null) {
                info.element(element);
            }
        }
    }
}