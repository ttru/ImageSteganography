package steganography;
/**
 * File: ImageHider.java
 * ---------------------------------------
 * Contains methods to 'hide' an image
 * within another, and to reveal images 
 * hidden using this program. Uses 
 * steganography and manipulation of pixel
 * bits to accomplish this.
 */
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Color;
import javax.imageio.*;
import java.io.*;
import java.util.*;

public class ImageHider {
    /* allowed file extensions */
    private static final String[] extensions = {"bmp", "gif", "jpeg", "jpg", "png", "wpng"};
    
    /* Returns true if filename string ends with allowed extensions */
    private static boolean hasValidExtension(String filename) {
        return Arrays.binarySearch(extensions, getExtension(filename)) >= 0;
    }                        
    
    /* Returns the extensions from a filename */
    private static String getExtension(String filename) {
        String[] filenameComponents = filename.split("\\.");
        return filenameComponents[filenameComponents.length - 1].toLowerCase();
    }

    /* Returns the luminance of an rgb value */
    private static int getLuminance(int rgb) {
        Color c = new Color(rgb);
        return (int)(0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue());
    }

    /* Returns an int close to a number such that number % factor == remainder. */
    private static int roundToMatchModulo(int number, int factor, int remainder) {
        if(number < factor) {
            return remainder;
        } else {
            while(number % factor != remainder) {
                number--;
            }
            return number;
        }
    }        

    /* Changes the rgb values of the base image to store information about the hidden image. */
    private static void hideImageBits(BufferedImage baseImage, BufferedImage hiddenImage, BufferedImage outputImage) {
        for(int r = 0; r < baseImage.getHeight(); r++) {
            for(int c = 0; c < baseImage.getWidth(); c++) {
                if(r < hiddenImage.getHeight() && c < hiddenImage.getWidth()) { 
                    Color baseColor = new Color(baseImage.getRGB(c,r), true);
                    // luminance will be between 0 and 255 (8 bits needed to store number)
                    int hiddenLuminance = getLuminance(hiddenImage.getRGB(c,r));

                    // break up the bits of the luminance into 4 pieces (2-bit each, so values 0-3)
                    int alphaBits = (hiddenLuminance << 24) >>> 30;
                    int redBits = (hiddenLuminance << 26) >>> 30;
                    int greenBits = (hiddenLuminance << 28) >>> 30;
                    int blueBits = (hiddenLuminance << 30) >>> 30;

                    // alter the color channels of the pixel so that the alpha value mod 4 equals the 
                    // corresponding partition of the luminance values
                    int newAlpha = roundToMatchModulo(baseColor.getAlpha(), 4, alphaBits);
                    int newRed = roundToMatchModulo(baseColor.getRed(), 4, redBits);
                    int newGreen = roundToMatchModulo(baseColor.getGreen(), 4, greenBits);
                    int newBlue = roundToMatchModulo(baseColor.getBlue(), 4, blueBits);

                    outputImage.setRGB(c, r, (newAlpha << 24) | (newRed << 16) | (newGreen << 16) | (newBlue << 16));
                }
            }
        }
    }

    /* Opens two image files, and 'hides' a grayscale image in the other by manipulating
       the color bits of the base image's pixels. Outputs the image to file. */
    public static void hideImage(String baseFile, String hiddenFile, String outputFile) {
        // use png output for alpha support
        if(!hasValidExtension(baseFile) || !hasValidExtension(hiddenFile) || !getExtension(outputFile).equals("png")) {
            System.err.println("Invalid file extension. Use only .bmp, .gif, .jpeg,  .jpg, .png, .wpng for input files, and only .png for output.");
            return;
        }
        try {
            BufferedImage baseImage = ImageIO.read(new File(baseFile));
            BufferedImage hiddenImage = ImageIO.read(new File(hiddenFile));
            BufferedImage outputImage = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

            hideImageBits(baseImage, hiddenImage, outputImage);
            
            ImageIO.write(outputImage, "png", new File(outputFile));
        } catch(IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /* Examine pixels of input image to recreate hidden image */
    private static void revealImageBits(BufferedImage inputImage, BufferedImage revealedImage) {
        for(int r = 0; r < inputImage.getHeight(); r++) {
            for(int c = 0; c < inputImage.getWidth(); c++) {
                Color inputColor = new Color(inputImage.getRGB(c,r), true);
                // look at each color channel mod 4
                int alphaBits = inputColor.getAlpha() % 4;
                int redBits = inputColor.getRed() % 4;
                int greenBits = inputColor.getBlue() % 4;
                int blueBits = inputColor.getGreen() % 4;

                // compose the values into the luminance of the hidden image's pixel at this point
                int revealedLuminance = (alphaBits << 6) | (redBits << 4) | (greenBits << 2) | (blueBits);
                // set the revealed image to have a grayscale pixel with that luminance
                revealedImage.setRGB(c, r, new Color(revealedLuminance, revealedLuminance, revealedLuminance).getRGB());
            }
        }
    }

    /* Opens an image outputted by hideImage above, and retrieves the grayscale hidden image. */
    public static void revealImage(String inputFile, String revealedFile) {
        // simple way to check that the files have image extensions
        if(!hasValidExtension(inputFile) || !hasValidExtension(revealedFile)) {
            System.err.println("Invalid file extension. Use .bmp, .gif, .jpeg,  .jpg, .png, .wpng only");
            return;
        }
        try {
            BufferedImage inputImage = ImageIO.read(new File(inputFile));
            BufferedImage revealedImage = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            
            revealImageBits(inputImage, revealedImage);
            
            ImageIO.write(revealedImage, getExtension(revealedFile), new File(revealedFile));
        } catch(IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // simple input handling
        if((args.length != 3 && args.length != 4) || (args.length == 3 && !args[0].toLowerCase().equals("reveal")) || (args.length == 4 && !args[0].toLowerCase().equals("hide"))) {
            System.out.println("usage: to hide image: java ImageHider hide path/to/base/image path/to/hidden/image path/to/output/image");
            System.out.println("       to reveal image: java ImageHider reveal path/to/encoded/image path/to/output/image");
        }
        
        if(args.length == 3) {
            revealImage(args[1], args[2]);
        }
        if(args.length == 4) {
            hideImage(args[1], args[2], args[3]);
        }
    }
            

}
