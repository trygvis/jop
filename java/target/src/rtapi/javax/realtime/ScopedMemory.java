/*
  This file is part of JOP, the Java Optimized Processor
    see <http://www.jopdesign.com/>
  This subset of javax.realtime is provided for the JSR 302
  Safety Critical Specification for Java

  Copyright (C) 2008, Martin Schoeberl (martin@jopdesign.com)

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

package javax.realtime;

//import com.jopdesign.io.IOFactory;
//import com.jopdesign.sys.Scope;
//import com.jopdesign.sys.Startup;
//
//public abstract class ScopedMemory extends MemoryArea {
//
//	Scope sc;
//
//	public ScopedMemory(long size) {
//		// super does nothing, but we have to invoke it
//		super(size);
//		sc = new Scope(size);
//	}
//
//	public ScopedMemory(int[] localMem) {
//		// super does nothing
//		super(0);
//		sc = new Scope(localMem);
//	}
//	
//	/**
//	 * We can only use one physical memory per CPU core
//	 */
//	private static boolean physInUse[] = new boolean[Runtime.getRuntime().availableProcessors()];
//
//	/**
//	 * Package private constructor to be used by LTPhysicalMemory
//	 * @param type
//	 * @param size
//	 */
//	ScopedMemory(Object type, long size) {
//		// super does nothing
//		super(0);
//		if (type==PhysicalMemoryManager.ON_CHIP_PRIVATE) {
//			synchronized(physInUse) {
//				if (size>Startup.getSPMSize()) {
//					throw new RuntimeException("Local memory is not big enough");
//				}
//				IOFactory fact = IOFactory.getFactory();
//				if (physInUse[fact.getSysDevice().cpuId]) {
//					throw new RuntimeException("Physical memory already in use");
//				}
//				physInUse[fact.getSysDevice().cpuId] = true;				
//				sc = new Scope(fact.getScratchpadMemory());
//			}
//		} else {
//			sc = new Scope(size);
//		}
//	}
//
//	public void enter(Runnable logic) throws RuntimeException {
//		sc.enter(logic);
//	}
//	
//	public long size() {
//		return sc.getSize();
//	}
//
//}

// this is the SCJ version
import static javax.safetycritical.annotate.Allocate.Area.SCOPED;
import static javax.safetycritical.annotate.Level.LEVEL_1;

import javax.safetycritical.annotate.Allocate;
import javax.safetycritical.annotate.BlockFree;
import javax.safetycritical.annotate.SCJAllowed;
import javax.safetycritical.annotate.SCJProtected;

@SCJAllowed
public abstract class ScopedMemory
  extends MemoryArea implements ScopedAllocationContext
{
  @Allocate({SCOPED})
  @BlockFree
  @SCJProtected
  public ScopedMemory(long size) 
  {
	  super(size);
  }

  @Allocate({SCOPED})
  @BlockFree
  @SCJProtected
  public ScopedMemory(SizeEstimator estimator) {}

  /**
   * Not @SCJAllowed
   *
   */
  public Object getPortal() throws MemoryAccessError, IllegalAssignmentError
  {
    return null; // dummy return
  }

  /**
   * Not @SCJAllowed
   */
  public void setPortal(Object object) {}
  
  @SCJAllowed
  public void resize(long size) {};


  public void join() throws InterruptedException {}
  
}
