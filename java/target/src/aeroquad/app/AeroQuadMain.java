package app;

import joprt.RtThread;
import com.jopdesign.io.*;
import com.jopdesign.sys.*;

public class AeroQuadMain {
    public static void main(String[] args) {
        System.out.println("Aeroquad starting...");

//        new T("RtThread 3-100", 3, 100000);
//        new T("RtThread 6-200", 6, 100000);
//        new T("RtThread 5-300", 5, 300);
//        new T("RtThread 4-400", 4, 400);

        PwmChannel pwm0 = AeroQuadIOFactory.instance.pwm0;
        PwmChannel pwm1 = AeroQuadIOFactory.instance.pwm1;

        RtThread.startMission();

        SerialPort sp = IOFactory.getFactory().getSerialPort();

        int value = 0;
		for (;;) {
            RtThread.sleepMs(100);
            System.out.println("pwm0.value=" + pwm0.get());
            pwm0.set(value);
            pwm1.set(256 - value);
            value += 10;
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
