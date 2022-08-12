package com.design_patterns;

import java.util.ArrayList;

/**
 * @Author: linK
 * @Date: 2022/8/11 15:41
 * @Description TODO
 */
public class Composite_After {
    public static StringBuffer compositeBuilder = new StringBuffer();

    public static void main(String[] args) {
        Directory02 music = new Directory02("MUSIC");
        Directory02 scorpions = new Directory02("SCORPIONS");
        Directory02 dio = new Directory02("DIO");
        File02 track1 = new File02("Don't wary, be happy.mp3");
        File02 track2 = new File02("track2.m3u");
        File02 track3 = new File02("Wind of change.mp3");
        File02 track4 = new File02("Big city night.mp3");
        File02 track5 = new File02("Rainbow in the dark.mp3");
        music.add(track1);
        music.add(scorpions);
        music.add(track2);
        scorpions.add(track3);
        scorpions.add(track4);
        scorpions.add(dio);
        dio.add(track5);
        music.ls();
        //MUSIC
        //   Don't wary, be happy.mp3
        //   SCORPIONS
        //      Wind of change.mp3
        //      Big city night.mp3
        //      DIO
        //         Rainbow in the dark.mp3
        //   track2.m3u
    }
}
// Define a "lowest common denominator"
interface AbstractFile0202 {
    void ls();
}

// File02 implements the "lowest common denominator"  实现“最小公分母”
class File02 implements AbstractFile0202 {
    private String name;

    public File02(String name) {
        this.name = name;
    }

    public void ls() {
        System.out.println(Composite_After.compositeBuilder + name);
    }
}

// Directory02 implements the "lowest common denominator"
class Directory02 implements AbstractFile0202 {
    private String name;
    private ArrayList includedFile02s = new ArrayList();

    public Directory02(String name) {
        this.name = name;
    }

    public void add(Object obj) {
        includedFile02s.add(obj);
    }

    public void ls() {
        System.out.println(Composite_After.compositeBuilder + name);
        Composite_After.compositeBuilder.append("   ");
        for (Object includedFile02 : includedFile02s) {
            // Leverage the "lowest common denominator"
            AbstractFile0202 obj = (AbstractFile0202) includedFile02;
            obj.ls();
        }
        Composite_After.compositeBuilder.setLength(Composite_After.compositeBuilder.length() - 3);
    }
}

