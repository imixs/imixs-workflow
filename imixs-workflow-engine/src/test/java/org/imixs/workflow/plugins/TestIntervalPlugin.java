package org.imixs.workflow.plugins;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.WorkflowMockEnvironment;
import org.imixs.workflow.engine.plugins.IntervalPlugin;
import org.imixs.workflow.exceptions.AdapterException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for IntervalPugin
 * 
 * @author rsoika
 */
public class TestIntervalPlugin {
    protected IntervalPlugin intervalPlugin = null;

    private static final Logger logger = Logger.getLogger(TestIntervalPlugin.class.getName());
    WorkflowMockEnvironment workflowMockEnvironment;
    ItemCollection documentContext;
    ItemCollection documentActivity;

    @Before
    public void setup() throws PluginException, ModelException, AdapterException {

        workflowMockEnvironment = new WorkflowMockEnvironment();
        workflowMockEnvironment.setModelPath("/bpmn/TestIntervalPlugin.bpmn");

        workflowMockEnvironment.setup();

        intervalPlugin = new IntervalPlugin();
        try {
            intervalPlugin.init(workflowMockEnvironment.getWorkflowService());
        } catch (PluginException e) {

            e.printStackTrace();
        }

        // prepare test workitem
        documentContext = new ItemCollection();
        logger.info("[TestIntervalPlugin] setup test data...");

        documentContext.replaceItemValue("reminder", new Date());

        workflowMockEnvironment.getDocumentService().save(documentContext);

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
            documentActivity = workflowMockEnvironment.getModel().getEvent(100, 99);
            intervalPlugin.run(documentContext, documentActivity);
        } catch (PluginException | ModelException e) {
            e.printStackTrace();
            Assert.fail();
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
        Assert.assertEquals(date.getMonthValue(), exptectedMonth);
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
        Assert.assertEquals(date.getYear(), now.getYear() + 1);
        Assert.assertEquals(1, date.getMonthValue());
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
        Assert.assertEquals(DayOfWeek.MONDAY, date.getDayOfWeek());
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
        Assert.assertEquals(exptectedMonth, date.getMonthValue());
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
        Assert.assertEquals(now.getDayOfMonth(), date.getDayOfMonth());
        Assert.assertEquals(now.getMonthValue(), date.getMonthValue());
        Assert.assertEquals(now.getYear() + 1, date.getYear());
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
        Assert.assertTrue(now.getDayOfMonth() != date.getDayOfMonth());
        Assert.assertEquals(now.getDayOfWeek(), date.getDayOfWeek());
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
            LocalDateTime date=null;

            // 1.12.2022 
            now = now.with(ChronoField.DAY_OF_MONTH,1);
            now = now.with(ChronoField.MONTH_OF_YEAR,12);
            now = now.with(ChronoField.YEAR_OF_ERA,2022);
            
            // 8:00
            now = now.with(ChronoField.MINUTE_OF_HOUR,0);
            now = now.with(ChronoField.HOUR_OF_DAY,8);
            String cron = "0 9,18 * * *";
            date = intervalPlugin.evalCron(cron,now);
            logger.log(Level.INFO, " => now={0}  result={1}", new Object[]{now, date});
            // expected 9:00
            Assert.assertTrue(date.getHour()==9);
        
    
            // 10:00
            now = now.with(ChronoField.MINUTE_OF_HOUR,0);
            now = now.with(ChronoField.HOUR_OF_DAY,10);
            date = intervalPlugin.evalCron(cron,now);
            logger.log(Level.INFO, " => now={0}  result={1}", new Object[]{now, date});
            // expected 18:00
            Assert.assertTrue(date.getHour()==18);
            
            
            // 22:00
            now = now.with(ChronoField.MINUTE_OF_HOUR,0);
            now = now.with(ChronoField.HOUR_OF_DAY,22);
            date = intervalPlugin.evalCron(cron,now);
            logger.log(Level.INFO, " => now={0}  result={1}", new Object[]{now, date});
            // expected 9:00 next day
            Assert.assertTrue(date.getHour()==9);
            Assert.assertTrue(date.getDayOfYear()==now.getDayOfYear()+1);
    
            
            
            
            cron = "15,30 * * * *";
            now = now.with(ChronoField.HOUR_OF_DAY,10);
            date = intervalPlugin.evalCron(cron,now);
            logger.log(Level.INFO, " => now={0}  result={1}", new Object[]{now, date});
            // expected 10:15
            Assert.assertTrue(date.getHour()==10);
            Assert.assertTrue(date.getMinute()==15);
            
            
            now = now.with(ChronoField.HOUR_OF_DAY,10);
            now = now.with(ChronoField.MINUTE_OF_HOUR,25);
            date = intervalPlugin.evalCron(cron,now);
            logger.log(Level.INFO, " => now={0}  result={1}", new Object[]{now, date});
            // expected 10:30 
            Assert.assertTrue(date.getHour()==10);
            Assert.assertTrue(date.getMinute()==30);
            
            
            
        } catch (PluginException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

}
