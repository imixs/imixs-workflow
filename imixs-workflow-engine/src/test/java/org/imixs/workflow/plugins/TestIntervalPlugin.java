/****************************************************************************
 * Copyright (c) 2022-2025 Imixs Software Solutions GmbH and others.
 * https://www.imixs.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * This Source Code may also be made available under the terms of the
 * GNU General Public License, version 2 or later (GPL-2.0-or-later),
 * which is available at https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 ****************************************************************************/

package org.imixs.workflow.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.MockWorkflowEnvironment;
import org.imixs.workflow.engine.plugins.IntervalPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbpmn.bpmn.BPMNModel;

/**
 * Test class for IntervalPlugin
 * 
 * @author rsoika
 */
public class TestIntervalPlugin {
    protected IntervalPlugin intervalPlugin = null;

    private static final Logger logger = Logger.getLogger(TestIntervalPlugin.class.getName());
    protected MockWorkflowEnvironment workflowEnvironment;
    ItemCollection workitem = null;
    ItemCollection documentContext;
    ItemCollection event;

    @BeforeEach
    public void setUp() throws PluginException, ModelException {
        workflowEnvironment = new MockWorkflowEnvironment();
        workflowEnvironment.setUp();
        workflowEnvironment.loadBPMNModelFromFile("/bpmn/TestIntervalPlugin.bpmn");
        workitem = workflowEnvironment.getDocumentService().load("W0000-00001");
        workitem.model("1.0.0").task(100);

        // prepare plugin
        intervalPlugin = new IntervalPlugin();
        try {
            intervalPlugin.init(workflowEnvironment.getWorkflowService());
        } catch (PluginException e) {
            fail(e.getMessage());
        }

        // prepare test workitem
        documentContext = new ItemCollection();
        logger.info("[TestIntervalPlugin] setup test data...");
        documentContext.replaceItemValue("reminder", new Date());
        workflowEnvironment.getDocumentService().save(documentContext);
    }

    /**
     * This test verifies the model configuration
     * 
     * @throws PluginException
     */
    @Test
    public void testBPMNModel() throws PluginException {
        logger.log(Level.INFO, "------------------ Ref Date   ={0}", documentContext.getItemValueDate("reminder"));
        try {
            workitem.event(99);

            BPMNModel model = workflowEnvironment.getModelManager().getModelByWorkitem(workitem);
            event = workflowEnvironment.getModelManager().loadEvent(workitem, model);
            // // 99);
            intervalPlugin.run(documentContext, event);
        } catch (PluginException | ModelException e) {
            e.printStackTrace();
            fail();
        }
        Date result = documentContext.getItemValueDate("reminder");
        logger.log(Level.INFO, "------------------  Result Date={0}", result);
    }

    /**
     * This test verifies the 1st day of month interval
     * 
     * @throws PluginException
     */
    @Test
    public void test1stDayInMonth() throws PluginException {
        // @monthly
        String cron = "0 0 1 * *";
        LocalDateTime date = intervalPlugin.evalCron(cron);
        logger.log(Level.INFO, "Result monthyl={0}", date);
        LocalDateTime now = LocalDateTime.now();
        int exptectedMonth = now.getMonthValue() + 1;
        if (exptectedMonth == 13) {
            // year switch
            exptectedMonth = 1;
        }
        assertEquals(date.getMonthValue(), exptectedMonth);
    }

    /**
     * This test verifies the 1st day of year interval
     * 
     * @throws PluginException
     */
    @Test
    public void test1stDayInYear() throws PluginException {
        // @monthly
        String cron = "0 0 1 1 *";
        LocalDateTime date = intervalPlugin.evalCron(cron);
        logger.log(Level.INFO, "Result monthly={0}", date);
        LocalDateTime now = LocalDateTime.now();
        assertEquals(date.getYear(), now.getYear() + 1);
        assertEquals(1, date.getMonthValue());
    }

    /**
     * This test verifies the 1 day of wee interval
     * 
     * @throws PluginException
     */
    @Test
    public void test1stDayOfWeek() throws PluginException {
        // @weekly
        String cron = "0 0 * * 1 ";
        LocalDateTime date = intervalPlugin.evalCron(cron);
        logger.log(Level.INFO, "Result monthyl={0}", date);
        // move to past one day...
        assertEquals(DayOfWeek.MONDAY, date.getDayOfWeek());
    }

    /**
     * This test verifies the monthly interval.
     * 
     * @throws PluginException
     */
    @Test
    public void testMonthly() throws PluginException {
        // @monthly
        // 1.1.2020 - > 1.2.2020
        LocalDateTime ldt = LocalDateTime.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime date = intervalPlugin.evalMacro("@monthly", ldt);
        logger.log(Level.INFO, "Now            ={0}", now);
        logger.log(Level.INFO, "Result @monthly={0}", date);
        int exptectedMonth = now.getMonthValue() + 1;
        if (exptectedMonth == 13) {
            // year switch
            exptectedMonth = 1;
        }
        assertEquals(exptectedMonth, date.getMonthValue());
    }

    /**
     * This test verifies the yearly interval.
     * 
     * @throws PluginException
     */
    @Test
    public void testYearly() throws PluginException {
        // @monthly
        // 1.1.2020 - > 1.1.2021
        LocalDateTime ldt = LocalDateTime.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime date = intervalPlugin.evalMacro("@yearly", ldt);
        logger.log(Level.INFO, "Now           ={0}", now);
        logger.log(Level.INFO, "Result @yearly={0}", date);
        assertEquals(now.getDayOfMonth(), date.getDayOfMonth());
        assertEquals(now.getMonthValue(), date.getMonthValue());
        assertEquals(now.getYear() + 1, date.getYear());
    }

    /**
     * This test verifies the weekly interval.
     * 
     * @throws PluginException
     */
    @Test
    public void testWeekly() throws PluginException {
        // @Weekly
        // 1.1.2020 - > 7.1.2021
        LocalDateTime ldt = LocalDateTime.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime date = intervalPlugin.evalMacro("@weekly", ldt);
        logger.log(Level.INFO, "Now           ={0}", now);
        logger.log(Level.INFO, "Result @weekly={0}", date);
        assertTrue(now.getDayOfMonth() != date.getDayOfMonth());
        assertEquals(now.getDayOfWeek(), date.getDayOfWeek());
    }

    /**
     * This test verifies different cron patterns with value lists
     * 
     * @throws PluginException
     */
    @Test
    public void testCronUnitValueList() {
        // m h d m w
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime date = null;

            // 1.12.2022
            now = now.with(ChronoField.DAY_OF_MONTH, 1);
            now = now.with(ChronoField.MONTH_OF_YEAR, 12);
            now = now.with(ChronoField.YEAR_OF_ERA, 2022);

            // 8:00
            now = now.with(ChronoField.MINUTE_OF_HOUR, 0);
            now = now.with(ChronoField.HOUR_OF_DAY, 8);
            String cron = "0 9,18 * * *";
            date = intervalPlugin.evalCron(cron, now);
            logger.log(Level.INFO, " => now={0}  result={1}", new Object[] { now, date });
            // expected 9:00
            assertTrue(date.getHour() == 9);

            // 10:00
            now = now.with(ChronoField.MINUTE_OF_HOUR, 0);
            now = now.with(ChronoField.HOUR_OF_DAY, 10);
            date = intervalPlugin.evalCron(cron, now);
            logger.log(Level.INFO, " => now={0}  result={1}", new Object[] { now, date });
            // expected 18:00
            assertTrue(date.getHour() == 18);

            // 22:00
            now = now.with(ChronoField.MINUTE_OF_HOUR, 0);
            now = now.with(ChronoField.HOUR_OF_DAY, 22);
            date = intervalPlugin.evalCron(cron, now);
            logger.log(Level.INFO, " => now={0}  result={1}", new Object[] { now, date });
            // expected 9:00 next day
            assertTrue(date.getHour() == 9);
            assertTrue(date.getDayOfYear() == now.getDayOfYear() + 1);

            cron = "15,30 * * * *";
            now = now.with(ChronoField.HOUR_OF_DAY, 10);
            date = intervalPlugin.evalCron(cron, now);
            logger.log(Level.INFO, " => now={0}  result={1}", new Object[] { now, date });
            // expected 10:15
            assertTrue(date.getHour() == 10);
            assertTrue(date.getMinute() == 15);

            now = now.with(ChronoField.HOUR_OF_DAY, 10);
            now = now.with(ChronoField.MINUTE_OF_HOUR, 25);
            date = intervalPlugin.evalCron(cron, now);
            logger.log(Level.INFO, " => now={0}  result={1}", new Object[] { now, date });
            // expected 10:30
            assertTrue(date.getHour() == 10);
            assertTrue(date.getMinute() == 30);

        } catch (PluginException e) {
            e.printStackTrace();
            fail();
        }
    }

}
