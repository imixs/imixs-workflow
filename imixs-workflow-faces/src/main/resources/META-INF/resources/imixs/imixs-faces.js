
// default jQuery date format
var dateDisplayFormat="yy-mm-dd";

/*
 * This method converts a Java Date String into the jQuery format. 
 * See: http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
 * http://docs.jquery.com/UI/Datepicker/formatDate
 * 
 * As jquery and javaDate format differs from each other we need to translate the JavaFormat
 * in a jquery understandable format.
 * Also the locale need to be set!
 */
function setDateFormat(javaDate) {
	
	// convert yyyy to yy,
	// yy to y, 
	if (javaDate.indexOf('yyyy')>-1) 
		javaDate=javaDate.replace('yyyy','yy');
	 else 
		if (javaDate.indexOf('yy')>-1) 
		 	javaDate=javaDate.replace('yy','y');
	
	// MMM to M, 
	// MM to m, 
	// M to m
		if (javaDate.indexOf('MMMM')>-1) 
		javaDate=javaDate.replace('MMMM','MM');
	 else 
		if (javaDate.indexOf('MMM')>-1) 
		 	javaDate=javaDate.replace('MMM','M');
		 else 
			if (javaDate.indexOf('MM')>-1) 
			 	javaDate=javaDate.replace('MM','mm');
		 	else 
				if (javaDate.indexOf('M')>-1) 
			 		javaDate=javaDate.replace('M','m');	
	
	// set jquery date format
	dateDisplayFormat=javaDate;
}



/* This method styles input elements an imixs page elements */
$.fn.imixsLayout = function(options) {
	return this.each(function() {

		$('input:submit,input:reset,input:button,button', this).button();

		// disable default layout for input
		// $('input,select,textarea', this).addClass('ui-state-default');

		$('.imixsdatatable', this).layoutImixsTable();

		$('.imixs-toggle-panel', this).layoutImixsToggelPanel();

		$(".imixs-tabs", this).tabs();

		// regional : 'de'
		$(".imixs-date", this).datepicker({
			showOtherMonths : true,
			selectOtherMonths : true,
			dateFormat : dateDisplayFormat
		});

	});
};

$.fn.layoutImixsTable = function(options) {
	var defaults = {
		css : 'styleTable'
	};
	options = $.extend(defaults, options);

	return this.each(function() {

		input = $(this);
		input.addClass(options.css);

		input.find('tr').on('mouseover mouseout', function(event) {
			if (event.type == 'mouseover') {
				$(this).children('td').addClass('ui-state-hover');

			} else {
				$(this).children('td').removeClass('ui-state-hover');
			}
		});

		input.find('th').addClass('ui-state-default');
		input.find('td').addClass('ui-widget-content');

		input.find('td').css('font-weight', 'normal');

		input.find('tr').each(function() {
			$(this).children('td:not(:first)').addClass('first');
			$(this).children('th:not(:first)').addClass('first');
		});
	});
};

$.fn.layoutImixsToggelPanel = function(options) {

	return this.each(function() {
		var togglepanels = $('h1', this);
		togglepanels.css('cursor', 'pointer');

		var anker = $('<a class="imixs-toggle-panel-link" />');
		togglepanels.wrapInner(anker);

		togglepanels.removeClass().addClass(
				'ui-state-default ui-widget-header ui-corner-all');
		var toggleIcon = $('<span></span>').addClass(
				'ui-icon ui-icon-triangle-1-e').css('float', 'left');
		toggleIcon.insertBefore('.imixs-toggle-panel-link', this);

		togglepanels.click(
				function() {
					$(this).next().toggle();

					var t1 = $('.ui-icon-triangle-1-s', this);
					var t2 = $('.ui-icon-triangle-1-e', this);
					t1.removeClass('ui-icon-triangle-1-s').addClass(
							'ui-icon-triangle-1-e');
					t2.removeClass('ui-icon-triangle-1-e').addClass(
							'ui-icon-triangle-1-s');

					return false;
				}).next().hide();

	});
};




$.fn.layoutImixsTooltip = function(options) {

	return this.each(function() {
		/* Imixs Tooltip support */		
		// tested: flipfit none flipfit is not usefull for long content. better use flip
		$( this ).prev().tooltip({ 
			 position: {  my: "left top", at: "left+10 bottom", collision: "flip" },
			 show: { duration: 800 },
			 content:  function() { 
				var tooltip=$(this).next();
				tooltip.hide();
				return tooltip.text();  
			} 
			 

	 	});							
		// hide all imixs tooltips 
		$( this ).hide();
	});
};
