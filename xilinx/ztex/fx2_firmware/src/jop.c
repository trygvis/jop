#include <autovector.h>
#include <delay.h>
#include <eputils.h>
#include <fx2bits.h>
#include <fx2extra.h>
#include <fx2ints.h>
#include <fx2macros.h>
#include <setupdat.h>
#include <serial.h>
#include <stdio.h>
#include "ztex.h"

#define SYNCDELAY() SYNCDELAY4;

volatile __bit dosuspend=FALSE;
volatile __bit got_sud=FALSE;

volatile WORD bitstream_chunk=0;
volatile WORD bitstream_chunk_left=0;
volatile __bit last_bitstream_chunk=FALSE;

void init_cpu() {
    SETCPUFREQ(CLK_48M);
}

void init_usb() {
    printf(__FILE__ ": init_usb()\n");

    REVCTL=0; // not using advanced endpoint controls

    RENUMERATE_UNCOND();

    SETIF48MHZ();

    USE_USB_INTS();

    ENABLE_SUDAV();
    ENABLE_USBRESET();
    ENABLE_HISPEED();
//    ENABLE_EP0OUT();
}

WORD count = 0;
void upload_chunk() {
    BYTE left = bitstream_chunk_left > sizeof(EP0BUF) ?
           sizeof(EP0BUF) : bitstream_chunk_left;

    putchar('2');
//    printf(__FILE__ ": bitstream_chunk_left=%d, left=%d\n", bitstream_chunk_left, left);
    if(bitstream_chunk_left <= 0) {
        return;
    }
    ztex_upload_bitstream(EP0BUF, left);
    bitstream_chunk_left -= left;
    count++;

    if(bitstream_chunk_left <= 0) {
        if(last_bitstream_chunk) {
            last_bitstream_chunk = FALSE;

            printf("count=%d\n", count);
            ztex_finish_bitstream_upload();
        }

        putchar('3');
        EP0CS |= bmHSNAK;
    }

    EP0BCL = 0;
    SYNCDELAY();
}

void main(void)
{
    init_cpu();

    sio0_init(57600);
    printf(__FILE__ ": Initializing..\n");

    init_usb();

    printf(__FILE__ ": Initialization complete\n");

    // ARM EP0
    EP0BCL = 0;

    EA=1;

    ztex_init();

    while(1) {
        if (got_sud) {
            handle_setupdata();
            got_sud = FALSE;
        }

        if(!(EP01STAT & bmBIT0) && bitstream_chunk_left > 0) {
            __critical {
            upload_chunk();
            }
        }
        /*
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

        EP0BCH = 0;
        EP0BCL = sizeof(struct ztex_descriptor);
        SUDPTRCTL = 0;
        SUDPTRH = (BYTE)((((unsigned short)descriptor) >> 8) & 0xff);
        SUDPTRL = (BYTE)(((unsigned short)descriptor) & 0xff);
        EP0CS |= bmHSNAK;
        return TRUE;
    }
    // Get FPGA state
    else if(bmRequestType == 0xc0 && bRequest == 0x30) {
        printf(__FILE__ ": get fpga state\n");

        ztex_get_status((struct ztex_status*)EP0BUF);

        EP0BCH = 0;
        EP0BCL = sizeof(struct ztex_status);
        EP0CS |= bmHSNAK;
        return TRUE;
    }
    // Reset FPGA
    else if(bmRequestType == 0x40 && bRequest == 0x31) {
        printf(__FILE__ ": Resetting FPGA\n");
        ztex_reset_fpga();
        last_bitstream_chunk = FALSE;
        count = 0;
        EP0CS |= bmHSNAK;
        return TRUE;
    }
    // Upload bitstream chunk
    else if(bmRequestType == 0x40 && bRequest == 0x32) {
        putchar('1');
//        printf(__FILE__ ": uploading chunk: size=%d\n", SETUP_LENGTH());
        // TODO: Fail the request unless the FPGA is unconfigured

        bitstream_chunk = SETUP_LENGTH();
        bitstream_chunk_left = bitstream_chunk;

        if(bitstream_chunk != 2048) {
            last_bitstream_chunk = TRUE;
        }

        // ARM EP0, but to not clear HSNAK. HSNAK is cleared when all the data
        // packets are sent.
        EP0BCL = 0;
        return TRUE;
    }

    printf(__FILE__ ": Unknown vendor command.\n"
        "  bmRequestType=0x%02x\n"
        "  bRequest=0x%02x\n", SETUPDAT[0], bRequest);
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
//
// -----------------------------------------------------------------------

void sudav_isr() __interrupt SUDAV_ISR {
//    printf("sudav_isr\n");
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

void ep0out_isr() __interrupt EP0OUT_ISR {
    /*
    __critical {
        upload_chunk();
    }
    */
}
