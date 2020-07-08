package com.hyperlynx.coppermech;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.EndRodBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolType;

public class CopperRod extends EndRodBlock implements IHeatable {

	//Has a FACING property.
		
	public CopperRod() {
		super(Block.Properties.create(Material.IRON).hardnessAndResistance(1.5f).harvestTool(ToolType.PICKAXE).sound(SoundType.METAL));
	}
	
	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder) {
		builder.add(HEAT);
		super.fillStateContainer(builder);
	}
	
	@Override
	public int tickRate(IWorldReader w) {
		return HEAT_TICK_SPEED;
	}
	
	@Override
	public boolean canProvidePower(BlockState state) {
		return state.get(HEAT) < 3;
	}
	
	@Override
	public boolean canAcceptHeat(BlockState state) {
		return state.get(HEAT) < 3;
	}
	
	@Override
	public int getStrongPower(BlockState state, IBlockReader br, BlockPos pos, Direction side) {
		return getWeakPower(state, br, pos, side);
	}
	
	@Override
	public int getWeakPower(BlockState state, IBlockReader br, BlockPos pos, Direction side) {
		BlockState attached_bs = br.getBlockState(pos.offset(state.get(FACING).getOpposite()));
		if(side.getOpposite().equals(state.get(FACING)) && attached_bs.getBlock() == ModBlocks.COIL.get()){
			if(attached_bs.get(HEAT) < 3 && state.get(HEAT) < 3) {
				return Math.min(attached_bs.get(CopperCoil.POWER) - (attached_bs.get(HEAT) + state.get(HEAT)), 0);
			}
		}
		
		return 0;
	}
	
	// Lie to the world about changing state when your neighbors do so that updates to coils will get neighbors to re-test your signal. 
	// You should find a better way to do this.
	@Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos other_pos, boolean isMoving) {
		if(!worldIn.getBlockState(other_pos).getBlock().equals(this)) worldIn.notifyNeighborsOfStateChange(pos, blockIn);
	}
	
	public static BlockPos posPointedAt(BlockPos pos, BlockState state) {
		return pos.offset(state.get(FACING));
	}
	
	public static BlockState blockStatePointedAt(IBlockReader br, BlockPos pos, BlockState state) {
		return br.getBlockState(pos.offset(state.get(FACING)));
	}
	
	public static BlockState attachedBlockState(IBlockReader br, BlockPos pos, BlockState state){
		return br.getBlockState(pos.offset(state.get(FACING).getOpposite()));
	}
	
	@Override
	public void acceptHeat(World worldIn, BlockPos pos, BlockState state, int amount) {
		for(int i = 0; i < amount; i++) {
			if(state.get(HEAT) < 3) {
				worldIn.setBlockState(pos, state.with(HEAT, state.get(HEAT) + 1));
			}
			else if(blockStatePointedAt(worldIn, pos, state).isFlammable(worldIn, posPointedAt(pos, state), state.get(FACING).getOpposite())){
				worldIn.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 0.4f, 1.0f);
				//TODO: ignite a fire
			}
		}
	}
	
	@Override
	public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand) {
		sinkHeat(worldIn, state, pos, posPointedAt(pos, state), rand);
		sinkHeat(worldIn, state, pos, pos.offset(state.get(FACING).getOpposite()), rand);
	}
	
	@OnlyIn(Dist.CLIENT)
    @Override
    public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
		if (stateIn.get(HEAT) > 2){
        	for(int i = 0; i < 3; i++) { 
        		worldIn.addParticle(ParticleTypes.FLAME, posPointedAt(pos, stateIn).getX(), posPointedAt(pos, stateIn).getY(), posPointedAt(pos, stateIn).getZ(), 0, 0, 0);
        	}
		}
		if (stateIn.get(HEAT) > 0){
        	for(int i = 0; i < 3; i++) { 
        		worldIn.addParticle(ParticleTypes.SMOKE, posPointedAt(pos, stateIn).getX(), posPointedAt(pos, stateIn).getY(), posPointedAt(pos, stateIn).getZ(), 0, 0, 0);
        	}
		}
    }
}
