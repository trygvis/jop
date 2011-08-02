-- Copyright (C) 2011, Trygve Laugstøl <trygvis@inamo.no>
--
-- This code is licensed under the Apache Software License.
--
-- A N-channel PWM signal generator.
--
-- Each channel has a terminal value where the output signal changes from '0' to '1'.
-- The internal clock is a 1MHz tick.
--
--        address map:
--
--          0   PWM channel #0 terminal value
--          1   PWM channel #1 terminal value
--              ...
--          N   PWM channel #N terminal value
--
-- 
-- TOOD: Use sc_decoder_in/sc_decoder_out for SimpCon signals
-- TODO: Either use one counter per channel or wait until the channel starts over to load the terminal value
-- TODO: Add enable signals per channel
-- TODO: Add prescaler per channel so longer signals can be generated.
-- TODO: Consider adding a set of "meta registers" that contains the current configuration of the module
--       Include channel width and channel count.

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;
use work.jop_types.all;
use work.sc_pack.all;

entity sc_pwm is
generic (
    addr_bits           : integer;
    clk_freq            : integer; -- in Hertz
    channel_count       : integer;
    bits_per_channel    : integer
);
port (
    clk     : in std_logic;
    reset   : in std_logic;

    -- SimpCon interface

    address : in std_logic_vector(addr_bits-1 downto 0);
    wr_data : in std_logic_vector(31 downto 0);
    rd      : in std_logic;
    wr      : in std_logic;
    rd_data : out std_logic_vector(31 downto 0);
    rdy_cnt : out unsigned(1 downto 0);

    --

    outputs : out std_logic_vector(0 to channel_count-1)
);
end;

architecture rtl of sc_pwm is

    subtype counter_t is unsigned(bits_per_channel-1 downto 0);
    type counter_array is array(0 to channel_count-1) of counter_t;

    signal counter          : counter_t;
    signal next_counter     : counter_t;

    signal terminal_values  : counter_array;
    signal next_outputs     : std_logic_vector(terminal_values'range);

    constant pwm_clk_divisor        : integer := clk_freq / 1000000;
    constant pwm_clk_divisor_bits   : integer := 8; -- TODO: Calculate this. ceil(log2(pwm_clk_divisor))

    signal tick_reg         : unsigned(pwm_clk_divisor_bits - 1 downto 0) := (others => '0');
    signal tick_next        : unsigned(pwm_clk_divisor_bits - 1 downto 0);
    signal tick             : std_logic;

begin

    rdy_cnt <= "00";    -- No wait states, always ready on one clock
    rd_data <= (others => '0');

    -- If this is to be implemented address needs to be registered
--    rd_data <= std_logic_vector(terminal_values(to_integer(unsigned(address)))) when rd='1'
--                else (others => '0');

    -------------------------------------------------------
    -- PWM generator

    pwm_channel: for i in terminal_values'range generate
        next_outputs(i) <= '0' when counter > terminal_values(i) else '1';

        process(clk, reset)
        begin
            if (reset='1') then
                terminal_values(i) <= (others => '0');
            elsif rising_edge(clk) then
                if wr='1' and to_integer(unsigned(address)) = i then
                    terminal_values(i) <= unsigned(wr_data(bits_per_channel-1 downto 0));
                end if;
            end if;
        end process;
    end generate;

    next_counter <= counter + 1;

    state: process(tick, reset)
    begin
        if (reset='1') then
            counter <= (others => '0');
            outputs <= (others => '0');
        elsif rising_edge(tick) then
            counter <= next_counter;
            outputs <= next_outputs;
        end if;
    end process;

    -------------------------------------------------------
    -- Tick generator
    assert clk_freq >= 1000000 report "clk_freq has to be at least 1MHz";

    tick_next <= (others => '0') when tick_reg=(pwm_clk_divisor - 1) else tick_reg + 1;
    tick <= '1' when tick_reg=(pwm_clk_divisor - 1) else '0';

    tick_p: process(clk, reset, tick_next)
    begin
        if reset = '1' then
            tick_reg <= (others => '0');
        elsif rising_edge(clk) then
            tick_reg <= tick_next;
        end if;
    end process;

end;
