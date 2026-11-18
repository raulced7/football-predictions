package com.raulcedroj.football_predictions.avatar;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvatarImageProcessorTest {

    private final AvatarImageProcessor processor = new AvatarImageProcessor();

    @Test
    void turnsAnyPhotoIntoASquareJpeg() throws IOException {
        byte[] result = processor.toSquareJpeg(image(1200, 800, "png"));

        BufferedImage output = ImageIO.read(new ByteArrayInputStream(result));
        assertThat(output.getWidth()).isEqualTo(AvatarImageProcessor.SIZE);
        assertThat(output.getHeight()).isEqualTo(AvatarImageProcessor.SIZE);
        // Los JPEG empiezan siempre por FF D8
        assertThat(result[0]).isEqualTo((byte) 0xFF);
        assertThat(result[1]).isEqualTo((byte) 0xD8);
    }

    @Test
    void acceptsJpegPhotos() throws IOException {
        assertThat(processor.toSquareJpeg(image(600, 900, "jpeg"))).isNotEmpty();
    }

    @Test
    void rejectsFilesThatAreNotImages() {
        assertThatThrownBy(() -> processor.toSquareJpeg("no soy una foto".getBytes()))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void rejectsUnsupportedFormats() throws IOException {
        assertThatThrownBy(() -> processor.toSquareJpeg(image(100, 100, "gif")))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("Formato no admitido");
    }

    @Test
    void rejectsHugeImagesBeforeDecodingThem() throws IOException {
        assertThatThrownBy(() -> processor.toSquareJpeg(image(5000, 10, "png")))
                .isInstanceOf(InvalidImageException.class)
                .hasMessageContaining("demasiado grande");
    }

    private static byte[] image(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, format, bytes);
        return bytes.toByteArray();
    }
}
