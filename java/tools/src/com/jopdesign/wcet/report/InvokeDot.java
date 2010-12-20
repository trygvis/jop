/*
  This file is part of JOP, the Java Optimized Processor
    see <http://www.jopdesign.com/>

  Copyright (C) 2008, Benedikt Huber (benedikt.huber@gmail.com)

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
package com.jopdesign.wcet.report;

import com.jopdesign.common.config.Config;
import com.jopdesign.wcet.ProjectConfig;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * This class invokes the .dot program to generate graphs.
 * As invoking dot is very time consuming, we cache output graphs using
 * md5s on the DOT file.
 *
 * @author Benedikt Huber, benedikt.huber@gmail.com
 *
 */
public class InvokeDot {
	private static final Logger logger = Logger.getLogger(InvokeDot.class);
	private static final String DEFAULT_CACHE_DIR = "dot-cache";
	public File getCacheFile(String filename) {
		return new File(cacheDir,filename);
	}
	public static void invokeDot(File dotFile, File outFile) throws IOException {
		Config c = Config.instance();
		File cacheDir = ProjectConfig.getOutDirStatic(DEFAULT_CACHE_DIR);
		InvokeDot id = new InvokeDot(c.getOption(ReportConfig.PROGRAM_DOT),cacheDir);
		id.runDot(dotFile, outFile);
	}
	private String dotBinary;
	private File cacheDir;
	private String format;
	public InvokeDot(String dotBinary, File cacheDir) {
		this.dotBinary = dotBinary;
		this.cacheDir  = cacheDir;
		this.format = "png";
	}
	public void setFormat(String format) {
		this.format = format;
	}
	public void runDot(File dotFile, File imageFile) throws IOException {
		byte[] md5;
		try {
			md5 = calculateMD5(dotFile);
		} catch (NoSuchAlgorithmException e) {
			throw new Error("Unexpected exception: MD5 Algorithm not available",e);
		}	
		if(cacheDir != null) {
			File cachedFile = getCacheFile(byteArrayToString(md5)+".png");
			if(! cachedFile.exists()) {
				runDot(dotFile,cachedFile,format);
			}
			copyFile(cachedFile,imageFile);
		} else {
			runDot(dotFile,imageFile,format);			
		}
	}
	
	private void runDot(File dotFile,File imageFile, String fmt) throws IOException {
		String cmd[] = { dotBinary, dotFile.getPath(), "-T"+fmt, "-o", imageFile.getPath() };
		Process p;
		logger.info("Invoking dot: "+Arrays.toString(cmd));
		p = Runtime.getRuntime().exec(cmd);
		int exitCode = -1;
		try { exitCode = p.waitFor(); } 
		catch (InterruptedException e) { logger.warn("Waiting for dot program interrupted: "+e); }
		if(exitCode != 0) {
			throw new IOException("Non-Zero exit code from dot program: " + exitCode);
		}
		if(! imageFile.exists()) {
			throw new IOException("Dot program run, but imagefile "+imageFile +" hasn't been created - Maybe an empty .dot file ?");
		}
	}

	private byte[] calculateMD5(File cgdot) throws NoSuchAlgorithmException, IOException {
		MessageDigest m = MessageDigest.getInstance("MD5");
		FileInputStream fis = new FileInputStream(cgdot);
	    byte[] buffer = new byte[1024];
	    int read=1;
	    while((read = fis.read(buffer)) > 0) {
	      m.update(buffer,0,read);
	    }
	    fis.close();
		return m.digest();
	}
	private String byteArrayToString(byte[] barray) {
		StringBuffer buf = new StringBuffer();
		for(Byte by : barray) {
			buf.append(String.format("%02x", (short) by));
		}
		return buf.toString();
	}
	public static void copyFile(File in, File out) throws IOException 
	{
		FileChannel inChannel = new FileInputStream(in).getChannel();
		FileChannel outChannel = new FileOutputStream(out).getChannel();
		try {
			inChannel.transferTo(0, inChannel.size(),outChannel);
		} 
		catch (IOException e) {
			throw e;
		}
		finally {
			if (inChannel != null) inChannel.close();
			if (outChannel != null) outChannel.close();
		}
	}
}
