package mekanism.client.render.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mekanism.client.gui.GuiUtils;
import mekanism.client.render.HUDRenderer;
import mekanism.common.Mekanism;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.curios.CuriosIntegration;
import mekanism.common.item.interfaces.IItemHUDProvider;
import mekanism.common.tags.MekanismTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.overlay.ExtendedGui;
import net.neoforged.neoforge.client.gui.overlay.IGuiOverlay;
import net.neoforged.neoforge.items.IItemHandler;

public class MekanismHUD implements IGuiOverlay {

    public static final MekanismHUD INSTANCE = new MekanismHUD();
    private static final EquipmentSlot[] EQUIPMENT_ORDER = {EquipmentSlot.OFFHAND, EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                                                            EquipmentSlot.FEET};

    private final HUDRenderer hudRenderer = new HUDRenderer();

    private MekanismHUD() {
    }

    @Override
    public void render(ExtendedGui gui, GuiGraphics guiGraphics, float partialTicks, int screenWidth, int screenHeight) {
        Minecraft minecraft = gui.getMinecraft();
        Player player = minecraft.player;
        if (!minecraft.options.hideGui && player != null && !player.isSpectator() && MekanismConfig.client.enableHUD.get()) {
            int count = 0;
            List<List<Component>> renderStrings = new ArrayList<>();
            for (EquipmentSlot slotType : EQUIPMENT_ORDER) {
                ItemStack stack = player.getItemBySlot(slotType);
                if (stack.getItem() instanceof IItemHUDProvider hudProvider) {
                    count += makeComponent(list -> hudProvider.addHUDStrings(list, player, stack, slotType), renderStrings);
                }
            }
            if (Mekanism.hooks.CuriosLoaded) {
                IItemHandler inv = CuriosIntegration.getCuriosInventory(player);
                if (inv != null) {
                    for (int i = 0, slots = inv.getSlots(); i < slots; i++) {
                        ItemStack stack = inv.getStackInSlot(i);
                        if (stack.getItem() instanceof IItemHUDProvider hudProvider) {
                            count += makeComponent(list -> hudProvider.addCurioHUDStrings(list, player, stack), renderStrings);
                        }
                    }
                }
            }
            Font font = gui.getFont();
            boolean reverseHud = MekanismConfig.client.reverseHUD.get();
            int maxTextHeight = screenHeight;
            if (count > 0) {
                float hudScale = MekanismConfig.client.hudScale.get();
                int xScale = (int) (screenWidth / hudScale);
                int yScale = (int) (screenHeight / hudScale);
                int start = (renderStrings.size() * 2) + (count * 9);
                int y = yScale - start;
                maxTextHeight = (int) (y * hudScale);
                PoseStack pose = guiGraphics.pose();
                pose.pushPose();
                pose.scale(hudScale, hudScale, hudScale);

                int backgroundColor = minecraft.options.getBackgroundColor(0.0F);
                if (backgroundColor != 0) {
                    //If we need to render the background behind it based on accessibility options
                    // calculate how big an area we need and draw it
                    int maxTextWidth = 0;
                    for (List<Component> group : renderStrings) {
                        for (Component text : group) {
                            int textWidth = font.width(text);
                            if (textWidth > maxTextWidth) {
                                maxTextWidth = textWidth;
                            }
                        }
                    }
                    int x = reverseHud ? xScale - maxTextWidth - 2 : 2;
                    GuiUtils.drawBackdrop(guiGraphics, Minecraft.getInstance(), x, y, maxTextWidth, maxTextHeight, 0xFFFFFFFF);
                }

                for (List<Component> group : renderStrings) {
                    for (Component text : group) {
                        int textWidth = font.width(text);
                        //Align text to right if hud is reversed, otherwise align to the left
                        //Note: that we always offset by 2 pixels from the edge of the screen regardless of how it is aligned
                        int x = reverseHud ? xScale - textWidth - 2 : 2;
                        guiGraphics.drawString(font, text, x, y, 0xFFC8C8C8);
                        y += 9;
                    }
                    y += 2;
                }
                pose.popPose();
            }

            if (player.getItemBySlot(EquipmentSlot.HEAD).is(MekanismTags.Items.MEKASUIT_HUD_RENDERER)) {
                hudRenderer.renderHUD(minecraft, guiGraphics, font, partialTicks, screenWidth, screenHeight, maxTextHeight, reverseHud);
            }
        }
    }

    private int makeComponent(Consumer<List<Component>> adder, List<List<Component>> initial) {
        List<Component> list = new ArrayList<>();
        adder.accept(list);
        int size = list.size();
        if (size > 0) {
            initial.add(list);
        }
        return size;
    }

}