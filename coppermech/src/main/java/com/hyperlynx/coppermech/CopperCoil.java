package com.hyperlynx.coppermech;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RotatedPillarBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.api.distmarker.Dist;

public class CopperCoil extends RotatedPillarBlock {
	
	public static final IntegerProperty POWER = BlockStateProperties.POWER_0_15;
	public static final IntegerProperty HEAT = HeatProperty.HEAT;
	public static final EnumProperty<Axis> AXIS = BlockStateProperties.AXIS;
	protected static final BooleanProperty COIL_POWERED = BooleanProperty.create("coil_powered");
	
	private final float AMBIENT_HEAT_CHANCE = 0.006f;
	private final int COIL_POWER_RANGE = 24;
	
	public CopperCoil() {
		super(Block.Properties.create(Material.IRON).hardnessAndResistance(2.0f).harvestTool(ToolType.PICKAXE).sound(SoundType.METAL));
		setDefaultState(getDefaultState().with(POWER, 0).with(HEAT, 0).with(AXIS, Direction.Axis.Y).with(COIL_POWERED, false));
	}
	
	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder) {
		builder.add(POWER, HEAT, AXIS, COIL_POWERED);
	}
	
	@Override
	public int tickRate(IWorldReader w) {
		return 15;
	}
	
	@Override
	public boolean ticksRandomly(BlockState state) {
		return state.get(POWER) > 0 || state.get(HEAT) > 0;
	}
	
	@Override
	public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos self, Direction side){
		return false;
	}
	
	@Override
	public boolean isBurning(BlockState state, IBlockReader world, BlockPos pos) {
		return state.get(HEAT) > 2;
	}
	
	@Override
	public void onEntityWalk(World world, BlockPos pos, Entity entity) {
		if(world.getBlockState(pos).get(HEAT) > 2) {
			entity.setFire(1);
		}
	}
	
	@Override
	public boolean isFireSource(BlockState state, IBlockReader world, BlockPos self, Direction side) {
		return state.get(HEAT) > 2;
	}
	
	@Override public boolean hasComparatorInputOverride(BlockState state) {
		return true;
	}
	
	@Override
	public int getComparatorInputOverride(BlockState state, World world, BlockPos pos) {
		return state.get(HEAT) * 2;
	}
	
	private boolean isConnectedCoil(BlockState state, IBlockReader blockAccess, BlockPos other_pos) {
		return blockAccess.getBlockState(other_pos).getBlock().equals(this) && blockAccess.getBlockState(other_pos).get(AXIS).equals(state.get(AXIS));
	}
	
	//Powers all coils that are connected to the one whose state and position are passed in with the same power as that coil.
	private void chainPower(BlockState state, World worldIn, BlockPos pos, int pow) {
		if(state.get(AXIS).isVertical()) {
			if(isConnectedCoil(state, worldIn, pos.up())){
				chainPowerRecursive(state, worldIn, pos,  Direction.UP, pow, 0);
			}
			if(isConnectedCoil(state, worldIn, pos.down())) {
				chainPowerRecursive(state, worldIn, pos, Direction.DOWN, pow, 0);
			}
		}
		else if(state.get(AXIS).equals(Direction.Axis.Z)) {
			if(isConnectedCoil(state, worldIn, pos.north())){
				chainPowerRecursive(state, worldIn, pos, Direction.NORTH, pow, 0);
			}
			if(isConnectedCoil(state, worldIn, pos.south())) {
				chainPowerRecursive(state, worldIn, pos, Direction.SOUTH, pow, 0);
			}		
		}
		else {
			if(isConnectedCoil(state, worldIn, pos.east())){
				chainPowerRecursive(state, worldIn, pos, Direction.EAST, pow, 0);
			}
			if(isConnectedCoil(state, worldIn, pos.west())) {
				chainPowerRecursive(state, worldIn, pos, Direction.WEST, pow, 0);
			}		
		}
	}
	
	private void chainPowerRecursive(BlockState state, World worldIn, BlockPos pos, Direction dir, int pow, int distance) {
		if(distance >= COIL_POWER_RANGE) return;
		BlockPos other_pos = pos.offset(dir);
		if(isConnectedCoil(state, worldIn, other_pos)) {
			int other_heat = worldIn.getBlockState(other_pos).get(HEAT);
			if(other_heat == 3) 
				pow = 0;
			else 
				pow -= other_heat;
			
			if(pow < 0) 
				pow = 0;
			
			worldIn.setBlockState(other_pos, worldIn.getBlockState(other_pos).with(POWER, pow).with(COIL_POWERED, pow > 0));
			worldIn.notifyNeighborsOfStateChange(other_pos, worldIn.getBlockState(other_pos).getBlock());

			chainPowerRecursive(state, worldIn, other_pos, dir, pow, distance + 1);
		}
	}
	
	@Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos other_pos, boolean isMoving) {
		if(worldIn.isRemote) return;
		
		int pow = worldIn.getStrongPower(pos);
		if(!state.get(COIL_POWERED) && state.get(POWER) != pow) {
			worldIn.setBlockState(pos, state.with(POWER, pow));
			chainPower(state, worldIn, pos, pow);
			worldIn.notifyNeighborsOfStateChange(pos, blockIn);
		}
		
		if(worldIn.getBlockState(other_pos).getMaterial().equals(Material.WATER) && state.get(HEAT) == 3) {
			HeatProperty.decrementHeat(worldIn, pos, state);
			HeatProperty.decrementHeat(worldIn, pos, state);
		}
    }

	@Override
	public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand) {
		int pow = state.get(POWER);
		if(pow > 12 || pow > 0 && (rand.nextFloat() < (AMBIENT_HEAT_CHANCE*pow))) {
			HeatProperty.incrementHeat(worldIn, pos, state);
		}
		
		HeatProperty.sinkHeat(worldIn, state, pos, pos.add(0, 1, 0), rand);
		HeatProperty.sinkHeat(worldIn, state, pos, pos.add(1, 0, 0), rand);
		HeatProperty.sinkHeat(worldIn, state, pos, pos.add(-1, 0, 0), rand);
		HeatProperty.sinkHeat(worldIn, state, pos, pos.add(0, 0, 1), rand);
		HeatProperty.sinkHeat(worldIn, state, pos, pos.add(0, 0, -1), rand);
		HeatProperty.sinkHeat(worldIn, state, pos, pos.add(0, -1, 0), rand);
		
		if(pow == 0) {
			HeatProperty.decrementHeat(worldIn, pos, state);
		}
	}
	
	@OnlyIn(Dist.CLIENT)
    @Override
    public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        if (worldIn.isAirBlock(pos.up())) {
        	if (stateIn.get(HEAT) == 3) {
	        	for(int i = 0; i < 7; i++) { 
	            	worldIn.addParticle(ParticleTypes.SMOKE, pos.getX() + rand.nextFloat(), pos.getY() + 1 - rand.nextFloat()*0.5, pos.getZ() + rand.nextFloat(), rand.nextFloat()*0.01, rand.nextFloat()*0.02, rand.nextFloat()*0.01);
	        	}
            	worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + rand.nextFloat(), pos.getY() + 1, pos.getZ() + rand.nextFloat(), 0, 0, 0);
        	}
        	else if (stateIn.get(HEAT) > 0) {
        		for(int i = 0; i < stateIn.get(HEAT); i++) { 
	            	worldIn.addParticle(ParticleTypes.SMOKE, pos.getX() + rand.nextFloat(), pos.getY() + 1 - rand.nextFloat()*0.5, pos.getZ() + rand.nextFloat(), rand.nextFloat()*0.01, rand.nextFloat()*0.015, rand.nextFloat()*0.01);
	        	}
        	}
        }
        else if (stateIn.get(HEAT) == 3){
        	for(int i = 0; i < 3; i++) { 
        		worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + rand.nextFloat()*1.1, pos.getY() + rand.nextFloat(), pos.getZ() + rand.nextFloat()*1.1, 0, 0, 0);
        	}
        }
        if (worldIn.getBlockState(pos.up()).getMaterial().equals(Material.WATER) && stateIn.get(HEAT) == 3) {
        	for(int i = 0; i < 5; i++) { 
        		worldIn.addParticle(ParticleTypes.CLOUD, pos.getX() + rand.nextFloat(), pos.getY() + 1, pos.getZ() + rand.nextFloat(), 0, rand.nextFloat()*0.1, 0);	        	
	        }
        }
    }
}
