package com.jopdesign.io;

import com.jopdesign.sys.Const;
import com.jopdesign.sys.JVMHelp;
import com.jopdesign.sys.Native;

public class AeroQuadIOFactory extends IOFactory {

    public final PwmChannel pwm0;
    public final PwmChannel pwm1;

    // Handles should be the first static fields!
    private static int PWM0_PTR;
    private static int PWM0_MTAB;

    private static int PWM1_PTR;
    private static int PWM1_MTAB;

    AeroQuadIOFactory() {
        pwm1 = (PwmChannel) makeHWObject(new PwmChannel(), Const.IO_BASE+0x31, 0);
        pwm0 = (PwmChannel) makeHWObject(new PwmChannel(), Const.IO_BASE+0x30, 1);

        System.out.println("PWM0_PTR=" + PWM0_PTR);
        System.out.println("PWM0_MTAB=" + PWM0_MTAB);

        System.out.println("PWM1_PTR=" + PWM1_PTR);
        System.out.println("PWM1_MTAB=" + PWM1_MTAB);
    };

    // that has to be overridden by each sub class to get
    // the correct cp
    private static Object makeHWObject(Object o, int address, int idx) {
        int cp = Native.rdIntMem(Const.RAM_CP);
        return JVMHelp.makeHWObject(o, address, idx, cp);
    }

    public static final AeroQuadIOFactory instance = new AeroQuadIOFactory();
}
