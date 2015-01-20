ImageSteganography
==================

Simple image steganography project

This is a simple program used to hide an image within another image. This is accomplished by changing the image that will be 
hidden to grayscale, then storing the information about the luminance of each grayscale pixel of that image in the bits of the 
pixels of another image. The program can hide an image in another and then recover the (grayscale) hidden image.

Wikipedia page of steganography for more details:
http://en.wikipedia.org/wiki/Steganography

* The idea for this project was inspired by part of an assignment from a Stanford introductory CS course that I helped section lead for in 2014. I extended it to support hiding of an entire image.
