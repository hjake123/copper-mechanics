package com.hyperlynx.coppermech;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
	//The ITEMS deferred register in which you can register items.
    public static final DeferredRegister<Item> ITEMS = new DeferredRegister<>(ForgeRegistries.ITEMS, CopperMech.MOD_ID);

    public static final RegistryObject<Item> COIL_ITEM = ITEMS.register("coil", 
    		() -> new BlockItem(ModBlocks.COIL.get(), new Item.Properties().group(ItemGroup.REDSTONE)));
}
