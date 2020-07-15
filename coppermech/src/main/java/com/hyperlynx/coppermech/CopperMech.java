package com.hyperlynx.coppermech;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hyperlynx.coppermech.ModBlocks;
import com.hyperlynx.coppermech.ModItems;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("coppermech")
public class CopperMech {
	
	protected static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "coppermech";
    
    public CopperMech() {
    	
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        ModBlocks.BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    private void setup(final FMLCommonSetupEvent event)
    {
    	
    }
}
