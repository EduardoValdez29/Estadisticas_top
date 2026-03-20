package com.proyectoestadistico.service;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.proyectoestadistico.model.TablaDatos;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ui.RectangleInsets;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public class GeneradorPDF {

    public void generarReporteConGraficas(Path destino,
                                          TablaDatos tabla,
                                          String introduccion,
                                          JFreeChart graficaBarras,
                                          JFreeChart graficaLineas,
                                          JFreeChart graficaDispersion) throws IOException {

        try (PdfWriter writer = new PdfWriter(destino.toFile());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4)) {

            document.add(new Paragraph("Reporte Estadístico").setFontSize(18));
            if (introduccion != null && !introduccion.isBlank()) {
                document.add(new Paragraph(introduccion).setFontSize(11));
                document.add(new Paragraph("\n"));
            }
            document.add(new Paragraph("Datos de origen (INEGI -> CSV)").setFontSize(11));
            document.add(new Paragraph("\n"));

            agregarTabla(document, tabla);

            if (graficaBarras != null) {
                document.add(new Paragraph("\nGráfica de barras").setFontSize(12));
                document.add(crearImagenDesdeChart(graficaBarras));
            } else {
                document.add(new Paragraph("\nNo se pudo generar la gráfica de barras."));
            }

            if (graficaLineas != null) {
                document.add(new Paragraph("\nGráfica de líneas").setFontSize(12));
                document.add(crearImagenDesdeChart(graficaLineas));
            } else {
                document.add(new Paragraph("\nNo se pudo generar la gráfica de líneas."));
            }

            if (graficaDispersion != null) {
                document.add(new Paragraph("\nGráfica de dispersión").setFontSize(12));
                document.add(crearImagenDesdeChart(graficaDispersion));
            } else {
                document.add(new Paragraph("\nNo se pudo generar la gráfica de dispersión."));
            }
        }
    }

    public void generarReporteConGraficas(Path destino,
                                          java.util.List<TablaDatos> tablas,
                                          String introduccion,
                                          JFreeChart graficaBarras,
                                          JFreeChart graficaLineas,
                                          JFreeChart graficaDispersion) throws IOException {

        try (PdfWriter writer = new PdfWriter(destino.toFile());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4)) {

            document.add(new Paragraph("Reporte Estadístico").setFontSize(18));
            if (introduccion != null && !introduccion.isBlank()) {
                document.add(new Paragraph(introduccion).setFontSize(11));
                document.add(new Paragraph("\n"));
            }
            document.add(new Paragraph("Datos de origen (INEGI -> CSV)").setFontSize(11));
            document.add(new Paragraph("\n"));

            if (tablas == null || tablas.isEmpty()) {
                document.add(new Paragraph("No hay tablas para mostrar."));
            } else {
                int i = 1;
                for (TablaDatos t : tablas) {
                    document.add(new Paragraph("Tabla " + i).setFontSize(12));
                    agregarTabla(document, t);
                    document.add(new Paragraph("\n"));
                    i++;
                }
            }

            if (graficaBarras != null) {
                document.add(new Paragraph("\nGráfica de barras").setFontSize(12));
                document.add(crearImagenDesdeChart(graficaBarras));
            } else {
                document.add(new Paragraph("\nNo se pudo generar la gráfica de barras."));
            }

            if (graficaLineas != null) {
                document.add(new Paragraph("\nGráfica de líneas").setFontSize(12));
                document.add(crearImagenDesdeChart(graficaLineas));
            } else {
                document.add(new Paragraph("\nNo se pudo generar la gráfica de líneas."));
            }

            if (graficaDispersion != null) {
                document.add(new Paragraph("\nGráfica de dispersión").setFontSize(12));
                document.add(crearImagenDesdeChart(graficaDispersion));
            } else {
                document.add(new Paragraph("\nNo se pudo generar la gráfica de dispersión."));
            }
        }
    }

    private void agregarTabla(Document document, TablaDatos tabla) {
        if (tabla.getNumeroColumnas() == 0) {
            document.add(new Paragraph("No hay datos para mostrar."));
            return;
        }

        // Tabla con una columna por cada encabezado
        Table table = new Table(tabla.getNumeroColumnas());

        // Encabezados
        for (String header : tabla.getEncabezados()) {
            table.addHeaderCell(header);
        }

        // Filas
        for (var fila : tabla.getFilas()) {
            for (String valor : fila) {
                table.addCell(valor);
            }
        }

        document.add(table);
    }

    private Image crearImagenDesdeChart(JFreeChart chart) throws IOException {
        int width = 500;
        int height = 300;
        chart.setPadding(new RectangleInsets(5, 5, 5, 5));
        BufferedImage bufferedImage = chart.createBufferedImage(width, height);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        com.itextpdf.io.image.ImageData imageData =
                com.itextpdf.io.image.ImageDataFactory.create(baos.toByteArray());
        return new Image(imageData).setAutoScale(true);
    }
}

