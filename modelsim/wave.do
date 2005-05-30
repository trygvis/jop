onerror {resume}
quietly WaveActivateNextPane {} 0
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/clk_int
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/int_res
add wave -noupdate -format Literal -radix ascii /tb_jop/cmp_jop/cmp_io/cmp_ua/char
add wave -noupdate -divider wishbone
add wave -noupdate -format Literal -radix hexadecimal -expand /tb_jop/cmp_jop/cmp_ext/wbtop/wb_in
add wave -noupdate -format Literal -radix hexadecimal -expand /tb_jop/cmp_jop/cmp_ext/wbtop/wb_out
add wave -noupdate -divider {wb slave}
add wave -noupdate -format Logic /tb_jop/cmp_jop/cmp_ext/wbtop/gsl__0/wbsl/rd
add wave -noupdate -format Logic /tb_jop/cmp_jop/cmp_ext/wbtop/gsl__0/wbsl/wr
add wave -noupdate -format Logic /tb_jop/cmp_jop/cmp_ext/wbtop/gsl__0/wbsl/ack
add wave -noupdate -format Logic /tb_jop/cmp_jop/cmp_ext/wbtop/gsl__0/wbsl/ena
add wave -noupdate -format Literal /tb_jop/cmp_jop/cmp_ext/wbtop/gsl__0/wbsl/cnt
add wave -noupdate -format Literal /tb_jop/cmp_jop/cmp_ext/wbtop/gsl__0/wbsl/xyz
add wave -noupdate -format Literal /tb_jop/cmp_jop/cmp_ext/wbtop/gsl__0/wbsl/wb_out
add wave -noupdate -format Literal /tb_jop/cmp_jop/cmp_ext/wbtop/gsl__0/wbsl/wb_in
add wave -noupdate -format Logic /tb_jop/cmp_jop/cmp_ext/wbtop/gsl__1/wbsl/rd
add wave -noupdate -format Logic /tb_jop/cmp_jop/cmp_ext/wbtop/gsl__1/wbsl/wr
add wave -noupdate -format Logic /tb_jop/cmp_jop/cmp_ext/wbtop/gsl__1/wbsl/ack
add wave -noupdate -format Logic /tb_jop/cmp_jop/cmp_ext/wbtop/gsl__1/wbsl/ena
add wave -noupdate -format Literal /tb_jop/cmp_jop/cmp_ext/wbtop/gsl__1/wbsl/cnt
add wave -noupdate -format Literal /tb_jop/cmp_jop/cmp_ext/wbtop/gsl__1/wbsl/xyz
add wave -noupdate -format Literal /tb_jop/cmp_jop/cmp_ext/wbtop/gsl__1/wbsl/wb_out
add wave -noupdate -format Literal /tb_jop/cmp_jop/cmp_ext/wbtop/gsl__1/wbsl/wb_in
add wave -noupdate -divider {wishbone end}
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_fch/pc
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/ir
add wave -noupdate -format Literal /tb_jop/cmp_jop/cmp_core/cmp_dec/sel_lmux
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/wr
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/rd
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/a
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/mem_rd
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/mem_addr_wr
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/mem_wr
add wave -noupdate -format Literal /tb_jop/cmp_jop/cmp_mem/state
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/wait_state
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/main_mem/addr
add wave -noupdate -format Logic /tb_jop/cmp_jop/cmp_mem/mem_bsy
add wave -noupdate -format Logic /tb_jop/cmp_jop/cmp_core/cmp_fch/bsy
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/main_mem/data
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/ram_data
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_ext/dout
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_fch/pcwait
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_fch/pc_inc
add wave -noupdate -divider div
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/sp
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/ext_addr
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/sel_amux
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/sel_rda
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/sel_wra
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/wr_ena
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/ena_b
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/ena_a
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/b
add wave -noupdate -divider {external signals}
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/main_mem/addr
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/main_mem/data
add wave -noupdate -format Logic /tb_jop/main_mem/ncs
add wave -noupdate -format Logic /tb_jop/main_mem/noe
add wave -noupdate -format Logic /tb_jop/main_mem/nwr
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/fl_a
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/fl_d
add wave -noupdate -format Logic /tb_jop/cmp_jop/fl_ncs
add wave -noupdate -format Logic /tb_jop/cmp_jop/fl_ncsb
add wave -noupdate -format Logic /tb_jop/cmp_jop/fl_noe
add wave -noupdate -format Logic /tb_jop/cmp_jop/fl_nwe
add wave -noupdate -format Logic /tb_jop/cmp_jop/fl_rdy
add wave -noupdate -divider cpm_mem
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/din
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/dout
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/ram_data
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/flash_data
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/ramb_d
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/rama_d
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/ram_addr
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/fl_a
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/mem_addr_wr
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/mem_wr_addr
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/mem_wr
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/mem_rd
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/rama_a
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/dout
add wave -noupdate -format Logic /tb_jop/cmp_jop/cmp_mem/mem_bsy
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/a
add wave -noupdate -divider execute
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/din
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(0)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(1)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(2)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(3)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(4)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(5)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(128)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(129)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(130)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(131)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(132)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(133)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(134)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(135)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(136)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(137)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(138)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(139)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(140)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(141)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(142)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(143)
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/clk_int
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/int_res
add wave -noupdate -format Literal -radix ascii /tb_jop/cmp_jop/cmp_io/cmp_ua/char
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_fch/pc
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/ir
add wave -noupdate -format Literal /tb_jop/cmp_jop/cmp_core/cmp_dec/sel_lmux
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_fch/pcwait
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/wr
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/rd
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/sp
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/ext_addr
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/sel_amux
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/sel_rda
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/sel_wra
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/wr_ena
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/ena_b
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_dec/ena_a
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/b
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/a
add wave -noupdate -divider {external signals}
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/main_mem/addr
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/main_mem/data
add wave -noupdate -format Logic /tb_jop/main_mem/ncs
add wave -noupdate -format Logic /tb_jop/main_mem/noe
add wave -noupdate -format Logic /tb_jop/main_mem/nwr
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/fl_a
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/fl_d
add wave -noupdate -format Logic /tb_jop/cmp_jop/fl_ncs
add wave -noupdate -format Logic /tb_jop/cmp_jop/fl_ncsb
add wave -noupdate -format Logic /tb_jop/cmp_jop/fl_noe
add wave -noupdate -format Logic /tb_jop/cmp_jop/fl_nwe
add wave -noupdate -format Logic /tb_jop/cmp_jop/fl_rdy
add wave -noupdate -divider cpm_mem
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/din
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/dout
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/ram_data
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/flash_data
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/ramb_d
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/rama_d
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/ram_addr
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/fl_a
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/mem_addr_wr
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/mem_wr_addr
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/mem_wr
add wave -noupdate -format Logic -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/mem_rd
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/rama_a
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_mem/dout
add wave -noupdate -format Logic /tb_jop/cmp_jop/cmp_mem/mem_bsy
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/a
add wave -noupdate -divider execute
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/din
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(0)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(1)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(2)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(3)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(4)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(5)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(128)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(129)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(130)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(131)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(132)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(133)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(134)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(135)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(136)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(137)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(138)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(139)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(140)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(141)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(142)
add wave -noupdate -format Literal -radix hexadecimal /tb_jop/cmp_jop/cmp_core/cmp_stk/cmp_ram/ram(143)
TreeUpdate [SetDefaultTree]
WaveRestoreCursors {{Cursor 1} {924379 ps} 0} {{Cursor 2} {1871478 ps} 0}
configure wave -namecolwidth 259
configure wave -valuecolwidth 54
configure wave -justifyvalue left
configure wave -signalnamewidth 0
configure wave -snapdistance 10
configure wave -datasetprefix 0
configure wave -rowmargin 4
configure wave -childrowmargin 2
configure wave -gridoffset 0
configure wave -gridperiod 1
configure wave -griddelta 40
configure wave -timeline 0
update
WaveRestoreZoom {0 ps} {2100 ns}
