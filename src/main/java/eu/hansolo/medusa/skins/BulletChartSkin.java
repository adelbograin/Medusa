/*
 * Copyright (c) 2015 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.medusa.skins;

import eu.hansolo.medusa.Fonts;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Marker;
import eu.hansolo.medusa.Section;
import eu.hansolo.medusa.events.UpdateEvent;
import eu.hansolo.medusa.events.UpdateEventListener;
import eu.hansolo.medusa.tools.Helper;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;


/**
 * Created by hansolo on 25.12.15.
 */
public class BulletChartSkin extends SkinBase<Gauge> implements Skin<Gauge> {
    private static final double          MINIMUM_WIDTH   = 50;
    private static final double          MINIMUM_HEIGHT  = 50;
    private static final double          MAXIMUM_WIDTH   = 1024;
    private static final double          MAXIMUM_HEIGHT  = 1024;
    private              double          preferredWidth  = 400;
    private              double          preferredHeight = 64;
    private              Pane            pane;
    private              double          width;
    private              double          height;
    private              double          aspectRatio;
    private              Orientation     orientation;
    private              Canvas          tickMarkCanvas;
    private              GraphicsContext tickMarksCtx;
    private              Canvas          sectionsCanvas;
    private              GraphicsContext sectionsCtx;
    private              Text            titleText;
    private              Text            unitText;
    private              Rectangle       barRect;
    private              Rectangle       thresholdRect;
    private              double          stepSize;
    private              Tooltip         barTooltip;
    private              Tooltip         thresholdTooltip;
    private              String          formatString;



    // ******************** Constructors **************************************
    public BulletChartSkin(Gauge gauge) {
        super(gauge);
        if (gauge.isAutoScale()) gauge.calcAutoScale();
        orientation      = gauge.getOrientation();
        barTooltip       = new Tooltip();
        thresholdTooltip = new Tooltip();
        formatString     = new StringBuilder("%.").append(Integer.toString(gauge.getDecimals())).append("f").toString();

        if (Orientation.VERTICAL == orientation) {
            preferredWidth  = 64;
            preferredHeight = 400;
        } else {
            preferredWidth  = 400;
            preferredHeight = 64;
        }
        gauge.setPrefSize(preferredWidth, preferredHeight);

        init();
        initGraphics();
        registerListeners();
    }


    // ******************** Initialization ************************************
    private void init() {
        if (Double.compare(getSkinnable().getPrefWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getPrefHeight(), 0.0) <= 0 ||
            Double.compare(getSkinnable().getWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getHeight(), 0.0) <= 0) {
            if (getSkinnable().getPrefWidth() > 0 && getSkinnable().getPrefHeight() > 0) {
                getSkinnable().setPrefSize(getSkinnable().getPrefWidth(), getSkinnable().getPrefHeight());
            } else {
                getSkinnable().setPrefSize(preferredWidth, preferredHeight);
            }
        }

        if (Double.compare(getSkinnable().getMinWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getMinHeight(), 0.0) <= 0) {
            getSkinnable().setMinSize(MINIMUM_WIDTH, MINIMUM_HEIGHT);
        }

        if (Double.compare(getSkinnable().getMaxWidth(), 0.0) <= 0 || Double.compare(getSkinnable().getMaxHeight(), 0.0) <= 0) {
            getSkinnable().setMaxSize(MAXIMUM_WIDTH, MAXIMUM_HEIGHT);
        }
    }

    private void initGraphics() {
        Orientation orientation = getSkinnable().getOrientation();

        aspectRatio = preferredHeight / preferredWidth;

        titleText = new Text(getSkinnable().getTitle());
        titleText.setTextOrigin(VPos.CENTER);

        unitText = new Text(getSkinnable().getUnit());
        unitText.setTextOrigin(VPos.CENTER);

        tickMarkCanvas    = new Canvas(0.79699248 * preferredWidth, 0.08333333 * preferredHeight);

        tickMarksCtx      = tickMarkCanvas.getGraphicsContext2D();

        sectionsCanvas    = new Canvas(0.79699248 * preferredWidth, 0.5 * preferredHeight);

        sectionsCtx       = sectionsCanvas.getGraphicsContext2D();

        barRect           = new Rectangle();
        Tooltip.install(barRect, barTooltip);

        thresholdRect     = new Rectangle();
        Tooltip.install(thresholdRect, thresholdTooltip);

        pane = new Pane();
        pane.getChildren().setAll(titleText,
                                  unitText,
                                  tickMarkCanvas,
                                  sectionsCanvas,
                                  barRect,
                                  thresholdRect);
        getChildren().setAll(pane);
    }

    private void registerListeners() {
        getSkinnable().widthProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable o) {BulletChartSkin.this.handleEvents("RESIZE");}
        });
        getSkinnable().heightProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable o) {BulletChartSkin.this.handleEvents("RESIZE");}
        });
        getSkinnable().getSections().addListener((ListChangeListener<Section>) new ListChangeListener<Section>() {
            @Override public void onChanged(Change<? extends Section> c) {BulletChartSkin.this.redraw();}
        });
        getSkinnable().getTickMarkSections().addListener((ListChangeListener<Section>) new ListChangeListener<Section>() {
            @Override public void onChanged(Change<? extends Section> c) {BulletChartSkin.this.redraw();}
        });
        getSkinnable().getTickLabelSections().addListener((ListChangeListener<Section>) new ListChangeListener<Section>() {
            @Override public void onChanged(Change<? extends Section> c) {BulletChartSkin.this.redraw();}
        });
        getSkinnable().getMarkers().addListener((ListChangeListener<Marker>) new ListChangeListener<Marker>() {
            @Override public void onChanged(Change<? extends Marker> c) {BulletChartSkin.this.redraw();}
        });
        getSkinnable().setOnUpdate(new UpdateEventListener() {
            @Override public void onUpdateEvent(UpdateEvent e) {BulletChartSkin.this.handleEvents(e.eventType.name());}
        });

        getSkinnable().currentValueProperty().addListener(new InvalidationListener() {
            @Override public void invalidated(Observable o) {BulletChartSkin.this.updateBar();}
        });
    }


    // ******************** Methods *******************************************
    protected void handleEvents(final String EVENT_TYPE) {
        if ("RESIZE".equals(EVENT_TYPE)) {
            resize();
        }else if ("REDRAW".equals(EVENT_TYPE)) {
            redraw();
        } else if ("VISIBILITY".equals(EVENT_TYPE)) {
            if (getSkinnable().getTitle().isEmpty()) {
                titleText.setVisible(false);
                titleText.setManaged(false);
            } else {
                titleText.setManaged(true);
                titleText.setVisible(true);
            }
            if (getSkinnable().getUnit().isEmpty()) {
                unitText.setVisible(false);
                unitText.setManaged(false);
            } else {
                unitText.setManaged(true);
                unitText.setVisible(true);
            }
            redraw();
        } else if ("RESIZE".equals(EVENT_TYPE)) {
            resize();
        } else if ("RECALC".equals(EVENT_TYPE)) {
            if (getSkinnable().isAutoScale()) getSkinnable().calcAutoScale();
            if (Orientation.VERTICAL == orientation) {
                width    = height / aspectRatio;
                stepSize = (0.79699248 * height) / getSkinnable().getRange();
            } else {
                height   = width / aspectRatio;
                stepSize = (0.79699248 * width) / getSkinnable().getRange();
            }
            resize();
            redraw();
        } else if ("FINISHED".equals(EVENT_TYPE)) {
            barTooltip.setText(String.format(Locale.US, formatString, getSkinnable().getValue()));
        }
    }

    private void drawTickMarks(final GraphicsContext CTX) {
        tickMarkCanvas.setCache(false);
        CTX.clearRect(0, 0, tickMarkCanvas.getWidth(), tickMarkCanvas.getHeight());
        CTX.setFill(getSkinnable().getMajorTickMarkColor());

        List<Section> tickMarkSections         = getSkinnable().getTickMarkSections();
        List<Section> tickLabelSections        = getSkinnable().getTickLabelSections();
        Color         majorTickMarkColor       = getSkinnable().getTickMarkColor();
        Color         tickLabelColor           = getSkinnable().getTickLabelColor();
        boolean       smallRange               = Double.compare(getSkinnable().getRange(), 10d) <= 0;
        double        minValue                 = getSkinnable().getMinValue();
        double        maxValue                 = getSkinnable().getMaxValue();
        double        tmpStepSize              = smallRange ? stepSize / 10 : stepSize;
        Font          tickLabelFont            = Fonts.robotoRegular(0.15 * (Orientation.VERTICAL == orientation ? width : height));
        boolean       tickMarkSectionsVisible  = getSkinnable().getTickMarkSectionsVisible();
        boolean       tickLabelSectionsVisible = getSkinnable().getTickLabelSectionsVisible();
        double        offsetX                  = 0.18345865 * width;
        double        offsetY                  = 0.1 * height;
        double        innerPointX              = 0;
        double        innerPointY              = 0;
        double        outerPointX              = 0.07 * width;
        double        outerPointY              = 0.08 * height;
        double        textPointX               = 0.55 * tickMarkCanvas.getWidth();
        double        textPointY               = 0.7 * tickMarkCanvas.getHeight();
        BigDecimal    minorTickSpaceBD         = BigDecimal.valueOf(getSkinnable().getMinorTickSpace());
        BigDecimal    majorTickSpaceBD         = BigDecimal.valueOf(getSkinnable().getMajorTickSpace());
        BigDecimal    counterBD                = BigDecimal.valueOf(getSkinnable().getMinValue());
        double        counter                  = minValue;
        double        range                    = getSkinnable().getRange();

        for (double i = 0 ; Double.compare(i, range) <= 0 ; i++) {
            if (Orientation.VERTICAL == orientation) {
                innerPointY = counter * tmpStepSize + offsetY;
                outerPointY = innerPointY;
                textPointY  = innerPointY;
            } else {
                innerPointX = counter * tmpStepSize + offsetX;
                outerPointX = innerPointX;
                textPointX  = innerPointX;
            }

            // Set the general tickmark color
            CTX.setStroke(getSkinnable().getTickMarkColor());
            if (Double.compare(counterBD.remainder(majorTickSpaceBD).doubleValue(), 0d) == 0) {
                // Draw major tick mark
                if (getSkinnable().getMajorTickMarksVisible()) {
                    CTX.setFill(tickMarkSectionsVisible ? Helper.getColorOfSection(tickMarkSections, counter, majorTickMarkColor) : majorTickMarkColor);
                    CTX.setStroke(tickMarkSectionsVisible ? Helper.getColorOfSection(tickMarkSections, counter, majorTickMarkColor) : majorTickMarkColor);
                    CTX.setLineWidth(1);
                    CTX.strokeLine(innerPointX, innerPointY, outerPointX, outerPointY);
                }
                // Draw tick label text
                if (getSkinnable().getTickLabelsVisible()) {
                    CTX.save();
                    CTX.translate(textPointX, textPointY);
                    CTX.setFont(tickLabelFont);
                    CTX.setTextAlign(TextAlignment.CENTER);
                    CTX.setTextBaseline(VPos.CENTER);
                    CTX.setFill(tickLabelSectionsVisible ? Helper.getColorOfSection(tickLabelSections, counter, tickLabelColor) : tickLabelColor);
                    if (Orientation.VERTICAL == orientation) {
                        CTX.fillText(Integer.toString((int) (maxValue - counter)), 0, 0);
                    } else {
                        CTX.fillText(Integer.toString((int) counter), 0, 0);
                    }
                    CTX.restore();
                }
            }
            counterBD = counterBD.add(minorTickSpaceBD);
            counter   = counterBD.doubleValue();
            if (counter > maxValue) break;
        }

        tickMarkCanvas.setCache(true);
        tickMarkCanvas.setCacheHint(CacheHint.QUALITY);
    }

    private void drawSections(final GraphicsContext CTX) {
        sectionsCanvas.setCache(false);
        CTX.clearRect(0, 0, sectionsCanvas.getWidth(), sectionsCanvas.getHeight());
        CTX.setFill(getSkinnable().getBackgroundPaint());
        if (Orientation.VERTICAL == orientation) {
            CTX.fillRect(0, 0, 0.5 * width, 0.89 * height);
        } else {
            CTX.fillRect(0, 0, 0.79699248 * width, 0.5 * height);
        }

        double minValue = getSkinnable().getMinValue();
        double maxValue = getSkinnable().getMaxValue();

        int listSize = getSkinnable().getSections().size();
        for (int i = 0 ; i < listSize ; i++) {
            final Section SECTION = getSkinnable().getSections().get(i);
            final double SECTION_START;
            if (Double.compare(SECTION.getStart(), maxValue) <= 0 && Double.compare(SECTION.getStop(), minValue) >= 0) {
                if (Double.compare(SECTION.getStart(), minValue) < 0 && Double.compare(SECTION.getStop(), maxValue) < 0) {
                    SECTION_START = minValue * stepSize;
                } else {
                    SECTION_START = (SECTION.getStart() - minValue) * stepSize;
                }
                final double SECTION_SIZE;
                if (Double.compare(SECTION.getStop(), maxValue) > 0) {
                    SECTION_SIZE = (maxValue - SECTION.getStart()) * stepSize;
                } else {
                    SECTION_SIZE = (SECTION.getStop() - SECTION.getStart()) * stepSize;
                }
                CTX.save();
                CTX.setFill(SECTION.getColor());
                if (Orientation.VERTICAL == orientation) {
                    CTX.fillRect(0.0, 0.89 * height - SECTION_START - SECTION_SIZE, 0.5 * width, SECTION_SIZE);
                } else {
                    CTX.fillRect(SECTION_START, 0.0, SECTION_SIZE, 0.5 * height);
                }
                CTX.restore();
            }
        }
        sectionsCanvas.setCache(true);
        sectionsCanvas.setCacheHint(CacheHint.QUALITY);
    }

    private void updateBar() {
        double currentValue = getSkinnable().getCurrentValue();
        if (Orientation.VERTICAL == orientation) {
            barRect.setY(height - 0.06 * width - currentValue * stepSize);
            barRect.setHeight(currentValue * stepSize);
            thresholdRect.setY(height - getSkinnable().getThreshold() * stepSize - 0.08625 * width);
        } else {
            barRect.setWidth(currentValue * stepSize);
            thresholdRect.setX(getSkinnable().getThreshold() * stepSize - 0.03125 * height + 0.1835 * width);
        }
    }

    private void adjustTextSize(final Text TEXT, final double MAX_WIDTH, final double DECREMENT_FACTOR) {
        double decrement = 0d;
        double size      = width < height ? width : height;
        while (TEXT.getLayoutBounds().getWidth() > MAX_WIDTH && TEXT.getFont().getSize() > 0) {
            TEXT.setFont(Fonts.robotoMedium(size * (DECREMENT_FACTOR - decrement)));
            decrement += 0.01;
        }
    }

    private void resize() {
        width  = getSkinnable().getWidth() - getSkinnable().getInsets().getLeft() - getSkinnable().getInsets().getRight();
        height = getSkinnable().getHeight() - getSkinnable().getInsets().getTop() - getSkinnable().getInsets().getBottom();

        double currentValue = getSkinnable().getCurrentValue();
        formatString        = new StringBuilder("%.").append(Integer.toString(getSkinnable().getDecimals())).append("f").toString();

        orientation = getSkinnable().getOrientation();
        if (Orientation.VERTICAL == orientation) {
            width = height / aspectRatio;
            stepSize = (0.89 * height) / getSkinnable().getRange();

            pane.setMaxSize(width, height);
            pane.relocate((getSkinnable().getWidth() - width) * 0.5, (getSkinnable().getHeight() - height) * 0.5);

            tickMarkCanvas.setWidth(0.39 * width);
            tickMarkCanvas.setHeight(height);
            tickMarkCanvas.relocate(0.578125 * width, 0);

            sectionsCanvas.setWidth(0.5 * width);
            sectionsCanvas.setHeight(0.89 * height);
            sectionsCanvas.relocate(0.078125 * width, 0.1 * height);

            barRect.setWidth(0.16666667 * width);
            barRect.setHeight(currentValue * stepSize);
            barRect.setX(0.078125 * width + (0.5 * width - barRect.getWidth()) * 0.5);
            barRect.setY(height - currentValue * stepSize);

            thresholdRect.setX(0.16145833 * width);
            thresholdRect.setY(height - getSkinnable().getThreshold() * stepSize - 0.03125 * width);
            thresholdRect.setWidth(0.33333333 * width);
            thresholdRect.setHeight(0.0625 * width);

            double maxTextWidth = width;
            titleText.setFont(Fonts.robotoRegular(0.24 * width));
            if (titleText.getLayoutBounds().getWidth() > maxTextWidth) { adjustTextSize(titleText, maxTextWidth, 0.3); }
            titleText.relocate((width - titleText.getLayoutBounds().getWidth()) * 0.5, 0.03 * width);

            unitText.setFont(Fonts.robotoRegular(0.15 * width));
            if (unitText.getLayoutBounds().getWidth() > maxTextWidth) { adjustTextSize(unitText, maxTextWidth, 0.2); }
            unitText.relocate((width - unitText.getLayoutBounds().getWidth()) * 0.5, 0.35 * width);
        } else {
            height   = width * aspectRatio;
            stepSize = (0.79699248 * width) / getSkinnable().getRange();

            pane.setMaxSize(width, height);
            pane.relocate((getSkinnable().getWidth() - width) * 0.5, (getSkinnable().getHeight() - height) * 0.5);

            tickMarkCanvas.setWidth(width);
            tickMarkCanvas.setHeight(0.29166667 * height);
            tickMarkCanvas.relocate(0, 0.60416667 * height);

            sectionsCanvas.setWidth(0.79699248 * width);
            sectionsCanvas.setHeight(0.5 * height);
            sectionsCanvas.relocate(0.18345865 * width, 0.10416667 * height);

            barRect.setWidth(currentValue * stepSize);
            barRect.setHeight(0.16666667 * height);
            barRect.setX(0.18345865 * width);
            barRect.setY(0.10416667 * height + (0.5 * height - barRect.getHeight()) * 0.5);

            thresholdRect.setX(getSkinnable().getThreshold() * stepSize - 0.03125 * height + 0.20300752 * width);
            thresholdRect.setY(0.1875 * height);
            thresholdRect.setWidth(0.0625 * height);
            thresholdRect.setHeight(0.33333333 * height);

            double maxTextWidth = 0.20300752 * width;
            titleText.setFont(Fonts.robotoMedium(0.24 * height));
            if (titleText.getLayoutBounds().getWidth() > maxTextWidth) { adjustTextSize(titleText, maxTextWidth, 0.3); }
            titleText.relocate(0.17593985 * width - (titleText.getLayoutBounds().getWidth()), 0.075 * height);

            unitText.setFont(Fonts.robotoRegular(0.15 * height));
            if (unitText.getLayoutBounds().getWidth() > maxTextWidth) { adjustTextSize(unitText, maxTextWidth, 0.2); }
            unitText.relocate(0.17593985 * width - (unitText.getLayoutBounds().getWidth()), 0.4 * height);
        }
        redraw();
    }

    private void redraw() {
        drawTickMarks(tickMarksCtx);
        drawSections(sectionsCtx);
        thresholdRect.setFill(getSkinnable().getThresholdColor());
        thresholdTooltip.setText(String.format(Locale.US, formatString, getSkinnable().getThreshold()));
        barRect.setFill(getSkinnable().getBarColor());
        titleText.setFill(getSkinnable().getTitleColor());
        unitText.setFill(getSkinnable().getUnitColor());
        updateBar();
    }
}
