package com.proyectoestadistico.service;

import com.proyectoestadistico.i18n.UiMessages;
import com.proyectoestadistico.model.TablaDatos;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.CategoryAxis;

public class GeneradorGraficas {

    private static final java.awt.Color BG0 = new java.awt.Color(0x0B1020);
    private static final java.awt.Color BG1 = new java.awt.Color(0x0F1630);
    private static final java.awt.Color GRID = new java.awt.Color(0x23345A);
    private static final java.awt.Color TEXT = new java.awt.Color(0xE6F1FF);

    private static final java.awt.Color S0 = new java.awt.Color(0x00E5FF);
    private static final java.awt.Color S1 = new java.awt.Color(0xFF2D95);
    private static final java.awt.Color S2 = new java.awt.Color(0x39FF88);
    private static final java.awt.Color LEGEND_BG = new java.awt.Color(0x141F3D);

    private void aplicarTema(CategoryPlot plot) {
        plot.setBackgroundPaint(BG1);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(GRID);
        plot.setDomainGridlinePaint(GRID);
        plot.setAxisOffset(new RectangleInsets(6, 6, 6, 6));

        if (plot.getDomainAxis() != null) {
            plot.getDomainAxis().setLabelPaint(TEXT);
            plot.getDomainAxis().setTickLabelPaint(TEXT);
            if (plot.getDomainAxis() instanceof CategoryAxis) {
                CategoryAxis axis = (CategoryAxis) plot.getDomainAxis();
                axis.setMaximumCategoryLabelLines(2);
                axis.setMaximumCategoryLabelWidthRatio(0.8f);
            }
        }
        if (plot.getRangeAxis() != null) {
            plot.getRangeAxis().setLabelPaint(TEXT);
            plot.getRangeAxis().setTickLabelPaint(TEXT);
            if (plot.getRangeAxis() instanceof NumberAxis) {
                NumberAxis axis = (NumberAxis) plot.getRangeAxis();
                axis.setAutoRangeIncludesZero(true);
                axis.setNumberFormatOverride(new java.text.DecimalFormat("#,##0.###"));
            }
        }
    }

    private void aplicarTema(XYPlot plot) {
        plot.setBackgroundPaint(BG1);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(GRID);
        plot.setDomainGridlinePaint(GRID);
        plot.setAxisOffset(new RectangleInsets(6, 6, 6, 6));

        if (plot.getDomainAxis() != null) {
            plot.getDomainAxis().setLabelPaint(TEXT);
            plot.getDomainAxis().setTickLabelPaint(TEXT);
            if (plot.getDomainAxis() instanceof NumberAxis) {
                NumberAxis axis = (NumberAxis) plot.getDomainAxis();
                axis.setNumberFormatOverride(new java.text.DecimalFormat("#,##0.###"));
            }
        }
        if (plot.getRangeAxis() != null) {
            plot.getRangeAxis().setLabelPaint(TEXT);
            plot.getRangeAxis().setTickLabelPaint(TEXT);
            if (plot.getRangeAxis() instanceof NumberAxis) {
                NumberAxis axis = (NumberAxis) plot.getRangeAxis();
                axis.setNumberFormatOverride(new java.text.DecimalFormat("#,##0.###"));
            }
        }
    }

    private void aplicarTemaGeneral(JFreeChart chart) {
        chart.setBackgroundPaint(BG0);
        if (chart.getTitle() != null) chart.getTitle().setPaint(TEXT);
        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(LEGEND_BG);
            chart.getLegend().setItemPaint(TEXT);
            chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(GRID));
        }
    }

    private DefaultCategoryDataset crearDatasetCategoria(TablaDatos tabla) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        if (tabla.getNumeroColumnas() < 2) {
            return dataset;
        }
        String serie = tabla.getEncabezados().get(1);
        for (var fila : tabla.getFilas()) {
            String categoria = fila.get(0);
            String valorStr = fila.get(1);
            try {
                double valor = Double.parseDouble(valorStr);
                dataset.addValue(valor, serie, categoria);
            } catch (NumberFormatException ignored) {
            }
        }
        return dataset;
    }

    private DefaultCategoryDataset crearDatasetCategoria(java.util.List<TablaDatos> tablas) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        if (tablas == null || tablas.isEmpty()) {
            return dataset;
        }

        for (TablaDatos tabla : tablas) {
            if (tabla == null || tabla.getNumeroColumnas() < 2) continue;
            String serie = tabla.getEncabezados().get(1);
            for (var fila : tabla.getFilas()) {
                if (fila.size() < 2) continue;
                String categoria = fila.get(0);
                String valorStr = fila.get(1);
                try {
                    double valor = Double.parseDouble(valorStr);
                    dataset.addValue(valor, serie, formatearCategoria(categoria));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return dataset;
    }

    private String formatearCategoria(String raw) {
        if (raw == null) return "";
        try {
            double d = Double.parseDouble(raw.trim());
            return new java.text.DecimalFormat("#,##0.###").format(d);
        } catch (NumberFormatException ex) {
            return raw;
        }
    }

    private XYSeriesCollection crearDatasetXY(TablaDatos tabla) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        if (tabla.getNumeroColumnas() < 2) {
            return dataset;
        }
        XYSeries serie = new XYSeries(UiMessages.get("chart.series.default"));
        for (var fila : tabla.getFilas()) {
            try {
                double x = Double.parseDouble(fila.get(0));
                double y = Double.parseDouble(fila.get(1));
                serie.add(x, y);
            } catch (NumberFormatException ignored) {
            }
        }
        dataset.addSeries(serie);
        return dataset;
    }

    private XYSeriesCollection crearDatasetXY(java.util.List<TablaDatos> tablas) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        if (tablas == null || tablas.isEmpty()) {
            return dataset;
        }

        for (TablaDatos tabla : tablas) {
            if (tabla == null || tabla.getNumeroColumnas() < 2) continue;
            String nombreSerie = tabla.getEncabezados().get(1);
            XYSeries serie = new XYSeries(nombreSerie);
            int idx = 0;
            for (var fila : tabla.getFilas()) {
                if (fila.size() < 2) continue;
                idx++;
                double x;
                double y;
                try {
                    x = Double.parseDouble(fila.get(0));
                } catch (NumberFormatException ex) {
                    x = idx;
                }
                try {
                    y = Double.parseDouble(fila.get(1));
                } catch (NumberFormatException ex) {
                    continue;
                }
                serie.add(x, y);
            }
            dataset.addSeries(serie);
        }

        return dataset;
    }

    public JFreeChart crearGraficaBarras(TablaDatos tabla) {
        var dataset = crearDatasetCategoria(tabla);
        JFreeChart chart = ChartFactory.createBarChart(
                UiMessages.get("chart.main.bar"),
                tabla.getNumeroColumnas() > 0 ? tabla.getEncabezados().get(0) : UiMessages.get("chart.axis.category"),
                tabla.getNumeroColumnas() > 1 ? tabla.getEncabezados().get(1) : UiMessages.get("chart.axis.value"),
                dataset
        );
        aplicarTemaGeneral(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        aplicarTema(plot);
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, S0);
        renderer.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
        renderer.setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator());
        return chart;
    }

    public JFreeChart crearGraficaBarras(java.util.List<TablaDatos> tablas) {
        var dataset = crearDatasetCategoria(tablas);
        String xLabel = (tablas != null && !tablas.isEmpty() && tablas.get(0) != null && tablas.get(0).getNumeroColumnas() > 0)
                ? tablas.get(0).getEncabezados().get(0)
                : UiMessages.get("chart.axis.category");
        JFreeChart chart = ChartFactory.createBarChart(
                UiMessages.get("chart.main.bar3"),
                xLabel,
                UiMessages.get("chart.axis.value"),
                dataset
        );
        aplicarTemaGeneral(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        aplicarTema(plot);
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, S0);
        renderer.setSeriesPaint(1, S1);
        renderer.setSeriesPaint(2, S2);
        renderer.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
        renderer.setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator());
        return chart;
    }

    public JFreeChart crearGraficaLineas(TablaDatos tabla) {
        var dataset = crearDatasetCategoria(tabla);
        JFreeChart chart = ChartFactory.createLineChart(
                UiMessages.get("chart.main.line"),
                tabla.getNumeroColumnas() > 0 ? tabla.getEncabezados().get(0) : UiMessages.get("chart.axis.category"),
                tabla.getNumeroColumnas() > 1 ? tabla.getEncabezados().get(1) : UiMessages.get("chart.axis.value"),
                dataset
        );
        aplicarTemaGeneral(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        aplicarTema(plot);
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, S1);
        renderer.setDefaultShapesVisible(true);
        renderer.setDefaultShapesFilled(true);
        renderer.setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator());
        return chart;
    }

    public JFreeChart crearGraficaLineas(java.util.List<TablaDatos> tablas) {
        var dataset = crearDatasetCategoria(tablas);
        String xLabel = (tablas != null && !tablas.isEmpty() && tablas.get(0) != null && tablas.get(0).getNumeroColumnas() > 0)
                ? tablas.get(0).getEncabezados().get(0)
                : UiMessages.get("chart.axis.category");
        JFreeChart chart = ChartFactory.createLineChart(
                UiMessages.get("chart.main.line3"),
                xLabel,
                UiMessages.get("chart.axis.value"),
                dataset
        );
        aplicarTemaGeneral(chart);
        CategoryPlot plot = chart.getCategoryPlot();
        aplicarTema(plot);
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, S0);
        renderer.setSeriesPaint(1, S1);
        renderer.setSeriesPaint(2, S2);
        renderer.setDefaultShapesVisible(true);
        renderer.setDefaultShapesFilled(true);
        renderer.setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator());
        return chart;
    }

    public JFreeChart crearGraficaDispersión(TablaDatos tabla) {
        var dataset = crearDatasetXY(tabla);
        JFreeChart chart = ChartFactory.createScatterPlot(
                UiMessages.get("chart.main.scatter"),
                tabla.getNumeroColumnas() > 0 ? tabla.getEncabezados().get(0) : UiMessages.get("chart.axis.x"),
                tabla.getNumeroColumnas() > 1 ? tabla.getEncabezados().get(1) : UiMessages.get("chart.axis.y"),
                dataset
        );
        aplicarTemaGeneral(chart);
        XYPlot plot = chart.getXYPlot();
        aplicarTema(plot);
        XYItemRenderer renderer = plot.getRenderer();
        renderer.setSeriesPaint(0, S2);
        renderer.setDefaultToolTipGenerator(new StandardXYToolTipGenerator());
        return chart;
    }

    public JFreeChart crearGraficaDispersión(java.util.List<TablaDatos> tablas) {
        var dataset = crearDatasetXY(tablas);
        String xLabel = (tablas != null && !tablas.isEmpty() && tablas.get(0) != null && tablas.get(0).getNumeroColumnas() > 0)
                ? tablas.get(0).getEncabezados().get(0)
                : UiMessages.get("chart.axis.x");
        JFreeChart chart = ChartFactory.createScatterPlot(
                UiMessages.get("chart.main.scatter3"),
                xLabel,
                UiMessages.get("chart.axis.y"),
                dataset
        );
        aplicarTemaGeneral(chart);
        XYPlot plot = chart.getXYPlot();
        aplicarTema(plot);
        XYItemRenderer renderer = plot.getRenderer();
        renderer.setSeriesPaint(0, S0);
        renderer.setSeriesPaint(1, S1);
        renderer.setSeriesPaint(2, S2);
        renderer.setDefaultToolTipGenerator(new StandardXYToolTipGenerator());
        return chart;
    }
}

