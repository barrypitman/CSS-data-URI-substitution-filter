package bazageous;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author barry
 * @since 2011/06/13 2:08 PM
 */
public class DataUriUtilsTest {

    @Test
    public void testParseLargeFileContents() throws Exception {
        String spritesCss = FileUtils.readFileToString(new File("src/main/webapp/css/background-images.css"));
        List<DataUriUtils.BackgroundImageReference> images = DataUriUtils.findBackgroundImages(spritesCss);
        assertThat(images.size(), greaterThan(20));
    }

    @Test
    @Ignore
    public void testParseLargeFileContentsWithoutMatches() throws Exception {
        String spritesCss = FileUtils.readFileToString(new File("src/main/webapp/css/inputs.css"));
        List<DataUriUtils.BackgroundImageReference> images = DataUriUtils.findBackgroundImages(spritesCss);
        assertThat(images.size(), equalTo(0));
    }

    @Test
    public void testFindBackgroundImages() throws Exception {
        String spritesCss = "background-image: url('../images/buttons/add-12x12.png')";
        assertEquals(1, DataUriUtils.findBackgroundImages(spritesCss).size());

        spritesCss = "background: url('/absolute/images/buttons/add-12x12.png') no-repeat";
        assertEquals(1, DataUriUtils.findBackgroundImages(spritesCss).size());

        spritesCss = "background-image:url('../images/buttons/add-12x12.png')";
        assertEquals(1, DataUriUtils.findBackgroundImages(spritesCss).size());

        spritesCss = "background-image: url(\"../images/buttons/add-12x12.png\")";
        assertEquals(1, DataUriUtils.findBackgroundImages(spritesCss).size());

        spritesCss = "background:  url('../images/buttons/add-12x12.png')";
        assertEquals(1, DataUriUtils.findBackgroundImages(spritesCss).size());

        spritesCss = "BACKGROUND: url('../images/buttons/add-12x12.png')";
        assertEquals(1, DataUriUtils.findBackgroundImages(spritesCss).size());

        spritesCss = "background: url(../images/buttons/add-12x12.png)";
        assertEquals(1, DataUriUtils.findBackgroundImages(spritesCss).size());

        spritesCss = "background: url(../images/buttons/add-12x12.png)} .newRule{background:  url(../images/buttons/add-12x12.png)}";
        assertEquals(2, DataUriUtils.findBackgroundImages(spritesCss).size());

        spritesCss = "background: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAIAAAD8GO2jAAAACXBIWXMAAC4jAAA)";
        assertEquals(0, DataUriUtils.findBackgroundImages(spritesCss).size());

        spritesCss = "background: url('http://www.google.co.za/images/nav_logo72.png') no-repeat;";
        assertEquals(0, DataUriUtils.findBackgroundImages(spritesCss).size());
    }

}

