#include "ztex.h"

// Make sure that this descriptor is put in the DSCR_AREA memory area.
// This are has to be kept even-aligned.
#pragma constseg DSCR_AREA

// 40 bytes, no padding required.
__code struct ztex_descriptor descriptor = {
    40,                                     // ZTEX_DESCRIPTOR_SIZE
    1,                                      // ZTEX_DESCRIPTOR_VERSION
    {'Z', 'T', 'E', 'X'},                   // ZTEXID
    {10, 11, 0, 0},                         // PRODUCT_ID
    0,                                      // FW_VERSION
    1,                                      // INTERFACE_VERSION
    {2, 0, 0, 0, 0, 0},                     // INTERFACE_CAPABILITIES
    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},   // MODULE_RESERVED
    {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}          // SN_STRING
};
