# Interval Plugin 

The Imixs Interval Plugin implements an mechanism to adjust a date field of a workitem based on a cron definition. The interval can be defined in the workflow result by setting a reference item by name and a cron definition. See the following example:


	<item name="interval">
	    <ref>reminder</ref>
	    <cron>0 15 * * 1-5</cron>
	</item>

This example will adjust the date item 'reminder' to 3:00pm the next working day (Mo-Fr). 



The Plugin only runs on scheduled activities. So using the interval plugin in a workflow model provides an easy way for scheduling periodical intervals.  

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


is stored in a field with the prafix 'keyinterval' followed by the name of an  existing DateTime field. See the following example:

###  Nonstandard predefined scheduling definitions

Also the following non-standard macros are supported by Imixs-Workflow:

|Entry        |Description                                    |Equivalent   |       
|-------------|:---------------------------------------------:|:-----------:|
|@yearly      | Run once a year at midnight of 1 January      | 0 0 1 1 *   |
|@monthly     | Run once a month at midnight of the first day of the month   |	0 0 1 * *   |
|@weekly      | Run once a week at midnight on Sunday morning | 0 0 * * 0  |
|@daily       | Run once a day at midnight                    | 0 0 * * *  |
|@hourly      | Run once an hour at the beginning of the hour | 0 * * * *  |

