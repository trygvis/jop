/*
  This file is part of JOP, the Java Optimized Processor
    see <http://www.jopdesign.com/>

  Copyright (C) 2011, Benedikt Huber (benedikt@vmars.tuwien.ac.at)

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package wcet.devel;

import joprt.RtThread;

import com.jopdesign.sys.Const;
import com.jopdesign.sys.Native;
import com.jopdesign.sys.Scope;
/**
 * Purpose: Analyze {@link Scope#enter(Runnable)}
 * Requires that in {@link com.jopdesign.sys.GC.USE_SCOPES} is set to {@code true}. 
 * @author Benedikt Huber (benedikt@vmars.tuwien.ac.at)
 *
 */
public class Scopes {
	   /* Debugging signals to manipulate the cache */
    final static int CACHE_FLUSH = -51;
    final static int CACHE_DUMP = -53;

    final static boolean MEASURE = true;
	final static boolean MEASURE_CACHE = false;

	static class Empty implements Runnable {
		@Override
		public void run() {
		}
	}
	static class Alloc implements Runnable {
		int x[];
		@Override
		public void run() {
			x = new int[1023];
		}
	}
	static class BigMethod implements Runnable {
		@Override
		public void run() {
			int val = 23;
		    val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  
		    val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  
		    val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  
		    val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;
		    val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  val += 123;  
		}
	}

	static int ts, te, to, tos;
	Scope scope;
	
	Empty empty;
	Alloc alloc;
	BigMethod bigMethod;

	public Scopes() {
		// scope overhead
		scope = new Scope(5000);

		empty = new Empty();		
		bigMethod = new BigMethod();
		alloc = new Alloc();
	}
	
	public void createThreads() {
		new RtThread(10, 5000000) { // 5 seconds should be enough time for the benchmark
			public void run() {
				for (;;) {
					scopeEmpty();
					if(MEASURE) {
						System.out.print("Cost for executing empty scope (excluding to): ");
						System.out.println(te-ts-to);
					}
					scopeAlloc();
					if(MEASURE) {
						System.out.print("Cost for executing scope allocating 1024 words (excluding to): ");
						System.out.println(te-ts-to);
					}
					scopeFull();
					if(MEASURE) {
						System.out.print("Cost for executing scope with one large method (excluding to): ");
						System.out.println(te-ts-to);
					}
					waitForNextPeriod();					
				}
			}
		};
	}

	public static void main(String[] args) {


		if(MEASURE) {
			ts = Native.rdMem(Const.IO_CNT);
			te = Native.rdMem(Const.IO_CNT);
			to = te-ts;
		}
		
		Scopes s = new Scopes();		
		s.createThreads();		
		RtThread.startMission();
	}
	
	public void scopeEmpty() {
		if(MEASURE) ts = Native.rdMem(Const.IO_CNT);
		scope.enter(empty);
		if(MEASURE) te = Native.rdMem(Const.IO_CNT);
	}
	public void scopeAlloc() {
		if(MEASURE) ts = Native.rdMem(Const.IO_CNT);
		scope.enter(alloc);
		if(MEASURE) te = Native.rdMem(Const.IO_CNT);
	}
	public void scopeFull() {
		if(MEASURE) ts = Native.rdMem(Const.IO_CNT);
		scope.enter(bigMethod);
		if(MEASURE) te = Native.rdMem(Const.IO_CNT);
	}
}
