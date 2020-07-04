package com.hyperlynx.coppermech;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RotatedPillarBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.Tag;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.api.distmarker.Dist;

public class CopperCoil extends RotatedPillarBlock {
	
	public static final IntegerProperty power = BlockStateProperties.POWER_0_15;
	public static final IntegerProperty heat = HeatProperty.HEAT;
	public static final EnumProperty<Axis> axis = BlockStateProperties.AXIS;
	public static final BooleanProperty coil_powered = BooleanProperty.create("coil_powered");
	
	private final float AMBIENT_HEAT_CHANCE = 0.003f;
	private final int COIL_POWER_RANGE = 24;
	
	public CopperCoil() {
		super(Block.Properties.create(Material.IRON).hardnessAndResistance(2.0f).harvestTool(ToolType.PICKAXE).sound(SoundType.METAL));
		setDefaultState(getDefaultState().with(power, 0).with(heat, 0).with(axis, Direction.Axis.Y).with(coil_powered, false));
	}
	
	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder) {
		builder.add(power, heat, axis, coil_powered);
	}
	
	@Override
	public int tickRate(IWorldReader w) {
		return 15;
	}
	
	@Override
	public boolean ticksRandomly(BlockState state) {
		return state.get(power) > 0 || state.get(heat) > 0;
	}
	
	@Override
	public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos self, Direction side){
		return true;
	}
	
	@Override
	public boolean isBurning(BlockState state, IBlockReader world, BlockPos pos) {
		return state.get(heat) > 2;
	}
	
	@Override
	public void onEntityWalk(World world, BlockPos pos, Entity entity) {
		if(world.getBlockState(pos).get(heat) > 2) {
			entity.setFire(1);
		}
	}
	
	@Override
	public boolean isFireSource(BlockState state, IBlockReader world, BlockPos self, Direction side) {
		return state.get(heat) > 2;
	}
	
	@Override public boolean hasComparatorInputOverride(BlockState state) {
		return true;
	}
	
	@Override
	public int getComparatorInputOverride(BlockState state, World world, BlockPos pos) {
		return state.get(heat) * 2;
	}
	
	private boolean isConnectedCoil(BlockState state, IBlockReader blockAccess, BlockPos other_pos) {
		return blockAccess.getBlockState(other_pos).getBlock().equals(this) && blockAccess.getBlockState(other_pos).get(axis).equals(state.get(axis));
	}
	
	//Powers all coils that are connected to the one whose state and position are passed in with the same power as that coil.
	private void chainPower(BlockState state, World worldIn, BlockPos pos, int pow) {
		if(state.get(axis).isVertical()) {
			if(isConnectedCoil(state, worldIn, pos.up())){
				chainPowerRecursive(state, worldIn, pos,  Direction.UP, pow, 0);
			}
			if(isConnectedCoil(state, worldIn, pos.down())) {
				chainPowerRecursive(state, worldIn, pos, Direction.DOWN, pow, 0);
			}
		}
		else if(state.get(axis).equals(Direction.Axis.Z)) {
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
			int other_heat = worldIn.getBlockState(other_pos).get(heat);
			if(other_heat == 3) 
				pow = 0;
			else 
				pow -= other_heat;
			
			if(pow < 0) 
				pow = 0;
			
			worldIn.setBlockState(other_pos, worldIn.getBlockState(other_pos).with(power, pow).with(coil_powered, pow > 0));

			chainPowerRecursive(state, worldIn, other_pos, dir, pow, distance + 1);
		}
	}
	
	@Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos other_pos, boolean isMoving) {
		if(worldIn.isRemote) return;
		
		int pow = worldIn.getStrongPower(pos);
		if(!state.get(coil_powered) && state.get(power) != pow) {
			worldIn.setBlockState(pos, state.with(power, pow));
			chainPower(state, worldIn, pos, pow);
		}
		
		if(worldIn.getBlockState(other_pos).getMaterial().equals(Material.WATER) && state.get(heat) == 3) {
			HeatProperty.decrementHeat(worldIn, pos, state);
			HeatProperty.decrementHeat(worldIn, pos, state);
		}
    }

	@Override
	public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand) {
		int pow = state.get(power);
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
        	if (stateIn.get(heat) == 3) {
	        	for(int i = 0; i < 7; i++) { 
	            	worldIn.addParticle(ParticleTypes.SMOKE, pos.getX() + rand.nextFloat(), pos.getY() + 1 - rand.nextFloat()*0.5, pos.getZ() + rand.nextFloat(), rand.nextFloat()*0.01, rand.nextFloat()*0.02, rand.nextFloat()*0.01);
	        	}
            	worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + rand.nextFloat(), pos.getY() + 1, pos.getZ() + rand.nextFloat(), 0, 0, 0);
        	}
        	else if (stateIn.get(heat) > 0) {
        		for(int i = 0; i < stateIn.get(heat); i++) { 
	            	worldIn.addParticle(ParticleTypes.SMOKE, pos.getX() + rand.nextFloat(), pos.getY() + 1 - rand.nextFloat()*0.5, pos.getZ() + rand.nextFloat(), rand.nextFloat()*0.01, rand.nextFloat()*0.015, rand.nextFloat()*0.01);
	        	}
        	}
        }
        else if (stateIn.get(heat) == 3){
        	for(int i = 0; i < 3; i++) { 
        		worldIn.addParticle(ParticleTypes.FLAME, pos.getX() + rand.nextFloat()*1.1, pos.getY() + rand.nextFloat(), pos.getZ() + rand.nextFloat()*1.1, 0, 0, 0);
        	}
        }
        if (worldIn.getBlockState(pos.up()).getMaterial().equals(Material.WATER) && stateIn.get(heat) == 3) {
        	for(int i = 0; i < 5; i++) { 
        		worldIn.addParticle(ParticleTypes.CLOUD, pos.getX() + rand.nextFloat(), pos.getY() + 1, pos.getZ() + rand.nextFloat(), 0, rand.nextFloat()*0.1, 0);	        	
	        }
        }
    }
}
