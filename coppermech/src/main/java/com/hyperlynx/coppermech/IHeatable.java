package com.hyperlynx.coppermech;

import java.util.Random;

import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IHeatable {
	
	public static final IntegerProperty HEAT = IntegerProperty.create("heat", 0, 3);
	
	public void acceptHeat(World worldIn, BlockPos pos, BlockState state, int amount);
	public void loseHeat(World worldIn, BlockPos pos, BlockState state, int amount);
	
	public default void acceptHeat(World worldIn, BlockPos pos, BlockState state) { acceptHeat(worldIn, pos, state, 1); }
	public default void loseHeat(World worldIn, BlockPos pos, BlockState state) { loseHeat(worldIn, pos, state, 1); }
	
	public default void sinkHeat(World worldIn, BlockState state, BlockPos pos, BlockPos other_pos, Random rand) {
		if(worldIn.getBlockState(other_pos).has(HEAT)){
			int other_temp = worldIn.getBlockState(other_pos).get(HEAT);
			if(other_temp < state.get(HEAT)) {
				acceptHeat(worldIn, other_pos, worldIn.getBlockState(other_pos)); 
				loseHeat(worldIn, pos, state);
				//The order of this exchange is important so that rods can check if they were being heated by a heat 3 coil.
			}
		}
		else if(worldIn.getBlockState(other_pos).isBurning(worldIn, other_pos)) {
			acceptHeat(worldIn, pos, state, 2);
		}
		if(worldIn.isRainingAt(pos) && worldIn.canSeeSky(pos)) {
			loseHeat(worldIn, pos, state);
			worldIn.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.4f, 1.0f);
		}
		if(worldIn.getBlockState(other_pos).getMaterial().equals(Material.WATER) || worldIn.getBlockState(other_pos).getMaterial().equals(Material.ICE)) {
			if(state.get(HEAT) == 3) {
				worldIn.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.7f, 1.0f);
			}
			loseHeat(worldIn, pos, state, 2);
		}
		if(worldIn.getBlockState(other_pos).getBlock().equals(ModBlocks.ROD.get())) {
			CopperRod.doHeatEffects(worldIn, other_pos, worldIn.getBlockState(other_pos)); 
			BlockState bs = CopperRod.blockStatePointedAt(worldIn, other_pos, worldIn.getBlockState(other_pos));
			if(bs.getBlock().equals(ModBlocks.COIL.get())) {
				acceptHeat(worldIn, CopperRod.posPointedAt(other_pos, worldIn.getBlockState(other_pos)), bs, 3);
				loseHeat(worldIn, pos, state, 3);
			}
		}
	}
}

//TODO: At the moment, this is not an extendable system, so figure out how to make a tag for all heat-bearing things and it will work like you want.
