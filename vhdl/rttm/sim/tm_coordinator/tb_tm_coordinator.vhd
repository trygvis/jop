-- TODO rewrite to test commit_allow port instead of internal signals

library ieee;

use std.textio.all;

use ieee.std_logic_1164.all;
use ieee.numeric_std.all;
-- TODO
use ieee.math_real.log2;
use ieee.math_real.ceil;

use work.tm_pack.all;

entity tb_tm_coordinator is
end tb_tm_coordinator;

architecture behav of tb_tm_coordinator is


--
--	Settings
--

constant cpu_cnt			: integer := 8;

--
--	Generic
--

signal finished				: boolean := false;

signal clk					: std_logic := '1';
signal reset				: std_logic;

constant cycle				: time := 10 ns;
constant reset_time			: time := 8 ns;

--
--	Testbench
--

subtype cpu_flags is std_logic_vector(0 to cpu_cnt-1);

signal commit_try			: cpu_flags;

begin

--
--	Testbench
--

	dut: entity work.tm_coordinator(rtl)
	generic map (
		cpu_cnt => cpu_cnt
		)
	port map (
		clk => clk,
		reset => reset,
		commit_token_request => commit_try,
		-- test internal next_token_grant signal
		commit_token_grant => open 
		);

	-- TODO use postponed process in parallel or ugly manual delay? 

	gen: process is
		alias next_commit_allow is 
			<< signal .dut.next_token_grant: cpu_flags >>;
	begin
		commit_try <= (others => '0');
	
		wait until falling_edge(reset);
		wait until rising_edge(clk);
				
		assert next_commit_allow= (0 to cpu_cnt-1 => '0');
		
		commit_try <= (others => '1');
		wait until rising_edge(clk);
		
		commit_try(3) <= '0';
		wait for 0 ns;
		
		assert next_commit_allow = cpu_flags'(0 => '1', others => '0');
		
		commit_try(1) <= '0';				
		wait for 0 ns;
		
		assert next_commit_allow = cpu_flags'(0 => '1', others => '0');		
		
		wait until rising_edge(clk);
		wait until rising_edge(clk);
		
		commit_try(0) <= '0';
		wait for 0 ns;
		
		wait until rising_edge(clk);
		
		assert next_commit_allow = cpu_flags'(2 => '1', others => '0');
		
		commit_try <= (others => '0');
		
		wait until rising_edge(clk);
		
		assert next_commit_allow = cpu_flags'(others => '0');
		
		finished <= true;
		write(output, "Test finished.");
		wait;
	end process gen;

--
--	Generic
--

	clock: process
	begin
	   	wait for cycle/2; clk <= not clk;
	   	if finished then
	   		wait;
	   	end if;
	end process clock;

	process
	begin
		reset <= '1';
		wait for reset_time;
		reset <= '0';
		wait;
	end process;
	

end;
