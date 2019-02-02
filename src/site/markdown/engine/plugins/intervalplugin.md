# Interval Plugin 

The Imixs Interval Plugin implements an mechanism to adjust a date field of a workitem based on a interval definition. The interval description is stored in a field with the prafix 'keyinterval' followed by the name of an  existing DateTime field. See the following example:
 
	 keyIntervalDatDate=monthly
	 datDate=01.01.2014 
 
Depending on the keyInterval value the next due date will be computed in case the current date lies in the past. The interval description can be a number of days or one of the following literals
 
  * daily - increment one day
  * weekly - increment 7 days
  * monthly - increment one month
  * yearly - increment one year

The Plugin only runs on scheduled activities. So using the interval plugin in a workflow model provides an easy way for scheduling periodical intervals.  The following interval values are currently supported:
 
|Value     | Description                                               |
|----------|-----------------------------------------------------------| 
| daily    | Adjust the date value for 1 day <br />Example: 15.01.2014 => 16.01.2014  |
| weekly   | Adjust the date value for 7 days <br />Example: 15.01.2014 => 22.01.2014  |
| monthly  | Adjust the date value for one month <br />Example: 15.01.2014 => 15.02.2014 |
| yearly   | Adjust the date value for one year <br />Example: 15.01.2014 => 15.01.2015  |
| [NUMBER] | Adjust the date value by a number of days<br />Example '5': 15.01.2014 => 20.01.2015   |
