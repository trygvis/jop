-- Copyright (C) 2011, Trygve Laugstøl <trygvis@inamo.no>
--
-- This code is licensed under the Apache Software License.
--
--		address map:
--
--			0	PWM channel #0 setting
--			1	PWM channel #1 setting
--              ...
--			N	PWM channel #N setting
--
-- TOOD: Use sc_decoder_in/sc_decoder_out for SimpCon signals

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;
use work.jop_types.all;
use work.sc_pack.all;

entity sc_pwm is
    generic (
        addr_bits           : integer;
        clk_freq            : integer;
        channel_count       : integer;
        bits_per_channel    : integer
        );
    port (
        clk		: in std_logic;
        reset	: in std_logic;

        -- SimpCon interface

        address	: in std_logic_vector(addr_bits-1 downto 0);
        wr_data	: in std_logic_vector(31 downto 0);
        rd, wr	: in std_logic;
        rd_data	: out std_logic_vector(31 downto 0);
        rdy_cnt	: out unsigned(1 downto 0);

        --

        outputs : out std_logic_vector(0 to channel_count-1)
        );
end;

architecture rtl of sc_pwm is

    subtype counter_t is std_logic_vector(bits_per_channel-1 downto 0);
    type counter_array is array(0 to channel_count-1) of counter_t;

    signal counter          : counter_t;
    signal next_counter     : counter_t;

    signal terminal_values  : counter_array;
    signal next_outputs     : std_logic_vector(terminal_values'range);

begin

    rdy_cnt <= "00";	-- No wait states, always ready on one clock
    rd_data <= (others => '0');

    next_counter <= std_logic_vector(unsigned(counter) + 1);

    t: for i in terminal_values'range generate
        next_outputs(i) <= '0' when counter = (counter'range => '0') else
                           '1' when counter = terminal_values(i) else
                           next_outputs(i);

        process(clk, reset)
        begin
            if (reset='1') then
                terminal_values(i) <= (others => '0');
            elsif rising_edge(clk) then
                if wr='1' and to_integer(unsigned(address)) = i then
                    terminal_values(i) <= wr_data(bits_per_channel-1 downto 0);
                end if;
            end if;
        end process;
    end generate;

    process(clk, reset)
    begin
        if (reset='1') then
            counter <= (others => '0');
            outputs <= (others => '0');
        elsif rising_edge(clk) then
            counter <= next_counter;
            outputs <= next_outputs;
        end if;
    end process;
end;
