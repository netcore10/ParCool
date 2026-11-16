package com.alrex.parcool.common.action.impl;

import com.alrex.parcool.api.SoundEvents;
import com.alrex.parcool.client.animation.impl.KongVaultAnimator;
import com.alrex.parcool.client.animation.impl.SpeedVaultAnimator;
import com.alrex.parcool.client.animation.impl.StepUpAnimator;
import com.alrex.parcool.client.input.KeyBindings;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.StaminaConsumeTiming;
import com.alrex.parcool.common.attachment.client.Animation;
import com.alrex.parcool.common.attachment.common.Parkourability;
import com.alrex.parcool.config.ParCoolConfig;
import com.alrex.parcool.utilities.WorldUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public class Vault extends Action {
	public enum TypeSelectionMode {
		SpeedVault, KongVault, Dynamic
	}
	public static final int MAX_TICK = 11;
	public static final int STEP_UP_TICK = 5;

	public enum AnimationType {
		SPEED_VAULT_RIGHT, SPEED_VAULT_LEFT, KONG_VAULT, STEP_UP
	}

	//only in client
	private double stepHeight = 0;
	private Vec3 stepDirection = null;
	@Nullable
	private AnimationType currentAnimation;

	@Nullable
	public AnimationType getCurrentAnimation() {
		if (!isDoing()) return null;
		return currentAnimation;
	}

	// Vanilla cancels sprinting on a head-on wall bump, which happens the moment you clip a step at an angle.
	// Keep vault available briefly after running stops so turning into the step still vaults.
	private static final int RUNNING_GRACE_TICKS = 20; // about 1s: long enough to turn the camera onto the step
	private int lastRunningTick = Integer.MIN_VALUE / 2;

	private boolean recentlyRunning(Player player) {
		// Only while still pushing forward: letting go or backing up ends the grace
		return player.zza > 0 && player.tickCount - lastRunningTick <= RUNNING_GRACE_TICKS;
	}

	// Quick step-up on block staircases: a run of full blocks rising one block per block forward. A lone 1-block ledge
	// still vaults; once stepping up a staircase, the top step is a step-up too.
	private static final int STEP_CHAIN_TICKS = 8;
	private int lastStepUpTick = Integer.MIN_VALUE / 2;
	private boolean rightLeadsNext;
	private double stepUpSpeed;

	private boolean isStepUp(Player player, Vec3 step, double wallHeight) {
		if (!ParCoolConfig.Client.Booleans.QuickStepUpOnStaircases.get()) return false;
		if (wallHeight >= 1.1) return false; // one block (wall height is measured in ~0.1 slices)
		return isStepChaining(player) || WorldUtil.isBlockStaircaseAhead(player, step);
	}

	/** Going up a block staircase right now: a step-up just happened and forward is still held. */
	public boolean isStepChaining(Player player) {
		return player.zza > 0 && player.tickCount - lastStepUpTick <= STEP_CHAIN_TICKS;
	}

	// Vanilla drops sprint on every step bump; on a staircase that toggled sprint and fast run each step and pumped
	// the FOV. Restore it on the bump tick, only while still climbing with forward held.
	@Override
	public void onClientTick(Player player, Parkourability parkourability) {
		if (player.isLocalPlayer() && isStepChaining(player) && !player.isSprinting()) {
			player.setSprinting(true);
		}
	}

	//@OnlyIn(Dist.CLIENT)
	@Override
	public boolean canStart(Player player, Parkourability parkourability, ByteBuffer startInfo) {
		// Track before any early return, so a failed angled approach still counts as "was running"
		if (parkourability.get(FastRun.class).canActWithRunning(player)) lastRunningTick = player.tickCount;
		Vec3 lookVec = player.getLookAngle();
		lookVec = new Vec3(lookVec.x(), 0, lookVec.z()).normalize();
		Vec3 step = WorldUtil.getVaultableStep(player);
		if (step == null) return false;
		step = step.normalize();
		//doing "vec/stepDirection" as complex number(x + z i) to calculate difference of player's direction to steps
		Vec3 dividedVec =
				new Vec3(
						lookVec.x() * step.x() + lookVec.z() * step.z(), 0,
						-lookVec.x() * step.z() + lookVec.z() * step.x()
				).normalize();
		if (dividedVec.x() < 0.5) { // within 60 degrees of facing the step (was 45)
			return false;
		}
		AnimationType animationType;
		switch (ParCoolConfig.Client.getInstance().VaultAnimationMode.get()) {
			case KongVault:
				animationType = AnimationType.KONG_VAULT;
				break;
			case SpeedVault:
				animationType = dividedVec.z() > 0 ? AnimationType.SPEED_VAULT_RIGHT : AnimationType.SPEED_VAULT_LEFT;
				break;
			default:
				if (dividedVec.x() > 0.99) {
					animationType = AnimationType.KONG_VAULT;
				} else {
					animationType = dividedVec.z() > 0 ? AnimationType.SPEED_VAULT_RIGHT : AnimationType.SPEED_VAULT_LEFT;
				}
				break;
		}
		double wallHeight = WorldUtil.getWallHeight(player);
		if (isStepUp(player, step, wallHeight)) animationType = AnimationType.STEP_UP;
		startInfo
				.put((byte) animationType.ordinal())
				.putDouble(step.x())
				.putDouble(step.y())
				.putDouble(step.z())
				.putDouble(wallHeight);

		return (!(ParCoolConfig.Client.Booleans.VaultKeyPressedNeeded.get() && !KeyBindings.getKeyVault().isDown())
				&& !player.isInWater()
				&& recentlyRunning(player)
				&& (player.onGround() || ParCoolConfig.Client.Booleans.EnableVaultInAir.get())
				&& wallHeight > player.getBbHeight() * 0.44 /*about 0.8*/
		);
	}

	@Override
	public boolean canContinue(Player player, Parkourability parkourability) {
		return getDoingTick() < (currentAnimation == AnimationType.STEP_UP ? STEP_UP_TICK : MAX_TICK);
	}

	private int getVaultAnimateTime() {
		return 2;
	}

	//@OnlyIn(Dist.CLIENT)
	@Override
	public void onStartInLocalClient(Player player, Parkourability parkourability, ByteBuffer startData) {
		currentAnimation = AnimationType.values()[startData.get()];
		if (ParCoolConfig.Client.Booleans.EnableActionSounds.get() && currentAnimation != AnimationType.STEP_UP)
            player.playSound(SoundEvents.VAULT.get(), 1f, 1f);
		stepDirection = new Vec3(startData.getDouble(), startData.getDouble(), startData.getDouble());
		stepHeight = startData.getDouble();
		// Step-ups keep running speed (the vault slows to a crawl on the way up)
		Vec3 motion = player.getDeltaMovement();
		stepUpSpeed = Math.max(Math.sqrt(motion.x() * motion.x() + motion.z() * motion.z()), 0.2);
		Animation animation = Animation.get(player);
		if (animation != null && currentAnimation != null) {
			switch (currentAnimation) {
				case SPEED_VAULT_RIGHT:
					animation.setAnimator(new SpeedVaultAnimator(SpeedVaultAnimator.Type.Right));
					break;
				case SPEED_VAULT_LEFT:
					animation.setAnimator(new SpeedVaultAnimator(SpeedVaultAnimator.Type.Left));
					break;
				case KONG_VAULT:
					animation.setAnimator(new KongVaultAnimator());
					break;
				case STEP_UP:
					animation.setAnimator(new StepUpAnimator(rightLeadsNext));
					rightLeadsNext = !rightLeadsNext;
					break;
			}
		}
	}

	//@OnlyIn(Dist.CLIENT)
	@Override
	public void onStartInOtherClient(Player player, Parkourability parkourability, ByteBuffer startData) {
		currentAnimation = AnimationType.values()[startData.get()];
		if (ParCoolConfig.Client.Booleans.EnableActionSounds.get() && currentAnimation != AnimationType.STEP_UP)
			player.playSound(SoundEvents.VAULT.get(), 1f, 1f);
		Animation animation = Animation.get(player);
		if (animation != null && currentAnimation != null) {
			switch (currentAnimation) {
				case SPEED_VAULT_RIGHT:
					animation.setAnimator(new SpeedVaultAnimator(SpeedVaultAnimator.Type.Right));
					break;
				case SPEED_VAULT_LEFT:
					animation.setAnimator(new SpeedVaultAnimator(SpeedVaultAnimator.Type.Left));
					break;
				case KONG_VAULT:
					animation.setAnimator(new KongVaultAnimator());
					break;
				case STEP_UP:
					animation.setAnimator(new StepUpAnimator(rightLeadsNext));
					rightLeadsNext = !rightLeadsNext;
					break;
			}
		}
	}

	//@OnlyIn(Dist.CLIENT)
	@Override
	public void onWorkingTickInLocalClient(Player player, Parkourability parkourability) {
		// A vault counts as running, so chained vaults (staircases) keep the grace window open
		lastRunningTick = player.tickCount;
		if (stepDirection == null) return;
		if (currentAnimation == AnimationType.STEP_UP) {
			lastStepUpTick = player.tickCount;
			// Carry running speed only while forward is held, so letting go stops on the step
			Vec3 forward = player.zza > 0 ? stepDirection.normalize().scale(stepUpSpeed) : Vec3.ZERO;
			if (getDoingTick() < getVaultAnimateTime()) {
				player.setDeltaMovement(
						forward.x(),
						((stepHeight + 0.02) / this.getVaultAnimateTime()) / (player.getBbHeight() / 1.8),
						forward.z()
				);
			} else if (getDoingTick() == getVaultAnimateTime()) {
				player.setDeltaMovement(forward.x(), 0, forward.z());
			}
			return;
		}
		if (getDoingTick() < getVaultAnimateTime()) {
			player.setDeltaMovement(
					stepDirection.x() / 10,
					((stepHeight + 0.02) / this.getVaultAnimateTime()) / (player.getBbHeight() / 1.8),
					stepDirection.z() / 10
			);
		} else if (getDoingTick() == getVaultAnimateTime()) {
			stepDirection = stepDirection.normalize();
			player.setDeltaMovement(
					stepDirection.x() * 0.45,
					0.075 * (player.getBbHeight() / 1.8),
					stepDirection.z() * 0.45
			);
		}
	}

	@Override
	public StaminaConsumeTiming getStaminaConsumeTiming() {
		return StaminaConsumeTiming.OnStart;
	}
}

