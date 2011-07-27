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

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;
use ieee.math_real.all;
use std.textio.all;
use work.sc_pack.all;
use work.sc_decoder_pack.all;

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

library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;
use work.sc_pack.all;
use work.sc_decoder_pack.all;

entity sc_pwm_tb is
end sc_pwm_tb;

architecture behavior of sc_pwm_tb is 

    constant lower_addr_bits    : integer := 4;
    constant clk_freq           : integer := 93000000;
    constant channel_count      : integer := 10;
    constant bits_per_channel   : integer := 4;

    constant slave_count : integer := 5;

    constant clk_period : time := 10 ns;

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
            clk => clk,
            reset => reset,
            address => lower_address,
            wr_data => sc_out.wr_data,
            rd => sc_slave_in(1).rd,
            wr => sc_slave_in(1).wr,
            rd_data => sc_slave_out(1).rd_data,
            rdy_cnt => sc_slave_out(1).rdy_cnt,
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
    begin

        sc_out.address <= (others => 'X');
        sc_out.wr_data <= (others => 'X');
        sc_out.rd <= '0';
        sc_out.wr <= '0';

        wait for 2*clk_period;
        reset <= '0';

		wait until rising_edge(clk);

        -- 16 selects the slave, 4 the register in the slave
        sc_write(clk, 16 + 4, 10, sc_out, sc_in);
        sc_write(clk, 16 + 2, 0, sc_out, sc_in);
        sc_write(clk, 16 + 1, 15, sc_out, sc_in);

        wait;
    end process;

end;
