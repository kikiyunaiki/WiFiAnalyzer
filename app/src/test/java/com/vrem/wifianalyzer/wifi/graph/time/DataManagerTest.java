/*
 * WiFi Analyzer
 * Copyright (C) 2017  VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.vrem.wifianalyzer.wifi.graph.time;

import android.support.annotation.NonNull;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.vrem.wifianalyzer.BuildConfig;
import com.vrem.wifianalyzer.RobolectricUtil;
import com.vrem.wifianalyzer.wifi.band.WiFiWidth;
import com.vrem.wifianalyzer.wifi.graph.tools.DataPointEquals;
import com.vrem.wifianalyzer.wifi.graph.tools.GraphViewWrapper;
import com.vrem.wifianalyzer.wifi.model.WiFiAdditional;
import com.vrem.wifianalyzer.wifi.model.WiFiDetail;
import com.vrem.wifianalyzer.wifi.model.WiFiSignal;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class DataManagerTest {
    private static final int LEVEL = -40;

    private GraphViewWrapper graphViewWrapper;
    private TimeGraphCache timeGraphCache;
    private DataManager fixture;

    @Before
    public void setUp() {
        RobolectricUtil.INSTANCE.getActivity();

        timeGraphCache = mock(TimeGraphCache.class);
        graphViewWrapper = mock(GraphViewWrapper.class);

        fixture = new DataManager();
        fixture.setTimeGraphCache(timeGraphCache);
    }

    @Test
    public void testAddSeriesDataIncreaseXValue() throws Exception {
        // setup
        List<WiFiDetail> wiFiDetails = new ArrayList<>();
        assertEquals(0, fixture.getXValue());
        // execute
        fixture.addSeriesData(graphViewWrapper, wiFiDetails);
        // validate
        assertEquals(1, fixture.getXValue());
    }

    @Test
    public void testAddSeriesDataIncreaseCounts() throws Exception {
        // setup
        List<WiFiDetail> wiFiDetails = new ArrayList<>();
        assertEquals(0, fixture.getScanCount());
        // execute
        fixture.addSeriesData(graphViewWrapper, wiFiDetails);
        // validate
        assertEquals(1, fixture.getScanCount());
    }

    @Test
    public void testAddSeriesDoesNotIncreasesScanCountWhenLimitIsReached() throws Exception {
        // setup
        List<WiFiDetail> wiFiDetails = new ArrayList<>();
        fixture.setScanCount(DataManager.MAX_SCAN_COUNT);
        // execute
        fixture.addSeriesData(graphViewWrapper, wiFiDetails);
        // validate
        assertEquals(DataManager.MAX_SCAN_COUNT, fixture.getScanCount());
    }

    @Test
    public void testAddSeriesSetHorizontalLabelsVisible() throws Exception {
        // setup
        List<WiFiDetail> wiFiDetails = new ArrayList<>();
        fixture.setScanCount(1);
        // execute
        fixture.addSeriesData(graphViewWrapper, wiFiDetails);
        // validate
        assertEquals(2, fixture.getScanCount());
        verify(graphViewWrapper).setHorizontalLabelsVisible(true);
    }

    @Test
    public void testAddSeriesDoesNotSetHorizontalLabelsVisible() throws Exception {
        // setup
        List<WiFiDetail> wiFiDetails = new ArrayList<>();
        // execute
        fixture.addSeriesData(graphViewWrapper, wiFiDetails);
        // validate
        verify(graphViewWrapper, never()).setHorizontalLabelsVisible(true);
    }

    @Test
    public void testAdjustDataAppendsData() throws Exception {
        // setup
        Set<WiFiDetail> wiFiDetails = new TreeSet<>();
        List<WiFiDetail> difference = makeWiFiDetails();
        int xValue = fixture.getXValue();
        Integer scanCount = fixture.getScanCount();
        DataPoint dataPoint = new DataPoint(xValue, DataManager.MIN_Y + DataManager.MIN_Y_OFFSET);
        when(graphViewWrapper.differenceSeries(wiFiDetails)).thenReturn(difference);
        // execute
        fixture.adjustData(graphViewWrapper, wiFiDetails);
        // validate
        for (WiFiDetail wiFiDetail : difference) {
            verify(graphViewWrapper).appendToSeries(
                argThat(equalTo(wiFiDetail)), argThat(new DataPointEquals(dataPoint)),
                argThat(equalTo(scanCount)), argThat(equalTo(false)));
            verify(timeGraphCache).add(wiFiDetail);
        }
        verify(timeGraphCache).clear();
    }

    @Test
    public void testGetNewSeries() throws Exception {
        // setup
        Set<WiFiDetail> wiFiDetails = new TreeSet<>(makeWiFiDetails());
        Set<WiFiDetail> moreWiFiDetails = new TreeSet<>(makeMoreWiFiDetails());
        when(timeGraphCache.active()).thenReturn(moreWiFiDetails);
        // execute
        Set<WiFiDetail> actual = fixture.getNewSeries(wiFiDetails);
        // validate
        assertTrue(actual.containsAll(wiFiDetails));
        assertTrue(actual.containsAll(moreWiFiDetails));
        verify(timeGraphCache).active();
    }

    @Test
    public void testAddDataToExistingSeries() throws Exception {
        // setup
        Integer scanCount = fixture.getScanCount();
        int xValue = fixture.getXValue();
        WiFiDetail wiFiDetail = makeWiFiDetail("SSID");
        DataPoint dataPoint = new DataPoint(xValue, wiFiDetail.getWiFiSignal().getLevel());
        when(graphViewWrapper.isNewSeries(wiFiDetail)).thenReturn(false);
        // execute
        fixture.addData(graphViewWrapper, wiFiDetail);
        // validate
        verify(graphViewWrapper).isNewSeries(wiFiDetail);
        verify(graphViewWrapper).appendToSeries(
            argThat(equalTo(wiFiDetail)), argThat(new DataPointEquals(dataPoint)),
            argThat(equalTo(scanCount)), argThat(equalTo(false)));
        verify(timeGraphCache).reset(wiFiDetail);
    }

    @Test
    public void testAddDataNewSeries() throws Exception {
        // setup
        WiFiDetail wiFiDetail = makeWiFiDetail("SSID");
        when(graphViewWrapper.isNewSeries(wiFiDetail)).thenReturn(true);
        // execute
        fixture.addData(graphViewWrapper, wiFiDetail);
        // validate
        verify(graphViewWrapper).isNewSeries(wiFiDetail);
        verify(timeGraphCache).reset(wiFiDetail);
        //noinspection unchecked
        verify(graphViewWrapper).addSeries(argThat(equalTo(wiFiDetail)), argThat(any(LineGraphSeries.class)), argThat(equalTo(Boolean.FALSE)));
    }

    private WiFiDetail makeWiFiDetail(@NonNull String SSID) {
        WiFiSignal wiFiSignal = new WiFiSignal(2455, 2455, WiFiWidth.MHZ_20, LEVEL);
        return new WiFiDetail(SSID, "BSSID", StringUtils.EMPTY, wiFiSignal, WiFiAdditional.EMPTY);
    }

    private List<WiFiDetail> makeWiFiDetails() {
        return Arrays.asList(makeWiFiDetail("SSID1"), makeWiFiDetail("SSID2"), makeWiFiDetail("SSID3"));
    }

    private List<WiFiDetail> makeMoreWiFiDetails() {
        return Arrays.asList(makeWiFiDetail("SSID4"), makeWiFiDetail("SSID5"));
    }

}