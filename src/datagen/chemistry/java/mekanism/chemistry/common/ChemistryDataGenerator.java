package mekanism.chemistry.common;

import mekanism.chemistry.client.ChemistryBlockStateProvider;
import mekanism.chemistry.client.ChemistryItemModelProvider;
import mekanism.chemistry.client.ChemistryLangProvider;
import mekanism.chemistry.client.ChemistrySoundProvider;
import mekanism.chemistry.common.loot.ChemistryLootProvider;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

@EventBusSubscriber(modid = MekanismChemistry.MODID, bus = Bus.MOD)
public class ChemistryDataGenerator {

    private ChemistryDataGenerator() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        if (event.includeClient()) {
            //Client side data generators
            gen.addProvider(new ChemistryLangProvider(gen));
            gen.addProvider(new ChemistrySoundProvider(gen, existingFileHelper));
            //Let the blockstate provider see models generated by the item model provider
            ChemistryItemModelProvider itemModelProvider = new ChemistryItemModelProvider(gen, existingFileHelper);
            gen.addProvider(itemModelProvider);
            gen.addProvider(new ChemistryBlockStateProvider(gen, itemModelProvider.existingFileHelper));
        }
        if (event.includeServer()) {
            //Server side data generators
            gen.addProvider(new ChemistryTagProvider(gen, existingFileHelper));
            gen.addProvider(new ChemistryLootProvider(gen));
            gen.addProvider(new ChemistryRecipeProvider(gen, existingFileHelper));
        }
    }
}
