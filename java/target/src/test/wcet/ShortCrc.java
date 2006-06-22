package wcet;

import com.jopdesign.sys.*;

public class ShortCrc {

	static int ts, te, to;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		ts = Native.rdMem(Const.IO_CNT);
		te = Native.rdMem(Const.IO_CNT);
		to = te-ts;
		// measurement + return takes 22+22+21=65 cycles
		// WCET measured: 1442/1552
		// WCET analysed: 1685-65=1620
		measure(123);
		int min = 0x7fffffff;
		int max = 0;
		int time = 0;
		int val = -1;
		for (int i=0; i<100000; ++i) { // @WCA loop=100
			val = measure(val);
			time = te-ts-to;
			if (time<min) min = time;
			if (time>max) max = time;
		}
		System.out.println(min);
		System.out.println(max);

	}
	
/*
	better values for polynom on short messages see:

		'Determining optimal cyclic codes for embedded networks'
		(www.cmu.edu)
*/
/**
*	claculate crc value with polynom x^8 + x^2 + x + 1
*	and initial value 0xff
*	on 32 bit data
*/
	
	static int measure(int val) {
		ts = Native.rdMem(Const.IO_CNT);
		int reg = -1;

		for (int i=0; i<32; ++i) { // @WCA loop=32
			reg <<= 1;
			if (val<0) reg |= 1;
			val <<=1;
			if ((reg & 0x100) != 0) reg ^= 0x07;
		}
		reg &= 0xff;
		te = Native.rdMem(Const.IO_CNT);		
		return reg;
	}
	
}