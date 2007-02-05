package org.labkey.flow.analysis.chart;

import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class HistRenderer extends XYBarRenderer
{
    /**
     * Draws the visual representation of a single data item.
     *
     * @param g2  the graphics device.
     * @param state  the renderer state.
     * @param dataArea  the area within which the plot is being drawn.
     * @param info  collects information about the drawing.
     * @param plot  the plot (can be used to obtain standard color
     *              information etc).
     * @param domainAxis  the domain axis.
     * @param rangeAxis  the range axis.
     * @param dataset  the dataset.
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @param crosshairState  crosshair information for the plot
     *                        (<code>null</code> permitted).
     * @param pass  the pass index.
     */
    public void drawItem(Graphics2D g2,
                         XYItemRendererState state,
                         Rectangle2D dataArea,
                         PlotRenderingInfo info,
                         XYPlot plot,
                         ValueAxis domainAxis,
                         ValueAxis rangeAxis,
                         XYDataset dataset,
                         int series,
                         int item,
                         CrosshairState crosshairState,
                         int pass) {

        if (!getItemVisible(series, item)) {
            return;
        }
        IntervalXYDataset intervalDataset = (IntervalXYDataset) dataset;

        RectangleEdge location = plot.getDomainAxisEdge();
        Number startXNumber = intervalDataset.getStartX(series, item);
        if (startXNumber == null) {
            return;
        }
        int translatedStartX = (int) Math.floor(domainAxis.valueToJava2D(
            startXNumber.doubleValue(), dataArea, location
        ));

        Number endXNumber = intervalDataset.getEndX(series, item);
        if (endXNumber == null) {
            return;
        }
        int translatedEndX = (int) Math.floor(domainAxis.valueToJava2D(
            endXNumber.doubleValue(), dataArea, location
        ));

        if (translatedStartX == translatedEndX && item != 0)
            return;
        double value0 = Double.MAX_VALUE;
        double value1 = Double.MIN_VALUE;
        for (int i = item; i >= 0; i --)
        {
            double value = intervalDataset.getYValue(series, i);
            value0 = Math.min(value, value0);
            value1 = Math.max(value, value1);
            double startX = intervalDataset.getStartXValue(series, i);
            int xlatedStartX = (int) Math.floor(domainAxis.valueToJava2D(startX, dataArea, location));
            if (xlatedStartX < translatedStartX)
                break;
        }
        int translatedValue0 = (int) rangeAxis.valueToJava2D(
            value0, dataArea, plot.getRangeAxisEdge()
        ) + 1;
        int translatedValue1 = (int) rangeAxis.valueToJava2D(
            value1, dataArea, plot.getRangeAxisEdge()
        );


        int translatedWidth = Math.max(
            1, Math.abs(translatedEndX - translatedStartX)
        );
        int translatedHeight = Math.abs(translatedValue1 - translatedValue0);

        Rectangle bar = null;
        PlotOrientation orientation = plot.getOrientation();
        if (orientation == PlotOrientation.HORIZONTAL) {
            bar = new Rectangle(
                Math.min(translatedValue0, translatedValue1),
                Math.min(translatedStartX, translatedEndX),
                translatedHeight, translatedWidth);
        }
        else if (orientation == PlotOrientation.VERTICAL) {
            bar = new Rectangle(
                Math.min(translatedStartX, translatedEndX),
                Math.min(translatedValue0, translatedValue1),
                translatedWidth, translatedHeight);
        }

        Paint itemPaint = getItemPaint(series, item);
        if (getGradientPaintTransformer()
                != null && itemPaint instanceof GradientPaint) {
            GradientPaint gp = (GradientPaint) itemPaint;
            itemPaint = getGradientPaintTransformer().transform(gp, bar);
        }
        g2.setPaint(itemPaint);
        g2.fill(bar);
        if (isDrawBarOutline()
                && Math.abs(translatedEndX - translatedStartX) > 3) {
            Stroke stroke = getItemOutlineStroke(series, item);
            Paint paint = getItemOutlinePaint(series, item);
            if (stroke != null && paint != null) {
                g2.setStroke(stroke);
                g2.setPaint(paint);
                g2.draw(bar);
            }
        }

        // TODO: we need something better for the item labels
        if (isItemLabelVisible(series, item)) {
            drawItemLabel(
                g2, orientation, dataset, series, item, bar.getCenterX(),
                bar.getY(), value1 < 0.0
            );
        }

        // add an entity for the item...
        if (info != null) {
            EntityCollection entities = info.getOwner().getEntityCollection();
            if (entities != null) {
                String tip = null;
                XYToolTipGenerator generator
                    = getToolTipGenerator(series, item);
                if (generator != null) {
                    tip = generator.generateToolTip(dataset, series, item);
                }
                String url = null;
                if (getURLGenerator() != null) {
                    url = getURLGenerator().generateURL(dataset, series, item);
                }
                XYItemEntity entity = new XYItemEntity(
                    bar, dataset, series, item, tip, url
                );
                entities.add(entity);
            }
        }

    }

}
