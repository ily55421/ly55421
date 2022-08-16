package com.design_patterns.structural_patterns;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @Author: linK
 * @Date: 2022/8/11 15:58
 * @Description TODO  Decorator - authenticate, input, encrypt, authenticate, decrypt, output
 */
public class Decorator_in_java extends Decorator  {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    public Decorator_in_java(LCD inner) {
        super(inner);
    }

    public void write(String[] s) {
        System.out.print("PASSWORD: ");
        try {
            in.readLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        super.write( s );
    }

    public void read(String[] s) {
        System.out.print("PASSWORD: ");
        try {
            in.readLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        super.read(s);
    }

    public static void main(String[] args) {
        // 有点像 迭代   查找到core 层 直到节点时终止
        LCD stream = new Decorator_in_java(new Scramble(new Core()));
        String[] str = {""};
        stream.write(str);
        System.out.println("main:     " + str[0]);
        stream.read(str);
        //PASSWORD: 21
        //INPUT:    3
        //encrypt:
        //main:     .
        //PASSWORD: 21
        //decrypt:
        //Output:   3
    }
}

interface LCD {
    void write(String[] s);
    void read(String[] s);
}

class Core implements LCD {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    public void write(String[] s) {
        System.out.print("INPUT:    ");
        try {
            s[0] = in.readLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void read(String[] s) {
        System.out.println("Output:   " + s[0]);
    }
}

class Decorator implements LCD {
    private LCD inner;

    public Decorator(LCD i) {
        inner = i;
    }

    public void write(String[] s) {
        inner.write(s);
    }

    public void read(String[] s) {
        inner.read(s);
    }
}

class Scramble extends Decorator {
    public Scramble(LCD inner) {
        super(inner);
    }

    public void write( String[] s ) {
        super.write(s);
        System.out.println("encrypt:");
        StringBuilder sb = new StringBuilder(s[0]);
        for (int i=0; i < sb.length(); i++) {
            sb.setCharAt(i, (char)(sb.charAt(i) - 5));
        }
        s[0] = sb.toString();
    }

    public void read(String[] s) {
        StringBuilder sb = new StringBuilder(s[0]);
        for (int i=0; i < sb.length(); i++) {
            sb.setCharAt(i, (char)(sb.charAt(i) + 5));
        }
        s[0] = sb.toString();
        System.out.println( "decrypt:" );
        super.read(s);
    }
}

