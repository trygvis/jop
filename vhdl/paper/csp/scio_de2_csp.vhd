--
--
--  This file is a part of JOP, the Java Optimized Processor
--
--  Copyright (C) 2001-2008, Martin Schoeberl (martin@jopdesign.com)
--
--  This program is free software: you can redistribute it and/or modify
--  it under the terms of the GNU General Public License as published by
--  the Free Software Foundation, either version 3 of the License, or
--  (at your option) any later version.
--
--  This program is distributed in the hope that it will be useful,
--  but WITHOUT ANY WARRANTY; without even the implied warranty of
--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
--  GNU General Public License for more details.
--
--  You should have received a copy of the GNU General Public License
--  along with this program.  If not, see <http://www.gnu.org/licenses/>.
--


--
--	scio_min.vhd
--
--	io devices for minimal configuration
--	only counter, wd and serial line, alle io pins are tri statet
--
--
--	io address mapping:
--
--	IO Base is 0xffffff80 for 'fast' constants (bipush)
--
--		0x00 0-3		system clock counter, us counter, timer int, wd bit
--		0x10 0-1		uart (download)
--		0x30 0-2		ps kbd controller
--		0x40			ps2 mouse controller
--
--	status word in uarts:
--		0	uart transmit data register empty
--		1	uart read data register full
--
--
--	todo:
--
--
--	2003-07-09	created
--	2005-08-27	ignore ncts on uart
--	2005-11-30	changed to SimpCon
--	2007-03-17	use records
--
--


Library IEEE;
use IEEE.std_logic_1164.all;
use ieee.numeric_std.all;

use work.jop_types.all;
use work.sc_pack.all;
use work.jop_config.all;

entity scio is
generic (cpu_id : integer := 0; cpu_cnt : integer := 1);
port (
	clk		: in std_logic;
	reset	: in std_logic;

--
--	SimpCon IO interface
--
	sc_io_out		: in sc_out_type;
	sc_io_in		: out sc_in_type;

--
--	Interrupts from IO devices
--
	irq_in			: out irq_bcf_type;
	irq_out			: in irq_ack_type;
	exc_req			: in exception_type;

-- CMP

	sync_out : in sync_out_type := NO_SYNC;
	sync_in	 : out sync_in_type;
	
	noc_in		: out sc_out_type;
	noc_out		: in sc_in_type;
	
-- serial interface

	txd			: out std_logic;
	rxd			: in std_logic;
	ncts		: in std_logic;
	nrts		: out std_logic;
	
--
--	LEDs
--
	oLEDR		: out std_logic_vector(17 downto 0);
--	oLEDG		: out std_logic_vector(7 downto 0);
	
--
--	Switches
--
	iSW			: in std_logic_vector(17 downto 0);
	
-- watch dog

	wd			: out std_logic;
-- add led and switch pins
-- core i/o pins
	l			: inout std_logic_vector(20 downto 1);
	r			: inout std_logic_vector(20 downto 1);
	t			: inout std_logic_vector(6 downto 1);
	b			: inout std_logic_vector(10 downto 1)

--ps2 kbd pins	
--	kbd_clk_in :in std_logic;
--	kbd_clk_out :out std_logic;
--	kbd_data_in :in std_logic;
--	kbd_data_out :out std_logic;
--	kbd_data_oe :out std_logic;
--	kbd_clk_oe :out std_logic;
	
-- ps2 mouse pins
--    ps2_clk      : inout std_logic;
--    ps2_data     : inout std_logic
    
-- remove the comment for RAM access counting
-- ram_cnt 	: in std_logic
 );
end scio;


architecture rtl of scio is

	constant SLAVE_CNT : integer := 6;
	-- SLAVE_CNT <= 2**DECODE_BITS
	-- take care of USB address 0x20!
	constant DECODE_BITS : integer := 3;
	-- number of bits that can be used inside the slave
	constant SLAVE_ADDR_BITS : integer := 4;

	type slave_bit is array(0 to SLAVE_CNT-1) of std_logic;
	signal sc_rd, sc_wr		: slave_bit;

	type slave_dout is array(0 to SLAVE_CNT-1) of std_logic_vector(31 downto 0);
	signal sc_dout			: slave_dout;

	type slave_rdy_cnt is array(0 to SLAVE_CNT-1) of unsigned(1 downto 0);
	signal sc_rdy_cnt		: slave_rdy_cnt;

	signal sel, sel_reg		: integer range 0 to 2**DECODE_BITS-1;
	
	-- remove the comment for RAM access counting 
	-- signal ram_count : std_logic;
	
--component kbd_cntrl is
--  generic ( 
--    addr_bits :integer := 23;
--    CLK_FREQ :integer := 50000000;
--    TIMEOUT_REG_WIDTH :integer := 13
--    );
--  port ( 
--    clk : in std_logic;
--    reset : in std_logic;
--    address		: in std_logic_vector(addr_bits-1 downto 0);
--    wr_data		: in std_logic_vector(31 downto 0);
--    rd : in std_logic;
--    wr		: in std_logic;
--    rd_data		: out std_logic_vector(31 downto 0);
--    rdy_cnt		: out unsigned(1 downto 0);
--    kbd_clk_oe :out std_logic;
--    kbd_data_oe :out std_logic;
--    kbd_clk_in :in std_logic;
--    kbd_clk_out :out std_logic;
--    kbd_data_in :in std_logic;
--    kbd_data_out :out std_logic
--    );
--end component;

--component mouse_cntrl is
--  generic (
--    addr_bits 	: integer := 23;
--    CLK_FREQ :integer := 50000000;
--    TIMEOUT_REG_WIDTH :integer := 13
--    );
--  port ( 
--    clk 			: in std_logic;
--    rst 			: in std_logic;
--    
--    address			: in std_logic_vector(addr_bits-1 downto 0);
--    wr_data			: in std_logic_vector(31 downto 0);
--    rd 				: in std_logic;
--    wr				: in std_logic;
--    rd_data			: out std_logic_vector(31 downto 0);
--    rdy_cnt			: out unsigned(1 downto 0);
--    
--    ps2_clk         : inout std_logic;
--    ps2_data        : inout std_logic
--    );
--end component;

begin

--
--	unused and input pins tri state
--
	l <= (others => 'Z');
	r <= (others => 'Z');
	t <= (others => 'Z');
	b <= (others => 'Z');

	assert SLAVE_CNT <= 2**DECODE_BITS report "Wrong constant in scio";

	sel <= to_integer(unsigned(sc_io_out.address(SLAVE_ADDR_BITS+DECODE_BITS-1 downto SLAVE_ADDR_BITS)));

	-- What happens when sel_reg > SLAVE_CNT-1??
	sc_io_in.rd_data <= sc_dout(sel_reg);
	sc_io_in.rdy_cnt <= sc_rdy_cnt(sel_reg);

	-- default for unused USB device
	sc_dout(2) <= (others => '0');
	sc_rdy_cnt(2) <= (others => '0');

	--
	-- Connect SLAVE_CNT simple test slaves
	--
	gsl: for i in 0 to SLAVE_CNT-1 generate

		sc_rd(i) <= sc_io_out.rd when i=sel else '0';
		sc_wr(i) <= sc_io_out.wr when i=sel else '0';

	end generate;

	--
	--	Register read and write mux selector
	--
	process(clk, reset)
	begin
		if (reset='1') then
			sel_reg <= 0;
		elsif rising_edge(clk) then
			if sc_io_out.rd='1' or sc_io_out.wr='1' then
				sel_reg <= sel;
			end if;
		end if;
	end process;
			
	sys: entity work.sc_sys generic map (
			addr_bits => SLAVE_ADDR_BITS,
			clk_freq => clk_freq,
			cpu_id => cpu_id,
			cpu_cnt => cpu_cnt
		)
		port map(
			clk => clk,
			reset => reset,

			address => sc_io_out.address(SLAVE_ADDR_BITS-1 downto 0),
			wr_data => sc_io_out.wr_data,
			rd => sc_rd(0),
			wr => sc_wr(0),
			rd_data => sc_dout(0),
			rdy_cnt => sc_rdy_cnt(0),

			irq_in => irq_in,
			irq_out => irq_out,
			exc_req => exc_req,
			
			sync_out => sync_out,
			sync_in => sync_in,
			
			wd => wd
			-- remove the comment for RAM access counting
			-- ram_count => ram_count
		);
		
	-- remove the comment for RAM access counting
	-- ram_count <= ram_cnt;

	ua: entity work.sc_uart generic map (
			addr_bits => SLAVE_ADDR_BITS,
			clk_freq => clk_freq,
			baud_rate => 115200,
			txf_depth => 2,
			txf_thres => 1,
			rxf_depth => 2,
			rxf_thres => 1
		)
		port map(
			clk => clk,
			reset => reset,

			address => sc_io_out.address(SLAVE_ADDR_BITS-1 downto 0),
			wr_data => sc_io_out.wr_data,
			rd => sc_rd(1),
			wr => sc_wr(1),
			rd_data => sc_dout(1),
			rdy_cnt => sc_rdy_cnt(1),

			txd	 => txd,
			rxd	 => rxd,
			ncts => '0',
			nrts => nrts
	);
	
	-- LEDs and switches moved from 4 to 5
	lw : entity work.led_switch
	port map
	(
		clk => clk,
		reset => reset,
		
		sc_rd => sc_rd(5),
		sc_rd_data => sc_dout(5),
		sc_wr => sc_wr(5),
		sc_wr_data => sc_io_out.wr_data,
		sc_rdy_cnt => sc_rdy_cnt(5),
		
		oLEDR => oLEDR,
--		oLEDG => oLEDG,
		iSW => iSW
	);
	
	-- NoC connection
	noc_in.address <= sc_io_out.address;
	noc_in.rd <= sc_rd(4);
	noc_in.wr <= sc_wr(4);
	noc_in.wr_data <= sc_io_out.wr_data;
	sc_dout(4) <= noc_out.rd_data;
	sc_rdy_cnt(4) <= noc_out.rdy_cnt;
	
	
--	ps2kbd : kbd_cntrl
--  generic map(
--    addr_bits => SLAVE_ADDR_BITS,
--    CLK_FREQ => 50000000,
--    TIMEOUT_REG_WIDTH => 13
--    )
--  port map( 
--    clk => clk,
--    reset => reset,
--    address	=> sc_io_out.address(SLAVE_ADDR_BITS-1 downto 0),
--    wr_data	=> sc_io_out.wr_data,
--    rd  => sc_rd(3),
--    wr	=> sc_wr(3),
--    rd_data		=> sc_dout(3),
--    rdy_cnt		=> sc_rdy_cnt(3),
--    kbd_clk_oe => kbd_clk_oe,
--    kbd_data_oe => kbd_data_oe,
--    kbd_clk_in => kbd_clk_in,
--    kbd_clk_out => kbd_clk_out,
--    kbd_data_in => kbd_data_in,
--    kbd_data_out => kbd_data_out
--    );    

-- ps2mouse : entity work.mouse_cntrl   
--  generic map (
--    addr_bits => SLAVE_ADDR_BITS,
--    CLK_FREQ => 50000000,
--    TIMEOUT_REG_WIDTH => 13
--  )
--  port map(
--    clk 	     => clk,
--    rst 		 => reset,
--    address		 => sc_io_out.address(SLAVE_ADDR_BITS-1 downto 0),
--    wr_data		 => sc_io_out.wr_data,
--    rd  		 => sc_rd(4),
--    wr			 => sc_wr(4),
--    rd_data		 => sc_dout(4),
--    rdy_cnt		 => sc_rdy_cnt(4),
--
--    ps2_clk      => ps2_clk,
--    ps2_data     => ps2_data
--  );
end rtl;
