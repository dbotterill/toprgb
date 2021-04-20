# TopRgbService


## Assignment

Bellow is a list of links leading to an image, read this list of images and find 3 most prevalent colors in the RGB scheme in hexadecimal format (#000000 - #FFFFFF) in each image, and write the result into a CSV file in a form of url,color,color,color.

Please focus on speed and resources. The solution should be able to handle input files with more than a billion URLs, using limited resources (e.g. 1 CPU, 512MB RAM). Keep in mind that there is no limit on the execution time, but make sure you are utilizing the provided resources as much as possible at any time during the program execution.

The test file is located here, [./src/test/resources/input.txt](./src/test/resources/input.txt)

## Build w/o Test Instructions


`./gradlew -x test clean fatJar`

## Test Instructions

`./gradlew test`

## Run Instructions

`java -jar ./build/libs/./build/libs/TopRgbService.jar -i <input filepath (required)> 
-o <output filepath (default ./toprgb.csv)> 
-t <number of threads for scanning image files (default 8)>  
-cs <chunk size for sorting external files (default 1000000000)>`

## Output File Protection

To guard against accidentally overwriting a file that took a very long time to create, if the output file exists, the service will append an underscore plus a timestamp in milliseconds to the end of the file.

For example:

`toprgb.csv_1618885848518`

## Limitations

The service relies on the fact that the input file gets sorted by the URLs.  If a URL points to a redirect, it's possible that the same target URL will be pointed to by more than one redirect.  This means the same image file could be scanned more than once.


