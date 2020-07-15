package com.hyperlynx.coppermech.items;

import com.hyperlynx.coppermech.ModBlocks;
import com.hyperlynx.coppermech.blocks.CopperCoil;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class Pointer extends Item {

	private static int RANGE = 64;
	
	public Pointer() {
		super(new Item.Properties().maxStackSize(1).group(ItemGroup.TOOLS));
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
		player.setActiveHand(hand);
		return ActionResult.resultSuccess(player.getHeldItem(hand));
	}
	
	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.BOW;
	}
	
	@Override
	public int getUseDuration(ItemStack stack) {
		return 100000;
	}
	
	@Override
	public void onUsingTick(ItemStack stack, LivingEntity player, int count) {
		Vec3d eye_vec = player.getEyePosition(1);
		Vec3d look_vec = player.getLook(1);
		Vec3d cast_vec = eye_vec.add(look_vec.x * RANGE, look_vec.y * RANGE, look_vec.z * RANGE);
		BlockRayTraceResult trace = player.world.rayTraceBlocks(new RayTraceContext(eye_vec, cast_vec, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.ANY, player));		
		Vec3d hit_vec = trace.getHitVec();
		
		if(!hit_vec.equals(null)) {
			
			player.world.addParticle(new RedstoneParticleData(1.0f, 0.1f, 0.05f, 1.2f), hit_vec.getX(), hit_vec.getY(), hit_vec.getZ(), 0, 0, 0);
		
			if(!player.world.isRemote) {
				BlockState state_hit = player.world.getBlockState(trace.getPos());
				if(state_hit.getBlock().equals(ModBlocks.COIL.get())) {
					
					// Remember the position of the coil to turn if off later.
					stack.getOrCreateTag().putInt("pos_x", trace.getPos().getX());
					stack.getOrCreateTag().putInt("pos_y", trace.getPos().getY());
					stack.getOrCreateTag().putInt("pos_z", trace.getPos().getZ());
					stack.getOrCreateTag().putBoolean("powering_coil", true);
					
					int pwr = (int) Math.round((1 - (eye_vec.distanceTo(hit_vec) / (RANGE))) * 15);
					if(state_hit.get(CopperCoil.POWER) < pwr) {
						CopperCoil cc = (CopperCoil) state_hit.getBlock(); // Safe because it is established that the block is a coil already.
						cc.updateRedstoneState(player.world, trace.getPos(), state_hit, cc, pwr);
					}
				}
				else {
					BlockPos old_pos = new BlockPos(stack.getOrCreateTag().getInt("pos_x"), stack.getOrCreateTag().getInt("pos_y"), stack.getOrCreateTag().getInt("pos_z"));
					if(!old_pos.equals(trace.getPos()) && stack.getOrCreateTag().getBoolean("powering_coil")) {
						BlockState bs = player.world.getBlockState(old_pos);
						CopperCoil cc = (CopperCoil) bs.getBlock();
						cc.updateRedstoneState(player.world, old_pos, bs, cc, player.world.getStrongPower(old_pos));
						stack.getOrCreateTag().putBoolean("powering_coil", false);
					}
				}
			}
		}
	}
	
	@Override
	public void onPlayerStoppedUsing(ItemStack stack, World world, LivingEntity player, int time) {
		stack.getOrCreateTag().putBoolean("powering_coil", false);
	}
	
	@Override
	public ItemStack onItemUseFinish(ItemStack stack, World world, LivingEntity player) {
		stack.getOrCreateTag().putBoolean("powering_coil", false);
		return stack;
	}
}
