package mekanism.client.render.item.gear;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import mekanism.client.model.ModelArmoredJetpack;
import mekanism.client.model.ModelJetpack;
import mekanism.client.render.item.MekanismISTER;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RenderJetpack extends MekanismISTER {

    public static final RenderJetpack RENDERER = new RenderJetpack(false);
    public static final RenderJetpack ARMORED_RENDERER = new RenderJetpack(true);

    private final boolean armored;
    private ModelJetpack jetpack;

    private RenderJetpack(boolean armored) {
        this.armored = armored;
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        if (armored) {
            jetpack = new ModelArmoredJetpack(getEntityModels());
        } else {
            jetpack = new ModelJetpack(getEntityModels());
        }
    }

    @Override
    public void renderByItem(@NotNull ItemStack stack, @NotNull TransformType transformType, @NotNull PoseStack matrix, @NotNull MultiBufferSource renderer,
          int light, int overlayLight) {
        matrix.pushPose();
        matrix.translate(0.5, 0.5, 0.5);
        matrix.mulPose(Vector3f.ZP.rotationDegrees(180));
        jetpack.render(matrix, renderer, light, overlayLight, stack.hasFoil());
        matrix.popPose();
    }
}