set qu_proj=cycbaseio
set projpath=quartus\%qu_proj%
set p1= bench
set p2=jbe
set p3=DoAll

cd asm
call jopser
cd ..
quartus_map %projpath%\jop
quartus_fit %projpath%\jop
quartus_asm %projpath%\jop
quartus_tan %projpath%\jop
rem jbi32 -dDO_PROGRAM=1 -aPROGRAM jbc\cycmin.jbc
cd %projpath%
quartus_pgm -c ByteBlasterMV -m JTAG jop.cdf
cd ..\..
cd java\target
call build %p1% %p2% %p3%
rem start ping -n 100 192.168.0.123
..\..\down -e dist\bin\%project%.jop COM1
cd ..\..
