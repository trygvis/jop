#include <fx2types.h>
#include <fx2regs.h>
#include <delay.h>
#include <stdio.h>

#include "ztex.h"

#include <string.h>

// Functions to interface to the ZTEX board
//
// Board schematics:
//   http://www.ztex.de/downloads/usb-fpga-1.11.pdf
//
// Spartan-6 FPGA Configuration User Guide:
//   http://www.xilinx.com/support/documentation/user_guides/ug380.pdf

/**
 * The ZTEX board use the "Slave SelectMAP" configuration interface mechanism which
 * is a simple 8-bit parallel interface.
 *
 * Connections between the FX2 chip and the FPGA:
 *
 * PA0: INIT_B
 * PA1: PROGRAM_B
 * PA2: CSO_B                       Not needed
 * PA3: CCLK
 * PA4: RD/WR
 * PA5: M1
 * PA6: M0
 * PA7: CSI_B
 *
 * PD => D[0:7]
 *
 * The Spartan's DONE signal is wired so that it pulls PROGRAM_B down so is
 * can be sampled to check the DONE signal.
 */

#define PORT_INIT_B     PA0
#define PORT_PROGRAM_B  PA1
#define PORT_CCLK       PA3
#define PORT_RDWR_B     PA4
#define PORT_M1         PA5
#define PORT_M0         PA6
#define PORT_CSI_B      PA7

#define bmINIT_B        bmBIT0
#define bmPROGRAM_B     bmBIT1
#define bmCCLK          bmBIT3
#define bmRDWR_B        bmBIT4
#define bmM1            bmBIT5
#define bmM0            bmBIT6
#define bmCSI_B         bmBIT7

extern __code struct ztex_descriptor descriptor;

static struct ztex_status ztex_status;
static BYTE sum;

void ztex_init() {
    printf(__FILE__ ": ztex_init()\n");

    // Make port A a "normal" IO port
    PORTACFG = 0x00;

    // This "ties" CSI_B and RDWR_B low as allowed from figure 2-8.
    IOA = bmPROGRAM_B | bmINIT_B | bmM1;

    // Configure output signals, 0 = in, 1 = out
    OEA = bmPROGRAM_B | bmCSI_B | bmRDWR_B | bmCCLK | bmM0 | bmM1;

    OED = 0xff;

    // This takes care of initializing the rest
    ztex_reset_fpga();
}

struct ztex_descriptor* ztex_get_descriptor() {
    return &descriptor;
}

void ztex_get_status(struct ztex_status* s) {
    // Sample the DONE signal as it'll pull down the PROGRAM_B port if it is
    // unconfigured.
    OEA &= ~bmPROGRAM_B;
    ztex_status.unconfigured = !PORT_PROGRAM_B;
    OEA |= bmPROGRAM_B;

    memcpy(s, &ztex_status, sizeof(struct ztex_status));
    printf("sum=0x%02x\n", sum);
    s->checksum = sum;
}

void ztex_reset_fpga() {
    unsigned short k;

    printf(__FILE__ ": ztex_reset_fpga()\n");

    memset(&ztex_status, 0, sizeof(struct ztex_status));
    ztex_status.unconfigured = 1;

    // Reset the FPGA by asserting PROGRAM_B while setting M[1:0]
    // The FPGA will sample the mode bits on a rising INIT_B
    //
    // TODO: Rewrite this to use PORT_ lines, it's easier to read
    // With the nice bit instructions of the 8051 there's hardly any need for default_a

    // This creates the sequence on figure 2-8.
    // See figure 2-10 for the sequence on loading the data.

    // INIT_B is made an output to reset the fpga.
    OEA |= bmINIT_B;
    PORT_PROGRAM_B = 0;
    PORT_INIT_B = 0;
    PORT_PROGRAM_B = 1;
    PORT_INIT_B = 1;

    OEA &= ~bmINIT_B;

//    delay(10);

    printf(__FILE__ ": waiting for reset to complete\n");

    k=0;
    while (!PORT_INIT_B && k<65535)
        k++;

    ztex_status.init_b_states = PORT_INIT_B ? 200 : 100;

    printf(__FILE__ ": reset complete, k=%d, PA0=%d\n", k, PA0);
}

void ztex_upload_bitstream(BYTE *bytes, BYTE count) {
    BYTE b;

    ztex_status.bytes_transferred += count;

    while(count--) {
        b = *bytes;
        IOD = b;
        sum += b;
        ztex_status.checksum += b;
        bytes++;
        PORT_CCLK = 1;
        PORT_CCLK = 0;
    }
}

void ztex_finish_bitstream_upload() {
    WORD k;

    ztex_status.init_b_states += PORT_INIT_B ? 20 : 10;

    k = 65535;
    while (k) {
        k--;
        PORT_CCLK = 1;
        PORT_CCLK = 0;
    }

    /*
    k=0;
    while (!PORT_INIT_B && k<65535) {
        k++;
        PORT_CCLK = 1;
        PORT_CCLK = 0;
    }
    */

//    printf(__FILE__ ": Finishing bitstream upload.\n"
//    "  checksum=0x%02x, k=%d\n", ztex_status.checksum, k);

//    printf(__FILE__ ": Finishing bitstream upload.\n"
//    "  k=%d\n", k);
//    printf(__FILE__ ": Finishing bitstream upload.\n k=");
//    printf("\n");

    ztex_status.init_b_states += PORT_INIT_B ? 2 : 1;
}
