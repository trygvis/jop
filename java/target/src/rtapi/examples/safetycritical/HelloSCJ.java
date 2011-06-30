/*
  This file is part of JOP, the Java Optimized Processor
    see <http://www.jopdesign.com/>

  Copyright (C) 2008-2011, Martin Schoeberl (martin@jopdesign.com)

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

package examples.safetycritical;

import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;
import javax.safetycritical.*;
import javax.safetycritical.io.SimplePrintStream;

/**
 * A minimal SCJ application - The SCJ Hello World
 * 
 * @author Martin Schoeberl
 * 
 */
public class HelloSCJ extends Mission implements Safelet {

	// work around...
	static HelloSCJ single;

	static SimplePrintStream out;

	// From Mission
	@Override
	protected void initialize() {

		OutputStream os = null;
		try {
			os = Connector.openOutputStream("console:");
		} catch (IOException e) {
			throw new Error("No console available");
		}
		out = new SimplePrintStream(os);

		PeriodicEventHandler peh = new PeriodicEventHandler(
				new PriorityParameters(11), new PeriodicParameters(
						new RelativeTime(0, 0), new RelativeTime(1000, 0)),
				new StorageParameters(10000, 1000, 1000)) {
			int cnt;

			public void handleAsyncEvent() {
				out.println("Ping " + cnt);
				++cnt;
				if (cnt > 5) {
					// getCurrentMission is not yet working
					single.requestTermination();
				}
			}
		};
		peh.register();
	}

	// Safelet methods
	@Override
	public MissionSequencer getSequencer() {
		// we assume this method is invoked only once
		StorageParameters sp = new StorageParameters(1000000, 0, 0);
		return new LinearMissionSequencer(new PriorityParameters(13), sp, this);
	}

	@Override
	public long missionMemorySize() {
		return 100000;
	}

	/**
	 * Within the JOP SCJ version we use a main method instead of a command line
	 * parameter or configuration file.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Hello");
		Terminal.getTerminal().writeln("Hello SCJ World!");
		single = new HelloSCJ();
		JopSystem.startMission(single);
	}

}
