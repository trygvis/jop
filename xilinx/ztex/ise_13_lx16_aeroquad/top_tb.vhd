library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

use work.jop_types.all;
use work.sc_pack.all;
use work.jop_config.all;

entity top_tb is
    constant ram_cnt     : integer := 4;     -- clock cycles for external ram
    constant rom_cnt     : integer := 15;    -- not used for S3K
    constant jpc_width   : integer := 11;    -- address bits of java bytecode pc = cache size
    constant block_bits  : integer := 4;     -- 2*block_bits is number of cache blocks = 4
    constant spm_width   : integer := 0;     -- size of scratchpad RAM (in number of address bits for 32-bit words)

    constant pwm_channel_count       : integer := 10;
    constant pwm_bits_per_channel    : integer := 16;
end top_tb;
 
architecture behavior of top_tb is

    signal clk : std_logic := '0';
    signal rst : std_logic := '1';
    signal uart0_rx : std_logic := '0';

    signal sc_mem_out       : sc_out_type;
    signal sc_mem_in        : sc_in_type;
    signal sc_io_out        : sc_out_type;
    signal sc_io_in         : sc_in_type;
    signal irq_in           : irq_bcf_type;
    signal irq_out          : irq_ack_type;
    signal exc_req          : exception_type;

    signal uart0_tx : std_logic;
    signal pwm : std_logic_vector(9 downto 0);

    constant clk_period : time := 10 ns;
 
begin

    u_cpu: entity work.jopcpu
        generic map(
            jpc_width => jpc_width,
            block_bits => block_bits,
            spm_width => spm_width
        )
        port map(
            clk => clk, 
            reset => rst,

            sc_mem_out => sc_mem_out, 
            sc_mem_in => sc_mem_in,

            sc_io_out => sc_io_out, 
            sc_io_in => sc_io_in,

            irq_in => irq_in, 
            irq_out => irq_out, 
            exc_req => exc_req
        );

    u_io: entity work.scio
        generic map(
            pwm_channel_count       => pwm_channel_count,
            pwm_bits_per_channel    => pwm_bits_per_channel
        )
        port map (
            clk => clk, 
            reset => rst,

            sc_io_out => sc_io_out, 
            sc_io_in => sc_io_in,

            irq_in => irq_in, 
            irq_out => irq_out, 
            exc_req => exc_req,

            txd         => uart0_tx,
            rxd         => uart0_rx,

            pwm_outputs => pwm
        );

    u_mem: entity work.mem_no_arbiter generic map(
            MEM_BITS => 14
        )
        port map(
            clk => clk,
            reset => rst,
            sc_mem_out => sc_mem_out,
            sc_mem_in => sc_mem_in
        );

    clk_process :process
    begin
        clk <= not clk;
        wait for clk_period/2;
    end process;

    stim_proc: process
    begin		
        wait for 100 ns;
        rst <= '0';

        wait for clk_period*10;

        -- insert stimulus here 

        wait;
    end process;

end;
