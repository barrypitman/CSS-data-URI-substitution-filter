package bazageous;

import nl.bitwalker.useragentutils.Browser;
import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Servlet filter designed to intercept requests to css files, and replace references to background images with
 * data uri's (if the client browser accepts them)
 *
 * @author barry
 * @since 2011/06/13 1:05 PM
 */
public class CssDataUriFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(CssDataUriFilter.class);
    private static final List<Browser> INCOMPATIBLE_BROWSERS = new ArrayList<Browser>();

    static {
        // browsers which do not support data uri's
        INCOMPATIBLE_BROWSERS.add(Browser.IE5);
        INCOMPATIBLE_BROWSERS.add(Browser.IE5_5);
        INCOMPATIBLE_BROWSERS.add(Browser.IE6);
        INCOMPATIBLE_BROWSERS.add(Browser.IE7);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        if (!isFilterEnabled(httpServletRequest) || !userAgentSupportsDataUris(httpServletRequest)) {
            LOG.info("Skipping css dataUri replacement");
            chain.doFilter(request, response);
            return;
        }

        //save a reference to the original print writer, so that we can eventually write the response back to the client
        PrintWriter originalPrintWriter = response.getWriter();

        try {
            CssFileResponseWrapper wrappedResponse = new CssFileResponseWrapper((HttpServletResponse) response);

            chain.doFilter(httpServletRequest, wrappedResponse);
            LOG.info("Performing background-image data uri replacement for request uri: " +
                    httpServletRequest.getRequestURI());

            String cssFileContents = wrappedResponse.toString();
            String finalResponse = replaceBackgroundImagesWithDataUris(httpServletRequest, httpServletResponse, cssFileContents);

            response.setContentLength(finalResponse.length());
            originalPrintWriter.write(finalResponse);

        } finally {
            originalPrintWriter.close();
        }
    }

    public void destroy() {
    }

    private String replaceBackgroundImagesWithDataUris(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, String responseString) throws ServletException, IOException {
        List<DataUriUtils.BackgroundImageReference> images = DataUriUtils.findBackgroundImages(responseString);
        StringBuilder responseBuffer = new StringBuilder(responseString);

        ListIterator<DataUriUtils.BackgroundImageReference> iterator = images.listIterator(images.size());

        // Iterate in reverse so that our indices into the original css file are not corrupts when we insert into the buffer.
        while (iterator.hasPrevious()) {
            DataUriUtils.BackgroundImageReference backgroundImageMatch = iterator.previous();
            String dataUriString = convertImageToDataUriString(httpServletRequest, httpServletResponse, backgroundImageMatch);
            if (dataUriString != null) {
                LOG.debug("Replacing css background image '" + backgroundImageMatch.getUrl() + "' with inline data uri");
                responseBuffer.replace(backgroundImageMatch.getStart(), backgroundImageMatch.getEnd(), dataUriString);
            }
        }

        return responseBuffer.toString();
    }

    private String convertImageToDataUriString(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, DataUriUtils.BackgroundImageReference backgroundImageMatch) throws ServletException, IOException {
        try {
            IncludedImageResponseWrapper imageResponseWrapper = new IncludedImageResponseWrapper(httpServletResponse);
            String backgroundImageUrl = backgroundImageMatch.getUrl();
            RequestDispatcher dispatcher = httpServletRequest.getRequestDispatcher(backgroundImageUrl);
            dispatcher.include(httpServletRequest, imageResponseWrapper);
            byte[] imageBytes = imageResponseWrapper.getOutput();
            if (imageBytes.length == 0) {
                LOG.info("Image '" + backgroundImageUrl + "' not found (404), skipping.");
                return null;
            }
            if (imageBytes.length > 32*1024) {
                LOG.info("Image '" + backgroundImageUrl + "' larger than 32KB (size '" + imageBytes.length + "'bytes), " +
                        "skipping.");
                return null;
            }
            return DataUriUtils.convertToDataUri(imageBytes, backgroundImageUrl);

        } catch (Exception e) {
            LOG.error("failed to include background image as data uri: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Test if the browser which reqested the file supported data uris
     */
    private boolean userAgentSupportsDataUris(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        Browser browser = Browser.parseUserAgentString(userAgent);
        return !INCOMPATIBLE_BROWSERS.contains(browser);
    }

    /**
     * Provide a global runtime mechanism of disabling this filter
     *
     * This implementation only enables the filter if request parameter 'useDataUri' is present.
     */
    private boolean isFilterEnabled(HttpServletRequest httpServletRequest) {
        return httpServletRequest.getParameter("useDataUri") != null;
    }

    /**
     * Request wrapper which captures the response from a servlet in a CharArrayWriter, suitable for text files.
     */
    public static class CssFileResponseWrapper extends HttpServletResponseWrapper {

        private final CharArrayWriter writer;

        public CssFileResponseWrapper(HttpServletResponse response) {
            super(response);
            writer = new CharArrayWriter();
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return new PrintWriter(writer);
        }

        @Override
        public String toString() {
            return writer.toString();
        }
    }

    /**
     * Request wrapper which captures the response from a servlet in a ByteArrayOutputStream, suitable for binary files.
     */
    public static class IncludedImageResponseWrapper extends HttpServletResponseWrapper {

        private final ByteArrayOutputStream stream;

        public IncludedImageResponseWrapper(HttpServletResponse response) {
            super(response);
            stream = new ByteArrayOutputStream();
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    stream.write(b);
                }
            };
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return new PrintWriter(new OutputStreamWriter(stream, "UTF-8"));
        }

        public byte[] getOutput() {
            return stream.toByteArray();
        }
    }
}
