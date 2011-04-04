#
#	Makefile
#
#	Should build JOP and all tools from scratch.
#
#	not included at the moment:
#		ACEX board
#		configuration CPLD compiling
#		Spartan-3 targets
#
#	You probably want to change the folloing parts in the Makefile:
#
#		QPROJ ... your Quartus FPGA project
#		COM_* ... your communication settings
#		TARGET_APP_PATH, MAIN_CLASS ... your target application
#
#	for a quick change you can also use command line arguments when invoking make:
#		make japp -e QPROJ=cycwrk TARGET_APP_PATH=java/target/src/bench MAIN_CLASS=jbe/DoAll
#
#


#
#	Set USB to true for an FTDI chip based board (dspio, usbmin, lego)
#
USB=true

#
#	com1 is the usual serial port
#	com5 is the FTDI VCOM for the USB download
#		use -usb to download the Java application
#		without the echo 'protocol' on USB
#
ifeq ($(USB),true)
	COM_PORT=COM5
	COM_FLAG=-e -usb
else
	COM_PORT=COM1
	COM_FLAG=-e
endif

#
#	Select the Quartus project
#
# 'some' different Quartus projects
QPROJ=cycmin cycbaseio cycbg dspio lego cycfpu cyc256x16 sopcmin usbmin cyccmp de2-70vga cycrttm de2-70rttm
# if you want to build only one Quartus project use e.q.:
ifeq ($(USB),true)
	QPROJ=usbmin
else
	QPROJ=cycmin
endif

#
#	Select the Xilinx project byt setting XFPGA to true
#	Currently only the ml50x is supported
#	with a full make integration
XPROJ=ml50x
XFPGA=false

# Altera FPGA configuration cable
BLASTER_TYPE=ByteBlasterMV
#BLASTER_TYPE=USB-Blaster

ifeq ($(WINDIR),)
	USBRUNNER=./USBRunner
	S=:
else
	USBRUNNER=USBRunner.exe
	S=\;
endif

#
#	Set CLDC11 to true to use the CLDC11 JDK
#
CLDC11=false

#
#	Kind of JDK 1.6
#
JDK16=false


# Number of cores for JopSim and RTTM simulation
CORE_CNT=1

# Which project do you want to be downloaded?
DLPROJ=$(QPROJ)
# Which project do you want to be programmed into the flash?
FLPROJ=$(DLPROJ)
# IP address for Flash programming
IPDEST=192.168.1.2
IPDEST=192.168.0.123

P1=test
P2=test
P3=HelloWorld
#P2=jvm
#P3=DoAll
#P1=rtapi
#P2=examples/scopes
#P3=LocalScope
#P3=LocalMatrixCalc

#P1=test
#P2=rtlib
#P3=CMPBuffer

#P1=common
#P2=ejip123/examples
#P3=HelloWorldHereIPing

#P1=bench
#P2=jbe
#P3=DoApp
# The test program for Basio and the NAND Flash
#P3=FlashBaseio

#P1=app
#P2=oebb
#P3=Main

P2=wcet
P3=Dispatch
WCET_METHOD=measure

#P1=.
#P2=dsvmmcp
#P3=TestDSVMMCP

################## end of configuration section ###################

#
#	some variables
#
TOOLS=java/tools
# setting for my Eclipse CVS project
# TOOLS=../../workspace/cvs_jop_tools
EXT_CP=java/lib/bcel-5.2.jar$(S)java/lib/jakarta-regexp-1.3.jar$(S)java/lib/RXTXcomm.jar$(S)java/lib/lpsolve55j.jar$(S)java/lib/log4j-1.2.15.jar$(S)java/lib/jgrapht-jdk1.5.jar$(S)java/lib/velocity-1.5.jar$(S)java/lib/velocity-dep-1.5.jar

# The line below makes the compilation crash, because it causes JOPizer to include a *lot*
# of classes which are actually not necessary.
#EXT_CP=java/jopeclipse/com.jopdesign.jopeclipse/lib/bcel-5.2.jar$(S)java/lib/jakarta-regexp-1.3.jar$(S)java/lib/RXTXcomm.jar$(S)java/lib/lpsolve55j.jar
#EXT_CP=java/lib/recompiled_bcel-5.2.jar$(S)java/lib/jakarta-regexp-1.3.jar$(S)java/lib/RXTXcomm.jar$(S)java/lib/lpsolve55j.jar

#TOOLS_JFLAGS=-d $(TOOLS)/dist/classes -classpath $(EXT_CP) -sourcepath $(TOOLS)/src$(S)$(TARGET_SRC_PATH)/common
TOOLS_JFLAGS=-g -d $(TOOLS)/dist/classes -classpath $(EXT_CP) -sourcepath $(TOOLS)/src$(S)$(TARGET_SRC_PATH)/common -encoding Latin1

PCTOOLS=java/pc
PCTOOLS_JFLAGS=-g -d $(PCTOOLS)/dist/classes -sourcepath $(PCTOOLS)/src -encoding Latin1


TARGET=java/target
TARGET_SRC_PATH=$(TARGET)/src

# changed to add another class to the tool chain
#TOOLS_CP=-classpath $(EXT_CP)$(S)$(TOOLS)/dist/lib/jop-tools.jar
TOOLS_CP=-classpath $(TOOLS)/dist/lib/jop-tools.jar$(S)$(TOOLS)/dist/lib/JopDebugger.jar$(S)$(EXT_CP)

ifeq ($(CLDC11),true)
	TARGET_SOURCE=$(TARGET_SRC_PATH)/common$(S)$(TARGET_SRC_PATH)/cldc11/cldc_orig$(S)$(TARGET_SRC_PATH)/cldc11/cldc_mod$(S)$(TARGET_SRC_PATH)/cldc11/jdk_base_orig$(S)$(TARGET_SRC_PATH)/cldc11/jdk_base_mod$(S)$(TARGET_SRC_PATH)/rtapi$(S)$(TARGET_APP_SOURCE_PATH)
else
ifeq ($(JDK16),true)
	TARGET_SOURCE=$(TARGET_SRC_PATH)/common$(S)$(TARGET_SRC_PATH)/jdk_base$(S)$(TARGET_SRC_PATH)/jdk16$(S)$(TARGET_SRC_PATH)/rtapi$(S)$(TARGET_APP_SOURCE_PATH)
else
	TARGET_SOURCE=$(TARGET_SRC_PATH)/common$(S)$(TARGET_SRC_PATH)/jdk_base$(S)$(TARGET_SRC_PATH)/jdk11$(S)$(TARGET_SRC_PATH)/rtapi$(S)$(TARGET_APP_SOURCE_PATH)
endif
endif
TARGET_JFLAGS=-d $(TARGET)/dist/classes -sourcepath $(TARGET_SOURCE) -bootclasspath "" -extdirs "" -classpath "" -source 1.5
GCC_PARAMS=

# uncomment this to use RTTM
#USE_RTTM=yes

ifeq ($(USE_RTTM),yes)
GCC_PARAMS=-DRTTM
endif

# uncomment this if you want floating point operations in hardware
# ATTN: be sure to choose 'cycfpu' as QPROJ else no FPU will be available
#GCC_PARAMS=-DFPU_ATTACHED

#
#	Add your application source pathes and class that contains the
#	main method here. We are using those simple P1/2/3 variables for
#		P1=directory, P2=package name, and P3=main class
#	for sources 'inside' the JOP source tree
#
#	TARGET_APP_PATH is the path to your application source
#
#	MAIN_CLASS is the class that contains the Main method with package names
#
TARGET_APP_PATH=$(TARGET_SRC_PATH)/$(P1)
MAIN_CLASS=$(P2)/$(P3)

# here an example how to define an application outside
# from the jop directory tree
#TARGET_APP_PATH=/usr2/muvium/jopaptalone/src
#MAIN_CLASS=com/muvium/eclipse/PeriodicTimer/JOPBootstrapLauncher


#	add more directoies here when needed
#		(and use \; to escape the ';' when using a list!)
TARGET_APP_SOURCE_PATH=$(TARGET_APP_PATH)$(S)$(TARGET_SRC_PATH)/bench$(S)$(TARGET_SRC_PATH)/app
TARGET_APP=$(TARGET_APP_PATH)/$(MAIN_CLASS).java


# setting for rup DSVM on JopCMP
#TARGET_APP_PATH=/usrx/DSVMFP/src

# just any name that the .jop file gets.
JOPBIN=$(P3).jop


#
#	Debugger stuff
#
# Added flags for development with JDWP
#DEBUG_PORT = 8000
DEBUG_PORT = 8001
DEBUG_PARAMETERS= -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=$(DEBUG_PORT)
#DEBUG_PARAMETERS= 

#DEBUG_JOPIZER=$(DEBUG_PARAMETERS)
DEBUG_JOPIZER=

#DEBUG_JOPSIM=$(DEBUG_PARAMETERS)
DEBUG_JOPSIM=

#
#	application optimization with ProGuard:
#	proguard.sourceforge.net/
#	uncomment following line to use it
#OPTIMIZE=mv java/target/dist/lib/classes.zip java/target/dist/lib/in.zip; java -jar java/lib/proguard.jar @optimize.pro

#
#	application optimization with JCopter
#
USE_JCOPTER?=yes
JCOPTER_OPT?=--dump-callgraph merged --dump-jvm-callgraph merged --callstring-length 2 --target-method $(WCET_METHOD)


# build everything from scratch
all:
	make directories
	make tools
ifeq ($(USB),true)
	make jopusb
else
	make jopser
endif
	make japp

# build the Java application and download it
japp:
	make java_app
	make config
	make download

# configure the FPGA
config:
ifeq ($(USB),true)
	make config_usb
else
ifeq ($(XFPGA),true)
	make config_xilinx
else
	make config_byteblaster
endif
endif

install:
	@echo nothing to install

# cleanup
EXTENSIONS=class rbf rpt sof pin summary ttf qdf dat wlf done qws

clean:
	for ext in $(EXTENSIONS); do \
		find `ls` -name \*.$$ext -print -exec rm -r -f {} \; ; \
	done
	-find `ls` -name jop.pof -print -exec rm -r -f {} \;
	-find `ls` -name db -print -exec rm -r -f {} \;
	-find `ls` -name incremental_db -print -exec rm -r -f {} \;
	-rm -rf asm/generated
	-rm -f vhdl/*.vhd
	-rm -rf $(TOOLS)/dist
	-rm -rf $(PCTOOLS)/dist
	-rm -rf $(TARGET)/dist
	-rm -rf modelsim/work
	-rm -rf modelsim/transcript
	-rm -rf modelsim/gaisler
	-rm -rf modelsim/grlib
	-rm -rf modelsim/techmap
	cd doc/book && make clean


#
#	build all the (Java) tools
#
tools:
	-rm -rf $(TOOLS)/dist
	mkdir $(TOOLS)/dist
	mkdir $(TOOLS)/dist/lib
	mkdir $(TOOLS)/dist/classes
	javac $(TOOLS_JFLAGS) $(TOOLS)/src/*.java
	javac $(TOOLS_JFLAGS) $(TOOLS)/src/org/apache/bcel/util/*.java
	javac $(TOOLS_JFLAGS) $(TOOLS)/src/com/jopdesign/build/*.java
	javac $(TOOLS_JFLAGS) $(TOOLS)/src/com/jopdesign/tools/*.java
	javac $(TOOLS_JFLAGS) $(TOOLS)/src/com/jopdesign/dfa/*.java
	javac $(TOOLS_JFLAGS) $(TOOLS)/src/com/jopdesign/jcopter/*.java
	javac $(TOOLS_JFLAGS) $(TOOLS)/src/com/jopdesign/wcet/*.java
	cp $(TOOLS)/src/com/jopdesign/wcet/report/*.vm $(TOOLS)/dist/classes/com/jopdesign/wcet/report
# quick hack to get the tools with the debugger ok
# the build.xml from the debugger contains the correct info
# but also some more (old?) stuff
# does not work as some Sun classes for JDWP are missing
#	javac $(TOOLS_JFLAGS) $(TOOLS)/src/com/jopdesign/debug/jdwp/*.java
	cd $(TOOLS)/dist/classes && jar cf ../lib/jop-tools.jar *


# we moved the pc stuff to it's own target to be
# NOT built on make all.
# It depends on javax.comm which is NOT installed
# by default - Blame SUN on this!
#
#	TODO: change it to RXTXcomm if it's working ok
#
pc:
	-rm -rf $(PCTOOLS)/dist
	mkdir $(PCTOOLS)/dist
	mkdir $(PCTOOLS)/dist/lib
	mkdir $(PCTOOLS)/dist/classes
#	make compile_java -e JAVA_DIR=$(PCTOOLS)/src
	javac $(PCTOOLS_JFLAGS) $(PCTOOLS)/src/udp/*.java
	cd $(PCTOOLS)/dist/classes && jar cf ../lib/jop-pc.jar *

#
# make target for the tools that are still in C
#
cprog:
	gcc c_src/amd.c -o amd.exe
	gcc c_src/e.c -o e.exe

#
#	compile and JOPize the application
#
java_app:
	-rm -rf $(TARGET)/dist
	-mkdir $(TARGET)/dist
	-mkdir $(TARGET)/dist/classes
	-mkdir $(TARGET)/dist/lib
	-mkdir $(TARGET)/dist/bin
	javac $(TARGET_JFLAGS) $(TARGET_SRC_PATH)/common/com/jopdesign/sys/*.java
ifeq ($(CLDC11),false)
	javac $(TARGET_JFLAGS) $(TARGET_SRC_PATH)/jdk_base/java/lang/annotation/*.java	# oh new Java 1.5 world!
endif
ifeq ($(USE_RTTM),yes)	
	javac $(TARGET_JFLAGS) $(TARGET_SRC_PATH)/common/rttm/internal/Utils.java
endif
	javac $(TARGET_JFLAGS) $(TARGET_APP)
	# WCETPreprocess, overwrite existing class files 
	java $(DEBUG_JOPIZER) $(TOOLS_CP) com.jopdesign.wcet.WcetPreprocess \
           -c $(TARGET)/dist/classes -o $(TARGET)/dist $(MAIN_CLASS)
ifeq ($(USE_JCOPTER),yes)
	# JOPizer	
	java $(DEBUG_JOPIZER) $(TOOLS_CP) com.jopdesign.jcopter.JCopter \
	   -c $(TARGET)/dist/classes -o $(TARGET)/dist --classdir $(TARGET)/dist/classes.opt \
	   $(JCOPTER_OPT) $(MAIN_CLASS)
	cd $(TARGET)/dist/classes.opt && jar cf ../lib/classes.zip *
else
	cd $(TARGET)/dist/classes && jar cf ../lib/classes.zip *
endif 
# use SymbolManager for Paulo's version of JOPizer instead
	java $(DEBUG_JOPIZER) $(TOOLS_CP) -Dmgci=false com.jopdesign.build.JOPizer \
		-cp $(TARGET)/dist/lib/classes.zip -o $(TARGET)/dist/bin/$(JOPBIN) $(MAIN_CLASS)
#	java $(DEBUG_JOPIZER) $(TOOLS_CP) -Dmgci=false com.jopdesign.debug.jdwp.jop.JopSymbolManager \
#		-cp $(TARGET)/dist/lib/classes.zip -o $(TARGET)/dist/bin/$(JOPBIN) $(MAIN_CLASS)
	java $(TOOLS_CP) com.jopdesign.tools.jop2dat $(TARGET)/dist/bin/$(JOPBIN)
	cp *.dat modelsim
	rm -f *.dat


#
#	project.sof fiels are used to boot from the serial line
#
jopser:
	make gen_mem -e ASM_SRC=jvm JVM_TYPE=SERIAL
ifeq ($(XFPGA),true)
	@echo $(XPROJ)
	cd xilinx/$(XPROJ) && make
else
	@echo $(QPROJ)
	for target in $(QPROJ); do \
		make qsyn -e QBT=$$target || exit; \
	done
endif


#
#	project.rbf fiels are used to boot from the USB interface
#
jopusb:
	make gen_mem -e ASM_SRC=jvm JVM_TYPE=USB
	@echo $(QPROJ)
	for target in $(QPROJ); do \
		make qsyn -e QBT=$$target || exit; \
		cd quartus/$$target && quartus_cpf -c jop.sof ../../rbf/$$target.rbf; \
	done

#
#	project.ttf files are used to boot from flash.
#
jopflash:
	make gen_mem -e ASM_SRC=jvm JVM_TYPE=FLASH
	@echo $(QPROJ)
	for target in $(QPROJ); do \
		make qsyn -e QBT=$$target || exit; \
		quartus_cpf -c quartus/$$target/jop.sof ttf/$$target.ttf; \
	done

BLOCK_SIZE=4096
#
#	assemble the microcode and generate on-chip memory files
#
gen_mem:
	rm -rf asm/generated
	mkdir asm/generated
	gcc -x c -E -C -P $(GCC_PARAMS) -D$(JVM_TYPE) asm/src/$(ASM_SRC).asm > asm/generated/jvmgen.asm
	java $(TOOLS_CP) com.jopdesign.tools.Jopa -s asm/generated -d asm/generated jvmgen.asm
# generate Xilinx and Actel memory files
	java $(TOOLS_CP) BlockGen -b $(BLOCK_SIZE) -pd -m xram_block asm/generated/ram.mif asm/generated/xram_block.vhd
	java $(TOOLS_CP) BlockGen -b 16384 -pd -m xram_block asm/generated/ram.mif asm/generated/xv4ram_block.vhd
	java $(TOOLS_CP) GenAsynROM -m actelram_initrom asm/generated/ram.mif asm/generated/actelram_initrom.vhd
# copy generated files into working directories
	cp asm/generated/*.vhd vhdl
	cp asm/generated/*.dat modelsim

# not used targets
#	rem java -cp ..\java\tools\dist\lib\jop-tools.jar BlockGen -b %blocksize% -pd -d 1024 -w 8 -m xjbc_block -o generated
#	rem java -cp ..\java\tools\dist\lib\jop-tools.jar BlockGen -b %blocksize% -pd -m xrom_block generated\rom.mif generated\xrom_block.vhd


#
#	Quartus build process
#		called by jopser, jopusb,...
#
qsyn:
	echo $(QBT)
	echo "building $(QBT)"
	-rm -rf quartus/$(QBT)/db
	-rm -f quartus/$(QBT)/jop.sof
	-rm -f jbc/$(QBT).jbc
	-rm -f rbf/$(QBT).rbf
	quartus_map quartus/$(QBT)/jop
	quartus_fit quartus/$(QBT)/jop
	quartus_asm quartus/$(QBT)/jop
#	quartus_tan quartus/$(QBT)/jop
	quartus_sta quartus/$(QBT)/jop

#
#	Modelsim target
#		without the tools
#
sim: java_app
	make gen_mem -e ASM_SRC=jvm JVM_TYPE=SIMULATION
	cd modelsim && make


#
#	Modelsim target for CMP version of JOP
#		without the tools
#
sim_cmp: java_app
	make gen_mem -e ASM_SRC=jvm JVM_TYPE=SIMULATION
	cd modelsim && make cmp

sim_csp: java_app
	make gen_mem -e ASM_SRC=jvm JVM_TYPE=SIMULATION
	cd modelsim && make csp


#
#	JopSim target
#		without the tools
#		use -Dcpucnt=# for a CMP simulation
#
jsim: java_app
	java $(DEBUG_JOPSIM) -cp java/tools/dist/lib/jop-tools.jar -Dlog="false" \
	com.jopdesign.tools.JopSim java/target/dist/bin/$(JOPBIN)

#
#	Simulate RTTM (Jopsim target)
#
jtmsim: java_app
	java $(DEBUG_JOPSIM) -cp java/tools/dist/lib/jop-tools.jar -Dcpucnt=$(CORE_CNT) \
	com.jopdesign.tools.TMSim java/target/dist/bin/$(JOPBIN)

#
#   Simulate RTTM (Modelsim target)
#
tmsim: java_app
	make gen_mem -e ASM_SRC=jvm JVM_TYPE=SIMULATION
	cd modelsim && ./sim_tm.bat -i -do sim_tm.do

tmsimcon: java_app
	make gen_mem -e ASM_SRC=jvm JVM_TYPE=SIMULATION
	cd modelsim && ./sim_tm.bat -c -do sim_tm_con.do

#
#	RTTM tests on hardware
#

ifeq ($(USB),true)
TEST_JAPP_CONFIG=config_usb
else
TEST_JAPP_CONFIG=config_byteblaster
endif
test_japp: java_app $(TEST_JAPP_CONFIG) test_download

rttm_tests:
	for t in `find java/target/src/test/rttm/tests/*.java -printf %f\\\\n|sed 's/\.java//'`; do \
		make test_japp -e P1=test P2=rttm/tests P3=$$t REFERENCE_PATTERN=java/target/src/test/rttm/tests/$$t.pattern || exit; \
	done

test_download:
	./down $(COM_FLAG) java/target/dist/bin/$(JOPBIN) $(COM_PORT)|java $(TOOLS_CP) com.jopdesign.tools.MatchPattern $(REFERENCE_PATTERN)

#
#	Simulate data cache
#
dcsim: java_app
	java $(DEBUG_JOPSIM) -cp java/tools/dist/lib/jop-tools.jar \
	com.jopdesign.tools.DCacheSim java/target/dist/bin/$(JOPBIN)

#
#	JopServer target
#		without the tools
#
jsim_server: java_app
	java $(DEBUG_JOPSIM) \
	-cp java/tools/dist/lib/jop-tools.jar$(S)$(TOOLS)/dist/lib/JopDebugger.jar -Dlog="false" \
	com.jopdesign.debug.jdwp.jop.JopServer java/target/dist/bin/$(JOPBIN)


config_byteblaster:
	cd quartus/$(DLPROJ) && quartus_pgm -c $(BLASTER_TYPE) -m JTAG jop.cdf

config_usb:
	cd rbf && ../$(USBRUNNER) $(DLPROJ).rbf

config_xilinx:
	cd xilinx/$(XPROJ) && make config


download:
#	java -cp java/tools/dist/lib/jop-tools.jar$(S)java/lib/RXTXcomm.jar com.jopdesign.tools.JavaDown \
#		$(COM_FLAG) java/target/dist/bin/$(JOPBIN) $(COM_PORT)
#
#	this is the download version with down.exe
	./down $(COM_FLAG) java/target/dist/bin/$(JOPBIN) $(COM_PORT)

#
#	flash programming
#
prog_flash: java_app
	quartus_pgm -c $(BLASTER_TYPE) -m JTAG -o p\;jbc/$(DLPROJ).jbc
	down java/target/dist/bin/$(JOPBBIN) $(COM_PORT)
	java -cp java/pc/dist/lib/jop-pc.jar udp.Flash java/target/dist/bin/$(JOPBIN) $(IPDEST)
	java -cp java/pc/dist/lib/jop-pc.jar udp.Flash ttf/$(FLPROJ).ttf $(IPDEST)
	quartus_pgm -c $(BLASTER_TYPE) -m JTAG -o p\;quartus/cycconf/cyc_conf.pof

#
#	flash programming for the BG hardware as an example
#
#prog_flash:
#	quartus_pgm -c ByteblasterMV -m JTAG -o p\;jbc/$(DLPROG).jbc
#	cd java/target && ./build.bat app oebb BgInit
#	down java/target/dist/bin/oebb_BgInit.jop $(COM_PORT)
#	cd java/target && ./build.bat app oebb Main
#	java -cp java/pc/dist/lib/jop-pc.jar udp.Flash java/target/dist/bin/oebb_Main.jop 192.168.1.2
#	java -cp java/pc/dist/lib/jop-pc.jar udp.Flash ttf/$(FLPROJ).ttf 192.168.1.2
#	quartus_pgm -c $(BLASTER_TYPE) -m JTAG -o p\;quartus/cycconf/cyc_conf.pof

erase_flash:
	java -cp java/pc/dist/lib/jop-pc.jar udp.Erase $(IPDEST)

pld_init:
	quartus_pgm -c $(BLASTER_TYPE) -m JTAG -o p\;quartus/cycconf/cyc_conf_init.pof

pld_conf:
	quartus_pgm -c $(BLASTER_TYPE) -m JTAG -o p\;quartus/cycconf/cyc_conf.pof

oebb:
	java -cp java/pc/dist/lib/jop-pc.jar udp.Flash java/target/dist/bin/oebb_Main.jop 192.168.1.2

# do the whole build process including flash programming
# for BG and baseio (TAL)
bg: directories tools jopflash jopser prog_flash

#
#	some directories for configuration files
#
directories: jbc ttf rbf

jbc:
	mkdir jbc

ttf:
	mkdir ttf

rbf:
	mkdir rbf

#
# this line configures the FPGA and programs the PLD
# but uses a .jbc file
#
# However, the order is not so perfect. We would prefere to first
# program the PLD.
#
xxx:
	quartus_pgm -c $(BLASTER_TYPE) -m JTAG -o p\;jbc/cycbg.jbc
	quartus_pgm -c $(BLASTER_TYPE) -m JTAG -o p\;jbc/cyc_conf.jbc


#
#	JOP porting test programs
#
#	TODO: combine all quartus stuff to a single target
#
jop_blink_test:
	make gen_mem -e ASM_SRC=blink JVM_TYPE=NOOP
	@echo $(QPROJ)
	for target in $(QPROJ); do \
		echo "building $$target"; \
		rm -rf quartus/$$target/db; \
		qp="quartus/$$target/jop"; \
		echo $$qp; \
		quartus_map $$qp; \
		quartus_fit $$qp; \
		quartus_asm $$qp; \
#		quartus_tan $$qp; \
		quartus_sta $$qp; \
		cd quartus/$$target && quartus_cpf -c jop.sof ../../rbf/$$target.rbf; \
	done
	make config
	e $(COM_PORT)


jop_testmon:
	make gen_mem -e ASM_SRC=testmon JVM_TYPE=NOOP
	@echo $(QPROJ)
	for target in $(QPROJ); do \
		echo "building $$target"; \
		rm -rf quartus/$$target/db; \
		qp="quartus/$$target/jop"; \
		echo $$qp; \
		quartus_map $$qp; \
		quartus_fit $$qp; \
		quartus_asm $$qp; \
#		quartus_tan $$qp; \
		quartus_sta $$qp; \
		cd quartus/$$target && quartus_cpf -c jop.sof ../../rbf/$$target.rbf; \
	done
	make config


#
#	UDP debugging
#
udp_dbg:
	java -cp java/pc/dist/lib/jop-pc.jar udp.UDPDbg


# WCET help
wcet_help:
	java $(TOOLS_CP) com.jopdesign.wcet.WCETAnalysis --help

# set library path to current directory for the Mac
DYLD_FALLBACK_LIBRARY_PATH:=.:$(DYLD_FALLBACK_LIBRARY_PATH)
export DYLD_FALLBACK_LIBRARY_PATH 


# WCET analyzer
# make before     : java_app
# make after (dot): (cd java/target/wcet/<project-name>; make)
#
# Makefile options:
# WCET_DFA: perform dataflow analysis
# WCET_UPPAAL: whether to use modelchecking for WCET analysis
# WCET_VERIFYTA: UPPAAL verifier executable
# WCET_OPTIONS: Additional WCET options (run 'make wcet_help')
#
# Profiling: add -Xss16M -agentlib:hprof=cpu=samples,interval=2,depth=8 to java arguments
# On Mac don't forget:
# export DYLD_FALLBACK_LIBRARY_PATH=.
WCET_DFA?=no
WCET_UPPAAL?=no
WCET_VERIFYTA?=verifyta	 # only needed if WCET_UPPAAL=yes
wcet:
	-mkdir -p $(TARGET)/wcet
	java -Xss16M -Xmx512M $(JAVA_OPT) \
	  $(TOOLS_CP) com.jopdesign.wcet.WCETAnalysis \
		--classpath $(TARGET)/dist/lib/classes.zip --sp $(TARGET_SOURCE) \
		--target-method $(WCET_METHOD) \
		-o "$(TARGET)/wcet/\$${projectname}" \
		--use-dfa $(WCET_DFA) \
		--uppaal $(WCET_UPPAAL) --uppaal-verifier $(WCET_VERIFYTA) \
		$(WCET_OPTIONS) $(MAIN_CLASS)	


# dotgraph works for wcet.WCETAnalyser
dotgraph:
	cd $(TARGET)/wcet/$(P2).$(P3)_$(WCET_METHOD)/report && make

dfa:
	java -Xss16M $(TOOLS_CP) com.jopdesign.dfa.Main \
		-cp $(TARGET)/dist/lib/classes.zip $(MAIN_CLASS)

test:
	java $(TOOLS_CP) com.jopdesign.wcet.CallGraph \
		-cp $(TARGET)/dist/lib/classes.zip -o $(TARGET)/wcet/$(P3)call.txt -sp $(TARGET_SOURCE) $(MAIN_CLASS)


###### end of Makefile #######







#
# some MS specific setting - just ignore it
#

# shortcut for my work in Eclipse on TCP/IP
eapp: ecl_app config_usb download

esim: ecl_app
	java $(DEBUG_JOPSIM) -cp java/tools/dist/lib/jop-tools.jar -Dlog="false" \
	com.jopdesign.tools.JopSim java/target/dist/bin/$(JOPBIN)

#
# do it from my eclipse workspace
#
ecl_app:
	cd ../../workspace/cvs_jop_target/classes && jar cf ../../../cpu/jop/java/target/dist/lib/classes.zip *
	java $(TOOLS_CP) -Dmgci=false com.jopdesign.build.JOPizer \
		-cp $(TARGET)/dist/lib/classes.zip -o $(TARGET)/dist/bin/$(JOPBIN) $(MAIN_CLASS)
	java $(TOOLS_CP) com.jopdesign.tools.jop2dat $(TARGET)/dist/bin/$(JOPBIN)
	cp *.dat modelsim
	rm -f *.dat

#
# test AppInfo
# MS: some temporary targets for AppInfo and libgraph tests
#
appinfo: tools
	java $(DEBUG_JOPIZER) $(TOOLS_CP) com.jopdesign.build.AppInfo \
		-cp $(TARGET)/dist/lib/classes.zip $(MAIN_CLASS)

testapp: tools
	make java_app
	-mkdir $(TARGET)/xxx
	java $(DEBUG_JOPIZER) $(TOOLS_CP) com.jopdesign.build.WcetPreprocess \
		-cp $(TARGET)/dist/lib/classes.zip -o $(TARGET)/xxx $(MAIN_CLASS)
	java $(DEBUG_JOPIZER) $(TOOLS_CP) -Dmgci=false com.jopdesign.build.JOPizer \
		-cp $(TARGET)/xxx -o $(TARGET)/dist/bin/$(JOPBIN) $(MAIN_CLASS)
