package com.design_patterns.creational_patterns;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @Author: linK
 * @Date: 2022/8/12 16:19
 * @Description TODO In class-based programming, the factory method pattern is a creational pattern that uses factory methods to deal with the problem of creating objects without having to specify the exact class of the object that will be created. This is done by creating objects by calling a factory method—either specified in an interface and implemented by child classes, or implemented in a base class and optionally overridden by derived classes—rather than by calling a constructor.
 * <p>
 * 在基于类的编程中，工厂方法模式是一种创建模式，它使用工厂方法来处理创建对象的问题，
 * 而不必指定将要创建的对象的确切类。
 * 这是通过调用工厂方法（在接口中指定并由子类实现，或在基类中实现并可选地由派生类覆盖）
 * 而不是通过调用构造函数来创建对象来完成的。
 */
public class Factory_Method {
    public static void main(String[] args) {
        DecodedImage decodedImage;
        ImageReader reader = null;
        Random random = new Random();
        args = random.nextInt(2) == 1 ? new String[]{"gif", "jpeg"} : new String[]{"jpeg", "gif"};

        String image = args[0];
        String format = image.substring(image.indexOf('.') + 1, (image.length()));
        if (format.equals("gif")) {
            reader = new GifReader(image);
        }
        if (format.equals("jpeg")) {
            reader = new JpegReader(image);
        }
        assert reader != null;
        decodedImage = reader.getDecodeImage();
        System.out.println(Arrays.stream(args).collect(Collectors.joining(",","[","]")));
        System.out.println(decodedImage);
        //[jpeg,gif]
        //jpeg: is decoded

        //[gif,jpeg]
        //gif: is decoded
    }
}

interface ImageReader {
    DecodedImage getDecodeImage();
}

class DecodedImage {
    private String image;

    public DecodedImage(String image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return image + ": is decoded";
    }
}

class GifReader implements ImageReader {
    private DecodedImage decodedImage;

    public GifReader(String image) {
        this.decodedImage = new DecodedImage(image);
    }

    @Override
    public DecodedImage getDecodeImage() {
        return decodedImage;
    }
}

class JpegReader implements ImageReader {
    private DecodedImage decodedImage;

    public JpegReader(String image) {
        decodedImage = new DecodedImage(image);
    }

    @Override
    public DecodedImage getDecodeImage() {
        return decodedImage;
    }
}
