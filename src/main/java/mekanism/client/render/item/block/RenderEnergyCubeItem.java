package mekanism.client.render.item.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mekanism.api.NBTConstants;
import mekanism.api.RelativeSide;
import mekanism.client.model.ModelEnergyCore;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.item.MekanismISTER;
import mekanism.client.render.tileentity.RenderEnergyCube;
import mekanism.common.attachments.containers.AttachedEnergyContainers;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.item.block.ItemBlockEnergyCube;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tier.EnergyCubeTier;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.tile.TileEntityEnergyCube.CubeSideState;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.ItemDataUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

public class RenderEnergyCubeItem extends MekanismISTER {

    public static final RenderEnergyCubeItem RENDERER = new RenderEnergyCubeItem();
    private ModelEnergyCore core;

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        core = new ModelEnergyCore(getEntityModels());
    }

    @Override
    public void renderByItem(@NotNull ItemStack stack, @NotNull ItemDisplayContext displayContext, @NotNull PoseStack matrix, @NotNull MultiBufferSource renderer,
          int light, int overlayLight) {
        EnergyCubeTier tier = ((ItemBlockEnergyCube) stack.getItem()).getTier();

        CubeSideState[] sideStates = new CubeSideState[EnumUtils.SIDES.length];
        CompoundTag configData = ItemDataUtils.getDataMapIfPresent(stack);
        if (configData != null && configData.contains(NBTConstants.COMPONENT_CONFIG, Tag.TAG_COMPOUND)) {
            CompoundTag sideConfig = configData.getCompound(NBTConstants.COMPONENT_CONFIG).getCompound(NBTConstants.CONFIG + TransmissionType.ENERGY.ordinal());
            //TODO: Maybe improve on this, but for now this is a decent way of making it not have disabled sides show
            for (RelativeSide side : EnumUtils.SIDES) {
                DataType dataType = DataType.byIndexStatic(sideConfig.getInt(NBTConstants.SIDE + side.ordinal()));
                CubeSideState state = CubeSideState.INACTIVE;
                if (dataType != DataType.NONE) {
                    state = dataType.canOutput() ? CubeSideState.ACTIVE_LIT : CubeSideState.ACTIVE_UNLIT;
                }
                sideStates[side.ordinal()] = state;
            }
        } else {
            for (RelativeSide side : EnumUtils.SIDES) {
                sideStates[side.ordinal()] = tier == EnergyCubeTier.CREATIVE || side == RelativeSide.FRONT ? CubeSideState.ACTIVE_LIT : CubeSideState.ACTIVE_UNLIT;
            }
        }
        ModelData modelData = ModelData.builder().with(TileEntityEnergyCube.SIDE_STATE_PROPERTY, sideStates).build();
        renderBlockItem(stack, displayContext, matrix, renderer, light, overlayLight, modelData);
        double energyPercentage = 0;
        AttachedEnergyContainers attachment = ContainerType.ENERGY.getAttachment(stack);
        if (attachment != null) {
            //TODO - 1.20.4: We know there is only a single energy container but we may still want to improve the handling of this
            energyPercentage = attachment.getEnergy(0).divideToLevel(attachment.getMaxEnergy(0));
        }
        if (energyPercentage > 0) {
            float ticks = Minecraft.getInstance().levelRenderer.getTicks() + MekanismRenderer.getPartialTick();
            float scaledTicks = 4 * ticks;
            matrix.pushPose();
            matrix.translate(0.5, 0.5, 0.5);
            matrix.scale(0.4F, 0.4F, 0.4F);
            matrix.translate(0, Math.sin(Math.toRadians(3 * ticks)) / 7, 0);
            matrix.mulPose(Axis.YP.rotationDegrees(scaledTicks));
            matrix.mulPose(RenderEnergyCube.coreVec.rotationDegrees(36F + scaledTicks));
            core.render(matrix, renderer, LightTexture.FULL_BRIGHT, overlayLight, tier.getBaseTier(), (float) energyPercentage);
            matrix.popPose();
        }
    }
}