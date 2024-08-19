package mekanism.common.integration.crafttweaker.bracket;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.annotation.BracketDumper;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.common.integration.crafttweaker.CrTConstants;
import mekanism.common.integration.crafttweaker.CrTUtils;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name(CrTConstants.CLASS_BRACKET_DUMPERS)
public class CrTBracketDumpers {

    /**
     * Bracket dumper to get all registered {@link Chemical chemicals} as a collection of their bracket representations.
     */
    @BracketDumper(value = CrTConstants.BRACKET_CHEMICAL, subCommandName = "chemicals")
    public static Collection<String> getChemicalStackDump() {
        return getDump(MekanismAPI.CHEMICAL_REGISTRY, chemical -> CrTUtils.fromChemical(chemical, 1).getCommandString());
    }
    
    /**
     * Bracket dumper to get all registered {@link mekanism.api.robit.RobitSkin robit skins} as a collection of their bracket representations.
     */
    @BracketDumper(value = CrTConstants.BRACKET_ROBIT_SKIN, subCommandName = "robitSkins")
    public static Collection<String> getRobitSkinDump() {
        return getDump(MekanismAPI.ROBIT_SKIN_REGISTRY_NAME, CrTConstants.BRACKET_ROBIT_SKIN);
    }

    /**
     * Bracket dumper to get all registered {@link mekanism.api.gear.ModuleData modules} as a collection of their bracket representations.
     */
    @BracketDumper(value = CrTConstants.BRACKET_MODULE_DATA, subCommandName = "moduleData")
    public static Collection<String> getModuleDataDump() {
        return getDump(MekanismAPI.MODULE_REGISTRY, CrTConstants.BRACKET_MODULE_DATA);
    }

    private static <V> Collection<String> getDump(Registry<V> registry, String bracket) {
        return getDump(registry, v -> "<" + bracket + ":" + registry.getKey(v) + ">");
    }

    private static <V> Collection<String> getDump(Registry<V> registry, Function<V, String> getter) {
        return registry.stream().map(getter).toList();
    }

    private static Collection<String> getDump(ResourceKey<? extends Registry<?>> registryKey, String bracket) {
        Optional<Registry<Object>> optionalRegistry = CraftTweakerAPI.getAccessibleElementsProvider()
              .registryAccess()
              .registry(registryKey);
        if (optionalRegistry.isEmpty()) {
            return Collections.emptyList();
        }
        Registry<?> registry = optionalRegistry.get();
        List<String> list = new ArrayList<>();
        for (ResourceLocation v : registry.keySet()) {
            list.add("<" + bracket + ":" + v + ">");
        }
        return list;
    }
}