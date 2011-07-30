-- Copyright (C) 2011, Trygve Laugstøl <trygvis@inamo.no>
--
-- This code is licensed under the Apache Software License.
--

library ieee;
use ieee.std_logic_1164.all;
use ieee.std_logic_arith.all;
use ieee.numeric_std.all;
use work.sc_pack.all;
use work.sc_decoder_pack.all;

entity sc_pwm_tb is
end sc_pwm_tb;

architecture behavior of sc_pwm_tb is 

    constant lower_addr_bits    : integer := 4;
    constant slave_count        : integer := 4;

    constant clk_freq           : integer := 93000000;
    constant channel_count      : integer := 10;
    constant bits_per_channel   : integer := 16;

    constant clk_period : time := 10.75 ns; -- 93MHz

    signal clk      : std_logic := '0';
    signal reset    : std_logic := '1';

    signal outputs  : std_logic_vector(0 to channel_count-1);

    -- SimpCon <=> CPU bus
    signal sc_in            : sc_in_type;
    signal sc_out           : sc_out_type;

    -- SimpCon <=> slave busses
    signal sc_slave_in      : sc_slave_in_array(0 to slave_count - 1);
    signal sc_slave_out     : sc_slave_out_array(0 to slave_count - 1);

    signal lower_address    : std_logic_vector(lower_addr_bits - 1 downto 0);

begin

    uut: entity work.sc_pwm
    generic map (
        addr_bits           => lower_addr_bits,
        clk_freq            => clk_freq,
        channel_count       => channel_count,
        bits_per_channel    => bits_per_channel
    )
    port map (
        clk     => clk,
        reset   => reset,
        address => lower_address,
        wr_data => sc_out.wr_data,
        rd      => sc_slave_in(3).rd,
        wr      => sc_slave_in(3).wr,
        rd_data => sc_slave_out(3).rd_data,
        rdy_cnt => sc_slave_out(3).rdy_cnt,
        outputs => outputs
    );

    decoder: entity work.sc_decoder
    generic map (
        lower_addr_bits => lower_addr_bits,
        slave_count     => slave_count
    )
    port map(
        clk             => clk,
        reset           => reset,
        sc_in           => sc_in,
        sc_out          => sc_out,
        sc_slave_in     => sc_slave_in,
        sc_slave_out    => sc_slave_out,
        lower_address   => lower_address
    );

    clk_p : process
    begin
        clk <= not clk;
        wait for clk_period/2;
    end process;

    stimulus: process
        variable data: std_logic_vector(31 downto 0);
    begin
        sc_out.address <= (others => 'X');
        sc_out.wr_data <= (others => 'X');
        sc_out.rd <= '0';
        sc_out.wr <= '0';

        wait for 2*clk_period;
        reset <= '0';

		wait until rising_edge(clk);
        sc_write(clk, std_logic_vector(to_signed(-80, 23)), std_logic_vector(to_signed(2200, 32)), sc_out, sc_in);
		wait until rising_edge(clk);
		wait until rising_edge(clk);
		wait until rising_edge(clk);
		wait until rising_edge(clk);
		wait until rising_edge(clk);
        sc_read(clk, std_logic_vector(to_signed(-80, 23)), data, sc_out, sc_in);

        wait;
    end process;
end;
