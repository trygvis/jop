-- Copyright (C) 2011, Trygve Laugstøl <trygvis@inamo.no>
--
-- This code is licensed under the Apache Software License.
--
-- A simple decoder for the SimpCon bus.
--
-- It takes the 32 bit address and decodes it into two array of read/write 
-- enable signals and one "lower_address" bus. Each device on the bus should 
-- get its own read/write signal while they all share the lower_address signal.
--
-- The 32 address is decoded into:
--   a[32:0] = XXX YYY ZZZ
-- 
-- * ZZZ Lower address
-- * YYY The device address
-- * XXX is ignored.
-- The actual size of each field depends on the generic configuration.
-- TODO: I guess they should be required that all XXX are '0' for the decoder to activate any flags.

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;
use ieee.math_real.all;
use std.textio.all;
use work.sc_pack.all;
use work.sc_decoder_pack.all;

entity sc_decoder is
    generic (
        lower_addr_bits : integer;
        slave_count    : integer
        );
    port (
        clk             : in std_logic;
        reset           : in std_logic;

		sc_in           : out sc_in_type;
		sc_out          : in sc_out_type;

        sc_slave_in     : out sc_slave_in_array(0 to slave_count - 1);
        sc_slave_out    : in sc_slave_out_array(0 to slave_count - 1);
        lower_address   : out std_logic_vector(lower_addr_bits - 1 downto 0)
    );
end entity;

architecture default of sc_decoder is
    constant x : real := real(slave_count);
    constant y : real := ceil(log2(x));
    constant slave_addr_bits  : integer := integer(y);
    signal slave, slave_reg    : integer range 0 to slave_count - 1;

    shared variable line : line;
begin
--    assert false report "SimpCon address decoder configuration:" severity note;
--    assert false report " Number of devices:   " & to_string(slave_count) severity note;
--    assert false report " Bits used to decode: " & to_string(slave_count_bits) severity note;

    write(line, "SimpCon address decoder configuration:");
    writeline(output, line);
    write(line, " Slave count:   ");
    write(line, slave_count);
    writeline(output, line);
    write(line, " Lower address bit count: ");
    write(line, lower_addr_bits);
    writeline(output, line);
    write(line, " Slave address bit count: ");
    write(line, slave_addr_bits);
    writeline(output, line);

    assert slave_count <= 2**slave_addr_bits report "Wrong constant in sc_decoder";
    assert (slave_addr_bits + lower_addr_bits) <= SC_ADDR_SIZE report "Wrong constant in sc_decoder";

    slave <= to_integer(unsigned(sc_out.address(slave_addr_bits+lower_addr_bits-1 downto lower_addr_bits)));
    lower_address <= sc_out.address(lower_addr_bits-1 downto 0);

    sc_in.rd_data <= sc_slave_out(slave_reg).rd_data;
    sc_in.rdy_cnt <= sc_slave_out(slave_reg).rdy_cnt;

    gsl: for i in 0 to slave_count-1 generate
        sc_slave_in(i).rd <= sc_out.rd when slave=i else '0';
        sc_slave_in(i).wr <= sc_out.wr when slave=i else '0';
    end generate;

    --
    --	Register read and write mux selector
    --
    process(clk, reset)
    begin
        if (reset='1') then
            slave_reg <= 0;
        elsif rising_edge(clk) then
            if sc_out.rd='1' or sc_out.wr='1' then
                slave_reg <= slave;
            end if;
        end if;
    end process;
end architecture;
