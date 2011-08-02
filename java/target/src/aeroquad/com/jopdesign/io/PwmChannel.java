package com.jopdesign.io;

/**
 * A single PWM channel.
 *
 * Reflects the implementation in sc_pwm.vhd.
 */
public final class PwmChannel extends HardwareObject {

    private volatile int value;

    public void set(int value) {
	this.value = value;
    }

    public int get() {
	return value;
    }
}
