#Layout & UI

The imixs-ui.js provides some layout methods to apply the jQuery UI widgets to a page section. See the following example to layout a jQuery UI tab navigation

			<script type="text/javascript" src="./js/imixs-ui.js"></script>
		
			<script>
				$(document).ready(function() {
					$('#form1').imixsLayout();
				});
			</script>
			
## Tab Navigation

The jQuery Tab Navigation can be used to split up a form into several sections: 

			...
			<form id="form1">
			<div class="imixs-tabs">
				<ul>
					<li><a href="#tab-1">Tab1</a></li>
					<li><a href="#tab-2">Tab2</a></li>
				</ul>
				<div id="tab-1">
					<p>Some data...</p>
				</div>
				<div id="tab-2" class="imixs-form-section">
					<p>Another page...</p>
				</div>
			</div>
			...

## Date Time Picker

An input field assigend to the class 'imixs-date' will be automatically displayed using the jQuery UI DatePicker component


	<dl>
		<dt>Created:</dt>
		<dd>
			<input type="text" class="imixs-date" id="from" />
			-
			<input type="text" class="imixs-date" id="to" />
		</dd>
	</dl> 			

Imixs-Script provides also a DateTime component to additional select hours and minutes:
	
	
	<input type="text" class="imixs-datetime" id="from" />

See the section [Documents](./documents.html) so see how to work with date values.
	
	