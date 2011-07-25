#ifndef ZTEX_H
#define ZTEX_H

#include "fx2types.h"

struct ztex_descriptor {
    BYTE size;                      // ZTEX_DESCRIPTOR_SIZE
    BYTE version;                   // ZTEX_DESCRIPTOR_VERSION
    BYTE id[4];                     // ZTEXID
    BYTE product_id[4];             // PRODUCT_ID
    BYTE fw_version;                // FW_VERSION
    BYTE interface_version;         // INTERFACE_VERSION
    BYTE interface_capabilities[6]; // INTERFACE_CAPABILITIES
    BYTE reserved[12];              // MODULE_RESERVED
    BYTE serial_number[10];         // SN_STRING
};

struct ztex_status {
    BYTE unconfigured;
    BYTE checksum;
    DWORD bytes_transferred;
    BYTE init_b_states;
    BYTE flash_result;
    BYTE bit_order;
};

struct ztex_descriptor* ztex_get_descriptor();

void ztex_get_status(struct ztex_status*);

void ztex_init();

void ztex_reset_fpga();

void ztex_upload_bitstream(BYTE *, BYTE);

void ztex_finish_bitstream_upload();

#endif
