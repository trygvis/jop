/* StringBuffer.java -- Growable strings
   Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

package java.lang;

// import java.io.Serializable;

// public final class StringBuffer implements Serializable, CharSequence
public final class StringBuffer {

  int count;

  char[] value;

  private final static int DEFAULT_CAPACITY = 16;

  public StringBuffer() {

    this(DEFAULT_CAPACITY);
  }

  public StringBuffer(int capacity) {

    value = new char[capacity];
  }

  public StringBuffer(String str) {

    // Unfortunately, because the size is 16 larger, we cannot share.
//    count = str.count;
count = str.length();
    value = new char[count + DEFAULT_CAPACITY];
for (int i=0; i<count; ++i) value[i] = str.value[i];
    // str.getChars(0, count, value, 0);
  }

  public String toString()
  {
    return new String(this);
  }

  public synchronized int length()
  {
    return count;
  }

  public synchronized int capacity()
  {
    return value.length;
  }

  public synchronized void setLength(int newLength)
  {
    if (newLength < 0)
//      throw new StringIndexOutOfBoundsException(newLength);
return;

    ensureCapacity_unsynchronized(newLength);
    while (count < newLength)
      value[count++] = '\0';
    count = newLength;
  }

  public synchronized char charAt(int index)
  {
    if (index < 0 || index >= count)
      // throw new StringIndexOutOfBoundsException(index);
return '0';
    return value[index];
  }

	void arraycopy(char[] src, int src_pos, char[] dst, int dst_pos, int count) {

		if (src_pos < 0 || dst_pos < 0 || count < 0 ||
			count+src_pos > src.length || 
			count+dst_pos > dst.length) {
			// throw ...
			return;
		}

		for (int i=0; i<count; ++i) dst[dst_pos+i] = src[src_pos+i];
	}

  public synchronized void getChars(int srcOffset, int srcEnd,
                                    char[] dst, int dstOffset)
  {
    int todo = srcEnd - srcOffset;
    if (srcOffset < 0 || srcEnd > count || todo < 0)
//      throw new StringIndexOutOfBoundsException();
return;
    arraycopy(value, srcOffset, dst, dstOffset, todo);
  }

  public synchronized void setCharAt(int index, char ch)
  {
    if (index < 0 || index >= count)
//      throw new StringIndexOutOfBoundsException(index);
return;
    // Call ensureCapacity to enforce copy-on-write.
    ensureCapacity_unsynchronized(count);
    value[index] = ch;
  }

  public synchronized StringBuffer append(String str)
  {
    if (str == null)
      str = "null";
    // int len = str.count;
    int len = str.length();
    ensureCapacity_unsynchronized(count + len);
    str.getChars(0, len, value, count);
    count += len;
    return this;
  }

/*
	only in 1.4 (not 1.3)!
  public synchronized StringBuffer append(StringBuffer stringBuffer)
  {
    if (stringBuffer == null)
      return append("null");
//    synchronized (stringBuffer)
//      {
        int len = stringBuffer.count;
        ensureCapacity_unsynchronized(count + len);
        arraycopy(stringBuffer.value, 0, value, count, len);
        count += len;
//      }
    return this;
  }
*/

  public StringBuffer append(char[] data)
  {
    return append(data, 0, data.length);
  }

  public synchronized StringBuffer append(char[] data, int offset, int count)
  {
    ensureCapacity_unsynchronized(this.count + count);
    arraycopy(data, offset, value, this.count, count);
    this.count += count;
    return this;
  }

  public synchronized StringBuffer append(char ch)
  {
    ensureCapacity_unsynchronized(count + 1);
    value[count++] = ch;
    return this;
  }

  public synchronized StringBuffer delete(int start, int end)
  {
    if (start < 0 || start > count || start > end)
//      throw new StringIndexOutOfBoundsException(start);
return this;
    if (end > count)
      end = count;
    // This will unshare if required.
    ensureCapacity_unsynchronized(count);
    if (count - end != 0)
      arraycopy(value, end, value, start, count - end);
    count -= end - start;
    return this;
  }

  public StringBuffer deleteCharAt(int index)
  {
    return delete(index, index + 1);
  }

  public synchronized StringBuffer replace(int start, int end, String str)
  {
    if (start < 0 || start > count || start > end)
//      throw new StringIndexOutOfBoundsException(start);
return this;

    // int len = str.count;
    int len = str.length();
    // Calculate the difference in 'count' after the replace.
    int delta = len - (end > count ? count : end) + start;
    ensureCapacity_unsynchronized(count + delta);

    if (delta != 0 && end < count)
      arraycopy(value, end, value, end + delta, count - end);

    str.getChars(0, len, value, start);
    count += delta;
    return this;
  }

  public synchronized StringBuffer insert(int offset,
                                          char[] str, int str_offset, int len)
  {
    if (offset < 0 || offset > count || len < 0
        || str_offset < 0 || str_offset + len > str.length)
//      throw new StringIndexOutOfBoundsException();
return this;
    ensureCapacity_unsynchronized(count + len);
    arraycopy(value, offset, value, offset + len, count - offset);
    arraycopy(str, str_offset, value, offset, len);
    count += len;
    return this;
  }

  public synchronized StringBuffer insert(int offset, String str)
  {
    if (offset < 0 || offset > count)
//      throw new StringIndexOutOfBoundsException(offset);
return this;
    if (str == null)
      str = "null";
    // int len = str.count;
    int len = str.length();
    ensureCapacity_unsynchronized(count + len);
    arraycopy(value, offset, value, offset + len, count - offset);
    str.getChars(0, len, value, offset);
    count += len;
    return this;
  }

  public StringBuffer insert(int offset, char[] data)
  {
    return insert(offset, data, 0, data.length);
  }

  public synchronized StringBuffer insert(int offset, char ch)
  {
    if (offset < 0 || offset > count)
//      throw new StringIndexOutOfBoundsException(offset);
return this;
    ensureCapacity_unsynchronized(count + 1);
    arraycopy(value, offset, value, offset + 1, count - offset);
    value[offset] = ch;
    count++;
    return this;
  }

  private void ensureCapacity_unsynchronized(int minimumCapacity)
  {
    // if (shared || minimumCapacity > value.length)
    if (minimumCapacity > value.length)
      {
        // We don't want to make a larger vector when `shared' is
        // set.  If we do, then setLength becomes very inefficient
        // when repeatedly reusing a StringBuffer in a loop.
        int max = (minimumCapacity > value.length
                   ? value.length * 2 + 2
                   : value.length);
        minimumCapacity = (minimumCapacity < max ? max : minimumCapacity);
        char[] nb = new char[minimumCapacity];
//        System.arraycopy(value, 0, nb, 0, count);
for (int i=0; i<count; ++i) nb[i] = value[i];
        value = nb;
//        shared = false;
      }
  }

}
