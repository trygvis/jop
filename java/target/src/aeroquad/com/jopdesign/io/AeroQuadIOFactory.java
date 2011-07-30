package com.jopdesign.io;

import com.jopdesign.sys.Const;
import com.jopdesign.sys.JVMHelp;
import com.jopdesign.sys.Native;

public class AeroQuadIOFactory extends IOFactory {

    private PwmController pwm;

    // Handles should be the first static fields!
    private static int PWM_PTR;
    private static int PWM_MTAB;

    AeroQuadIOFactory() {
        pwm = (PwmController) makeHWObject(new PwmController(), Const.IO_BASE+0x30, 0);

        System.out.println("PWM_PTR=" + PWM_PTR);
        System.out.println("PWM_MTAB=" + PWM_MTAB);
    };

    // that has to be overridden by each sub class to get
    // the correct cp
    private static Object makeHWObject(Object o, int address, int idx) {
        int cp = Native.rdIntMem(Const.RAM_CP);
        return JVMHelp.makeHWObject(o, address, idx, cp);
    }

    static AeroQuadIOFactory single = new AeroQuadIOFactory();

    public static AeroQuadIOFactory getAeroQuadIOFactory() {		
        return single;
    }

    public PwmController getPwmController() {
        return pwm;
    }
}
