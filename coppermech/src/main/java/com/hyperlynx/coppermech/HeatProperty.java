package com.hyperlynx.coppermech;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HeatProperty {
	public static final IntegerProperty HEAT = IntegerProperty.create("heat", 0, 3);
	
	public static void incrementHeat(World worldIn, BlockPos pos, BlockState state) {
		if(state.get(HEAT) < 3) {
			worldIn.setBlockState(pos, state.with(HEAT, state.get(HEAT) + 1));
		}
	}
	
	public static void decrementHeat(World worldIn, BlockPos pos, BlockState state) {
		if(state.get(HEAT) > 0){
			worldIn.setBlockState(pos, state.with(HEAT, state.get(HEAT) - 1));
		}
	}
	
	public static void sinkHeat(World worldIn, BlockState state, BlockPos pos, BlockPos other_pos, Random rand) {
		if(worldIn.getBlockState(other_pos).has(HEAT)){
			int other_temp = worldIn.getBlockState(other_pos).get(HEAT);
			if(other_temp < state.get(HEAT)) {
				incrementHeat(worldIn, other_pos, worldIn.getBlockState(other_pos)); 
				decrementHeat(worldIn, pos, state);
			}
		}
		else if(worldIn.getBlockState(other_pos).isBurning(worldIn, other_pos)) {
			incrementHeat(worldIn, pos, state);
			incrementHeat(worldIn, pos, state);
		}
		if(worldIn.isRainingAt(pos) && worldIn.canSeeSky(pos)) {
			decrementHeat(worldIn, pos, state);
			worldIn.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.4f, 1.0f);
		}
		if(worldIn.getBlockState(other_pos).getMaterial().equals(Material.WATER) || worldIn.getBlockState(other_pos).getMaterial().equals(Material.ICE)) {
			
			if(state.get(HEAT) == 3) 
				worldIn.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.7f, 1.0f);

			decrementHeat(worldIn, pos, state);
			decrementHeat(worldIn, pos, state);
		}
	}
	
	/*NOTE: directional propagation (for rods)
	 * 
	 * if(state.get(axis).isVertical()) {
				HeatProperty.propagateHeat(worldIn, state, pos, pos.add(0, 1, 0));
				//Heat does not propagate downwards.
			}
			else if(state.get(axis).equals(Direction.Axis.X)) 
			{
				HeatProperty.propagateHeat(worldIn, state, pos, pos.add(1, 0, 0));
				HeatProperty.propagateHeat(worldIn, state, pos, pos.add(-1, 0, 0));
			}
			else {
				HeatProperty.propagateHeat(worldIn, state, pos, pos.add(0, 0, 1));
				HeatProperty.propagateHeat(worldIn, state, pos, pos.add(0, 0, -1));
			}
	 * 
	 * */
}
