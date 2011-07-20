#include <fx2types.h>
#include <fx2regs.h>
#include <delay.h>

#include "ztex.h"

__xdata BYTE fpga_checksum;         // checksum
__xdata DWORD fpga_bytes;           // transfered bytes
__xdata BYTE fpga_init_b;           // init_b state (should be 222 after configuration)
__xdata BYTE fpga_flash_result;     // result of automatic fpga configuarion from Flash

/**
 * TODO: Add SYNCDELAY before this function.
 */
void reset_fpga() {
    BYTE mode = bmBIT5;
    unsigned short k;
    IFCONFIG = bmBIT7;
    PORTACFG = 0;
    PORTCCFG = 0;

    OEA = bmBIT1 | bmBIT3 | bmBIT4 | bmBIT5 | bmBIT6 | bmBIT7;
    IOA = bmBIT7 | mode;
    delay(10);

    OEC &= ~bmBIT3;

    IOA = bmBIT1 | mode;                                // ready for configuration
    k=0;
    while (!PA0 && k<65535)
        k++;

    fpga_init_b = PA0 ? 200 : 100;
    fpga_bytes = 0;
    fpga_checksum = 0;
}
