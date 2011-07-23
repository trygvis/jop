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

extern struct ztex_descriptor descriptor;

struct ztex_status status;

void ztex_init() {
    printf(__FILE__ ": ztex_init()\n");

    // Make port A a "normal" IO port
    PORTACFG = 0x00;

    // Configure output signals, 0 = in, 1 = out
    OEA = bmPROGRAM_B | bmCSI_B | bmRDWR_B | bmCCLK | bmM0 | bmM1;

    OED = 0xff;

    // This takes care of initializing the rest
    ztex_reset_fpga();
}

#define dump_bits(b) (b); printf(__FILE__ ": %c%c%c%c %c%c%c%c\n", ((b) & bmBIT7) ? '1' : '0', ((b) & bmBIT6) ? '1' : '0', ((b) & bmBIT5) ? '1' : '0', ((b) & bmBIT4) ? '1' : '0', ((b) & bmBIT3) ? '1' : '0', ((b) & bmBIT2) ? '1' : '0', ((b) & bmBIT1) ? '1' : '0', ((b) & bmBIT0) ? '1' : '0');

/**
 * The default value of port a when idle. 
 * CSI_B  = 0 => enable SelectMAP interface 
 * RDWR_B = 0 => write
 * CCLK is low, the FPGA samples on rising edge
 * M[1:0] = b10 => Slave SelectMAP
 */
#define default_a (bmPROGRAM_B | bmINIT_B | bmM1)

struct ztex_descriptor* ztex_get_descriptor() {
    return &descriptor;
}

void ztex_get_status(struct ztex_status* s) {
    memcpy(s, &status, sizeof(struct ztex_status));
}

/**
 * TODO: Add SYNCDELAY before this function.
 */
void ztex_reset_fpga() {
    unsigned short k;

    printf(__FILE__ ": ztex_reset_fpga()\n");

    memset(&status, 0, sizeof(struct ztex_status));
    status.unconfigured = 1;

    // Reset the FPGA by asserting PROGRAM_B while setting M[1:0]
    // The FPGA will sample the mode bits on a rising INIT_B
    //
    // TODO: Rewrite this to use PORT_ lines, it's easier to read
    // With the nice bit instructions of the 8051 there's hardly any need for default_a
    IOA = dump_bits(default_a);
    IOA = dump_bits(default_a & ~bmPROGRAM_B);
    IOA = dump_bits(default_a & ~(bmPROGRAM_B | bmINIT_B));
    IOA = dump_bits(default_a & ~bmINIT_B);
    IOA = dump_bits(default_a);

//    delay(10);

    printf(__FILE__ ": waiting for reset to complete\n");

    // Make PORT_INIT_B an input so it can be sampled
    OEA &= ~bmINIT_B;

    k=0;
    while (!PORT_INIT_B && k<65535)
        k++;

    status.init_b_states = PA0 ? 200 : 100;
    status.bytes_transferred = 0;
    status.checksum = 0;

    printf(__FILE__ ": reset complete, k=%d, PA0=%d\n", k, PA0);

    // Make PORT_INIT_B an output again
    OEA |= bmINIT_B;
}

void ztex_send_data(BYTE *bytes, WORD count) {
    WORD i;

    for(i = 0; i < count; i++) {
        IOD = *bytes;
        bytes++;
        PORT_CCLK = 1;
        PORT_CCLK = 0;
    }
}
