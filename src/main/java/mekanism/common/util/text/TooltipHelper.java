package mekanism.common.util.text;

import mekanism.api.text.ITooltipHelper;
import mekanism.common.MekanismLang;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UnitDisplayUtils;
import mekanism.common.util.UnitDisplayUtils.RadiationUnit;
import net.minecraft.network.chat.Component;

/**
 * @apiNote Do not instantiate this class directly as it will be done via the service loader. Instead, access instances of this via {@link ITooltipHelper#INSTANCE}
 */
public class TooltipHelper implements ITooltipHelper {

    @Override
    public Component getEnergyPerMBDisplayShort(long energy) {
        return MekanismLang.GENERIC_PER_MB.translate(MekanismUtils.getEnergyDisplayShort(energy));
    }

    @Override
    public Component getRadioactivityDisplayShort(double radioactivity) {
        return UnitDisplayUtils.getDisplayShort(radioactivity, RadiationUnit.SVH, 2);
    }

    @Override
    public String getFormattedNumber(long number) {
        return TextUtils.format(number);
    }

    @Override
    public Component getPercent(double ratio) {
        return TextUtils.getPercent(ratio);
    }

    @Override
    public Component getEnergyDisplay(long joules, boolean perTick) {
        Component energyDisplay = MekanismUtils.getEnergyDisplayShort(joules);
        return perTick ? MekanismLang.GENERIC_PER_TICK.translate(energyDisplay) : energyDisplay;
    }

    @Override
    public Component getFluidDisplay(long mb, boolean perTick) {
        Component formattedMb = Component.literal(getFormattedNumber(mb));
        return perTick ? MekanismLang.GENERIC_PER_TICK.translate(formattedMb) : formattedMb;
    }
}