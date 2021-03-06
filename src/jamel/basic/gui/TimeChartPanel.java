package jamel.basic.gui;

import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Line2D;

import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * A ChartPanel that contain a time chart.
 */
@SuppressWarnings("serial")
public class TimeChartPanel extends JamelChartPanel {

	/** line */
	private final static Shape line = new Line2D.Double(0, 0, 15, 0);

	/**
	 * Creates a panel with its chart.
	 * @param title the title of the chart.
	 * @param data the dataset.
	 * @param colors the colors.
	 * @param legend the legend.
	 */
	public TimeChartPanel(String title, XYSeriesCollection data, Paint[] colors,String[] legend) {
		super(title, data, colors, legend);
	}

	@Override
	protected LegendItem getNewLegendItem(String label, Paint color) {
		return new LegendItem(label, null, null, null, line, basicStroke, color);
	}
	
	@Override
	protected XYItemRenderer getNewRenderer(final XYDataset dataset, final Paint[] colors) {
		return new XYLineAndShapeRenderer(true, false){{
			final DefaultDrawingSupplier drawingSupplier = new DefaultDrawingSupplier();
			for (int i = 0; i < dataset.getSeriesCount(); i++) {
				this.setSeriesPaint(i, drawingSupplier.getNextPaint());
			}				
			if (colors!=null) {
				for (int i = 0; i < colors.length; i++) {
					this.setSeriesPaint(i, colors[i]);
				}
			}
		}};
	}

	@Override
	protected NumberAxis getNewXAxis(String label) {
		return getTimeAxis(label);
	}

	/**
	 * Adds a marker to the chart.
	 * @param marker the marker to be added.
	 */
	public void addMarker(ValueMarker marker) {
		this.getChart().getXYPlot().addDomainMarker(marker);
	}

}

// ***
