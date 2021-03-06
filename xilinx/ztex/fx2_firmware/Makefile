CC=sdcc
CPP=sdcpp
CPP=cpp

AS=asx8051
FX2LIBDIR?=fx2lib

SDCCFLAGS=-mmcs51 \
          --code-size 0x3c00 \
          --xram-size 0x0200 \
          --xram-loc 0x3c00 \
          -I$(FX2LIBDIR)/include

ifeq ($(DEVICE),)
OPT_D=
else
OPT_D=-d=$(DEVICE)
endif

#
# Targets
#

firmwares=src/jop.c

all: target $(patsubst src/%.c,target/%.rel,$(firmwares)) $(patsubst src/%.c,target/%.ihx,$(firmwares))

#
# Dependencies
#

# Compile units dependencies
target/jop.rel: src/ztex.h
target/ztex.rel: src/ztex.h

# Link unit dependencies
target/jop.ihx: target/jop-descriptor.rel target/jop-dscr.rel target/ztex.rel target/ztex-descriptor.rel

splint:
	splint \
		-booltype BOOL \
		-I$(FX2LIBDIR)/include \
		-I/usr/share/sdcc/include \
		-I/usr/share/sdcc/include/mcs51 \
		$(wildcard src/*.c)

#
# Common targets
#

clean:
	@echo Cleaning...
	@rm -rf target

target:
	@mkdir -p target

list:
	cycfx2prog --list

program-%: target/%.ihx
	cycfx2prog $(OPT_D) prg:$< run

#
# Rules
#

target/%.rel: src/%.c
	@echo CC $<
	@mkdir -p target
	@$(CC) $(SDCCFLAGS) -c -o $@ $<

target/%.rel: src/%.asm
	@echo AS $<
	@$(AS) -logsp $<
	@mkdir -p target
	@mv src/*.rel target/
	@mv src/*.lst target/
	@mv src/*.sym target/

target/%.ihx: target/%.rel
	@echo LD $@
	@$(CC) $(SDCCFLAGS) -Wl"-b INT2JT=0x3D00" -Wl"-b DSCR_AREA=0x3e00" -o $@ $^ fx2.lib -L$(FX2LIBDIR)/lib
