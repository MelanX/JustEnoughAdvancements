package de.melanx.jea.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.melanx.jea.AdvancementInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementWidgetType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.TooltipFlag;
import org.moddingx.libx.render.RenderHelper;

import java.util.List;

public class AdvancementDisplayHelper {

    public static void renderAdvancement(PoseStack poseStack, MultiBufferSource buffer, AdvancementInfo info, AdvancementWidgetType state) {
        poseStack.pushPose();
        RenderHelper.resetColor();
        RenderSystem.setShaderTexture(0, AdvancementWidget.WIDGETS_LOCATION);
        GuiComponent.blit(poseStack, 0, 0, 0, info.getDisplay().getFrame().getTexture(), 128 + (state.getIndex() * 26), 26, 26, 256, 256);
        poseStack.translate(0, 0, 20);
        RenderSystem.getModelViewStack().pushPose();
        RenderSystem.applyModelViewMatrix();
        Minecraft.getInstance().getItemRenderer().renderAndDecorateFakeItem(poseStack, info.getDisplay().getIcon(), 5, 5);
        RenderSystem.getModelViewStack().popPose();
        RenderHelper.resetColor();
        poseStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }
    
    public static void addAdvancementTooltipToList(AdvancementInfo info, List<Component> list, TooltipFlag flag) {
        list.add(info.getFormattedDisplayName());
        list.add(info.getDisplay().getDescription());
        AdvancementProgress progress = ClientAdvancementProgress.getProgress(info.id);
        if (progress != null && progress.getPercent() >= 1) {
            list.add(info.getDisplay().getFrame().getDisplayName().copy().withStyle(ChatFormatting.YELLOW));
        } else if (progress != null && progress.getPercent() < 1 && progress.getPercent() > 0) {
            list.add(Component.translatable("jea.advancement.partial", Mth.clamp(Math.round(progress.getPercent() * 100), 1, 99) + "%").withStyle(ChatFormatting.YELLOW));
        } else {
            list.add(Component.translatable("jea.advancement.incomplete").withStyle(ChatFormatting.YELLOW));
        }
        if (flag.isAdvanced()) {
            list.add(Component.literal(info.getId().toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
