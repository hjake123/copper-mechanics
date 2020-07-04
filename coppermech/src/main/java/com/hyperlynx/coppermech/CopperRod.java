package com.hyperlynx.coppermech;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.EndRodBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolType;

public class CopperRod extends EndRodBlock{

	//Has a FACING property.
	
	//Defines what is considered a coil.
	
	public CopperRod() {
		super(Block.Properties.create(Material.IRON).hardnessAndResistance(1.5f).harvestTool(ToolType.PICKAXE).sound(SoundType.METAL));
	}
	
	@Override
	public boolean canProvidePower(BlockState state) {
		return true;
	}
	
	@Override
	public int getStrongPower(BlockState state, IBlockReader br, BlockPos pos, Direction side) {
		return getWeakPower(state, br, pos, side);
	}
	
	@Override
	public int getWeakPower(BlockState state, IBlockReader br, BlockPos pos, Direction side) {
		BlockState attached_bs = br.getBlockState(pos.offset(state.get(FACING).getOpposite()));
		LOGGER.info("Facing " + state.get(FACING));
		LOGGER.info("Testing from the " + side.getOpposite());
		if(side.getOpposite().equals(state.get(FACING)) && attached_bs.getBlock() == ModBlocks.COIL.get()){
			LOGGER.info("Found a coil to the " + state.get(FACING).getOpposite());
			if(attached_bs.get(CopperCoil.HEAT) < 3) {
				LOGGER.info("Power sent " + side);
				return attached_bs.get(CopperCoil.POWER);
			}
		}
		
		return 0;
	}
	
	//Lie to the world about changing state when your neighbors do so that updates to coils will get neighbors to re-test your signal.
	@Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos other_pos, boolean isMoving) {
		worldIn.notifyNeighborsOfStateChange(pos, blockIn);
	}
	
	@OnlyIn(Dist.CLIENT)
    @Override
    public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
		
    }

}
