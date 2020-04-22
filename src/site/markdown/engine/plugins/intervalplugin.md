# Interval Plugin 

The Imixs *IntervalPlugin* provides an easy way for scheduling periodical intervals.  
The Plugin implements an mechanism to adjust the value of a date item based on a macro or a cron definition.
 The interval can be defined in the workflow result by setting a reference item by name and a cron definition. See the following example:

	<item name="interval">
	    <ref>reminder</ref>
	    <cron>0 15 * * 1-5</cron>
	</item>

This example will adjust the date item 'reminder' to 3:00pm the next working day (Mo-Fr). 

	<item name="interval">
	    <ref>reminder</ref>
	    <macro>@monthly</macro>
	</item>

this example will adjust the value of the date item 'reminder' per 1 month. For example: 15.01.2020 => 15.02.2020 


**Note:** The IntervalPlugin only runs on scheduled activities. 

## Cron Expression

The syntax of a cron expression is  made of five fields:

	 ┌───────────── minute (0 - 59)
	 │ ┌───────────── hour (0 - 23)
	 │ │ ┌───────────── day of the month (1 - 31)
	 │ │ │ ┌───────────── month (1 - 12)
	 │ │ │ │ ┌───────────── day of the week (0 - 6) (Sunday to Saturday);
	 │ │ │ │ │                                   
	 │ │ │ │ │
	 │ │ │ │ │
	 * * * * * 



See the following examples how you can schedule workitems on intervals:

| Cron        |Description                                    |       
|-------------|:---------------------------------------------|
| 0 0 1 1 *   | Schedule once a year at midnight of 1 January      |
| 0 0 1 * *   | Schedule once a month at midnight of the first day of the month   |	
| 0 0 * * 0   | Schedule once a week at midnight on Sunday morning |
| 0 0 * * *   | Schedule once a day at midnight                    |
| 0 * * * *   | Schedule once an hour at the beginning of the hour |
| 0 15 * * 7  | Schedule every Sunday on 3:00 pm |


###  Macro Definitions

The following macros can be used to schedule a workitem based on given date item value:

|Macro        |Description                                    |  
|-------------|:---------------------------------------------|
|@yearly      | Run every year on the same day and month      |
|@monthly     | Run every month on the same day               |
|@weekly      | Run every week on the same day of week        |
|@daily       | Run every day on the same hour and the same minute     |
|@hourly      | Run every hour on the same minute             |


