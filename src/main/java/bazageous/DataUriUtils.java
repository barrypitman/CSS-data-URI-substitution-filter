package bazageous;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for dealing with data uris and css files
 *
 * @author barry
 * @since 2011/06/13 1:58 PM
 */
public abstract class DataUriUtils {

    private static final Logger LOG = Logger.getLogger(DataUriUtils.class);
    private static final Pattern BACKGROUND_IMG = Pattern.compile("\\bbackground(-image)?:\\s*url\\(['|\"]?(.*?)['|\"]?\\)", Pattern.CASE_INSENSITIVE);
    private static final Map<String, String> FILESUFFIX_MIMETYPE_MAP = new HashMap<String, String>();

    static {
        FILESUFFIX_MIMETYPE_MAP.put("gif", "image/gif");
        FILESUFFIX_MIMETYPE_MAP.put("jpeg", "image/jpeg");
        FILESUFFIX_MIMETYPE_MAP.put("jpg", "image/jpeg");
        FILESUFFIX_MIMETYPE_MAP.put("png", "image/png");
    }

    private DataUriUtils() {
    }

    /**
     * @param cssFileContents the contents of a css file/snippet to parse
     * @return a list of the url strings used in background(-image) attributes extracted from the css file
     */
    public static List<BackgroundImageReference> findBackgroundImages(String cssFileContents) {
        Matcher matcher = BACKGROUND_IMG.matcher(cssFileContents);
        List<BackgroundImageReference> backgroundImageReferences = new ArrayList<BackgroundImageReference>();
        int index = 0;
        while (matcher.find(index)) {
            String group = matcher.group(2);
            //don't match existing data: uris
            if (group.contains("data:") || group.contains("://")) {
                LOG.debug("Skipping css match '" + group + "' either because it is already a data uri, or refers to an external image.");
            } else {
                backgroundImageReferences.add(new BackgroundImageReference(matcher.start(2), matcher.end(2), group));
            }
            index = matcher.end();
        }
        return backgroundImageReferences;
    }

    public static String convertToDataUri(byte[] data, String filename) {
        StringBuilder builder = new StringBuilder();
        String suffix = FilenameUtils.getExtension(filename);
        String mimeType = FILESUFFIX_MIMETYPE_MAP.get(suffix);
        if (mimeType == null) {
            mimeType = "image/png";
            LOG.warn("Unknown file extension for filename '" + filename + "', returning '" + mimeType + "'");
        }
        builder.append("data:").append(mimeType).append(";base64,").append(new String(Base64.encodeBase64(data)));
        return builder.toString();
    }

    /**
     * Holds a match to a background image reference.
     */
    public static class BackgroundImageReference {
        private final int start;
        private final int end;
        private final String url;

        public BackgroundImageReference(int start, int end, String url) {
            this.start = start;
            this.end = end;
            this.url = url;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public String getUrl() {
            return url;
        }
    }
}