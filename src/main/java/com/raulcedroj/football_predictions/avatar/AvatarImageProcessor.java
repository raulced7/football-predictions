package com.raulcedroj.football_predictions.avatar;

import org.springframework.stereotype.Component;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Convierte cualquier foto aceptada en un JPEG cuadrado de {@value #SIZE} px generado desde cero.
 * Así nunca se guarda el archivo tal y como lo sube el usuario: se pierden los metadatos (incluida
 * la ubicación GPS de las fotos del móvil) y cualquier contenido que no sea la propia imagen.
 */
@Component
public class AvatarImageProcessor {

    static final int SIZE = 256;
    private static final int MAX_SIDE = 4096;
    private static final float JPEG_QUALITY = 0.85f;
    private static final Set<String> ACCEPTED_FORMATS = Set.of("jpeg", "png");

    public byte[] toSquareJpeg(byte[] input) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
            Iterator<ImageReader> readers = in == null ? null : ImageIO.getImageReaders(in);
            if (readers == null || !readers.hasNext()) {
                throw new InvalidImageException("El archivo no es una imagen válida. Usa una foto JPG o PNG.");
            }
            ImageReader reader = readers.next();
            try {
                if (!ACCEPTED_FORMATS.contains(reader.getFormatName().toLowerCase(Locale.ROOT))) {
                    throw new InvalidImageException("Formato no admitido. Usa una foto JPG o PNG.");
                }
                reader.setInput(in, true, true);
                // Se comprueba el tamaño antes de cargar la imagen: una imagen de 50.000 × 50.000 px
                // ocupa poco comprimida, pero llenaría la memoria al descomprimirla
                if (reader.getWidth(0) > MAX_SIDE || reader.getHeight(0) > MAX_SIDE) {
                    throw new InvalidImageException("La imagen es demasiado grande (máximo 4096 px por lado).");
                }
                return encodeJpeg(squareThumbnail(reader.read(0)));
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new InvalidImageException("No se ha podido leer la imagen. Prueba con otra foto JPG o PNG.");
        }
    }

    /** Recorta el centro en cuadrado y lo reduce a SIZE × SIZE, con fondo blanco si había transparencia. */
    private static BufferedImage squareThumbnail(BufferedImage source) {
        int side = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - side) / 2;
        int y = (source.getHeight() - side) / 2;

        BufferedImage target = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, SIZE, SIZE);
            g.drawImage(source, 0, 0, SIZE, SIZE, x, y, x + side, y + side, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private static byte[] encodeJpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(out);
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }
}
