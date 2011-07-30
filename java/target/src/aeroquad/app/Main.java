package app;

import joprt.RtThread;
import com.jopdesign.io.*;
import com.jopdesign.sys.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("Aeroquad starting...");

//        new T("RtThread 3-100", 3, 100000);
//        new T("RtThread 6-200", 6, 100000);
//        new T("RtThread 5-300", 5, 300);
//        new T("RtThread 4-400", 4, 400);

//        PwmController pwm = AeroQuadIOFactory.getAeroQuadIOFactory().getPwmController();

        RtThread.startMission();

        SerialPort sp = IOFactory.getFactory().getSerialPort();

        int value = 0;
		for (;;) {
            RtThread.sleepMs(100);
            sp.write('a');
            /*
            System.out.println("pwm.channel0=" + pwm.channel0);
            System.out.println("pwm.channel1=" + pwm.channel1);
            System.out.println("PWM: channel0=" + value);
            Native.wr(value, Const.IO_BASE+0x31);
            pwm.set(0, value);
            value += 100;
            */
		}
    }

    public static class T extends RtThread {
        private final String msg;

        public T(String msg, int priority, int timeUs) {
            super(priority, timeUs);
            this.msg = msg;
        }

        public void run() {
            while(true) {
                System.out.println(msg);
                waitForNextPeriod();
//                RtThread.sleepMs(100);
            }
        }
    }
}
