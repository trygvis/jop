#include <autovector.h>
#include <delay.h>
#include <eputils.h>
#include <fx2bits.h>
#include <fx2extra.h>
#include <fx2ints.h>
#include <fx2macros.h>
#include <fx2regs.h>
// #include <fx2timer.h>
#include <setupdat.h>
#include <serial.h>
#include "ztex.h"

#define SYNCDELAY() SYNCDELAY4;

volatile __bit dosuspend=FALSE;
volatile __bit got_sud;
volatile WORD counter;

void init_usb() {
    REVCTL=0; // not using advanced endpoint controls

    got_sud=FALSE;
    RENUMERATE_UNCOND();

//    SETCPUFREQ(CLK_12M);
    SETCPUFREQ(CLK_48M);

    SETIF48MHZ();

    USE_USB_INTS();

    ENABLE_SUDAV();
    ENABLE_USBRESET();
    ENABLE_HISPEED();

    // Endpoint 1 in
    EP1INCFG &= ~bmVALID + EPCFG_TYPE_BULK;
    SYNCDELAY();

    // Endpoint 1 out
    EP1OUTCFG &= ~bmVALID + EPCFG_TYPE_BULK;
    SYNCDELAY();

    // Endpoint 2
    EP2CFG &= ~bmVALID;
    SYNCDELAY();

    // Endpoint 4
    EP4CFG &= ~bmVALID;
    SYNCDELAY();

    // Endpoint 6
    EP6CFG &= ~bmVALID;
    SYNCDELAY();

    // Endpoint 8
    EP8CFG &= ~bmVALID;
    SYNCDELAY();
}

void init_port_a() {
    PORTACFG=0x00;      // port A = IO
    OEA = 0x00;         // port A[0:7] = in
}

void main(void)
{
char c = 0;
    init_usb();
    // TODO: init ports
    init_port_a();
//    init_port_b();

    sio0_init(9600);

    // Arm the endpoint to tell the host that we're ready to receive
    EP1INBC = 0x80;
    SYNCDELAY();

    EA=1;

//    reset_fpga();

    PORTECFG=0x00;      // port E = IO
    OEE = 0xFF;         // port E[0:7] = out

    // loop endlessly
    while(1) {
        if (got_sud) {
            handle_setupdata();
            got_sud = FALSE;
        }

        IOE = 0xff;
        IOE = 0x00;
        putchar(c);
        c++;

        if(c== 0) {
            delay(100);
        }

        /*
        // If EP1 out busy (meaning does not not valid data), twiddle tumbs
        if(EP01STAT & bmBIT1) {
            continue;
        }

        EP1OUTBC=0x00; // Arms EP1 out
        SYNCDELAY();
        */
    }
}

// -----------------------------------------------------------------------
//
// -----------------------------------------------------------------------

BOOL handle_vendorcommand(BYTE cmd) {
    switch(cmd) {
        // request
        case 0x30:				// get fpga state
            break;
        // command
        case 0x31:				// reset fpga
            break;
        // command
        case 0x32:				// send fpga configuration data
            break;
    }
    return FALSE;
}

BOOL handle_get_interface(BYTE ifc, BYTE* alt_ifc) {
    if (ifc==0) {
        *alt_ifc=0;
        return TRUE;
    }
    else {
        return FALSE;
    }
}

BOOL handle_set_interface(BYTE ifc, BYTE alt_ifc) {
    if (ifc == 0 && alt_ifc == 0) {
        // SEE TRM 2.3.7
        RESETTOGGLE(0x02);
        RESETTOGGLE(0x86);
        RESETFIFO(0x02);
        EP2BCL = 0x80;
        SYNCDELAY();
        EP2BCL = 0x80;
        SYNCDELAY();
        RESETFIFO(0x86);
        return TRUE;
    }

    return FALSE;
}

BYTE handle_get_configuration() {
    return 1;
}

BOOL handle_set_configuration(BYTE cfg) {
    return cfg==1 ? TRUE : FALSE; // we only handle cfg 1
}

void handle_reset_ep(BYTE ep) {
    // silence warning
    ep = ep;
}

// -----------------------------------------------------------------------
// Timer
// -----------------------------------------------------------------------

// void timer0_isr() __interrupt TF0_ISR {
//     fx2_timer0_isr();
// }

// -----------------------------------------------------------------------
//
// -----------------------------------------------------------------------

// TODO: Add sut_isr

void sudav_isr() __interrupt SUDAV_ISR {
    got_sud=TRUE;
    CLEAR_SUDAV();
}

void usbreset_isr() __interrupt USBRESET_ISR {
    handle_hispeed(FALSE);
    CLEAR_USBRESET();
}

void hispeed_isr() __interrupt HISPEED_ISR {
    handle_hispeed(TRUE);
    CLEAR_HISPEED();
}

void resume_isr() __interrupt RESUME_ISR {
    CLEAR_RESUME();
}

void suspend_isr() __interrupt SUSPEND_ISR {
    dosuspend=TRUE;
    CLEAR_SUSPEND();
}
