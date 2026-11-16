package com.alrex.parcool.client.animation.impl;

import com.alrex.parcool.client.animation.Animator;
import com.alrex.parcool.client.animation.PlayerModelRotator;
import com.alrex.parcool.client.animation.PlayerModelTransformer;
import com.alrex.parcool.common.action.impl.Vault;
import com.alrex.parcool.common.attachment.common.Parkourability;
import com.alrex.parcool.utilities.Easing;
import net.minecraft.world.entity.player.Player;

/**
 * Quick step-up on a block staircase: the leading knee drives up onto the next block, the trailing leg pushes off, arms
 * pump opposite, body leans in. The leading leg alternates each step so a staircase reads as running up it.
 */
public class StepUpAnimator extends Animator {
	private final boolean rightLeads;

	public StepUpAnimator(boolean rightLeads) {
		this.rightLeads = rightLeads;
	}

	@Override
	public boolean shouldRemoved(Player player, Parkourability parkourability) {
		return getTick() >= Vault.STEP_UP_TICK;
	}

	/** 0 -> 1 -> 0 over the step: up fast, settle slower. */
	private float factor(float partialTick) {
		float phase = (getTick() + partialTick) / Vault.STEP_UP_TICK;
		return new Easing(phase)
				.squareOut(0, 0.35f, 0, 1)
				.sinInOut(0.35f, 1, 1, 0)
				.get();
	}

	@Override
	public void rotatePost(Player player, Parkourability parkourability, PlayerModelRotator rotator) {
		rotator
				.startBasedCenter()
				.rotatePitchFrontward(18 * factor(rotator.getPartialTick()))
				.end();
	}

	@Override
	public void animatePost(Player player, Parkourability parkourability, PlayerModelTransformer transformer) {
		float f = factor(transformer.getPartialTick());
		float lead = (float) Math.toRadians(-80);  // knee up onto the step
		float push = (float) Math.toRadians(30);   // trailing leg pushing off
		float armForward = (float) Math.toRadians(-60);
		float armBack = (float) Math.toRadians(40);
		if (rightLeads) {
			transformer
					.rotateRightLeg(lead, 0, 0, f)
					.rotateLeftLeg(push, 0, 0, f)
					.rotateLeftArm(armForward, 0, 0, f)
					.rotateRightArm(armBack, 0, 0, f)
					.end();
		} else {
			transformer
					.rotateLeftLeg(lead, 0, 0, f)
					.rotateRightLeg(push, 0, 0, f)
					.rotateRightArm(armForward, 0, 0, f)
					.rotateLeftArm(armBack, 0, 0, f)
					.end();
		}
	}
}
