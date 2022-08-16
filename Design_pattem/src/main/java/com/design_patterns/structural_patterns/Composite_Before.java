package com.design_patterns.structural_patterns;

import java.util.ArrayList;

/**
 * @Author: linK
 * @Date: 2022/8/11 15:38
 * @Description TODO
 */
public class Composite_Before {
    public static StringBuilder compositeBuilder = new StringBuilder();

    /**
     * 层次遍历的算法
     * 深度优先 和 广度优先
     *
     * @param args
     */
    public static void main(String[] args) {
        Directory music = new Directory("MUSIC");
        Directory scorpions = new Directory("SCORPIONS");
        Directory dio = new Directory("DIO");
        File track1 = new File("Don't wary, be happy.mp3");
        File track2 = new File("track2.m3u");
        File track3 = new File("Wind of change.mp3");
        File track4 = new File("Big city night.mp3");
        File track5 = new File("Rainbow in the dark.mp3");
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

class File {
    private String name;

    public File(String name) {
        this.name = name;
    }

    public void ls() {
        System.out.println(Composite_Before.compositeBuilder + name);
    }
}

class Directory {
    private String name;
    private ArrayList includedFiles = new ArrayList();

    public Directory(String name) {
        this.name = name;
    }

    public void add(Object obj) {
        includedFiles.add(obj);
    }

    public void ls() {
        System.out.println(Composite_Before.compositeBuilder + name);
        Composite_Before.compositeBuilder.append("   ");
        for (Object obj : includedFiles) {
            // Recover the type of this object
            String name = obj.getClass().getSimpleName();
            if (name.equals("Directory")) {
                ((Directory) obj).ls();
            } else {
                ((File) obj).ls();
            }
        }
        Composite_Before.compositeBuilder.setLength(Composite_Before.compositeBuilder.length() - 3);
    }
}

