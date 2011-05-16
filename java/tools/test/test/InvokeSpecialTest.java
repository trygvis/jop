/*
 * This file is part of JOP, the Java Optimized Processor
 *   see <http://www.jopdesign.com/>
 *
 * Copyright (C) 2011, Stefan Hepp (stefan@stefant.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package test;

/**
 * @author Stefan Hepp (stefan@stefant.org)
 */
public class InvokeSpecialTest {
    public static void main(String[] args) {
        C c = new C();
        B b = new B();
        System.out.println("   C " + c.b());
        System.out.println("(B)C " + ((B)c).b());
        System.out.println("   B " + b.b());
    }
}

class A {
    public int a() {
        return 1;
    }
}
class B extends A {
    public int b() {
        return super.a();
    }
    public int a() {
        return 2;
    }
}
class C extends B {
    public int a() {
        return 3;
    }
}