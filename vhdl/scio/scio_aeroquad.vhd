-- Copyright (C) 2011, Trygve Laugstøl <trygvis@inamo.no>
--
-- This code is licensed under the Apache Software License.
--

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

use work.jop_types.all;
use work.sc_pack.all;
use work.sc_decoder_pack.all;
use work.jop_config.all;

entity scio is
generic (
    cpu_id                  : integer := 0;
    cpu_cnt                 : integer := 1;
    pwm_channel_count       : integer;
    pwm_bits_per_channel    : integer
);
port (
    clk         : in std_logic;
	reset       : in std_logic;

--
--	SimpCon IO interface
--
    sc_io_out   : in sc_out_type;
    sc_io_in    : out sc_in_type;

--
--	Interrupts from IO devices
--
    irq_in      : out irq_bcf_type;
    irq_out     : in irq_ack_type;
    exc_req     : in exception_type;

-- CMP

    sync_out    : in sync_out_type := NO_SYNC;
    sync_in	    : out sync_in_type;

--
-- Pins from devices connected to the bus
--

-- Device #0 -  watch dog

    wd          : out std_logic;

-- Device #1 - serial interface

    txd         : out std_logic;
    rxd         : in std_logic;

-- Device #2 - PWM

    pwm_outputs : out std_logic_vector(0 to pwm_channel_count-1)
);
end scio;

architecture rtl of scio is

    constant slave_count        : integer := 3;
    constant lower_addr_bits    : integer := 4;

--    -- SimpCon <=> CPU bus
--    signal sc_in            : sc_in_type;
--    signal sc_out           : sc_out_type;
--
--    -- SimpCon <=> slave busses
--    signal sc_slave_in      : sc_slave_in_array(0 to slave_count - 1);
--    signal sc_slave_out     : sc_slave_out_array(0 to slave_count - 1);
--
--    signal lower_address    : std_logic_vector(lower_addr_bits - 1 downto 0);

    constant SLAVE_CNT          : integer := 3;
    constant DECODE_BITS        : integer := 2;
    -- number of bits that can be used inside the slave
    constant SLAVE_ADDR_BITS    : integer := 4;

    type slave_bit is array(0 to SLAVE_CNT-1) of std_logic;
    signal sc_rd, sc_wr         : slave_bit;

    type slave_dout is array(0 to SLAVE_CNT-1) of std_logic_vector(31 downto 0);
    signal sc_dout              : slave_dout;

    type slave_rdy_cnt is array(0 to SLAVE_CNT-1) of unsigned(1 downto 0);
    signal sc_rdy_cnt           : slave_rdy_cnt;

    signal sel, sel_reg         : integer range 0 to 2**DECODE_BITS-1;

begin

    assert SLAVE_CNT <= 2**DECODE_BITS report "Wrong constant in scio";

    sel <= to_integer(unsigned(sc_io_out.address(SLAVE_ADDR_BITS+DECODE_BITS-1 downto SLAVE_ADDR_BITS)));

    sc_io_in.rd_data <= sc_dout(sel_reg);
    sc_io_in.rdy_cnt <= sc_rdy_cnt(sel_reg);

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

--    sc_decoder: entity work.sc_decoder
--    generic map (
--        lower_addr_bits => lower_addr_bits,
--        slave_count     => slave_count
--    )
--    port map(
--        clk             => clk,
--        reset           => reset,
--        sc_in           => sc_in,
--        sc_out          => sc_out,
--        sc_slave_in     => sc_slave_in,
--        sc_slave_out    => sc_slave_out,
--        lower_address   => lower_address
--    );

    -- Device #0
    sys: entity work.sc_sys generic map (
        addr_bits => lower_addr_bits,
        clk_freq => clk_freq,
        cpu_id => cpu_id,
        cpu_cnt => cpu_cnt
    )
    port map(
        clk => clk,
        reset => reset,

--        address => lower_address,
--        wr_data => sc_io_out.wr_data,
--        rd => sc_slave_in(0).rd,
--        wr => sc_slave_in(0).wr,
--        rd_data => sc_slave_out(0).rd_data,
--        rdy_cnt => sc_slave_out(0).rdy_cnt,

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
    );

    -- Device #1
    uart0: entity work.sc_uart generic map (
        addr_bits => lower_addr_bits,
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

--        address => lower_address,
--        wr_data => sc_io_out.wr_data,
--        rd => sc_slave_in(1).rd,
--        wr => sc_slave_in(1).wr,
--        rd_data => sc_slave_out(1).rd_data,
--        rdy_cnt => sc_slave_out(1).rdy_cnt,

        address => sc_io_out.address(SLAVE_ADDR_BITS-1 downto 0),
        wr_data => sc_io_out.wr_data,
        rd => sc_rd(1),
        wr => sc_wr(1),
        rd_data => sc_dout(1),
        rdy_cnt => sc_rdy_cnt(1),

        txd	 => txd,
        rxd	 => rxd,
        ncts => '0',
        nrts => open
    );

    -- Device #2
    pwm: entity work.sc_pwm generic map (
        addr_bits => lower_addr_bits,
        clk_freq => clk_freq,
        channel_count => pwm_channel_count,
        bits_per_channel => pwm_bits_per_channel
    )
    port map(
        clk => clk,
        reset => reset,

--        address => lower_address,
--        wr_data => sc_io_out.wr_data,
--        rd => sc_slave_in(2).rd,
--        wr => sc_slave_in(2).wr,
--        rd_data => sc_slave_out(2).rd_data,
--        rdy_cnt => sc_slave_out(2).rdy_cnt,

        address => sc_io_out.address(lower_addr_bits-1 downto 0),
        wr_data => sc_io_out.wr_data,
        rd => sc_rd(2),
        wr => sc_wr(2),
        rd_data => sc_dout(2),
        rdy_cnt => sc_rdy_cnt(2),

        outputs => pwm_outputs
    );

end rtl;
