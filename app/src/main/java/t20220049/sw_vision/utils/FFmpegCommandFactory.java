package t20220049.sw_vision.utils;

public class FFmpegCommandFactory {
    public static String mp4ToTsCommand(String inputFile, String outputFile) {
        return "ffmpeg -i " + inputFile + " -c copy -bsf:v h264_mp4toannexb " + outputFile;
    }

    public static String cpCommand(String inputFile, String outputFile) {
        return "ffmpeg -i " + inputFile + " -c:v copy " + outputFile;
    }

    public static String mp4ToMpegCommand(String inputFile, String outputFile) {
        return "ffmpeg -i " + inputFile + " -qscale 4 " + outputFile;
    }

    public static String combineTsCommand(String[] inputFiles, String outputFile) {
        StringBuilder combine = new StringBuilder();
        for (int i = 0; i < inputFiles.length; i++) {
            if (i == 0) combine.append(inputFiles[i]);
            else combine.append("|").append(inputFiles[i]);
        }
        return "ffmpeg -i concat:" + combine + " -c copy -bsf:a aac_adtstoasc -movflags +faststart " + outputFile;
    }

    public static String cutMp4Command(String inputFile, String outputFile, double startTime, double durance) {
        return "ffmpeg -ss "
                + Double.valueOf(startTime).toString()
                + " -t "
                + Double.valueOf(durance).toString()
                + " -accurate_seek -i "
                + inputFile
                + " -codec copy "
                + outputFile;
    }
}
