package com.glodblock.github.extendedae.datagen;

import appeng.api.ids.AETags;
import appeng.datagen.providers.tags.ConventionTags;
import com.glodblock.github.extendedae.ExtendedAE;
import com.glodblock.github.extendedae.common.EAEItemAndBlock;
import com.glodblock.github.extendedae.common.EAERegistryHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class EAEBlockTagProvider extends BlockTagsProvider {

    public EAEBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ExtendedAE.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        for (var block : EAERegistryHandler.INSTANCE.getBlocks()) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
        }
        tag(AETags.GROWTH_ACCELERATABLE)
                .add(EAEItemAndBlock.FULLY_ENTROIZED_FLUIX_BUDDING)
                .add(EAEItemAndBlock.MOSTLY_ENTROIZED_FLUIX_BUDDING)
                .add(EAEItemAndBlock.HALF_ENTROIZED_FLUIX_BUDDING)
                .add(EAEItemAndBlock.HARDLY_ENTROIZED_FLUIX_BUDDING);
        tag(ConventionTags.BUDDING_BLOCKS_BLOCKS)
                .add(EAEItemAndBlock.FULLY_ENTROIZED_FLUIX_BUDDING)
                .add(EAEItemAndBlock.MOSTLY_ENTROIZED_FLUIX_BUDDING)
                .add(EAEItemAndBlock.HALF_ENTROIZED_FLUIX_BUDDING)
                .add(EAEItemAndBlock.HARDLY_ENTROIZED_FLUIX_BUDDING);
        tag(ConventionTags.BUDS_BLOCKS)
                .add(EAEItemAndBlock.ENTRO_BUD_SMALL)
                .add(EAEItemAndBlock.ENTRO_BUD_MEDIUM)
                .add(EAEItemAndBlock.ENTRO_BUD_LARGE);
    }
}
