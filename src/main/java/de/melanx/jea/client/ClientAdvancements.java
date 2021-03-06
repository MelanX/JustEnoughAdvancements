package de.melanx.jea.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.melanx.jea.AdvancementInfo;
import de.melanx.jea.JustEnoughAdvancements;
import de.melanx.jea.JustEnoughAdvancementsJEIPlugin;
import de.melanx.jea.api.client.IAdvancementInfo;
import de.melanx.jea.api.client.Jea;
import de.melanx.jea.api.client.criterion.ICriterionInfo;
import de.melanx.jea.recipe.CriterionRecipe;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.advancements.Criterion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.*;
import java.util.function.Function;

public class ClientAdvancements {
    
    private static ImmutableMap<ResourceLocation, AdvancementInfo> advancements = ImmutableMap.of();
    
    public static void update(Set<AdvancementInfo> info) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            //noinspection UnstableApiUsage
            advancements = info.stream().collect(ImmutableMap.toImmutableMap(x -> x.id, Function.identity()));
            JustEnoughAdvancementsJEIPlugin.runtimeOptional(runtime -> updateAdvancementIngredientsJEI(runtime, 3));
            // We trigger a recipes updated event here so JEI will reload recipes and show our
            // new advancement information.
            ClientPlayNetHandler connection = Minecraft.getInstance().getConnection();
            //noinspection ConstantConditions
            if (connection != null && connection.getRecipeManager() != null) {
                MinecraftForge.EVENT_BUS.post(new RecipesUpdatedEvent(connection.getRecipeManager()));
            }
            JeiUtil.triggerReload();
        }
    }
    
    private static void updateAdvancementIngredientsJEI(IJeiRuntime runtime, int tries) {
        try {
            IIngredientManager imgr = runtime.getIngredientManager();
            Collection<IAdvancementInfo> ingredients = ImmutableList.copyOf(imgr.getAllIngredients(Jea.ADVANCEMENT_TYPE));
            if (!ingredients.isEmpty()) {
                imgr.removeIngredientsAtRuntime(Jea.ADVANCEMENT_TYPE, ingredients);
            }
            if (!advancements.isEmpty()) {
                imgr.addIngredientsAtRuntime(Jea.ADVANCEMENT_TYPE, getIAdvancements());
            }
        } catch (ConcurrentModificationException e) {
            if (tries > 0) {
                JustEnoughAdvancements.logger.warn("Failed to update advancement ingredients for JEI. Trying again.");
                updateAdvancementIngredientsJEI(runtime, tries - 1);
            } else {
                JustEnoughAdvancements.logger.error("Failed to update advancement ingredients for JEI. Ignoring this for now. Advancements might be out of sync.");
            }
        }
    }
    
    public static Set<ResourceLocation> getAdvancementIds() {
        return advancements.keySet();
    }
    
    public static Collection<AdvancementInfo> getAdvancements() {
        return advancements.values();
    }
    
    public static Collection<IAdvancementInfo> getIAdvancements() {
        //noinspection unchecked
        return (Collection<IAdvancementInfo>) (Collection<?>) advancements.values();
    }
    
    public static AdvancementInfo getInfo(ResourceLocation key) {
        return advancements.get(key);
    }

    public static List<CriterionRecipe> collectRecipes() {
        List<CriterionRecipe> recipes = new ArrayList<>();
        for (AdvancementInfo info : ClientAdvancements.getAdvancements()) {
            for (Map.Entry<String, Criterion> criterion : info.getCriteria().entrySet()) {
                if (criterion.getValue().getCriterionInstance() != null) {
                    if (info.getCriteriaSerializerIds().containsKey(criterion.getKey())) {
                        ResourceLocation serializerId = info.getCriteriaSerializerIds().get(criterion.getKey());
                        ICriterionInfo<?> c = Jea.getCriterionInfo(info, criterion.getKey(), serializerId);
                        if (c != null && c.criterionClass().isAssignableFrom(criterion.getValue().getCriterionInstance().getClass())) {
                            List<String> criterionGroup = null;
                            for (List<String> completionGroup : info.getCompletion()) {
                                if (completionGroup.contains(criterion.getKey())) {
                                    criterionGroup = completionGroup;
                                    break;
                                }
                            }
                            if (criterionGroup == null) {
                                criterionGroup = ImmutableList.of(criterion.getKey());
                            }
                            recipes.add(new CriterionRecipe(info, criterion.getKey(), c, criterionGroup));
                        }
                    }
                }
            }
        }
        JustEnoughAdvancements.logger.info("Collected " + recipes.size() + " advancement criterion recipes.");
        return ImmutableList.copyOf(recipes);
    }
}
