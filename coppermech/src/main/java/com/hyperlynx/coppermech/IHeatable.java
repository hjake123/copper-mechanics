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
	public static final int HEAT_TICK_SPEED = 10;
	
	// These should be overridden for any heat using block.
	public default boolean canAcceptHeat(BlockState state) {
		return state.getMaterial().equals(Material.WATER) || state.getMaterial().equals(Material.ICE) || state.getMaterial().equals(Material.IRON);
	}
	
	public default boolean canLoseHeat(BlockState state) {
		return false;
	}
	
	public default void acceptHeat(World worldIn, BlockPos pos, BlockState state, int amount) {
		// Do nothing; the heat is voided.
	}

	public default void loseHeat(World worldIn, BlockPos pos, BlockState state, int amount) {
		for(int i = 0; i < amount; i++) {
			if(state.get(HEAT) > 0) {
				worldIn.setBlockState(pos, state.with(HEAT, state.get(HEAT) - 1));
			}
		}
	}
	
	public default void acceptHeat(World worldIn, BlockPos pos, BlockState state) { acceptHeat(worldIn, pos, state, 1); }
	public default void loseHeat(World worldIn, BlockPos pos, BlockState state) { loseHeat(worldIn, pos, state, 1); }
	
	public default void sinkHeat(World worldIn, BlockState state, BlockPos pos, BlockPos other_pos, Random rand) {
		if(canAcceptHeat(worldIn.getBlockState(other_pos))){
			int other_temp = 0;
			if(worldIn.getBlockState(other_pos).has(HEAT))
				other_temp = worldIn.getBlockState(other_pos).get(HEAT);
			if(other_temp < state.get(HEAT)) {
				acceptHeat(worldIn, other_pos, worldIn.getBlockState(other_pos)); 
				loseHeat(worldIn, pos, state);
			}
			
		}
		else if(worldIn.getBlockState(other_pos).isBurning(worldIn, other_pos)) {
			acceptHeat(worldIn, pos, state, 2);
		}
		if(worldIn.isRainingAt(pos) && worldIn.canSeeSky(pos)) {
			loseHeat(worldIn, pos, state);
			worldIn.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.4f, 1.0f);
		}
	}
}