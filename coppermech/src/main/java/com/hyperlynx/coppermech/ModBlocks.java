package com.hyperlynx.coppermech;

import com.hyperlynx.coppermech.blocks.CopperCoil;
import com.hyperlynx.coppermech.blocks.CopperRod;

import net.minecraft.block.Block;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = new DeferredRegister<>(ForgeRegistries.BLOCKS, CopperMech.MOD_ID);

    public static final RegistryObject<Block> COIL = BLOCKS.register("coil", () -> new CopperCoil());
    public static final RegistryObject<Block> ROD = BLOCKS.register("rod", () -> new CopperRod());
}
