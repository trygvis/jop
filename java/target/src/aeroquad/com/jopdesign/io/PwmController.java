package com.jopdesign.io;

public final class PwmController extends HardwareObject {

    public volatile int channel0;
    public volatile int channel1;

    public void set(int channel, int value) {
        switch(channel) {
            case 0: channel0 = value; break;
        }
    }
}
