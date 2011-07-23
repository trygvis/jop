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
#include <stdio.h>
#include "ztex.h"

#define SYNCDELAY() SYNCDELAY4;

volatile __bit dosuspend=FALSE;
volatile __bit got_sud;
volatile WORD counter;

void init_cpu() {
//    SETCPUFREQ(CLK_12M);
    SETCPUFREQ(CLK_48M);
}

void init_usb() {
    printf("init_usb()\n");
    REVCTL=0; // not using advanced endpoint controls

    got_sud=FALSE;
    RENUMERATE_UNCOND();

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

void main(void)
{
    init_cpu();
    sio0_init(57600);
    printf("Initializing..\n");

    init_usb();
    ztex_init();

    // Arm the endpoint to tell the host that we're ready to receive
    EP1INBC = 0x80;
    SYNCDELAY();

    printf("Initialization complete\n");
    EA=1;

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

BOOL handle_vendorcommand(BYTE bRequest) {
    BYTE bmRequestType = SETUPDAT[0];
    struct ztex_descriptor* descriptor;

    SUDPTRCTL = 1;

    // get ZTEX descriptor
    if(bmRequestType == 0xc0 && bRequest == 0x22) {
        printf(__FILE__ ": get descriptor\n");

        descriptor = ztex_get_descriptor();

        SUDPTRCTL = 0;
        EP0BCH = 0;
        EP0BCL = sizeof(struct ztex_descriptor);
        SUDPTRH = (BYTE)((((unsigned short)descriptor) >> 8) & 0xff);
        SUDPTRL = (BYTE)(((unsigned short)descriptor) & 0xff);
        return TRUE;
    }
    // Get FPGA state
    else if(bmRequestType == 0xc0 && bRequest == 0x30) {
        printf(__FILE__ ": get fpga state\n");

        ztex_get_status((struct ztex_status*)EP0BUF);

        EP0BCH = 0;
        EP0BCL = sizeof(struct ztex_status);
        return TRUE;
    }
    // Reset FPGA
    else if(bmRequestType == 0x40 && bRequest == 0x31) {
        printf(__FILE__ ": Resetting FPGA\n");
        ztex_reset_fpga();
        return TRUE;
    }
    // Upload bitstream chunk
    /*
    else if(bmRequestType == 0x40 && bRequest == 0x32) {
        printf(__FILE__ ": Uploading bitstream, byte count=%d\n", SETUPDAT[6]);
        // As long as we're dealing with EP0 transfers, only the lower byte
        // count anyway.
        ztex_send_data(EP0BUF, SETUPDAT[6]);
        return TRUE;
    }
    */

    printf(__FILE__ ": Unknown vendor command. bmRequestType=0x%02x, bRequest=0x%02x\n", SETUPDAT[0], bRequest);
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
