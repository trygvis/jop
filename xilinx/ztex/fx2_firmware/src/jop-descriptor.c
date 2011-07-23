#include <setupdat.h>
#include <descriptors.h>

#pragma constseg DSCR_AREA

#define bNumEndpoints 2

__code struct device_descriptor dev_dscr = {
    sizeof(struct device_descriptor),
    DSCR_DEVICE_TYPE,
    0x0200,                 // bcdUSB
    0xff,                   // bDeviceClass
    0xff,                   // bDeviceSubClass
    0xff,                   // bDeviceProtocol
    64,                     // bMaxPacketSize0
    0x221a,                 // idVendor
    0x0100,                 // idProduct
    0x0000,                 // bcdDevice
    2,                      // iManufacturer
    1,                      // iProduct
    1,                      // iSerialNumber
    1                       // bNumConfigurations
};

__code struct qualifier_descriptor dev_qual_dscr = {
    sizeof(struct qualifier_descriptor),
    DSCR_DEVQUAL_TYPE,
    0x0200,                 // bcdUSB
    0xff,                   // bDeviceClass
    0xff,                   // bDeviceSubClass
    0xff,                   // bDeviceProtocol
    64,                     // bMaxPacketSize0
    1,                      // bNumConfigurations
    0
};

__code struct highspd_dscr_t {
    struct configuration_descriptor descriptor;
    struct interface_descriptor interface;
    struct endpoint_descriptor endpoint81;
    struct endpoint_descriptor endpoint01;
} highspd_dscr = {
    {
        sizeof(struct configuration_descriptor),
        DSCR_CONFIG_TYPE,
        sizeof(struct highspd_dscr_t),
        1,                  // bNumInterfaces
        1,                  // bConfigurationValue
        4,                  // iConfiguration
        0xc0,               // bmAttributes
        0x32                // bMaxPower
    },
    {
        sizeof(struct interface_descriptor),
        DSCR_INTERFACE_TYPE,
        0,                  // bInterfaceNumber
        0,                  // bAlternateSetting
        bNumEndpoints,
        0xff,               // bInterfaceClass
        0xff,               // bInterfaceSubClass
        0xff,               // bInterfaceProtocol
        0                   // iInterface
    },
    {
        sizeof(struct endpoint_descriptor),
        DSCR_ENDPOINT_TYPE,
        0x81,               // bEndpointAddress
        ENDPOINT_TYPE_BULK, // bmAttributes
        512,                // wMaxPacketSize
        0x00                // bInterval
    },
    {
        sizeof(struct endpoint_descriptor),
        DSCR_ENDPOINT_TYPE,
        0x01,               // bEndpointAddress
        ENDPOINT_TYPE_BULK, // bmAttributes
        512,                // wMaxPacketSize
        0x00                // bInterval
    },
};

__code struct fullspd_dscr_t {
    struct configuration_descriptor descriptor;
    struct interface_descriptor interface;
    struct endpoint_descriptor endpoint81;
    struct endpoint_descriptor endpoint01;
} fullspd_dscr = {
    {
        sizeof(struct configuration_descriptor),
        DSCR_CONFIG_TYPE,
        sizeof(struct fullspd_dscr_t),
        1,
        1,
        0,
        0x80,
        0x32
    },
    {
        sizeof(struct interface_descriptor),
        DSCR_INTERFACE_TYPE,
        0,                  // bInterfaceNumber
        0,                  // bAlternateSetting
        bNumEndpoints,
        0xff,               // bInterfaceClass
        0xff,               // bInterfaceSubClass
        0xff,               // bInterfaceProtocol
        0                   // iInterface
    },
    {
        sizeof(struct endpoint_descriptor),
        DSCR_ENDPOINT_TYPE,
        0x81,               // bEndpointAddress
        ENDPOINT_TYPE_BULK, // bmAttributes
        512,                // wMaxPacketSize
        0x00                // bInterval
    },
    {
        sizeof(struct endpoint_descriptor),
        DSCR_ENDPOINT_TYPE,
        0x01,               // bEndpointAddress
        ENDPOINT_TYPE_BULK, // bmAttributes
        512,                // wMaxPacketSize
        0x00                // bInterval
    },
};

// Strings are no go for now, need to adjust setupdat.c
//#define USB_STRING(str) {sizeof(str) + 1, DSCR_STRING_TYPE, str}
//
//__code __at 0x3e00+sizeof(struct device_descriptor)+sizeof(struct qualifier_descriptor)+sizeof(struct highspd_dscr_t)+sizeof(struct fullspd_dscr_t)
//__code
//struct usb_string dev_strings[2] = {
//    {sizeof("H\0i\0!") + 1, DSCR_STRING_TYPE, "H\0i\0!"}
//    {sizeof("H\0i\0!") + 1, DSCR_STRING_TYPE, "H\0i\0!"}
//    USB_STRING("H\0i\0!\0"),
//    USB_STRING("T\0h\0e\0r\0e\0")
//};
