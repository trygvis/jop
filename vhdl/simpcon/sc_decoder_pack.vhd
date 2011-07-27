-- Copyright (C) 2011, Trygve Laugstøl <trygvis@inamo.no>
--
-- This code is licensed under the Apache Software License.

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

package sc_decoder_pack is

    -- Signals from SimpCon bus to slave.
    type sc_slave_in is record
        rd      : std_logic;
        wr      : std_logic;
    end record;

    -- Signals from SimpCon slave to bus.
    -- It is tempting to add lower_address to the record as well - Trygve
    type sc_slave_out is record
        rd_data : std_logic_vector(31 downto 0);
        rdy_cnt : unsigned(1 downto 0);
    end record;

    type sc_slave_in_array is array (integer range <>) of sc_slave_in;
    type sc_slave_out_array is array (integer range <>) of sc_slave_out;
end package;
