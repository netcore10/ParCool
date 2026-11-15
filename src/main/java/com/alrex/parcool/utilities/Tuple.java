package com.alrex.parcool.utilities;

// Replacement for net.minecraft.util.Tuple, removed in 26.x
public record Tuple<A, B>(A a, B b) {
	public A getA() {
		return a;
	}

	public B getB() {
		return b;
	}
}
